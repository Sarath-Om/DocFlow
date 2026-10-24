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
    assertEquals("Saved Locally", SyncState.SYNCED.label)
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

  @Test
  fun `mlkit ocr extracted text from scanned pdf enables full text search in Room FTS4 and snippet extraction`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val userId = "ocr-search-user"

    val page1Bmp = android.graphics.Bitmap.createBitmap(120, 160, android.graphics.Bitmap.Config.ARGB_8888)
    val page2Bmp = android.graphics.Bitmap.createBitmap(120, 160, android.graphics.Bitmap.Config.ARGB_8888)

    val simulatedMlKitOcrText = buildString {
      appendLine("=== SECTION 1: SCANNED DOCUMENT ===")
      appendLine("Title: Vendor Invoice 2026")
      appendLine("Pages: 2")
      appendLine()
      appendLine("=== SECTION 2: OCR TEXT ===")
      appendLine("=== PAGE 1 OCR TEXT ===")
      appendLine("ACME SEMICONDUCTOR CORP - INVOICE #INV-98412")
      appendLine("Total Due: $14,850.00 USD | Net 30 Terms")
      appendLine()
      appendLine("=== PAGE 2 OCR TEXT ===")
      appendLine("Wire Routing Number: 021000021 | Remittance Approved by Chief Financial Officer")
    }

    val pdfResult = com.example.data.util.ImageProcessingPdfPipeline.convertBitmapsToPdfFile(
        context = context,
        fileName = "invoice_98412.pdf",
        bitmaps = listOf(page1Bmp, page2Bmp),
        ocrText = simulatedMlKitOcrText
    ).getOrThrow()

    val scannedPdfDoc = CachedDocumentEntity(
        id = "pdf-ocr-98412",
        userId = userId,
        title = "Vendor Invoice 2026",
        folderId = "root",
        category = "Finance",
        fileType = "pdf",
        fileName = "invoice_98412.pdf",
        fileSize = pdfResult.fileSize,
        content = simulatedMlKitOcrText,
        contentPreview = CachedDocumentEntity.buildPreview(simulatedMlKitOcrText, false),
        tags = "pdf, scanned, ocr, mlkit, finance",
        localFilePath = pdfResult.pdfFile.absolutePath,
        pageCount = 2L,
        updatedAtMillis = 1761120000000L
    )

    val unrelatedPdfDoc = CachedDocumentEntity(
        id = "pdf-ocr-other",
        userId = userId,
        title = "Employee Handbook",
        folderId = "root",
        category = "Work",
        fileType = "pdf",
        fileName = "handbook.pdf",
        fileSize = 2048L,
        content = "Standard vacation policy and office hours.",
        contentPreview = "Standard vacation policy and office hours.",
        tags = "pdf, work, hr",
        pageCount = 5L,
        updatedAtMillis = 1761110000000L
    )

    dao.upsertDocuments(listOf(scannedPdfDoc, unrelatedPdfDoc))

    // 1. Verify SQLite FTS4 full-text search finds the PDF by deep Page 2 OCR text ("Remittance")
    val ftsMatches = dao.searchCachedDocumentsFts(userId, "Remittance*").first()
    assertEquals(1, ftsMatches.size)
    assertEquals("pdf-ocr-98412", ftsMatches.first().id)

    // 2. Verify substring search and dedicated stored PDF search find invoice number from Page 1 OCR
    val pdfMatches = dao.searchStoredPdfDocuments(userId, "INV-98412").first()
    assertEquals(1, pdfMatches.size)
    assertEquals("pdf-ocr-98412", pdfMatches.first().id)

    // 3. Verify OCR snippet extractor highlights the exact matching phrase inside the stored PDF
    val snippet = scannedPdfDoc.extractMatchingOcrSnippet("INV-98412")
    assertNotNull(snippet)
    assertTrue(snippet!!.contains("INV-98412"))

    // 4. Verify quick-preview metadata details (page count, size, last modified timestamp)
    val loadedDoc = dao.getCachedDocumentById(userId, "pdf-ocr-98412")!!
    assertEquals(2L, loadedDoc.pageCount)
    assertTrue(loadedDoc.fileSize > 0L)
    assertEquals(1761120000000L, loadedDoc.updatedAtMillis)
  }

  @Test
  fun `export individual document version and note as pdf files generates valid pdf and indexes in Room`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val userId = "pdf-export-user"

    val sourceDoc = com.example.data.model.DocEntity(
        id = "doc-contract-7",
        userId = userId,
        title = "Master Service Agreement",
        folderId = "root",
        category = "Contract",
        fileType = "docx",
        fileName = "msa.docx",
        fileSize = 4096L,
        content = "SECTION 1: MASTER AGREEMENT"
    )

    val versionSnapshot = com.example.data.local.DocumentVersionEntity(
        id = "ver-701",
        documentId = "doc-contract-7",
        userId = userId,
        versionNumber = 3,
        versionLabel = "Final Board Review Draft",
        authorName = "Sarath Om",
        stateVector = "sv-3",
        snapshotContent = "SECTION 1: MASTER AGREEMENT\nAll document versions stay local on your phone.\nVerified clause 4.2.",
        byteSize = 128L,
        createdAtMillis = 1761125000000L
    )

    val versionPdfResult = com.example.data.util.PdfLibAnnotationEngine.exportVersionSnapshotToPdf(
        context = context,
        document = sourceDoc,
        version = versionSnapshot
    )
    assertTrue(versionPdfResult.isSuccess)
    val exportedVer = versionPdfResult.getOrThrow()
    assertTrue(exportedVer.pdfFile.exists())
    assertTrue(exportedVer.pdfFile.length() > 0L)

    val noteEntity = com.example.data.local.DocumentCommentEntity(
        id = "note-901",
        documentId = "doc-contract-7",
        userId = userId,
        authorName = "Legal Counsel",
        authorRole = "EDITOR",
        anchorLineOrText = "Clause 4.2 Liability Cap",
        body = "Ensure liability cap matches the offline privacy warranty.",
        isSuggestion = true,
        suggestedReplacement = "Liability is strictly capped at zero for free local APK distribution.",
        status = "OPEN",
        createdAtMillis = 1761126000000L
    )

    val notePdfResult = com.example.data.util.PdfLibAnnotationEngine.exportNoteOrCommentToPdf(
        context = context,
        document = sourceDoc,
        comment = noteEntity
    )
    assertTrue(notePdfResult.isSuccess)
    val exportedNote = notePdfResult.getOrThrow()
    assertTrue(exportedNote.pdfFile.exists())
    assertTrue(exportedNote.pdfFile.length() > 0L)

    // Store the exported version PDF in Room and verify it is searchable
    val versionDocEntity = CachedDocumentEntity(
        id = "exported-ver-doc-1",
        userId = userId,
        title = "Master Service Agreement - v3 (Final Board Review Draft)",
        folderId = "root",
        category = "Contract",
        fileType = "pdf",
        fileName = exportedVer.pdfFile.name,
        fileSize = exportedVer.pdfFile.length(),
        content = versionSnapshot.snapshotContent,
        tags = "pdf, version-export, v3",
        localFilePath = exportedVer.pdfFile.absolutePath,
        pageCount = 1L
    )
    dao.upsertDocument(versionDocEntity)

    val retrieved = dao.getCachedDocumentById(userId, "exported-ver-doc-1")
    assertNotNull(retrieved)
    assertEquals("pdf", retrieved!!.fileType)
    assertTrue(retrieved.parsedTags().contains("version-export"))
  }

  @Test
  fun `settings preferences persist light mode default language pin and legal urls`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Verify Light Mode is the default
    assertEquals("LIGHT", com.example.data.util.OnDeviceAiAndBackupEngine.getSavedThemeModeName(context))
    com.example.data.util.OnDeviceAiAndBackupEngine.saveThemeModeName(context, "DARK")
    assertEquals("DARK", com.example.data.util.OnDeviceAiAndBackupEngine.getSavedThemeModeName(context))
    com.example.data.util.OnDeviceAiAndBackupEngine.saveThemeModeName(context, "LIGHT")
    assertEquals("LIGHT", com.example.data.util.OnDeviceAiAndBackupEngine.getSavedThemeModeName(context))

    // Verify primary language selection
    com.example.data.util.OnDeviceAiAndBackupEngine.setPrimaryLanguage(
        context,
        com.example.data.util.TranslationLanguage.HINDI
    )
    assertEquals(
        com.example.data.util.TranslationLanguage.HINDI,
        com.example.data.util.OnDeviceAiAndBackupEngine.getPrimaryLanguage(context)
    )

    // Verify Private Safe PIN set, verify, and reset
    com.example.data.util.OnDeviceAiAndBackupEngine.setPrivateSafePin(context, "5678")
    assertTrue(com.example.data.util.OnDeviceAiAndBackupEngine.hasPrivateSafePin(context))
    assertTrue(com.example.data.util.OnDeviceAiAndBackupEngine.verifyPrivateSafePin(context, "5678"))
    com.example.data.util.OnDeviceAiAndBackupEngine.clearPrivateSafePin(context)

    // Verify Privacy Policy and Terms of Service links from single source of truth
    assertEquals("https://docflow.free.je/privacy/", com.example.data.util.DocFlowLegalConstants.PRIVACY_POLICY_URL)
    assertEquals("https://docflow.free.je/terms/", com.example.data.util.DocFlowLegalConstants.TERMS_OF_SERVICE_URL)
    assertEquals(
        com.example.data.util.DocFlowLegalConstants.PRIVACY_POLICY_URL,
        com.example.data.util.OnDeviceAiAndBackupEngine.PRIVACY_POLICY_URL
    )
    assertEquals(
        com.example.data.util.DocFlowLegalConstants.TERMS_OF_SERVICE_URL,
        com.example.data.util.OnDeviceAiAndBackupEngine.TERMS_OF_SERVICE_URL
    )
    assertTrue(com.example.data.util.DocFlowLegalConstants.isAllowListedLegalUrl("https://docflow.free.je/privacy/"))
    assertTrue(com.example.data.util.DocFlowLegalConstants.isAllowListedLegalUrl("https://docflow.free.je/terms/"))
    assertTrue(!com.example.data.util.DocFlowLegalConstants.isAllowListedLegalUrl("https://evil.example.com/privacy"))

    // Verify legal policy version acceptance persistence
    com.example.data.util.OnDeviceAiAndBackupEngine.acceptCurrentLegalPolicy(context)
    assertTrue(com.example.data.util.OnDeviceAiAndBackupEngine.hasAcceptedCurrentLegalPolicy(context))
    assertEquals(
        com.example.data.util.DocFlowLegalConstants.LEGAL_POLICY_VERSION,
        com.example.data.util.OnDeviceAiAndBackupEngine.getAcceptedLegalVersion(context)
    )
  }

  @Test
  fun `pdf watermark config persists and renders watermarked pdf and ai mic noise clean sanitizes transcript`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val customWatermark = com.example.data.util.PdfWatermarkConfig(
        enabled = true,
        text = "Confidential • गोपनीय",
        opacityPercent = 35,
        positionIndex = 9, // 9 = Tile across page
        rotationDegrees = -45,
        fontSizeSp = 42,
        colorHex = "#DC2626",
        pageRangeMode = "ALL",
        layerBehindContent = false
    )
    com.example.data.util.OnDeviceAiAndBackupEngine.saveWatermarkConfig(context, customWatermark)
    val loadedWatermark = com.example.data.util.OnDeviceAiAndBackupEngine.getSavedWatermarkConfig(context)
    assertEquals("Confidential • गोपनीय", loadedWatermark.text)
    assertEquals(35, loadedWatermark.opacityPercent)
    assertEquals(9, loadedWatermark.positionIndex)
    assertEquals("#DC2626", loadedWatermark.colorHex)

    val bmp = android.graphics.Bitmap.createBitmap(120, 160, android.graphics.Bitmap.Config.ARGB_8888)
    val pdfOut = com.example.data.util.ImageProcessingPdfPipeline.convertBitmapsToPdfFile(
        context = context,
        fileName = "watermarked_test.pdf",
        bitmaps = listOf(bmp),
        ocrText = "Sample local document content",
        watermarkConfig = customWatermark,
        pageSizeOption = "A4",
        orientationOption = "PORTRAIT",
        marginOption = "SMALL"
    ).getOrThrow()
    assertTrue(pdfOut.pdfFile.exists())
    assertTrue(pdfOut.fileSize > 0L)

    // Verify AI Mic Noise Clean text post-processing removes noise markers and capitalizes sentences
    val cleanedSpeech = com.example.data.util.AiMicNoiseCleanController.cleanTranscribedText(
        rawText = "[noise] um quarterly revenue grew by 18 percent uh stored on this device",
        level = com.example.data.util.NoiseCleanLevel.HIGH
    )
    assertTrue(cleanedSpeech.contains("Quarterly revenue grew by 18 percent"))
    assertTrue(!cleanedSpeech.contains("[noise]"))

    // Verify local device storage space queries return positive byte counts
    assertTrue(com.example.data.util.OnDeviceAiAndBackupEngine.getDeviceTotalSpaceBytes(context) > 0L)
    assertTrue(com.example.data.util.OnDeviceAiAndBackupEngine.getDeviceFreeSpaceBytes(context) >= 0L)
  }

  @Test
  fun `selected versions and notes export to pdf and batch scan reorder and delete persist to local sqlite`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val userId = "local-owner"

    val sourceDoc = com.example.data.model.DocEntity(
        id = "doc-source-10",
        userId = userId,
        title = "Device Architecture Spec",
        folderId = "root",
        category = "Work",
        fileType = "docx",
        fileName = "device_arch.docx",
        fileSize = 2048L,
        content = "Original architecture specification stored on this device."
    )

    val ver1 = com.example.data.local.DocumentVersionEntity(
        id = "ver-101",
        documentId = sourceDoc.id,
        userId = userId,
        versionNumber = 1,
        versionLabel = "Initial Baseline",
        authorName = "Local Owner",
        stateVector = "sv-1",
        snapshotContent = "Baseline content v1",
        byteSize = 240L
    )
    val ver2 = com.example.data.local.DocumentVersionEntity(
        id = "ver-102",
        documentId = sourceDoc.id,
        userId = userId,
        versionNumber = 2,
        versionLabel = "Reviewed Update",
        authorName = "Local Owner",
        stateVector = "sv-2",
        snapshotContent = "Updated content v2 with tables",
        byteSize = 310L
    )
    val note1 = com.example.data.local.DocumentCommentEntity(
        id = "note-201",
        documentId = sourceDoc.id,
        userId = userId,
        authorName = "Local Owner",
        authorRole = "OWNER",
        anchorLineOrText = "Section 2",
        body = "Verify local SQLite index updates immediately after PDF export.",
        status = "OPEN"
    )

    val exportResult = com.example.data.util.PdfLibAnnotationEngine.exportSelectedVersionsAndNotesToPdf(
        context = context,
        document = sourceDoc,
        selectedVersions = listOf(ver1, ver2),
        selectedNotes = listOf(note1),
        customExportTitle = "Device Architecture Spec (Exported Selection)"
    ).getOrThrow()

    assertTrue(exportResult.pdfFile.exists())
    assertTrue(exportResult.pdfFile.length() > 0L)
    assertEquals(3, exportResult.annotationCount)

    // Persist exported PDF into Room SQLite and verify Created by me + Recent query
    val exportedEntity = CachedDocumentEntity(
        id = "doc-exported-pdf-1",
        userId = userId,
        title = "Device Architecture Spec (Exported Selection)",
        folderId = "root",
        category = "Notes",
        fileType = "pdf",
        fileName = exportResult.pdfFile.name,
        fileSize = exportResult.pdfFile.length(),
        content = "Exported 2 versions and 1 note",
        tags = "pdf, version-export, note-export, created-by-me",
        createdByMe = true
    )
    dao.upsertDocument(exportedEntity)

    val savedExport = dao.getCachedDocumentById(userId, "doc-exported-pdf-1")
    assertNotNull(savedExport)
    assertTrue(savedExport!!.createdByMe)
    assertEquals("pdf", savedExport.fileType)

    // Simulate Batch Scan 3 pages -> Reorder page 3 before page 2 -> Delete page 1 -> Save 2 pages to Room SQLite
    val batchPages = mutableListOf(
        "Page 1 OCR" to android.graphics.Bitmap.createBitmap(60, 80, android.graphics.Bitmap.Config.ARGB_8888),
        "Page 2 OCR" to android.graphics.Bitmap.createBitmap(60, 80, android.graphics.Bitmap.Config.ARGB_8888),
        "Page 3 OCR" to android.graphics.Bitmap.createBitmap(60, 80, android.graphics.Bitmap.Config.ARGB_8888)
    )
    // Reorder: move index 2 left to index 1
    val movedPage = batchPages.removeAt(2)
    batchPages.add(1, movedPage)
    assertEquals("Page 3 OCR", batchPages[1].first)

    // Delete first page (index 0)
    batchPages.removeAt(0)
    assertEquals(2, batchPages.size)
    assertEquals("Page 3 OCR", batchPages[0].first)
    assertEquals("Page 2 OCR", batchPages[1].first)

    val batchPdf = com.example.data.util.ImageProcessingPdfPipeline.convertBitmapsToPdfFile(
        context = context,
        fileName = "reordered_batch_scan.pdf",
        bitmaps = batchPages.map { it.second },
        ocrText = batchPages.joinToString("\n") { it.first }
    ).getOrThrow()

    assertEquals(2, batchPdf.pageCount)
    assertTrue(batchPdf.pdfFile.exists())
  }

  @Test
  fun `note share link engine encodes and decodes #v1 payload with password expiry tamper and 6000 char guard`() {
    val nowMs = System.currentTimeMillis()

    // 1. Unencrypted link round-trip
    val openLinkRes = com.example.data.util.NoteShareLinkEngine.generateShareLink(
        title = "Sprint Architecture Notes",
        body = "# Decisions\n- [x] Use Deflate + Base64URL\n- [ ] Add QR code sheet",
        createdAtMillis = nowMs,
        expiryOption = com.example.data.util.NoteLinkExpiryOption.SEVEN_DAYS,
        password = ""
    )
    assertTrue(openLinkRes is com.example.data.util.NoteLinkGenerationResult.Success)
    val openSuccess = openLinkRes as com.example.data.util.NoteLinkGenerationResult.Success
    assertTrue(openSuccess.url.startsWith("https://docflow.free.je/n/#v1."))
    assertTrue(!openSuccess.record.hasPassword)

    val decodedOpen = com.example.data.util.NoteShareLinkEngine.decodeShareLink(
        urlOrFragment = openSuccess.url,
        password = "",
        nowMillis = nowMs + 1000L
    )
    assertTrue(decodedOpen is com.example.data.util.NoteLinkDecodeResult.Success)
    val openPayload = (decodedOpen as com.example.data.util.NoteLinkDecodeResult.Success).payload
    assertEquals("Sprint Architecture Notes", openPayload.title)
    assertTrue(openPayload.body.contains("Use Deflate + Base64URL"))

    // 2. Password-protected AES-256-GCM + PBKDF2 link round-trip + wrong password check
    val encLinkRes = com.example.data.util.NoteShareLinkEngine.generateShareLink(
        title = "Board Compensation Memo",
        body = "Confidential board notes stored inside #v1 fragment.",
        createdAtMillis = nowMs,
        expiryOption = com.example.data.util.NoteLinkExpiryOption.THIRTY_DAYS,
        password = "correct-horse-battery"
    )
    assertTrue(encLinkRes is com.example.data.util.NoteLinkGenerationResult.Success)
    val encUrl = (encLinkRes as com.example.data.util.NoteLinkGenerationResult.Success).url

    // Missing password returns PasswordRequired
    val missingPwd = com.example.data.util.NoteShareLinkEngine.decodeShareLink(encUrl, password = "", nowMillis = nowMs)
    assertTrue(missingPwd is com.example.data.util.NoteLinkDecodeResult.PasswordRequired)

    // Wrong password returns WrongPassword
    val wrongPwd = com.example.data.util.NoteShareLinkEngine.decodeShareLink(encUrl, password = "wrong-password", nowMillis = nowMs)
    assertTrue(wrongPwd is com.example.data.util.NoteLinkDecodeResult.WrongPassword)

    // Correct password unlocks note
    val rightPwd = com.example.data.util.NoteShareLinkEngine.decodeShareLink(encUrl, password = "correct-horse-battery", nowMillis = nowMs)
    assertTrue(rightPwd is com.example.data.util.NoteLinkDecodeResult.Success)
    assertEquals("Board Compensation Memo", (rightPwd as com.example.data.util.NoteLinkDecodeResult.Success).payload.title)

    // 3. Expired link check
    val expiredDecode = com.example.data.util.NoteShareLinkEngine.decodeShareLink(
        urlOrFragment = openSuccess.url,
        password = "",
        nowMillis = nowMs + 9L * 24L * 60L * 60L * 1000L // 9 days later (> 7 days)
    )
    assertTrue(expiredDecode is com.example.data.util.NoteLinkDecodeResult.Expired)

    // 4. Tampered / corrupted link check
    val tamperedDecode = com.example.data.util.NoteShareLinkEngine.decodeShareLink(
        urlOrFragment = "https://docflow.free.je/n/#v1.corrupted_payload_$$$",
        password = "",
        nowMillis = nowMs
    )
    assertTrue(tamperedDecode is com.example.data.util.NoteLinkDecodeResult.Invalid)

    // 5. > 6,000 char URL guard check (incompressible random data)
    val rng = java.util.Random(42L)
    val hugeRandomBody = buildString(12000) {
      val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()"
      repeat(9500) { append(alphabet[rng.nextInt(alphabet.length)]) }
    }
    val tooLongRes = com.example.data.util.NoteShareLinkEngine.generateShareLink(
        title = "Huge Uncompressible Note",
        body = hugeRandomBody,
        createdAtMillis = nowMs,
        expiryOption = com.example.data.util.NoteLinkExpiryOption.NEVER,
        password = ""
    )
    assertTrue(tooLongRes is com.example.data.util.NoteLinkGenerationResult.TooLarge)
  }
}

