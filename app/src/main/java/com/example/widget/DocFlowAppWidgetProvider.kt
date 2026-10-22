package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.CachedDocumentEntity
import com.example.data.local.DocFlowDatabase
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DocFlowAppWidgetProvider : AppWidgetProvider() {

    private val providerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        providerScope.launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DocFlowAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val pendingResult = goAsync()
                providerScope.launch {
                    try {
                        for (appWidgetId in appWidgetIds) {
                            updateAppWidget(context, appWidgetManager, appWidgetId)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private suspend fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_docflow_quick_access)

        // 1. One-Tap Quick Scan Pending Intent
        val scanIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_SCANNER
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val scanPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_SCAN,
            scanIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_quick_scan, scanPendingIntent)

        // 2. Header Brand Tap Pending Intent (Opens Workspace)
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_MAIN,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_header, mainPendingIntent)

        // 3. Query Room for the top 3 recently accessed documents
        val recentDocs = try {
            val db = DocFlowDatabase.getInstance(context)
            db.docFlowDao().getRecentDocuments(3)
        } catch (_: Exception) {
            emptyList()
        }

        if (recentDocs.isEmpty()) {
            views.setViewVisibility(R.id.widget_docs_container, View.GONE)
            views.setViewVisibility(R.id.widget_empty_container, View.VISIBLE)
            views.setOnClickPendingIntent(R.id.widget_empty_container, scanPendingIntent)
        } else {
            views.setViewVisibility(R.id.widget_docs_container, View.VISIBLE)
            views.setViewVisibility(R.id.widget_empty_container, View.GONE)

            // Populate Item 1
            bindDocumentRow(
                context = context,
                views = views,
                index = 0,
                itemContainerId = R.id.widget_doc_item_1,
                titleId = R.id.widget_doc_1_title,
                detailsId = R.id.widget_doc_1_details,
                badgeId = R.id.widget_doc_1_badge,
                doc = recentDocs.getOrNull(0)
            )

            // Populate Item 2
            bindDocumentRow(
                context = context,
                views = views,
                index = 1,
                itemContainerId = R.id.widget_doc_item_2,
                titleId = R.id.widget_doc_2_title,
                detailsId = R.id.widget_doc_2_details,
                badgeId = R.id.widget_doc_2_badge,
                doc = recentDocs.getOrNull(1)
            )

            // Populate Item 3
            bindDocumentRow(
                context = context,
                views = views,
                index = 2,
                itemContainerId = R.id.widget_doc_item_3,
                titleId = R.id.widget_doc_3_title,
                detailsId = R.id.widget_doc_3_details,
                badgeId = R.id.widget_doc_3_badge,
                doc = recentDocs.getOrNull(2)
            )
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun bindDocumentRow(
        context: Context,
        views: RemoteViews,
        index: Int,
        itemContainerId: Int,
        titleId: Int,
        detailsId: Int,
        badgeId: Int,
        doc: CachedDocumentEntity?
    ) {
        if (doc == null) {
            views.setViewVisibility(itemContainerId, View.GONE)
            return
        }

        views.setViewVisibility(itemContainerId, View.VISIBLE)
        views.setTextViewText(titleId, doc.title)

        val timeStr = formatRelativeTime(
            if (doc.lastAccessedAtMillis > 0) doc.lastAccessedAtMillis else doc.updatedAtMillis
        )
        val categoryLabel = doc.category.ifBlank { "General" }
        views.setTextViewText(detailsId, "$categoryLabel • $timeStr")
        views.setTextViewText(badgeId, doc.fileType.uppercase(Locale.ROOT))

        // Create One-Tap PendingIntent to open this specific document
        val docIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_DOCUMENT
            putExtra(EXTRA_DOCUMENT_ID, doc.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val docPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_DOC_BASE + index,
            docIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(itemContainerId, docPendingIntent)
    }

    companion object {
        const val ACTION_OPEN_DOCUMENT = "com.example.docflow.ACTION_OPEN_DOCUMENT"
        const val ACTION_OPEN_SCANNER = "com.example.docflow.ACTION_OPEN_SCANNER"
        const val ACTION_UPDATE_WIDGET = "com.example.docflow.ACTION_UPDATE_WIDGET"
        const val EXTRA_DOCUMENT_ID = "extra_document_id"

        private const val REQUEST_CODE_MAIN = 1000
        private const val REQUEST_CODE_SCAN = 1001
        private const val REQUEST_CODE_DOC_BASE = 2000

        fun notifyDocumentOpened(context: Context) {
            val intent = Intent(context, DocFlowAppWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            context.sendBroadcast(intent)
        }

        fun formatRelativeTime(timestampMillis: Long): String {
            val diff = System.currentTimeMillis() - timestampMillis
            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
                diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
                diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
                else -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
            }
        }
    }
}
