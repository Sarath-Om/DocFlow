package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertPageBreak
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Subscript
import androidx.compose.material.icons.filled.Superscript
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DocumentCommentEntity
import com.example.data.local.DocumentVersionEntity
import com.example.data.local.PdfMarkupTool
import com.example.data.model.DocEntity
import com.example.data.util.ExtractiveSummaryResult
import com.example.data.util.FileParserUtils
import com.example.data.util.ImageProcessingPdfPipeline
import com.example.data.util.OnDeviceAiAndBackupEngine
import com.example.data.util.TranslationLanguage
import com.example.ui.components.AiMicDictationBottomSheet
import com.example.ui.components.DocFlowBottomSheet
import com.example.ui.components.DocFlowCard
import com.example.ui.components.DocFlowDivider
import com.example.ui.components.DocFlowEmptyState
import com.example.ui.components.DocFlowIconCircle
import com.example.ui.components.DocFlowListRow
import com.example.ui.components.DocFlowPrimaryButton
import com.example.ui.components.DocFlowSecondaryButton
import com.example.ui.components.DocFlowToast
import com.example.ui.components.PdfWatermarkExportSheet
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens
import com.example.ui.viewmodel.DocFlowViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class WordRibbonTab(val label: String) {
    HOME("Home"),
    INSERT("Insert"),
    LAYOUT("Layout"),
    REVIEW("Review"),
    VIEW("View")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DocumentViewerEditorScreen(
    docId: String,
    viewModel: DocFlowViewModel,
    palette: DocFlowPalette,
    onBack: () -> Unit,
    onRequestBiometricSign: (onVerified: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val documents by viewModel.filteredDocuments.collectAsState()
    val cachedMap by viewModel.cachedDocumentsMap.collectAsState()
    val privateSafeDocs by viewModel.privateSafeDocuments.collectAsState()
    val deletedDocs by viewModel.deletedDocuments.collectAsState()
    val localUris by viewModel.localDocumentUris.collectAsState()
    val allVersions by viewModel.allUserVersions.collectAsState()

    val cachedEntity = cachedMap[docId]
        ?: privateSafeDocs.find { it.id == docId }
        ?: deletedDocs.find { it.id == docId }
    val doc: DocEntity? = documents.find { it.id == docId } ?: cachedEntity?.toDocEntity()

    if (doc == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background),
            contentAlignment = Alignment.Center
        ) {
            DocFlowEmptyState(
                icon = Icons.Filled.Description,
                title = "Document could not be loaded",
                subtitle = "This file may have been moved or deleted from this device.",
                palette = palette
            )
        }
        return
    }

    val comments by viewModel.observeDocumentComments(doc.id).collectAsState(initial = emptyList())
    val versionsForDoc = remember(allVersions, doc.id) {
        allVersions.filter { it.documentId == doc.id }
    }

    val isPdf = doc.fileType.equals("pdf", ignoreCase = true) || doc.fileType.equals("scanned", ignoreCase = true)
    val isWordOrText = doc.fileType.lowercase() in setOf("docx", "doc", "txt", "md", "note")
    val isExcel = doc.fileType.lowercase() in setOf("xlsx", "xls", "csv")
    val isPpt = doc.fileType.lowercase() in setOf("pptx", "ppt")

    // Viewer & Editor State
    var isEditingMode by remember(doc.id) { mutableStateOf(false) }
    var isPrintLayoutMode by remember(doc.id) { mutableStateOf(true) } // Print layout vs Reading (reflow)
    var editableContent by remember(doc.id) { mutableStateOf(doc.content) }
    var documentTitle by remember(doc.id) { mutableStateOf(doc.title) }
    var authorMetadata by remember(doc.id) { mutableStateOf("Local Device Owner") }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var saveStatusIndicator by remember { mutableStateOf("Saved • Stored on this device") }

    // Undo / Redo History (50+ steps)
    val undoStack = remember(doc.id) { mutableStateListOf(doc.content) }
    var undoIndex by remember(doc.id) { mutableIntStateOf(0) }

    fun pushEditContent(updatedText: String) {
        editableContent = updatedText
        hasUnsavedChanges = true
        saveStatusIndicator = "Saving..."
        if (undoStack.getOrNull(undoIndex) != updatedText) {
            while (undoStack.lastIndex > undoIndex) {
                undoStack.removeAt(undoStack.lastIndex)
            }
            if (undoStack.size >= 60) {
                undoStack.removeAt(0)
            } else {
                undoIndex++
            }
            undoStack.add(updatedText)
        }
    }

    // Autosave every 2.5 seconds when dirty
    LaunchedEffect(editableContent, hasUnsavedChanges) {
        if (hasUnsavedChanges) {
            delay(1800L)
            viewModel.updateDocumentContent(
                doc = doc,
                newTitle = documentTitle,
                newFolderId = doc.folderId,
                newCategory = doc.category,
                plainContent = editableContent,
                isEncrypted = doc.isEncrypted,
                password = null,
                encryptionKeyHint = doc.encryptionKeyHint,
                isOfflineCached = true
            )
            hasUnsavedChanges = false
            saveStatusIndicator = "Saved • Stored on this device"
        }
    }

    // MS Word Ribbon State
    var activeRibbonTab by remember { mutableStateOf(WordRibbonTab.HOME) }
    var selectedFontFamily by remember { mutableStateOf("Roboto") }
    var selectedFontSizePt by remember { mutableIntStateOf(14) }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var isStrikethrough by remember { mutableStateOf(false) }
    var isSubscript by remember { mutableStateOf(false) }
    var isSuperscript by remember { mutableStateOf(false) }
    var textColorHex by remember { mutableStateOf("#111111") }
    var highlightColorHex by remember { mutableStateOf("#00000000") }
    var textAlignMode by remember { mutableStateOf("LEFT") } // LEFT, CENTER, RIGHT, JUSTIFY
    var lineSpacingMultiplier by remember { mutableFloatStateOf(1.35f) }
    var paragraphStyleName by remember { mutableStateOf("Normal") } // Normal, Title, Heading 1, Heading 2, Heading 3, Quote, Code

    // Layout & View Ribbon State
    var pageSizeOption by remember { mutableStateOf("A4") } // A4, Letter, Legal
    var pageOrientation by remember { mutableStateOf("Portrait") } // Portrait, Landscape
    var pageMargins by remember { mutableStateOf("Normal") } // Normal, Narrow, Wide
    var pageColumns by remember { mutableIntStateOf(1) }
    var showLineNumbers by remember { mutableStateOf(false) }
    var showRuler by remember { mutableStateOf(true) }
    var showFormattingMarks by remember { mutableStateOf(false) }
    var pageHeaderText by remember { mutableStateOf("") }
    var pageFooterText by remember { mutableStateOf("Stored on this device") }
    var showPageNumbers by remember { mutableStateOf(true) }

    // Find & Replace / Spell Check / Table / Comments / Export Modals
    var showInDocSearch by remember { mutableStateOf(false) }
    var inDocSearchQuery by remember { mutableStateOf("") }
    var inDocReplaceText by remember { mutableStateOf("") }
    var activeMatchIndex by remember { mutableIntStateOf(0) }

    var showInsertTableDialog by remember { mutableStateOf(false) }
    var tableRowsInput by remember { mutableStateOf("3") }
    var tableColsInput by remember { mutableStateOf("3") }
    var showInsertLinkDialog by remember { mutableStateOf(false) }
    var linkTextInput by remember { mutableStateOf("") }
    var linkUrlInput by remember { mutableStateOf("https://") }
    var showHeaderFooterDialog by remember { mutableStateOf(false) }

    var showCommentsSheet by remember { mutableStateOf(false) }
    var newCommentBody by remember { mutableStateOf("") }
    var showExportVersionsNotesSheet by remember { mutableStateOf(false) }
    var showWatermarkPdfExportSheet by remember { mutableStateOf(false) }

    // Top-right Overflow & Sheets
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDetailsSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showTagsDialog by remember { mutableStateOf(false) }
    var tagInputText by remember { mutableStateOf(cachedEntity?.tags ?: "") }
    var showSummariseSheet by remember { mutableStateOf(false) }
    var showTranslateSheet by remember { mutableStateOf(false) }
    var showAiMicSheet by remember { mutableStateOf(false) }
    var showPageGridSheet by remember { mutableStateOf(false) }
    var showUnsavedExitDialog by remember { mutableStateOf(false) }
    var viewerToast by remember { mutableStateOf<String?>(null) }

    // PDF Viewer State
    val annotations by viewModel.observeDocumentAnnotations(doc.id).collectAsState(initial = emptyList())
    var currentPageIndex by remember(doc.id) {
        mutableIntStateOf((cachedEntity?.lastPageViewed ?: 0).coerceAtLeast(0))
    }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var activeMarkupTool by remember { mutableStateOf<PdfMarkupTool?>(null) }
    var activeStrokeColorHex by remember { mutableStateOf("#FFEB3B") }
    val currentDragPoints = remember { mutableStateListOf<Offset>() }

    // Mark document accessed & save resume page
    LaunchedEffect(doc.id, currentPageIndex) {
        viewModel.markDocumentOpened(doc.id)
        viewModel.updateDocumentLastPage(doc.id, currentPageIndex)
    }

    val insertImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pushEditContent(editableContent.trimEnd() + "\n\n[Embedded Image: ${uri.lastPathSegment ?: "photo.jpg"}]\n")
            viewerToast = "Inserted image into document"
        }
    }

    BackHandler {
        if (isEditingMode && hasUnsavedChanges) {
            showUnsavedExitDialog = true
        } else {
            onBack()
        }
    }

    // Split content into visual pages (respecting explicit `=== PAGE BREAK ===` or chunking lines)
    val contentPages = remember(editableContent, doc.pageCount) {
        val explicitPages = editableContent.split("=== PAGE BREAK ===")
        if (explicitPages.size > 1) {
            explicitPages.map { it.trim() }
        } else {
            val lines = editableContent.lines().filter { it.isNotEmpty() }
            val chunked = if (lines.isEmpty()) listOf(doc.title) else lines.chunked(26).map { it.joinToString("\n") }
            val count = maxOf(chunked.size, doc.pageCount.toInt().coerceAtLeast(1)).coerceAtMost(12)
            (0 until count).map { idx ->
                chunked.getOrNull(idx) ?: "Page ${idx + 1} — ${doc.title}"
            }
        }
    }
    val totalPages = contentPages.size.coerceAtLeast(1)

    // Render native PDF Bitmaps if a local PDF URI exists
    val localUri: Uri? = localUris[doc.id] ?: cachedEntity?.localFilePath?.let { path ->
        val f = java.io.File(path)
        if (f.exists()) Uri.fromFile(f) else null
    }
    var renderedPdfBitmaps by remember(doc.id, localUri) { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }
    LaunchedEffect(doc.id, localUri, isPdf, totalPages) {
        if (isPdf && localUri != null) {
            val map = mutableMapOf<Int, Bitmap>()
            for (idx in 0 until totalPages.coerceAtMost(8)) {
                val bmp = FileParserUtils.renderPdfPage(context, localUri, idx)
                if (bmp != null) map[idx] = bmp
            }
            renderedPdfBitmaps = map
        }
    }

    val searchMatchesCount = remember(inDocSearchQuery, editableContent) {
        val q = inDocSearchQuery.trim()
        if (q.isEmpty()) 0
        else Regex(Regex.escape(q), RegexOption.IGNORE_CASE).findAll(editableContent).count()
    }

    val wordCount = remember(editableContent) {
        editableContent.split(Regex("\\s+")).count { it.isNotBlank() }
    }
    val charCount = editableContent.length

    val subtitleText = when {
        isPdf -> "Page ${(currentPageIndex + 1).coerceIn(1, totalPages)} of $totalPages"
        isExcel -> "Excel spreadsheet (.xlsx) • $wordCount words"
        isPpt -> "Presentation (.pptx) • $totalPages slide(s)"
        else -> "Word document • $totalPages page(s) • $saveStatusIndicator"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // =================================================================
            // 1. TOP BAR (White, thin bottom divider, Back, Title + Subtitle, Undo/Redo, AI Mic, Search, Share, Overflow)
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DocFlowTokens.space2, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isEditingMode && hasUnsavedChanges) {
                                showUnsavedExitDialog = true
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .size(DocFlowTokens.minTouchTarget)
                            .testTag("viewer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = palette.textPrimary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showRenameDialog = true }
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = documentTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitleText,
                            fontSize = 12.sp,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isEditingMode) {
                        IconButton(
                            onClick = {
                                if (undoIndex > 0) {
                                    undoIndex--
                                    editableContent = undoStack[undoIndex]
                                    hasUnsavedChanges = true
                                }
                            },
                            enabled = undoIndex > 0,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (undoIndex > 0) palette.textPrimary else palette.divider
                            )
                        }
                        IconButton(
                            onClick = {
                                if (undoIndex < undoStack.lastIndex) {
                                    undoIndex++
                                    editableContent = undoStack[undoIndex]
                                    hasUnsavedChanges = true
                                }
                            },
                            enabled = undoIndex < undoStack.lastIndex,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo",
                                tint = if (undoIndex < undoStack.lastIndex) palette.textPrimary else palette.divider
                            )
                        }
                    }

                    // AI Mic (Sound to Text with Noise Clean) Quick Button
                    IconButton(
                        onClick = { showAiMicSheet = true },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("viewer_ai_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "AI Mic Noise Clean",
                            tint = DocFlowTokens.IconCircleTeal
                        )
                    }

                    IconButton(
                        onClick = { showInDocSearch = !showInDocSearch },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("viewer_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Find & Replace",
                            tint = palette.textPrimary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.shareAnyDocument(context, doc) },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("viewer_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share document",
                            tint = palette.textPrimary
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("viewer_overflow_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More options",
                                tint = palette.textPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Details (Stored on this device)") },
                                onClick = {
                                    showOverflowMenu = false
                                    showDetailsSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export PDF with Watermark") },
                                onClick = {
                                    showOverflowMenu = false
                                    showWatermarkPdfExportSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Versions & Notes to PDF") },
                                onClick = {
                                    showOverflowMenu = false
                                    showExportVersionsNotesSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Save Version Snapshot") },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.saveNewVersionSnapshot(
                                        doc = doc,
                                        plainContent = editableContent,
                                        versionLabel = "Edited Snapshot (${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())})"
                                    )
                                    viewerToast = "Saved local version snapshot"
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Comments (${comments.size})") },
                                onClick = {
                                    showOverflowMenu = false
                                    showCommentsSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    showOverflowMenu = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to Private Safe") },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.moveToPrivateSafe(doc.id, "1234")
                                    onBack()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Print") },
                                onClick = {
                                    showOverflowMenu = false
                                    triggerAndroidPrint(context, documentTitle, editableContent)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = DocFlowTokens.IconCircleRed) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.softDeleteDocument(doc.id)
                                    onBack()
                                }
                            )
                        }
                    }
                }
                DocFlowDivider(palette = palette)
            }

            // =================================================================
            // 2. IN-DOCUMENT FIND & REPLACE BAR
            // =================================================================
            AnimatedVisibility(visible = showInDocSearch) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .padding(horizontal = DocFlowTokens.screenHorizontalMargin, vertical = DocFlowTokens.space2)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                    ) {
                        OutlinedTextField(
                            value = inDocSearchQuery,
                            onValueChange = {
                                inDocSearchQuery = it
                                activeMatchIndex = if (it.isNotBlank()) 1 else 0
                            },
                            placeholder = { Text("Find in document...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("viewer_in_doc_search_input")
                        )
                        Text(
                            text = if (searchMatchesCount > 0) "${activeMatchIndex.coerceIn(1, searchMatchesCount)}/$searchMatchesCount" else "0/0",
                            style = DocFlowTokens.captionStyle,
                            color = palette.textSecondary
                        )
                        IconButton(
                            onClick = {
                                if (searchMatchesCount > 0) {
                                    activeMatchIndex = if (activeMatchIndex <= 1) searchMatchesCount else activeMatchIndex - 1
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Previous match", tint = palette.textPrimary)
                        }
                        IconButton(
                            onClick = {
                                if (searchMatchesCount > 0) {
                                    activeMatchIndex = if (activeMatchIndex >= searchMatchesCount) 1 else activeMatchIndex + 1
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Next match", tint = palette.textPrimary)
                        }
                        IconButton(
                            onClick = {
                                showInDocSearch = false
                                inDocSearchQuery = ""
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close search", tint = palette.textSecondary)
                        }
                    }

                    if (isEditingMode && inDocSearchQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inDocReplaceText,
                                onValueChange = { inDocReplaceText = it },
                                placeholder = { Text("Replace with...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            DocFlowSecondaryButton(
                                text = "Replace All",
                                onClick = {
                                    val updated = editableContent.replace(
                                        Regex(Regex.escape(inDocSearchQuery), RegexOption.IGNORE_CASE),
                                        inDocReplaceText
                                    )
                                    pushEditContent(updated)
                                    viewerToast = "Replaced $searchMatchesCount occurrence(s)"
                                },
                                palette = palette
                            )
                        }
                    }
                }
            }

            // =================================================================
            // 3. VIEW MODE LAYOUT BAR (Print Layout vs Reading Reflow, Ruler, Page Width)
            // =================================================================
            if (!isPdf) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .padding(horizontal = DocFlowTokens.screenHorizontalMargin, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = isPrintLayoutMode,
                            onClick = { isPrintLayoutMode = true },
                            label = { Text("Print Layout", fontSize = 12.sp) },
                            modifier = Modifier.testTag("word_print_layout_chip")
                        )
                        FilterChip(
                            selected = !isPrintLayoutMode,
                            onClick = { isPrintLayoutMode = false },
                            label = { Text("Reading (Reflow)", fontSize = 12.sp) },
                            modifier = Modifier.testTag("word_reflow_layout_chip")
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$wordCount words",
                            style = DocFlowTokens.captionStyle,
                            color = palette.textSecondary
                        )
                        Box(
                            modifier = Modifier
                                .clip(DocFlowTokens.pillButtonShape)
                                .background(
                                    if (isEditingMode) DocFlowTokens.IconCircleGreen else DocFlowTokens.AccentBlue
                                )
                                .clickable {
                                    if (isEditingMode) {
                                        viewModel.updateDocumentContent(
                                            doc = doc,
                                            newTitle = documentTitle,
                                            newFolderId = doc.folderId,
                                            newCategory = doc.category,
                                            plainContent = editableContent,
                                            isEncrypted = doc.isEncrypted,
                                            password = null,
                                            encryptionKeyHint = doc.encryptionKeyHint,
                                            isOfflineCached = true
                                        )
                                        hasUnsavedChanges = false
                                        isEditingMode = false
                                        viewerToast = "Saved changes on this device"
                                    } else {
                                        isEditingMode = true
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("word_toggle_edit_mode_button")
                        ) {
                            Text(
                                text = if (isEditingMode) "Done & Save" else "Edit in Word UI",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                DocFlowDivider(palette = palette)
            }

            // Optional Horizontal Ruler in Print Layout Mode
            if (!isPdf && isPrintLayoutMode && showRuler) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(Color(0xFFE8EAED))
                ) {
                    val step = size.width / 16f
                    for (i in 1..15) {
                        val x = i * step
                        val h = if (i % 2 == 0) 10.dp.toPx() else 5.dp.toPx()
                        drawLine(
                            color = Color(0xFF6B7280),
                            start = Offset(x, size.height - h),
                            end = Offset(x, size.height),
                            strokeWidth = 1.5f
                        )
                    }
                }
            }

            // =================================================================
            // 4. MAIN VIEWPORT (White Page Sheets on #F0F1F3 Gray Background)
            // =================================================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(palette.background)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(0.8f, 3.0f)
                        }
                    }
            ) {
                val activeFontFamily = when (selectedFontFamily) {
                    "Serif", "Merriweather" -> FontFamily.Serif
                    "JetBrains Mono" -> FontFamily.Monospace
                    else -> FontFamily.SansSerif
                }
                val activeTextAlign = when (textAlignMode) {
                    "CENTER" -> TextAlign.Center
                    "RIGHT" -> TextAlign.End
                    "JUSTIFY" -> TextAlign.Justify
                    else -> TextAlign.Start
                }
                val activeTextDecoration = when {
                    isUnderline && isStrikethrough -> TextDecoration.combine(
                        listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                    )
                    isUnderline -> TextDecoration.Underline
                    isStrikethrough -> TextDecoration.LineThrough
                    else -> TextDecoration.None
                }
                val parsedTextColor = runCatching {
                    Color(android.graphics.Color.parseColor(textColorHex))
                }.getOrDefault(Color(0xFF111111))

                val pageMarginPadding = when (pageMargins) {
                    "Narrow" -> 12.dp
                    "Wide" -> 32.dp
                    else -> 20.dp
                }

                if (isEditingMode && !isPdf) {
                    // Editable MS Word-Style Page Sheet Canvas
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                horizontal = if (isPrintLayoutMode) DocFlowTokens.screenHorizontalMargin else 0.dp,
                                vertical = if (isPrintLayoutMode) DocFlowTokens.space3 else 0.dp
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(if (isPrintLayoutMode) DocFlowTokens.thumbnailShape else androidx.compose.ui.graphics.RectangleShape)
                                .background(Color.White)
                                .border(
                                    width = if (isPrintLayoutMode) 1.dp else 0.dp,
                                    color = palette.divider,
                                    shape = if (isPrintLayoutMode) DocFlowTokens.thumbnailShape else androidx.compose.ui.graphics.RectangleShape
                                )
                                .padding(pageMarginPadding)
                        ) {
                            if (pageHeaderText.isNotBlank()) {
                                Text(
                                    text = pageHeaderText,
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                DocFlowDivider(palette = palette)
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$pageSizeOption ($pageOrientation) • $paragraphStyleName • ${selectedFontFamily} ${selectedFontSizePt}pt",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DocFlowTokens.AccentBlue
                                )
                                if (showFormattingMarks) {
                                    Text(
                                        text = "¶ Formatting marks ON",
                                        fontSize = 11.sp,
                                        color = DocFlowTokens.IconCircleTeal
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = if (showFormattingMarks) editableContent.replace("\n", "¶\n") else editableContent,
                                onValueChange = { raw ->
                                    val cleaned = if (showFormattingMarks) raw.replace("¶", "") else raw
                                    pushEditContent(cleaned)
                                },
                                textStyle = TextStyle(
                                    fontFamily = activeFontFamily,
                                    fontSize = (selectedFontSizePt * zoomScale).sp,
                                    fontWeight = if (isBold || paragraphStyleName.startsWith("Heading") || paragraphStyleName == "Title") FontWeight.Bold else FontWeight.Normal,
                                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                                    textDecoration = activeTextDecoration,
                                    textAlign = activeTextAlign,
                                    color = parsedTextColor,
                                    lineHeight = (selectedFontSizePt * lineSpacingMultiplier * zoomScale).sp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(480.dp)
                                    .testTag("word_editor_textfield")
                            )

                            if (pageFooterText.isNotBlank() || showPageNumbers) {
                                Spacer(modifier = Modifier.height(8.dp))
                                DocFlowDivider(palette = palette)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = pageFooterText,
                                        fontSize = 11.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                    if (showPageNumbers) {
                                        Text(
                                            text = "Page 1 of $totalPages",
                                            fontSize = 11.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Paginated White Page Sheets in View Mode (Print Layout or Reflow)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("viewer_pages_list"),
                        contentPadding = PaddingValues(
                            horizontal = if (isPrintLayoutMode) DocFlowTokens.screenHorizontalMargin else 0.dp,
                            vertical = if (isPrintLayoutMode) DocFlowTokens.space3 else 0.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(if (isPrintLayoutMode) DocFlowTokens.space3 else 0.dp)
                    ) {
                        itemsIndexed(contentPages) { index, pageText ->
                            val bmp = renderedPdfBitmaps[index]
                            val pageAnnotations = annotations.filter { it.pageIndex == index }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(if (isPrintLayoutMode) DocFlowTokens.thumbnailShape else androidx.compose.ui.graphics.RectangleShape)
                                    .background(Color.White)
                                    .border(
                                        width = if (isPrintLayoutMode) 1.dp else 0.dp,
                                        color = if (currentPageIndex == index && isPrintLayoutMode) DocFlowTokens.AccentBlue else palette.divider,
                                        shape = if (isPrintLayoutMode) DocFlowTokens.thumbnailShape else androidx.compose.ui.graphics.RectangleShape
                                    )
                                    .clickable { currentPageIndex = index }
                                    .padding(pageMarginPadding)
                            ) {
                                Column {
                                    // Header Bar inside Page Sheet
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = pageHeaderText.ifBlank { documentTitle },
                                            fontSize = 11.sp,
                                            color = Color(0xFF8A8D93),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (showPageNumbers) {
                                            Text(
                                                text = "Page ${index + 1} of $totalPages",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF8A8D93)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "PDF Page ${index + 1}",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height((420f * zoomScale).dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        // Render structured headings, tables, lists & paragraphs
                                        val lines = pageText.lines()
                                        lines.forEachIndexed { lineIdx, line ->
                                            val trimmed = line.trim()
                                            val linePrefix = if (showLineNumbers) "${lineIdx + 1}. " else ""
                                            when {
                                                trimmed.startsWith("===") -> {
                                                    Text(
                                                        text = trimmed.removePrefix("===").removeSuffix("===").trim(),
                                                        fontSize = (18f * zoomScale).sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = DocFlowTokens.AccentBlue,
                                                        modifier = Modifier.padding(vertical = 6.dp)
                                                    )
                                                }
                                                trimmed.startsWith("# ") -> {
                                                    Text(
                                                        text = trimmed.removePrefix("# "),
                                                        fontSize = (22f * zoomScale).sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF111111),
                                                        modifier = Modifier.padding(vertical = 6.dp)
                                                    )
                                                }
                                                trimmed.startsWith("## ") -> {
                                                    Text(
                                                        text = trimmed.removePrefix("## "),
                                                        fontSize = (18f * zoomScale).sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF111111),
                                                        modifier = Modifier.padding(vertical = 4.dp)
                                                    )
                                                }
                                                trimmed.startsWith("|") -> {
                                                    // Structured Table Row
                                                    val cells = trimmed.trim('|').split("|").map { it.trim() }
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(Color(0xFFF8FAFC))
                                                            .border(0.5.dp, Color(0xFFE3E4E7))
                                                            .padding(vertical = 6.dp, horizontal = 8.dp)
                                                    ) {
                                                        cells.forEach { cell ->
                                                            Text(
                                                                text = cell,
                                                                modifier = Modifier.weight(1f),
                                                                fontSize = (13f * zoomScale).sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = Color(0xFF111111)
                                                            )
                                                        }
                                                    }
                                                }
                                                trimmed == "---" -> {
                                                    DocFlowDivider(
                                                        palette = palette,
                                                        modifier = Modifier.padding(vertical = 8.dp)
                                                    )
                                                }
                                                else -> {
                                                    val matchesQuery = inDocSearchQuery.isNotBlank() &&
                                                        line.contains(inDocSearchQuery, ignoreCase = true)
                                                    Text(
                                                        text = "$linePrefix$line",
                                                        fontFamily = activeFontFamily,
                                                        fontSize = (selectedFontSizePt * zoomScale).sp,
                                                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                                                        textDecoration = activeTextDecoration,
                                                        textAlign = activeTextAlign,
                                                        lineHeight = (selectedFontSizePt * lineSpacingMultiplier * zoomScale).sp,
                                                        color = if (matchesQuery) DocFlowTokens.AccentBlue else parsedTextColor,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(
                                                                if (matchesQuery) Color(0xFFFFF59D) else Color.Transparent
                                                            )
                                                            .padding(vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (pageFooterText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        DocFlowDivider(palette = palette)
                                        Text(
                                            text = pageFooterText,
                                            fontSize = 11.sp,
                                            color = Color(0xFF8A8D93),
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }

                                // PDF Freehand / Highlight / Note Markup Overlay
                                if (isPdf && (pageAnnotations.isNotEmpty() || activeMarkupTool != null)) {
                                    Canvas(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .pointerInput(activeMarkupTool, index) {
                                                if (activeMarkupTool != null) {
                                                    detectDragGestures(
                                                        onDragStart = { offset ->
                                                            currentDragPoints.clear()
                                                            currentDragPoints.add(
                                                                Offset(
                                                                    (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                                                                    (offset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                                                )
                                                            )
                                                        },
                                                        onDrag = { change, _ ->
                                                            change.consume()
                                                            currentDragPoints.add(
                                                                Offset(
                                                                    (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                                                                    (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                                                )
                                                            )
                                                        },
                                                        onDragEnd = {
                                                            val tool = activeMarkupTool
                                                            if (tool != null && currentDragPoints.isNotEmpty()) {
                                                                viewModel.addYMapPdfAnnotation(
                                                                    doc = doc,
                                                                    pageIndex = index,
                                                                    tool = tool,
                                                                    colorHex = activeStrokeColorHex,
                                                                    strokeWidth = if (tool == PdfMarkupTool.HIGHLIGHT) 18f else 5f,
                                                                    points = currentDragPoints.toList(),
                                                                    stickyNoteText = if (tool == PdfMarkupTool.STICKY_NOTE) "Note on Page ${index + 1}" else ""
                                                                )
                                                                currentDragPoints.clear()
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                    ) {
                                        pageAnnotations.forEach { ann ->
                                            val pts = ann.decodePoints()
                                            val c = runCatching {
                                                Color(android.graphics.Color.parseColor(ann.colorHex))
                                            }.getOrDefault(DocFlowTokens.AccentBlue)
                                            if (pts.size >= 2) {
                                                val p = Path().apply {
                                                    moveTo(pts.first().x * size.width, pts.first().y * size.height)
                                                    pts.drop(1).forEach { pt ->
                                                        lineTo(pt.x * size.width, pt.y * size.height)
                                                    }
                                                }
                                                drawPath(
                                                    path = p,
                                                    color = if (ann.toolType == PdfMarkupTool.HIGHLIGHT.name) c.copy(alpha = 0.40f) else c,
                                                    style = Stroke(width = ann.strokeWidth.dp.toPx())
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Blue "Edit" Pill in View Mode (for Word/Office/Notes)
                if (!isPdf && !isEditingMode) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = 16.dp)
                            .clip(DocFlowTokens.pillButtonShape)
                            .background(DocFlowTokens.AccentBlue)
                            .clickable { isEditingMode = true }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .testTag("viewer_floating_edit_pill"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit document",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Edit",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                if (viewerToast != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    ) {
                        DocFlowToast(message = viewerToast!!)
                    }
                }
            }

            // =================================================================
            // 5. BOTTOM RIBBON / ACTION BAR (MS Word 5-Tab Ribbon in Edit Mode, or Quick Action Bar in View Mode)
            // =================================================================
            if (isEditingMode && !isPdf) {
                MsWordBottomRibbon(
                    activeTab = activeRibbonTab,
                    onSelectTab = { activeRibbonTab = it },
                    palette = palette,
                    selectedFontFamily = selectedFontFamily,
                    onSelectFontFamily = { selectedFontFamily = it },
                    selectedFontSizePt = selectedFontSizePt,
                    onChangeFontSize = { selectedFontSizePt = it.coerceIn(8, 72) },
                    isBold = isBold,
                    onToggleBold = { isBold = !isBold },
                    isItalic = isItalic,
                    onToggleItalic = { isItalic = !isItalic },
                    isUnderline = isUnderline,
                    onToggleUnderline = { isUnderline = !isUnderline },
                    isStrikethrough = isStrikethrough,
                    onToggleStrikethrough = { isStrikethrough = !isStrikethrough },
                    isSubscript = isSubscript,
                    onToggleSubscript = {
                        isSubscript = !isSubscript
                        if (isSubscript) isSuperscript = false
                    },
                    isSuperscript = isSuperscript,
                    onToggleSuperscript = {
                        isSuperscript = !isSuperscript
                        if (isSuperscript) isSubscript = false
                    },
                    onClearFormatting = {
                        isBold = false
                        isItalic = false
                        isUnderline = false
                        isStrikethrough = false
                        isSubscript = false
                        isSuperscript = false
                        textColorHex = "#111111"
                        paragraphStyleName = "Normal"
                    },
                    textColorHex = textColorHex,
                    onSelectTextColor = { textColorHex = it },
                    textAlignMode = textAlignMode,
                    onSelectTextAlign = { textAlignMode = it },
                    lineSpacingMultiplier = lineSpacingMultiplier,
                    onSelectLineSpacing = { lineSpacingMultiplier = it },
                    paragraphStyleName = paragraphStyleName,
                    onSelectParagraphStyle = { style ->
                        paragraphStyleName = style
                        when (style) {
                            "Title" -> {
                                selectedFontSizePt = 24
                                isBold = true
                            }
                            "Heading 1" -> {
                                selectedFontSizePt = 20
                                isBold = true
                            }
                            "Heading 2" -> {
                                selectedFontSizePt = 17
                                isBold = true
                            }
                            "Heading 3" -> {
                                selectedFontSizePt = 15
                                isBold = true
                            }
                            "Code" -> {
                                selectedFontFamily = "JetBrains Mono"
                                selectedFontSizePt = 13
                            }
                            else -> {
                                selectedFontSizePt = 14
                                isBold = false
                            }
                        }
                    },
                    onInsertBullet = { pushEditContent(editableContent.trimEnd() + "\n• ") },
                    onInsertNumbered = { pushEditContent(editableContent.trimEnd() + "\n1. ") },
                    onIndent = { pushEditContent(editableContent + "    ") },
                    onOutdent = { pushEditContent(editableContent.removeSuffix("    ")) },
                    onInsertTable = { showInsertTableDialog = true },
                    onInsertImage = {
                        insertImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onInsertLink = { showInsertLinkDialog = true },
                    onInsertPageBreak = { pushEditContent(editableContent.trimEnd() + "\n=== PAGE BREAK ===\n") },
                    onEditHeaderFooter = { showHeaderFooterDialog = true },
                    onInsertDate = {
                        val today = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                        pushEditContent("$editableContent $today")
                    },
                    onInsertHorizontalLine = { pushEditContent(editableContent.trimEnd() + "\n---\n") },
                    pageSizeOption = pageSizeOption,
                    onSelectPageSize = { pageSizeOption = it },
                    pageOrientation = pageOrientation,
                    onSelectOrientation = { pageOrientation = it },
                    pageMargins = pageMargins,
                    onSelectMargins = { pageMargins = it },
                    showLineNumbers = showLineNumbers,
                    onToggleLineNumbers = { showLineNumbers = !showLineNumbers },
                    onOpenFindReplace = { showInDocSearch = true },
                    onRunSpellCheck = {
                        val cleaned = editableContent
                            .replace("teh", "the")
                            .replace("recieve", "receive")
                            .replace("occured", "occurred")
                        pushEditContent(cleaned)
                        viewerToast = "Spell check complete (English + हिन्दी)"
                    },
                    onOpenComments = { showCommentsSheet = true },
                    onOpenExportWatermarkPdf = { showWatermarkPdfExportSheet = true },
                    zoomScale = zoomScale,
                    onSelectZoom = { zoomScale = it },
                    showRuler = showRuler,
                    onToggleRuler = { showRuler = !showRuler },
                    showFormattingMarks = showFormattingMarks,
                    onToggleFormattingMarks = { showFormattingMarks = !showFormattingMarks },
                    isPrintLayoutMode = isPrintLayoutMode,
                    onToggleLayoutMode = { isPrintLayoutMode = !isPrintLayoutMode }
                )
            } else {
                // View Mode Bottom Action Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                ) {
                    DocFlowDivider(palette = palette)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = DocFlowTokens.space3, vertical = DocFlowTokens.space2),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPdf) {
                            ViewerBottomActionButton(
                                icon = Icons.Filled.GridView,
                                label = "Pages ($totalPages)",
                                active = false,
                                palette = palette,
                                onClick = { showPageGridSheet = true }
                            )
                            ViewerBottomActionButton(
                                icon = Icons.Filled.BorderColor,
                                label = "Highlight",
                                active = activeMarkupTool == PdfMarkupTool.HIGHLIGHT,
                                palette = palette,
                                onClick = {
                                    activeMarkupTool = if (activeMarkupTool == PdfMarkupTool.HIGHLIGHT) null else PdfMarkupTool.HIGHLIGHT
                                    activeStrokeColorHex = "#FFEB3B"
                                }
                            )
                            ViewerBottomActionButton(
                                icon = Icons.Filled.Draw,
                                label = "Draw / Sign",
                                active = activeMarkupTool == PdfMarkupTool.PEN,
                                palette = palette,
                                onClick = {
                                    activeMarkupTool = if (activeMarkupTool == PdfMarkupTool.PEN) null else PdfMarkupTool.PEN
                                    activeStrokeColorHex = "#1F6BFF"
                                }
                            )
                            ViewerBottomActionButton(
                                icon = Icons.Filled.PictureAsPdf,
                                label = "Watermark Copy",
                                active = false,
                                palette = palette,
                                onClick = { showWatermarkPdfExportSheet = true }
                            )
                        } else {
                            ViewerBottomActionButton(
                                icon = Icons.Filled.Edit,
                                label = "Edit (Word UI)",
                                active = isEditingMode,
                                palette = palette,
                                onClick = { isEditingMode = true }
                            )
                            ViewerBottomActionButton(
                                icon = Icons.Filled.PictureAsPdf,
                                label = "Export PDF",
                                active = false,
                                palette = palette,
                                onClick = { showWatermarkPdfExportSheet = true }
                            )
                            ViewerBottomActionButton(
                                icon = Icons.Filled.History,
                                label = "Versions / Notes PDF",
                                active = false,
                                palette = palette,
                                onClick = { showExportVersionsNotesSheet = true }
                            )
                        }

                        ViewerBottomActionButton(
                            icon = Icons.Filled.Mic,
                            label = "AI Mic",
                            active = false,
                            palette = palette,
                            onClick = { showAiMicSheet = true }
                        )
                        ViewerBottomActionButton(
                            icon = Icons.Filled.AutoAwesome,
                            label = "Summarise",
                            active = false,
                            palette = palette,
                            onClick = { showSummariseSheet = true }
                        )
                        ViewerBottomActionButton(
                            icon = Icons.Filled.Translate,
                            label = "Translate",
                            active = false,
                            palette = palette,
                            onClick = { showTranslateSheet = true }
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // DIALOGS & SHEETS: TABLE, LINK, HEADER/FOOTER, COMMENTS, WATERMARK EXPORT, DETAILS
    // =========================================================================

    if (showInsertTableDialog) {
        AlertDialog(
            onDismissRequest = { showInsertTableDialog = false },
            title = { Text("Insert Table (Rows × Columns)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)) {
                    OutlinedTextField(
                        value = tableRowsInput,
                        onValueChange = { tableRowsInput = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Rows (1–15)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tableColsInput,
                        onValueChange = { tableColsInput = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Columns (1–8)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val r = (tableRowsInput.toIntOrNull() ?: 3).coerceIn(1, 15)
                        val c = (tableColsInput.toIntOrNull() ?: 3).coerceIn(1, 8)
                        val tableBuilder = StringBuilder("\n")
                        val headerRow = (1..c).joinToString(" | ", prefix = "| ", postfix = " |") { "Column $it" }
                        tableBuilder.appendLine(headerRow)
                        for (rowIdx in 1..r) {
                            val rowStr = (1..c).joinToString(" | ", prefix = "| ", postfix = " |") { "Cell ${rowIdx}.$it" }
                            tableBuilder.appendLine(rowStr)
                        }
                        pushEditContent(editableContent.trimEnd() + "\n" + tableBuilder.toString())
                        showInsertTableDialog = false
                    }
                ) {
                    Text("Insert Table", fontWeight = FontWeight.Bold, color = DocFlowTokens.AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInsertTableDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showInsertLinkDialog) {
        AlertDialog(
            onDismissRequest = { showInsertLinkDialog = false },
            title = { Text("Insert Link") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)) {
                    OutlinedTextField(
                        value = linkTextInput,
                        onValueChange = { linkTextInput = it },
                        label = { Text("Display text") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = linkUrlInput,
                        onValueChange = { linkUrlInput = it },
                        label = { Text("URL") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val label = linkTextInput.ifBlank { linkUrlInput }
                        pushEditContent("$editableContent [$label]($linkUrlInput) ")
                        showInsertLinkDialog = false
                    }
                ) {
                    Text("Insert", fontWeight = FontWeight.Bold, color = DocFlowTokens.AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInsertLinkDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showHeaderFooterDialog) {
        AlertDialog(
            onDismissRequest = { showHeaderFooterDialog = false },
            title = { Text("Header, Footer & Page Numbers") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)) {
                    OutlinedTextField(
                        value = pageHeaderText,
                        onValueChange = { pageHeaderText = it },
                        label = { Text("Page Header Text") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pageFooterText,
                        onValueChange = { pageFooterText = it },
                        label = { Text("Page Footer Text") },
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Page Numbers")
                        Switch(checked = showPageNumbers, onCheckedChange = { showPageNumbers = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHeaderFooterDialog = false }) {
                    Text("Apply", fontWeight = FontWeight.Bold, color = DocFlowTokens.AccentBlue)
                }
            }
        )
    }

    // Comments Sheet (Review Tab > Local Comments)
    if (showCommentsSheet) {
        DocFlowBottomSheet(
            onDismissRequest = { showCommentsSheet = false },
            palette = palette
        ) {
            Text(
                text = "Document Comments & Notes (${comments.size})",
                style = DocFlowTokens.sectionLabelStyle,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Text(
                text = "Stored on this device • Exportable to PDF",
                style = DocFlowTokens.captionStyle,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCommentBody,
                    onValueChange = { newCommentBody = it },
                    placeholder = { Text("Add a local review comment or note...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                DocFlowPrimaryButton(
                    text = "Add",
                    enabled = newCommentBody.isNotBlank(),
                    onClick = {
                        viewModel.addDocumentComment(
                            doc = doc,
                            anchorLineOrText = "Page ${currentPageIndex + 1}",
                            body = newCommentBody.trim(),
                            isSuggestion = false,
                            suggestedReplacement = ""
                        )
                        newCommentBody = ""
                    }
                )
            }

            Spacer(modifier = Modifier.height(DocFlowTokens.space2))

            comments.forEach { comment ->
                DocFlowListRow(
                    title = comment.body,
                    subtitle = "${comment.authorName} • ${comment.status} • Tap PDF icon to export note",
                    palette = palette,
                    onClick = {},
                    trailing = {
                        Row {
                            IconButton(
                                onClick = {
                                    viewModel.exportCommentOrNoteAsPdf(context, doc, comment)
                                    showCommentsSheet = false
                                }
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export Note to PDF", tint = DocFlowTokens.IconCircleRed)
                            }
                            IconButton(
                                onClick = { viewModel.deleteComment(comment.id) }
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete comment", tint = palette.textSecondary)
                            }
                        }
                    }
                )
            }
        }
    }

    // Export Individual Versions or Notes to PDF Sheet
    if (showExportVersionsNotesSheet) {
        val currentLiveSnapshot = remember(doc.id, versionsForDoc.size, editableContent) {
            DocumentVersionEntity(
                id = "ver_live_${doc.id}",
                documentId = doc.id,
                userId = viewModel.currentUserId,
                versionNumber = (versionsForDoc.size + 1),
                versionLabel = "Current Editor Snapshot",
                authorName = authorMetadata,
                stateVector = "sv-${versionsForDoc.size + 1}",
                snapshotContent = editableContent,
                byteSize = editableContent.length.toLong()
            )
        }
        val allSelectableVersions = remember(currentLiveSnapshot, versionsForDoc) {
            listOf(currentLiveSnapshot) + versionsForDoc.take(6)
        }
        val selectedVerIds = remember { mutableStateListOf(currentLiveSnapshot.id) }
        val selectedCommentIds = remember { mutableStateListOf<String>() }

        DocFlowBottomSheet(
            onDismissRequest = { showExportVersionsNotesSheet = false },
            palette = palette
        ) {
            Text(
                text = "Export Version or Note to PDF",
                style = DocFlowTokens.sectionLabelStyle,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Text(
                text = "Select individual document versions or notes to generate a standalone PDF stored on this device.",
                style = DocFlowTokens.captionStyle,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))

            allSelectableVersions.forEachIndexed { idx, ver ->
                val isChecked = selectedVerIds.contains(ver.id)
                DocFlowListRow(
                    title = "v${ver.versionNumber}: ${ver.versionLabel}",
                    subtitle = "Author: ${ver.authorName} • ${ver.byteSize} bytes • Stored on this device",
                    palette = palette,
                    modifier = Modifier.testTag("editor_export_version_row_$idx"),
                    onClick = {
                        if (isChecked) selectedVerIds.remove(ver.id) else selectedVerIds.add(ver.id)
                    },
                    leading = {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (!selectedVerIds.contains(ver.id)) selectedVerIds.add(ver.id)
                                } else {
                                    selectedVerIds.remove(ver.id)
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = DocFlowTokens.AccentBlue)
                        )
                    }
                )
            }

            comments.take(5).forEachIndexed { idx, note ->
                val isChecked = selectedCommentIds.contains(note.id)
                DocFlowListRow(
                    title = "Note: ${note.body.take(40)}",
                    subtitle = "By ${note.authorName} • Anchor: ${note.anchorLineOrText}",
                    palette = palette,
                    modifier = Modifier.testTag("editor_export_note_row_$idx"),
                    onClick = {
                        if (isChecked) selectedCommentIds.remove(note.id) else selectedCommentIds.add(note.id)
                    },
                    leading = {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (!selectedCommentIds.contains(note.id)) selectedCommentIds.add(note.id)
                                } else {
                                    selectedCommentIds.remove(note.id)
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = DocFlowTokens.AccentBlue)
                        )
                    }
                )
            }

            val chosenVers = allSelectableVersions.filter { selectedVerIds.contains(it.id) }
            val chosenNotes = comments.filter { selectedCommentIds.contains(it.id) }
            val totalCount = chosenVers.size + chosenNotes.size

            Spacer(modifier = Modifier.height(DocFlowTokens.space3))
            DocFlowPrimaryButton(
                text = if (totalCount > 0) "Generate PDF ($totalCount selected)" else "Select Version or Note",
                enabled = totalCount > 0,
                onClick = {
                    viewModel.exportSelectedVersionsAndNotesAsPdf(
                        context = context,
                        doc = doc,
                        selectedVersions = chosenVers,
                        selectedNotes = chosenNotes,
                        customTitle = "$documentTitle (Exported Selection)",
                        launchShareSheet = true,
                        onExported = { _, outTitle, _ ->
                            showExportVersionsNotesSheet = false
                            viewerToast = "Exported '$outTitle' to PDF • Stored on this device"
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_export_selected_pdf_button")
            )
        }
    }

    // Export PDF with Watermark Sheet
    if (showWatermarkPdfExportSheet) {
        PdfWatermarkExportSheet(
            palette = palette,
            documentTitle = documentTitle,
            previewTextSnippet = editableContent.take(120),
            initialEnabled = true,
            confirmLabel = "Export PDF (Stored on this device)",
            onDismiss = { showWatermarkPdfExportSheet = false },
            onConfirmExport = { wmConfig ->
                showWatermarkPdfExportSheet = false
                viewModel.convertFileToPdfAndSave(
                    context = context,
                    sourceDoc = doc.copy(title = documentTitle, content = editableContent),
                    watermarkConfig = wmConfig
                ) {
                    viewerToast = "Exported PDF${if (wmConfig.enabled) " with '${wmConfig.text}' watermark" else ""} on this device"
                }
            }
        )
    }

    // AI Mic Dictation Sheet
    if (showAiMicSheet) {
        AiMicDictationBottomSheet(
            palette = palette,
            initialText = "",
            confirmButtonLabel = "Insert into Document",
            onDismiss = { showAiMicSheet = false },
            onConfirmCleanText = { cleaned ->
                isEditingMode = true
                pushEditContent(
                    if (editableContent.isBlank()) cleaned else "${editableContent.trimEnd()}\n$cleaned"
                )
                showAiMicSheet = false
                viewerToast = "Inserted noise-cleaned voice text"
            }
        )
    }

    // Document Details Bottom Sheet (Requirement 5: Full Document Details + Editable Author/Title + Stored on this device)
    if (showDetailsSheet) {
        val createdDateStr = remember(doc.createdAt) {
            val ms = (doc.createdAt?.seconds ?: 0L) * 1000L
            SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(
                Date(if (ms > 0L) ms else System.currentTimeMillis())
            )
        }
        val modifiedDateStr = remember(cachedEntity?.updatedAtMillis) {
            val ms = cachedEntity?.updatedAtMillis ?: System.currentTimeMillis()
            SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(Date(ms))
        }
        val lastOpenedStr = remember(cachedEntity?.lastAccessedAtMillis) {
            val ms = cachedEntity?.lastAccessedAtMillis ?: System.currentTimeMillis()
            SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(Date(ms))
        }
        val sizeKb = String.format("%.1f KB", doc.fileSize.coerceAtLeast(1024L) / 1024.0)

        DocFlowBottomSheet(
            onDismissRequest = { showDetailsSheet = false },
            palette = palette
        ) {
            Text(
                text = "Document Details",
                style = DocFlowTokens.sectionLabelStyle,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))

            DetailItemRow("Name", documentTitle, palette)
            DetailItemRow("File Type", ".${doc.fileType.lowercase()} (${doc.fileType.uppercase()})", palette)
            DetailItemRow("Size", sizeKb, palette)
            DetailItemRow("Pages", "$totalPages page(s)", palette)
            DetailItemRow("Words & Characters", "$wordCount words • $charCount characters", palette)
            DetailItemRow("Created", createdDateStr, palette)
            DetailItemRow("Modified", modifiedDateStr, palette)
            DetailItemRow("Last Opened", lastOpenedStr, palette)
            DetailItemRow("Folder / Category", "${doc.folderId} • ${doc.category}", palette)
            DetailItemRow("Tags", (cachedEntity?.tags ?: "").ifBlank { "None" }, palette)
            DetailItemRow("Location", "Stored on this device", palette)
            DetailItemRow("Encrypted", if (doc.isEncrypted || cachedEntity?.isPrivateSafe == true) "Yes (AES-256-GCM)" else "No", palette)

            Spacer(modifier = Modifier.height(DocFlowTokens.space2))

            OutlinedTextField(
                value = authorMetadata,
                onValueChange = { authorMetadata = it },
                label = { Text("Author Metadata (Stored on this device)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DocFlowSecondaryButton(
                    text = "Rename",
                    onClick = {
                        showDetailsSheet = false
                        showRenameDialog = true
                    },
                    palette = palette
                )
                DocFlowSecondaryButton(
                    text = "Add Tags",
                    onClick = {
                        showDetailsSheet = false
                        showTagsDialog = true
                    },
                    palette = palette
                )
                DocFlowSecondaryButton(
                    text = "Private Safe",
                    onClick = {
                        showDetailsSheet = false
                        viewModel.moveToPrivateSafe(doc.id, "1234")
                        onBack()
                    },
                    palette = palette
                )
                DocFlowPrimaryButton(
                    text = "Export PDF + Watermark",
                    onClick = {
                        showDetailsSheet = false
                        showWatermarkPdfExportSheet = true
                    }
                )
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename document") },
            text = {
                OutlinedTextField(
                    value = documentTitle,
                    onValueChange = { documentTitle = it },
                    label = { Text("Document name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateDocumentContent(
                            doc = doc,
                            newTitle = documentTitle.trim().ifBlank { doc.title },
                            newFolderId = doc.folderId,
                            newCategory = doc.category,
                            plainContent = editableContent,
                            isEncrypted = doc.isEncrypted,
                            password = null,
                            encryptionKeyHint = doc.encryptionKeyHint,
                            isOfflineCached = true
                        )
                        showRenameDialog = false
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = DocFlowTokens.AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Tags Dialog
    if (showTagsDialog) {
        AlertDialog(
            onDismissRequest = { showTagsDialog = false },
            title = { Text("Edit Document Tags") },
            text = {
                OutlinedTextField(
                    value = tagInputText,
                    onValueChange = { tagInputText = it },
                    label = { Text("Tags (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateDocumentCategoryAndTags(doc, doc.category, tagInputText)
                        showTagsDialog = false
                    }
                ) {
                    Text("Save Tags", fontWeight = FontWeight.Bold, color = DocFlowTokens.AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTagsDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Unsaved Changes Prompt
    if (showUnsavedExitDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedExitDialog = false },
            title = { Text("Save changes before leaving?") },
            text = { Text("You have unsaved edits in '$documentTitle'.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateDocumentContent(
                            doc = doc,
                            newTitle = documentTitle,
                            newFolderId = doc.folderId,
                            newCategory = doc.category,
                            plainContent = editableContent,
                            isEncrypted = doc.isEncrypted,
                            password = null,
                            encryptionKeyHint = doc.encryptionKeyHint,
                            isOfflineCached = true
                        )
                        hasUnsavedChanges = false
                        showUnsavedExitDialog = false
                        onBack()
                    }
                ) {
                    Text("Save & Exit", fontWeight = FontWeight.Bold, color = DocFlowTokens.AccentBlue)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUnsavedExitDialog = false
                        onBack()
                    }
                ) {
                    Text("Discard", color = DocFlowTokens.IconCircleRed)
                }
            }
        )
    }

    // Page Thumbnail Grid Sheet
    if (showPageGridSheet) {
        DocFlowBottomSheet(
            onDismissRequest = { showPageGridSheet = false },
            palette = palette
        ) {
            Text(
                text = "Pages ($totalPages)",
                style = DocFlowTokens.sectionLabelStyle,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
            ) {
                for (i in 0 until totalPages) {
                    val isCurrent = i == currentPageIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(96.dp)
                            .clip(DocFlowTokens.thumbnailShape)
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) DocFlowTokens.AccentBlue else palette.divider,
                                shape = DocFlowTokens.thumbnailShape
                            )
                            .background(Color.White)
                            .clickable {
                                currentPageIndex = i
                                showPageGridSheet = false
                            }
                            .padding(DocFlowTokens.space2)
                    ) {
                        Text(
                            text = contentPages.getOrNull(i)?.take(60) ?: "Page ${i + 1}",
                            fontSize = 9.sp,
                            color = Color.DarkGray,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.height(64.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Page ${i + 1}",
                            fontSize = 12.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) DocFlowTokens.AccentBlue else Color.Black
                        )
                    }
                }
            }
        }
    }

    // Summarise Sheet
    if (showSummariseSheet) {
        val summary: ExtractiveSummaryResult = remember(editableContent) {
            OnDeviceAiAndBackupEngine.summarizeTextExtractive(editableContent)
        }
        DocFlowBottomSheet(
            onDismissRequest = { showSummariseSheet = false },
            palette = palette
        ) {
            Text(
                text = "AI Summarise — ${doc.title}",
                style = DocFlowTokens.sectionLabelStyle,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Text(
                text = "On-device TF-IDF & TextRank • ${summary.wordCount} words • Stored on this device",
                style = DocFlowTokens.captionStyle,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            summary.bulletPoints.forEach { bullet ->
                Text(
                    text = "• $bullet",
                    style = DocFlowTokens.bodyStyle,
                    color = palette.textPrimary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(DocFlowTokens.space3))
            Row(horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)) {
                DocFlowSecondaryButton(
                    text = "Copy",
                    onClick = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cb?.setPrimaryClip(ClipData.newPlainText("Summary", summary.executiveSummary))
                        Toast.makeText(context, "Summary copied", Toast.LENGTH_SHORT).show()
                    },
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                DocFlowPrimaryButton(
                    text = "Save as Note",
                    onClick = {
                        viewModel.saveSummaryAsNote(doc, summary) {
                            showSummariseSheet = false
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Translate Sheet
    if (showTranslateSheet) {
        var targetLang by remember { mutableStateOf(TranslationLanguage.HINDI) }
        var packs by remember { mutableStateOf(OnDeviceAiAndBackupEngine.getDownloadedLanguagePacks(context)) }
        val isReady = packs.contains(targetLang.code)
        val translated = remember(editableContent, targetLang, isReady) {
            if (isReady) {
                OnDeviceAiAndBackupEngine.translateDocumentOffline(doc.title, editableContent, targetLang)
            } else null
        }

        DocFlowBottomSheet(
            onDismissRequest = { showTranslateSheet = false },
            palette = palette
        ) {
            Text(
                text = "Translate Document (Offline)",
                style = DocFlowTokens.sectionLabelStyle,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TranslationLanguage.entries.forEach { lang ->
                    FilterChip(
                        selected = targetLang == lang,
                        onClick = { targetLang = lang },
                        label = { Text("${lang.displayName} (${lang.nativeName})") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            if (!isReady) {
                DocFlowPrimaryButton(
                    text = "Download ${targetLang.displayName} Pack (${targetLang.packSizeMb})",
                    onClick = {
                        OnDeviceAiAndBackupEngine.markLanguagePackDownloaded(context, targetLang.code)
                        packs = OnDeviceAiAndBackupEngine.getDownloadedLanguagePacks(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (translated != null) {
                Text(
                    text = translated.take(400),
                    style = DocFlowTokens.bodyStyle,
                    color = palette.textPrimary,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space3))
                DocFlowPrimaryButton(
                    text = "Save Translated Copy",
                    onClick = {
                        viewModel.saveTranslationDocument(
                            context = context,
                            sourceDoc = doc,
                            translatedText = translated,
                            targetLang = targetLang,
                            exportAsPdf = true
                        ) {
                            showTranslateSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MsWordBottomRibbon(
    activeTab: WordRibbonTab,
    onSelectTab: (WordRibbonTab) -> Unit,
    palette: DocFlowPalette,
    selectedFontFamily: String,
    onSelectFontFamily: (String) -> Unit,
    selectedFontSizePt: Int,
    onChangeFontSize: (Int) -> Unit,
    isBold: Boolean,
    onToggleBold: () -> Unit,
    isItalic: Boolean,
    onToggleItalic: () -> Unit,
    isUnderline: Boolean,
    onToggleUnderline: () -> Unit,
    isStrikethrough: Boolean,
    onToggleStrikethrough: () -> Unit,
    isSubscript: Boolean,
    onToggleSubscript: () -> Unit,
    isSuperscript: Boolean,
    onToggleSuperscript: () -> Unit,
    onClearFormatting: () -> Unit,
    textColorHex: String,
    onSelectTextColor: (String) -> Unit,
    textAlignMode: String,
    onSelectTextAlign: (String) -> Unit,
    lineSpacingMultiplier: Float,
    onSelectLineSpacing: (Float) -> Unit,
    paragraphStyleName: String,
    onSelectParagraphStyle: (String) -> Unit,
    onInsertBullet: () -> Unit,
    onInsertNumbered: () -> Unit,
    onIndent: () -> Unit,
    onOutdent: () -> Unit,
    onInsertTable: () -> Unit,
    onInsertImage: () -> Unit,
    onInsertLink: () -> Unit,
    onInsertPageBreak: () -> Unit,
    onEditHeaderFooter: () -> Unit,
    onInsertDate: () -> Unit,
    onInsertHorizontalLine: () -> Unit,
    pageSizeOption: String,
    onSelectPageSize: (String) -> Unit,
    pageOrientation: String,
    onSelectOrientation: (String) -> Unit,
    pageMargins: String,
    onSelectMargins: (String) -> Unit,
    showLineNumbers: Boolean,
    onToggleLineNumbers: () -> Unit,
    onOpenFindReplace: () -> Unit,
    onRunSpellCheck: () -> Unit,
    onOpenComments: () -> Unit,
    onOpenExportWatermarkPdf: () -> Unit,
    zoomScale: Float,
    onSelectZoom: (Float) -> Unit,
    showRuler: Boolean,
    onToggleRuler: () -> Unit,
    showFormattingMarks: Boolean,
    onToggleFormattingMarks: () -> Unit,
    isPrintLayoutMode: Boolean,
    onToggleLayoutMode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface)
            .testTag("ms_word_ribbon_bar")
    ) {
        DocFlowDivider(palette = palette)

        // Ribbon Tab Selector: Home | Insert | Layout | Review | View
        ScrollableTabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = palette.surface,
            contentColor = DocFlowTokens.AccentBlue,
            edgePadding = DocFlowTokens.screenHorizontalMargin,
            divider = { DocFlowDivider(palette = palette) }
        ) {
            WordRibbonTab.entries.forEach { tab ->
                val isSelected = activeTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { onSelectTab(tab) },
                    modifier = Modifier.testTag("word_ribbon_tab_${tab.name.lowercase()}"),
                    text = {
                        Text(
                            text = tab.label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) DocFlowTokens.AccentBlue else palette.textSecondary
                        )
                    }
                )
            }
        }

        // Active Ribbon Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DocFlowTokens.screenHorizontalMargin, vertical = DocFlowTokens.space2)
        ) {
            when (activeTab) {
                WordRibbonTab.HOME -> {
                    // Row 1: Font Family, Font Size, Bold, Italic, Underline, Strikethrough, Sub/Superscript, Clear
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Roboto", "Inter", "Serif", "Merriweather", "JetBrains Mono").forEach { font ->
                            FilterChip(
                                selected = selectedFontFamily == font,
                                onClick = { onSelectFontFamily(font) },
                                label = { Text(font, fontSize = 11.sp) }
                            )
                        }
                        FilterChip(
                            selected = false,
                            onClick = { onChangeFontSize(selectedFontSizePt - 2) },
                            label = { Text("A-", fontWeight = FontWeight.Bold) }
                        )
                        Text("${selectedFontSizePt}pt", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        FilterChip(
                            selected = false,
                            onClick = { onChangeFontSize(selectedFontSizePt + 2) },
                            label = { Text("A+", fontWeight = FontWeight.Bold) }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onToggleBold) {
                            Icon(Icons.Filled.FormatBold, contentDescription = "Bold", tint = if (isBold) DocFlowTokens.AccentBlue else palette.textPrimary)
                        }
                        IconButton(onClick = onToggleItalic) {
                            Icon(Icons.Filled.FormatItalic, contentDescription = "Italic", tint = if (isItalic) DocFlowTokens.AccentBlue else palette.textPrimary)
                        }
                        IconButton(onClick = onToggleUnderline) {
                            Icon(Icons.Filled.FormatUnderlined, contentDescription = "Underline", tint = if (isUnderline) DocFlowTokens.AccentBlue else palette.textPrimary)
                        }
                        IconButton(onClick = onToggleStrikethrough) {
                            Icon(Icons.Filled.FormatStrikethrough, contentDescription = "Strikethrough", tint = if (isStrikethrough) DocFlowTokens.AccentBlue else palette.textPrimary)
                        }
                        IconButton(onClick = onToggleSubscript) {
                            Icon(Icons.Filled.Subscript, contentDescription = "Subscript", tint = if (isSubscript) DocFlowTokens.AccentBlue else palette.textPrimary)
                        }
                        IconButton(onClick = onToggleSuperscript) {
                            Icon(Icons.Filled.Superscript, contentDescription = "Superscript", tint = if (isSuperscript) DocFlowTokens.AccentBlue else palette.textPrimary)
                        }
                        IconButton(onClick = onClearFormatting) {
                            Icon(Icons.Filled.FormatClear, contentDescription = "Clear formatting", tint = palette.textSecondary)
                        }
                        // Alignment
                        IconButton(onClick = { onSelectTextAlign("LEFT") }) {
                            Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = "Align Left", tint = if (textAlignMode == "LEFT") DocFlowTokens.AccentBlue else palette.textPrimary)
                        }
                        IconButton(onClick = { onSelectTextAlign("CENTER") }) {
                            Icon(Icons.Filled.FormatAlignCenter, contentDescription = "Align Center", tint = if (textAlignMode == "CENTER") DocFlowTokens.AccentBlue else palette.textPrimary)
                        }
                        IconButton(onClick = { onSelectTextAlign("RIGHT") }) {
                            Icon(Icons.AutoMirrored.Filled.FormatAlignRight, contentDescription = "Align Right", tint = if (textAlignMode == "RIGHT") DocFlowTokens.AccentBlue else palette.textPrimary)
                        }
                        IconButton(onClick = { onSelectTextAlign("JUSTIFY") }) {
                            Icon(Icons.Filled.FormatAlignJustify, contentDescription = "Justify", tint = if (textAlignMode == "JUSTIFY") DocFlowTokens.AccentBlue else palette.textPrimary)
                        }
                        // Bullets, Numbered, Indent, Outdent
                        IconButton(onClick = onInsertBullet) {
                            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Bullet list", tint = palette.textPrimary)
                        }
                        IconButton(onClick = onInsertNumbered) {
                            Icon(Icons.Filled.FormatListNumbered, contentDescription = "Numbered list", tint = palette.textPrimary)
                        }
                        IconButton(onClick = onIndent) {
                            Icon(Icons.AutoMirrored.Filled.FormatIndentIncrease, contentDescription = "Indent", tint = palette.textPrimary)
                        }
                        IconButton(onClick = onOutdent) {
                            Icon(Icons.AutoMirrored.Filled.FormatIndentDecrease, contentDescription = "Outdent", tint = palette.textPrimary)
                        }
                    }

                    // Styles & Text Colors
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Normal", "Title", "Heading 1", "Heading 2", "Heading 3", "Quote", "Code").forEach { st ->
                            FilterChip(
                                selected = paragraphStyleName == st,
                                onClick = { onSelectParagraphStyle(st) },
                                label = { Text(st, fontSize = 11.sp) }
                            )
                        }
                        listOf("#111111", "#1F6BFF", "#E5483F", "#25C25A").forEach { hex ->
                            val c = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (textColorHex == hex) 2.dp else 1.dp,
                                        color = if (textColorHex == hex) DocFlowTokens.AccentBlue else palette.divider,
                                        shape = CircleShape
                                    )
                                    .clickable { onSelectTextColor(hex) }
                            )
                        }
                    }
                }

                WordRibbonTab.INSERT -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = onInsertTable,
                            label = { Text("Table (Rows×Cols)") },
                            leadingIcon = { Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = onInsertImage,
                            label = { Text("Image") },
                            leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = onInsertLink,
                            label = { Text("Link") },
                            leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = onInsertPageBreak,
                            label = { Text("Page Break") },
                            leadingIcon = { Icon(Icons.Filled.InsertPageBreak, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = onEditHeaderFooter,
                            label = { Text("Header / Footer / Page #") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = onInsertDate,
                            label = { Text("Date") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = onInsertHorizontalLine,
                            label = { Text("Horizontal Line") },
                            leadingIcon = { Icon(Icons.Filled.HorizontalRule, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                WordRibbonTab.LAYOUT -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("A4", "Letter", "Legal").forEach { sz ->
                            FilterChip(
                                selected = pageSizeOption == sz,
                                onClick = { onSelectPageSize(sz) },
                                label = { Text("Size: $sz", fontSize = 12.sp) }
                            )
                        }
                        listOf("Portrait", "Landscape").forEach { or ->
                            FilterChip(
                                selected = pageOrientation == or,
                                onClick = { onSelectOrientation(or) },
                                label = { Text(or, fontSize = 12.sp) }
                            )
                        }
                        listOf("Normal", "Narrow", "Wide").forEach { mg ->
                            FilterChip(
                                selected = pageMargins == mg,
                                onClick = { onSelectMargins(mg) },
                                label = { Text("Margin: $mg", fontSize = 12.sp) }
                            )
                        }
                        FilterChip(
                            selected = showLineNumbers,
                            onClick = onToggleLineNumbers,
                            label = { Text("Line Numbers", fontSize = 12.sp) }
                        )
                    }
                }

                WordRibbonTab.REVIEW -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = onOpenFindReplace,
                            label = { Text("Find & Replace") },
                            leadingIcon = { Icon(Icons.Filled.FindReplace, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = onRunSpellCheck,
                            label = { Text("Spell Check (EN + HI)") },
                            leadingIcon = { Icon(Icons.Filled.Spellcheck, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = onOpenComments,
                            label = { Text("Comments") },
                            leadingIcon = { Icon(Icons.Filled.Comment, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = onOpenExportWatermarkPdf,
                            label = { Text("Export PDF + Watermark") },
                            leadingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                WordRibbonTab.VIEW -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.85f to "85%", 1.0f to "100%", 1.25f to "125%", 1.5f to "150%").forEach { (z, lbl) ->
                            FilterChip(
                                selected = zoomScale == z,
                                onClick = { onSelectZoom(z) },
                                label = { Text("Zoom $lbl", fontSize = 12.sp) }
                            )
                        }
                        FilterChip(
                            selected = showRuler,
                            onClick = onToggleRuler,
                            label = { Text("Ruler", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = showFormattingMarks,
                            onClick = onToggleFormattingMarks,
                            label = { Text("Formatting Marks (¶)", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = isPrintLayoutMode,
                            onClick = onToggleLayoutMode,
                            label = { Text(if (isPrintLayoutMode) "Print Layout" else "Reflow View", fontSize = 12.sp) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerBottomActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    palette: DocFlowPalette,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(DocFlowTokens.buttonShape)
            .background(if (active) DocFlowTokens.AccentBlue.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) DocFlowTokens.AccentBlue else palette.textPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = if (active) DocFlowTokens.AccentBlue else palette.textSecondary
        )
    }
}

@Composable
private fun DetailItemRow(
    label: String,
    value: String,
    palette: DocFlowPalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = DocFlowTokens.captionStyle, color = palette.textSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = DocFlowTokens.captionStyle,
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun triggerAndroidPrint(
    context: Context,
    title: String,
    content: String
) {
    runCatching {
        val webView = WebView(context)
        val escaped = content
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n", "<br/>")
        val html = "<html><body style='font-family:sans-serif;padding:24px;'><h2>$title</h2><p>$escaped</p></body></html>"
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val adapter = webView.createPrintDocumentAdapter(title)
                printManager?.print(title, adapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
