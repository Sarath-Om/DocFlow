package com.example.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class BackgroundSyncWorkerState(
    val isScheduledOrRunning: Boolean = false,
    val statusLabel: String = "Idle • Network Ready",
    val lastSyncedCount: Int = 0,
    val targetBackend: String = "Supabase + Cloud",
    val runAttemptCount: Int = 0
)

/**
 * Coordinates WorkManager background synchronization for offline documents & folders.
 * Automatically queues a network-constrained OneTimeWorkRequest and PeriodicWorkRequest
 * with exponential backoff so pending changes upload to Supabase as soon as network is restored.
 */
object DocFlowSyncManager {

    const val UNIQUE_ONE_TIME_SYNC_WORK = "docflow_supabase_one_time_sync"
    const val UNIQUE_PERIODIC_SYNC_WORK = "docflow_supabase_periodic_sync"
    const val TAG_SUPABASE_SYNC = "tag_docflow_supabase_sync"

    fun buildConnectedSyncConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    fun buildOneTimeSyncRequest(userId: String = ""): OneTimeWorkRequest {
        val inputData = workDataOf(OfflineSyncWorker.KEY_USER_ID to userId)
        return OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(buildConnectedSyncConstraints())
            .setInputData(inputData)
            .addTag(TAG_SUPABASE_SYNC)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
    }

    /**
     * Enqueues an immediate network-constrained sync request.
     * If the device is currently offline, WorkManager holds the job in ENQUEUED state
     * and executes it automatically the moment network connectivity is restored.
     */
    fun enqueueOfflineSyncWhenConnected(
        context: Context,
        userId: String = "",
        replaceExisting: Boolean = true
    ) {
        runCatching {
            val workManager = WorkManager.getInstance(context.applicationContext)
            val request = buildOneTimeSyncRequest(userId)
            val policy = if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
            workManager.enqueueUniqueWork(
                UNIQUE_ONE_TIME_SYNC_WORK,
                policy,
                request
            )
        }
    }

    /**
     * Schedules a periodic 15-minute background reconciliation job constrained to connected networks.
     */
    fun schedulePeriodicBackgroundSync(context: Context, userId: String = "") {
        runCatching {
            val workManager = WorkManager.getInstance(context.applicationContext)
            val inputData = workDataOf(OfflineSyncWorker.KEY_USER_ID to userId)
            val periodicRequest = PeriodicWorkRequestBuilder<OfflineSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(buildConnectedSyncConstraints())
                .setInputData(inputData)
                .addTag(TAG_SUPABASE_SYNC)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_SYNC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }

    /**
     * Observes live WorkManager state for the Supabase offline sync worker.
     */
    fun observeSyncWorkerState(context: Context): Flow<BackgroundSyncWorkerState> {
        val workManager = WorkManager.getInstance(context.applicationContext)
        return workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_ONE_TIME_SYNC_WORK)
            .map { workInfos ->
                val info = workInfos.firstOrNull()
                if (info == null) {
                    BackgroundSyncWorkerState()
                } else {
                    val syncedCount = info.outputData.getInt(OfflineSyncWorker.KEY_SYNCED_COUNT, 0)
                    val target = info.outputData.getString(OfflineSyncWorker.KEY_SYNC_TARGET) ?: "Supabase + Cloud"
                    val label = when (info.state) {
                        WorkInfo.State.ENQUEUED -> "Queued • Waiting for Network Restore"
                        WorkInfo.State.RUNNING -> "Uploading Offline Changes to Supabase..."
                        WorkInfo.State.SUCCEEDED -> "Synced ($target)"
                        WorkInfo.State.FAILED -> "Sync Failed • Tap to Retry"
                        WorkInfo.State.BLOCKED -> "Waiting for Network Prerequisites"
                        WorkInfo.State.CANCELLED -> "Sync Cancelled"
                    }
                    BackgroundSyncWorkerState(
                        isScheduledOrRunning = info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED,
                        statusLabel = label,
                        lastSyncedCount = syncedCount,
                        targetBackend = target,
                        runAttemptCount = info.runAttemptCount
                    )
                }
            }
    }
}
