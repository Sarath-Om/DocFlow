package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CachedDocumentEntity
import com.example.data.local.DocFlowDao
import com.example.data.local.DocFlowDatabase
import com.example.data.local.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var database: DocFlowDatabase
  private lateinit var dao: DocFlowDao

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, DocFlowDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    dao = database.docFlowDao()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DocFlow", appName)
  }

  @Test
  fun `cached document entity parses tags and preview correctly`() {
    val preview = CachedDocumentEntity.buildPreview("Confidential business plan with financial models", false)
    assertTrue(preview.contains("Confidential business plan"))

    val tags = CachedDocumentEntity.defaultTagsFor("Q4 Report", "Finance", "pdf", "real-data, verified")
    assertTrue(tags.contains("pdf"))
    assertTrue(tags.contains("real-data"))
  }

  @Test
  fun `sync states provide human readable labels`() {
    assertEquals("Cloud Synced", SyncState.SYNCED.label)
    assertEquals("Local Only", SyncState.LOCAL_ONLY.label)
  }

  @Test
  fun `room database categorizes documents into Work Personal and Finance and updates tags`() = runBlocking {
    val userId = "test-user-123"

    val workDoc = CachedDocumentEntity(
        id = "doc-work-1",
        userId = userId,
        title = "Client Presentation",
        folderId = "root",
        category = "Work",
        fileType = "pdf",
        fileName = "presentation.pdf",
        fileSize = 1024L,
        content = "Executive quarterly slides",
        tags = "work, client, q3"
    )

    val personalDoc = CachedDocumentEntity(
        id = "doc-pers-1",
        userId = userId,
        title = "Passport Scan",
        folderId = "root",
        category = "Personal",
        fileType = "png",
        fileName = "passport.png",
        fileSize = 2048L,
        content = "Valid passport copy",
        tags = "personal, id, travel"
    )

    val financeDoc = CachedDocumentEntity(
        id = "doc-fin-1",
        userId = userId,
        title = "Tax Return 2024",
        folderId = "root",
        category = "Finance",
        fileType = "pdf",
        fileName = "tax_return.pdf",
        fileSize = 4096L,
        content = "IRS 1040 statement",
        tags = "finance, tax, 2024"
    )

    // Insert all documents
    dao.upsertDocuments(listOf(workDoc, personalDoc, financeDoc))

    // Verify all 3 are stored
    val allDocs = dao.observeCachedDocuments(userId).first()
    assertEquals(3, allDocs.size)

    // Observe distinct categories
    val categories = dao.observeCategories(userId).first()
    assertTrue(categories.contains("Finance"))
    assertTrue(categories.contains("Personal"))
    assertTrue(categories.contains("Work"))

    // Update category of workDoc to Finance
    dao.updateDocumentCategory("doc-work-1", "Finance")
    // Update tags of workDoc to include new tags
    dao.updateDocumentTags("doc-work-1", "finance, audit, q3, verified")

    val updatedDoc = dao.getCachedDocumentById(userId, "doc-work-1")
    assertNotNull(updatedDoc)
    assertEquals("Finance", updatedDoc!!.category)
    assertTrue(updatedDoc.parsedTags().contains("audit"))
    assertTrue(updatedDoc.parsedTags().contains("verified"))
  }

  @Test
  fun `home screen widget query returns last three recently opened documents in priority order`() = runBlocking {
    val userId = "widget-user"
    val docs = listOf(
        CachedDocumentEntity(
            id = "doc-1",
            userId = userId,
            title = "Oldest Opened Doc",
            folderId = "root",
            category = "Work",
            fileType = "pdf",
            fileName = "d1.pdf",
            fileSize = 512L,
            content = "Content 1",
            lastAccessedAtMillis = 1000L
        ),
        CachedDocumentEntity(
            id = "doc-2",
            userId = userId,
            title = "Second Recent Doc",
            folderId = "root",
            category = "Personal",
            fileType = "docx",
            fileName = "d2.docx",
            fileSize = 512L,
            content = "Content 2",
            lastAccessedAtMillis = 3000L
        ),
        CachedDocumentEntity(
            id = "doc-3",
            userId = userId,
            title = "Third Recent Doc",
            folderId = "root",
            category = "Finance",
            fileType = "xlsx",
            fileName = "d3.xlsx",
            fileSize = 512L,
            content = "Content 3",
            lastAccessedAtMillis = 2000L
        ),
        CachedDocumentEntity(
            id = "doc-4",
            userId = userId,
            title = "Most Recently Opened Doc",
            folderId = "root",
            category = "Work",
            fileType = "pdf",
            fileName = "d4.pdf",
            fileSize = 512L,
            content = "Content 4",
            lastAccessedAtMillis = 5000L
        )
    )
    dao.upsertDocuments(docs)

    val topThree = dao.getRecentDocuments(3)
    assertEquals(3, topThree.size)
    assertEquals("doc-4", topThree[0].id)
    assertEquals("doc-2", topThree[1].id)
    assertEquals("doc-3", topThree[2].id)
  }

  @Test
  fun `workManager sync manager builds connected network constraint and reconciles pending offline changes`() = runBlocking {
    val constraints = com.example.data.sync.DocFlowSyncManager.buildConnectedSyncConstraints()
    assertEquals(androidx.work.NetworkType.CONNECTED, constraints.requiredNetworkType)

    val oneTimeRequest = com.example.data.sync.DocFlowSyncManager.buildOneTimeSyncRequest("sync-user-1")
    assertTrue(oneTimeRequest.tags.contains(com.example.data.sync.DocFlowSyncManager.TAG_SUPABASE_SYNC))

    // Insert a pending offline document and verify getAllPendingSyncDocuments + state transition
    val pendingOfflineDoc = CachedDocumentEntity(
        id = "offline-doc-99",
        userId = "sync-user-1",
        title = "Offline Field Report",
        folderId = "root",
        category = "Work",
        fileType = "docx",
        fileName = "report.docx",
        fileSize = 2048L,
        content = "Authored while offline",
        syncState = SyncState.PENDING_UPDATE.name
    )
    dao.upsertDocument(pendingOfflineDoc)

    val pendingList = dao.getAllPendingSyncDocuments()
    assertEquals(1, pendingList.size)
    assertEquals("offline-doc-99", pendingList.first().id)

    // Simulate worker marking document synced after Supabase upload
    dao.updateDocumentSyncState("offline-doc-99", SyncState.SYNCED.name)
    val remainingPending = dao.getAllPendingSyncDocuments()
    assertTrue(remainingPending.isEmpty())
  }

  @Test
  fun `edge detection filter processes captured bitmaps and compiles multipage pdf into local Room storage`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val page1Bmp = android.graphics.Bitmap.createBitmap(80, 100, android.graphics.Bitmap.Config.ARGB_8888)
    val page2Bmp = android.graphics.Bitmap.createBitmap(80, 100, android.graphics.Bitmap.Config.ARGB_8888)

    val edgeFiltered1 = com.example.data.util.ImageProcessingPdfPipeline.processDocumentBitmap(
        source = page1Bmp,
        filter = com.example.ui.screens.ScanFilter.EDGE_DETECTION,
        rotationDegrees = 0f
    )
    val edgeFiltered2 = com.example.data.util.ImageProcessingPdfPipeline.processDocumentBitmap(
        source = page2Bmp,
        filter = com.example.ui.screens.ScanFilter.EDGE_DETECTION,
        rotationDegrees = 0f
    )
    assertEquals(80, edgeFiltered1.width)
    assertEquals(100, edgeFiltered1.height)

    val pdfResult = com.example.data.util.ImageProcessingPdfPipeline.convertBitmapsToPdfFile(
        context = context,
        fileName = "edge_detected_multipage.pdf",
        bitmaps = listOf(edgeFiltered1, edgeFiltered2)
    ).getOrThrow()

    assertTrue(pdfResult.pdfFile.exists())
    assertEquals(2, pdfResult.pageCount)

    val scannedRoomDoc = CachedDocumentEntity(
        id = "scan-multipage-1",
        userId = "scanner-user",
        title = "Scanned Multipage Document",
        folderId = "root",
        category = "Work",
        fileType = "pdf",
        fileName = "edge_detected_multipage.pdf",
        fileSize = pdfResult.fileSize,
        content = "=== SECTION 1: SCANNED DOCUMENT ===\nPages: 2",
        localFilePath = pdfResult.pdfFile.absolutePath,
        pageCount = pdfResult.pageCount.toLong(),
        syncState = SyncState.PENDING_CREATE.name
    )
    dao.upsertDocument(scannedRoomDoc)

    val loaded = dao.getCachedDocumentById("scanner-user", "scan-multipage-1")
    assertNotNull(loaded)
    assertEquals(2L, loaded!!.pageCount)
    assertEquals(pdfResult.pdfFile.absolutePath, loaded.localFilePath)
  }

  @Test
  fun `pdf markup tools draw highlight and text box save locally in Room and queue for WorkManager Firebase sync`() = runBlocking {
    val userId = "markup-user"
    val docId = "pdf-doc-101"

    val drawAnnotation = com.example.data.local.PdfAnnotationYMapEntity(
        ymapKey = "ymap_${docId}_p0_draw",
        documentId = docId,
        userId = userId,
        pageIndex = 0,
        toolType = com.example.data.local.PdfMarkupTool.PEN.name,
        colorHex = "#3B82F6",
        strokeWidth = 4.5f,
        normalizedPointsEncoded = "0.1000,0.2000;0.4000,0.5000",
        syncState = SyncState.PENDING_CREATE.name
    )
    val highlightAnnotation = com.example.data.local.PdfAnnotationYMapEntity(
        ymapKey = "ymap_${docId}_p0_highlight",
        documentId = docId,
        userId = userId,
        pageIndex = 0,
        toolType = com.example.data.local.PdfMarkupTool.HIGHLIGHT.name,
        colorHex = "#FBBF24",
        strokeWidth = 18f,
        normalizedPointsEncoded = "0.1500,0.3000;0.7500,0.3000",
        syncState = SyncState.PENDING_CREATE.name
    )
    val textBoxAnnotation = com.example.data.local.PdfAnnotationYMapEntity(
        ymapKey = "ymap_${docId}_p0_textbox",
        documentId = docId,
        userId = userId,
        pageIndex = 0,
        toolType = com.example.data.local.PdfMarkupTool.TEXT_BOX.name,
        colorHex = "#00F4FE",
        strokeWidth = 12f,
        normalizedPointsEncoded = "0.2500,0.4500",
        stickyNoteText = "Verified Page 1 Annotation",
        syncState = SyncState.PENDING_CREATE.name
    )

    dao.upsertPdfAnnotation(drawAnnotation)
    dao.upsertPdfAnnotation(highlightAnnotation)
    dao.upsertPdfAnnotation(textBoxAnnotation)

    val docAnnotations = dao.observeDocumentAnnotations(docId).first()
    assertEquals(3, docAnnotations.size)
    assertTrue(docAnnotations.any { it.toolType == com.example.data.local.PdfMarkupTool.PEN.name })
    assertTrue(docAnnotations.any { it.toolType == com.example.data.local.PdfMarkupTool.HIGHLIGHT.name })
    assertTrue(docAnnotations.any {
      it.toolType == com.example.data.local.PdfMarkupTool.TEXT_BOX.name &&
          it.stickyNoteText == "Verified Page 1 Annotation"
    })

    // Verify all 3 annotations are counted in pending sync for WorkManager
    val pendingCount = dao.observePendingSyncCount(userId).first()
    assertEquals(3, pendingCount)

    val pendingAnnotations = dao.getPendingSyncAnnotations(userId)
    assertEquals(3, pendingAnnotations.size)

    // Simulate WorkManager Firebase sync completion
    pendingAnnotations.forEach { ann ->
      dao.updateAnnotationSyncState(ann.ymapKey, SyncState.SYNCED.name)
    }

    val remainingAfterSync = dao.getPendingSyncAnnotations(userId)
    assertTrue(remainingAfterSync.isEmpty())
    assertEquals(0, dao.observePendingSyncCount(userId).first())
  }
}



