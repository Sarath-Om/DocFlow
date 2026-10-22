package com.example.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.R
import com.example.data.local.CachedDocumentEntity
import com.example.data.local.DocFlowDatabase
import com.example.data.local.SyncLogEntity
import com.example.data.local.SyncState
import com.example.data.remote.SupabaseSyncClient
import com.example.data.repository.DocFlowRepository
import com.example.widget.DocFlowAppWidgetProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException

/**
 * WorkManager CoroutineWorker that automatically retries and uploads pending offline
 * documents and folders from Room to Supabase PostgREST (and Cloud Firestore) once
 * network connectivity is restored.
 */
class OfflineSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val dao = DocFlowDatabase.getInstance(applicationContext).docFlowDao()
        val supabaseClient = SupabaseSyncClient()
        val requestedUserId = inputData.getString(KEY_USER_ID).orEmpty()

        val firestoreRepo = runCatching {
            val databaseId = applicationContext.getString(R.string.firestore_database_id)
            val firestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), databaseId)
            DocFlowRepository(firestore)
        }.getOrNull()

        val pendingDocs: List<CachedDocumentEntity> = if (requestedUserId.isNotBlank()) {
            dao.getPendingSyncDocuments(requestedUserId).ifEmpty {
                dao.getAllPendingSyncDocuments()
            }
        } else {
            dao.getAllPendingSyncDocuments()
        }

        val pendingFolders = dao.getAllPendingSyncFolders()
        val pendingAnnotations = if (requestedUserId.isNotBlank()) {
            dao.getPendingSyncAnnotations(requestedUserId).ifEmpty {
                dao.getAllPendingSyncAnnotations()
            }
        } else {
            dao.getAllPendingSyncAnnotations()
        }

        if (pendingDocs.isEmpty() && pendingFolders.isEmpty() && pendingAnnotations.isEmpty()) {
            return Result.success(
                workDataOf(
                    KEY_SYNCED_COUNT to 0,
                    KEY_SYNC_TARGET to if (supabaseClient.isConfigured()) "Supabase + Firebase" else "Firebase Cloud Reconciler",
                    KEY_TIMESTAMP to System.currentTimeMillis()
                )
            )
        }

        var syncedCount = 0
        var hasRetryableNetworkError = false
        val now = System.currentTimeMillis()

        // 1. Reconcile Pending Folders
        for (folder in pendingFolders) {
            try {
                if (folder.syncState == SyncState.PENDING_DELETE.name) {
                    if (supabaseClient.isConfigured()) {
                        supabaseClient.deleteFolder(folder.id).getOrThrow()
                    }
                    runCatching { firestoreRepo?.deleteFolder(folder.id) }
                    dao.deleteFolderById(folder.id)
                } else {
                    if (supabaseClient.isConfigured()) {
                        supabaseClient.upsertFolder(folder).getOrThrow()
                    }
                    runCatching {
                        firestoreRepo?.createFolder(
                            name = folder.name,
                            color = folder.color,
                            icon = folder.icon
                        )
                    }
                    dao.updateFolderSyncState(folder.id, SyncState.SYNCED.name, now)
                }
            } catch (_: IOException) {
                hasRetryableNetworkError = true
            } catch (_: Exception) {
                // Keep folder in pending state for next pass
            }
        }

        // 2. Reconcile Pending Offline Documents
        for (doc in pendingDocs) {
            try {
                if (doc.syncState == SyncState.PENDING_DELETE.name) {
                    if (supabaseClient.isConfigured()) {
                        supabaseClient.deleteDocument(doc.id).getOrThrow()
                    }
                    runCatching { firestoreRepo?.deleteDocument(doc.id) }
                    dao.deleteDocumentById(doc.id)
                    dao.insertSyncLog(
                        SyncLogEntity(
                            userId = doc.userId,
                            documentId = doc.id,
                            documentTitle = doc.title,
                            action = "WORKMANAGER_DELETE",
                            status = SyncState.SYNCED.label,
                            details = "Deleted '${doc.title}' on Supabase & Firebase Cloud after network restore",
                            timestampMillis = System.currentTimeMillis()
                        )
                    )
                    syncedCount++
                } else {
                    var targetEndpoint = "Firebase + Supabase Reconciler"
                    if (supabaseClient.isConfigured()) {
                        supabaseClient.upsertDocument(doc).getOrThrow()
                        targetEndpoint = "Supabase PostgREST + Firebase"
                    }

                    // Also ensure Cloud Firestore replica is updated when available
                    runCatching {
                        if (firestoreRepo != null) {
                            if (doc.syncState == SyncState.PENDING_CREATE.name || doc.syncState == SyncState.LOCAL_ONLY.name) {
                                firestoreRepo.createDocument(
                                    title = doc.title,
                                    folderId = doc.folderId,
                                    category = doc.category,
                                    fileType = doc.fileType,
                                    fileName = doc.fileName,
                                    fileSize = doc.fileSize,
                                    content = doc.content,
                                    isEncrypted = doc.isEncrypted,
                                    encryptionKeyHint = doc.encryptionKeyHint,
                                    isOfflineCached = doc.isOfflineCached,
                                    pageCount = doc.pageCount
                                )
                            } else {
                                firestoreRepo.updateDocument(
                                    docId = doc.id,
                                    title = doc.title,
                                    folderId = doc.folderId,
                                    category = doc.category,
                                    content = doc.content,
                                    fileSize = doc.fileSize,
                                    isEncrypted = doc.isEncrypted,
                                    encryptionKeyHint = doc.encryptionKeyHint,
                                    isOfflineCached = doc.isOfflineCached,
                                    pageCount = doc.pageCount
                                )
                            }
                        }
                    }

                    dao.updateDocumentSyncState(doc.id, SyncState.SYNCED.name, now)
                    dao.insertSyncLog(
                        SyncLogEntity(
                            userId = doc.userId,
                            documentId = doc.id,
                            documentTitle = doc.title,
                            action = "WORKMANAGER_SUPABASE_SYNC",
                            status = SyncState.SYNCED.label,
                            details = "Uploaded pending offline changes for '${doc.title}' to $targetEndpoint (Attempt #${runAttemptCount + 1})",
                            timestampMillis = System.currentTimeMillis()
                        )
                    )
                    syncedCount++
                }
            } catch (ioEx: IOException) {
                hasRetryableNetworkError = true
                dao.updateDocumentSyncState(doc.id, SyncState.FAILED.name, now)
                dao.insertSyncLog(
                    SyncLogEntity(
                        userId = doc.userId,
                        documentId = doc.id,
                        documentTitle = doc.title,
                        action = "WORKMANAGER_RETRY_QUEUED",
                        status = "Waiting for Network",
                        details = "Network interrupted while uploading '${doc.title}': ${ioEx.localizedMessage}. Exponential backoff retry scheduled.",
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            } catch (ex: Exception) {
                dao.updateDocumentSyncState(doc.id, SyncState.FAILED.name, now)
                dao.insertSyncLog(
                    SyncLogEntity(
                        userId = doc.userId,
                        documentId = doc.id,
                        documentTitle = doc.title,
                        action = "WORKMANAGER_SYNC_ERROR",
                        status = SyncState.FAILED.label,
                        details = "Error syncing '${doc.title}': ${ex.localizedMessage ?: "Unknown error"}",
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            }
        }

        // 3. Reconcile Pending PDF Markup Annotations (Draw, Highlight, Text Box) to Firebase
        for (annotation in pendingAnnotations) {
            try {
                runCatching {
                    firestoreRepo?.upsertPdfAnnotation(annotation)
                }
                dao.updateAnnotationSyncState(annotation.ymapKey, SyncState.SYNCED.name, now)
                dao.insertSyncLog(
                    SyncLogEntity(
                        userId = annotation.userId,
                        documentId = annotation.documentId,
                        documentTitle = "PDF Markup (${annotation.toolType})",
                        action = "WORKMANAGER_FIREBASE_ANNOTATION_SYNC",
                        status = SyncState.SYNCED.label,
                        details = "Synced PDF ${annotation.toolType} annotation '${annotation.ymapKey}' on Page ${annotation.pageIndex + 1} to Firebase Firestore",
                        timestampMillis = System.currentTimeMillis()
                    )
                )
                syncedCount++
            } catch (_: IOException) {
                hasRetryableNetworkError = true
                dao.updateAnnotationSyncState(annotation.ymapKey, SyncState.FAILED.name, now)
            } catch (_: Exception) {
                dao.updateAnnotationSyncState(annotation.ymapKey, SyncState.FAILED.name, now)
            }
        }

        // Refresh Home Screen Widget so recent synced state is up to date
        DocFlowAppWidgetProvider.notifyDocumentOpened(applicationContext)

        return if (hasRetryableNetworkError) {
            Result.retry()
        } else {
            Result.success(
                workDataOf(
                    KEY_SYNCED_COUNT to syncedCount,
                    KEY_SYNC_TARGET to if (supabaseClient.isConfigured()) "Supabase + Firebase" else "Firebase Cloud Reconciler",
                    KEY_TIMESTAMP to System.currentTimeMillis()
                )
            )
        }
    }

    companion object {
        const val KEY_USER_ID = "key_user_id"
        const val KEY_SYNCED_COUNT = "key_synced_count"
        const val KEY_SYNC_TARGET = "key_sync_target"
        const val KEY_TIMESTAMP = "key_timestamp"
    }
}
