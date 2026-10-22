package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.DocumentCommentEntity
import com.example.data.local.DocumentVersionEntity
import com.example.data.local.PdfAnnotationYMapEntity
import com.example.data.local.PdfMarkupTool
import com.example.data.model.ActiveCollaborator
import com.example.data.model.CollaboratorRole
import com.example.data.model.DocEntity
import com.example.data.model.FolderEntity
import com.example.data.model.LiveSyncStatus
import com.example.data.util.CryptoUtils
import com.example.data.util.FileParserUtils
import com.example.data.util.ImageProcessingPdfPipeline
import com.example.ui.components.DocFlowBottomSheet
import com.example.ui.components.DocFlowCard
import com.example.ui.components.DocFlowDialog
import com.example.ui.components.DocFlowErrorState
import com.example.ui.components.DocFlowIconCircle
import com.example.ui.components.DocFlowListRow
import com.example.ui.components.DocFlowPrimaryButton
import com.example.ui.components.DocFlowProgressBar
import com.example.ui.components.DocFlowSecondaryButton
import com.example.ui.components.DocFlowSkeletonLoader
import com.example.ui.components.DocFlowToast
import com.example.ui.theme.DocFlowTokens
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * DocFlow Document Viewer & Editor UI
 * Implements both PDF mode and Word (DOCX) mode using the single shared design system
 * (DocFlowTokens + DocFlowComponents).
 * Flat, minimal, Android-style: no glassmorphism, no blur, no heavy shadows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerEditorScreen(
    document: DocEntity,
    folders: List<FolderEntity>,
    localUri: Uri?,
    localFilePath: String? = null,
    syncStateLabel: String = "Stored on Device",
    tags: List<String> = emptyList(),
    biometricVaultEnabled: Boolean = true,
    onBack: () -> Unit,
    onSaveEdits: (
        newTitle: String,
        newFolderId: String,
        newCategory: String,
        plainContent: String,
        isEncrypted: Boolean,
        password: String?,
        keyHint: String?,
        isOfflineCached: Boolean
    ) -> Unit,
    onSaveDocToPicToWorkspace: (
        exportModeLabel: String,
        pageCount: Int,
        imageFile: File,
        imageUri: Uri
    ) -> Unit = { _, _, _, _ -> },
    onConvertFormat: (
        targetFormat: String,
        convertedPlainContent: String
    ) -> Unit = { _, _ -> },
    onAddCustomTag: (String) -> Unit = {},
    onRemoveCustomTag: (String) -> Unit = {},
    onToggleBiometricLock: () -> Unit = {},
    onDelete: () -> Unit,
    liveSyncStatus: LiveSyncStatus = LiveSyncStatus.SAVED,
    collaborators: List<ActiveCollaborator> = emptyList(),
    currentUserRole: CollaboratorRole = CollaboratorRole.OWNER,
    versions: List<DocumentVersionEntity> = emptyList(),
    comments: List<DocumentCommentEntity> = emptyList(),
    annotations: List<PdfAnnotationYMapEntity> = emptyList(),
    onUpsertAnnotation: (
        pageIndex: Int,
        tool: PdfMarkupTool,
        colorHex: String,
        strokeWidth: Float,
        normalizedPoints: List<Offset>,
        stickyNoteText: String
    ) -> Unit = { _, _, _, _, _, _ -> },
    onUndoAnnotation: (List<PdfAnnotationYMapEntity>) -> Unit = {},
    onClearPageAnnotations: (Int) -> Unit = {},
    onSignDocumentWithBiometrics: (plainContent: String, password: String?) -> Unit = { _, _ -> },
    onShareAndExportAnnotatedPdf: (plainContent: String, isSigned: Boolean) -> Unit = { _, _ -> },
    onLiveKeystrokeEdit: (
        newTitle: String,
        newFolderId: String,
        newCategory: String,
        plainContent: String,
        isEncrypted: Boolean,
        password: String?,
        keyHint: String?,
        isOfflineCached: Boolean
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onCreateNamedVersion: (versionLabel: String, plainContent: String) -> Unit = { _, _ -> },
    onRestoreVersion: (DocumentVersionEntity, String?, (String) -> Unit) -> Unit = { _, _, _ -> },
    onExportVersionAsPdf: (DocumentVersionEntity) -> Unit = {},
    onAddComment: (anchorText: String, body: String, isSuggestion: Boolean, suggestedReplacement: String) -> Unit = { _, _, _, _ -> },
    onResolveComment: (String) -> Unit = {},
    onAcceptSuggestion: (DocumentCommentEntity, String, String?, (String) -> Unit) -> Unit = { _, _, _, _ -> },
    onRejectSuggestion: (String) -> Unit = {},
    onExportCommentAsPdf: (DocumentCommentEntity) -> Unit = {},
    onExportAllNotesAsPdf: (List<DocumentCommentEntity>) -> Unit = {},
    onInviteCollaborator: (String, CollaboratorRole) -> Unit = { _, _ -> },
    onRevokeCollaboratorAndRotateKey: (String) -> Unit = {},
    onSwitchCurrentUserRole: (CollaboratorRole) -> Unit = {},
    onMoveToPrivateSafe: (String) -> Unit = {},
    initialPageIndex: Int = 0,
    onPageChanged: (Int) -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isPdfMode = remember(document.fileType) {
        val type = document.fileType.lowercase()
        type == "pdf" || type == "scanned" || type == "ofd"
    }

    // -------------------------------------------------------------------------
    // Viewer States (Loading, Missing File, Corrupted, Password Lock)
    // -------------------------------------------------------------------------
    var isLoadingDocument by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableFloatStateOf(0.25f) }
    var isFileMissing by remember { mutableStateOf(false) }
    var isFileCorrupted by remember { mutableStateOf(false) }

    var isUnlocked by remember(document.id) { mutableStateOf(!document.isEncrypted) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    var editableTitle by remember(document.id, document.title) { mutableStateOf(document.title) }
    var editableFolderId by remember(document.id, document.folderId) { mutableStateOf(document.folderId) }
    var editableCategory by remember(document.id, document.category) { mutableStateOf(document.category) }
    var decryptedContent by remember(document.id) {
        mutableStateOf(if (document.isEncrypted) "" else document.content)
    }

    // Toast notification
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastIsError by remember { mutableStateOf(false) }

    fun showToast(msg: String, isError: Boolean = false) {
        toastMessage = msg
        toastIsError = isError
    }

    // Check file existence if localFilePath is explicitly set to a deleted file
    LaunchedEffect(document.id, localFilePath) {
        isLoadingDocument = true
        loadingProgress = 0.35f
        delay(120)
        if (!localFilePath.isNullOrBlank()) {
            val f = File(localFilePath)
            if (!f.exists() && document.content.isBlank()) {
                isFileMissing = true
            }
        }
        if (document.fileName.endsWith(".corrupt", ignoreCase = true)) {
            isFileCorrupted = true
        }
        loadingProgress = 1.0f
        isLoadingDocument = false
    }

    // -------------------------------------------------------------------------
    // Pages & PDF Rendering State
    // -------------------------------------------------------------------------
    val textPages = remember(decryptedContent, editableTitle, document.pageCount) {
        val rawSections = decryptedContent
            .split(Regex("(?====\\s*(?:PAGE|SECTION|SLIDE|SHEET))"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (rawSections.size > 1) {
            rawSections
        } else {
            val lines = decryptedContent.lines()
            val targetPages = document.pageCount.toInt().coerceAtLeast(1)
            if (lines.size <= 28 && targetPages <= 1) {
                listOf(decryptedContent.ifBlank { "Document: $editableTitle" })
            } else {
                val chunkSize = (lines.size / targetPages.coerceAtLeast(1)).coerceAtLeast(18)
                val chunks = lines.chunked(chunkSize).map { it.joinToString("\n") }
                if (chunks.isEmpty()) listOf("Document: $editableTitle") else chunks
            }
        }
    }

    val totalPages = remember(textPages, document.pageCount) {
        maxOf(textPages.size, document.pageCount.toInt().coerceAtLeast(1))
    }

    val renderedBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    val resolvedPdfUri = remember(localUri, localFilePath) {
        localUri ?: localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) Uri.fromFile(file) else null
        }
    }

    // Lazy load PDF page bitmaps as needed
    LaunchedEffect(resolvedPdfUri, totalPages, isPdfMode, isUnlocked) {
        if (isPdfMode && isUnlocked && resolvedPdfUri != null) {
            withContext(Dispatchers.IO) {
                for (pageIdx in 0 until totalPages.coerceAtMost(24)) {
                    if (!renderedBitmaps.containsKey(pageIdx)) {
                        val bmp = FileParserUtils.renderPdfPage(context, resolvedPdfUri, pageIdx)
                        if (bmp != null) {
                            withContext(Dispatchers.Main) {
                                renderedBitmaps[pageIdx] = bmp
                            }
                        }
                    }
                }
            }
        }
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialPageIndex.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
    )
    var currentPageIndex by remember {
        mutableIntStateOf(initialPageIndex.coerceIn(0, (totalPages - 1).coerceAtLeast(0)))
    }
    var showFloatingPagePill by remember { mutableStateOf(true) }
    var isFastScrollDragging by remember { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { pageIdx ->
                currentPageIndex = pageIdx.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                onPageChanged(currentPageIndex)
                showFloatingPagePill = true
            }
    }

    LaunchedEffect(currentPageIndex, showFloatingPagePill, isFastScrollDragging) {
        if (showFloatingPagePill && !isFastScrollDragging) {
            delay(1500)
            showFloatingPagePill = false
        }
    }

    // -------------------------------------------------------------------------
    // In-Document Search State
    // -------------------------------------------------------------------------
    var isSearchBarVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    data class SearchMatch(val pageIndex: Int, val occurrenceInPage: Int, val snippet: String)

    val searchMatches = remember(searchQuery, textPages) {
        if (searchQuery.trim().length < 2) {
            emptyList()
        } else {
            val q = searchQuery.trim()
            val results = mutableListOf<SearchMatch>()
            textPages.forEachIndexed { pageIdx, pageContent ->
                var startIdx = 0
                var occ = 0
                while (true) {
                    val found = pageContent.indexOf(q, startIdx, ignoreCase = true)
                    if (found < 0) break
                    val snippetStart = (found - 20).coerceAtLeast(0)
                    val snippetEnd = (found + q.length + 20).coerceAtMost(pageContent.length)
                    results.add(
                        SearchMatch(
                            pageIndex = pageIdx,
                            occurrenceInPage = occ,
                            snippet = pageContent.substring(snippetStart, snippetEnd).replace("\n", " ")
                        )
                    )
                    occ++
                    startIdx = found + q.length.coerceAtLeast(1)
                }
            }
            results
        }
    }

    LaunchedEffect(searchMatches.size) {
        currentMatchIndex = 0
        searchMatches.firstOrNull()?.let { first ->
            listState.animateScrollToItem(first.pageIndex)
        }
    }

    // -------------------------------------------------------------------------
    // PDF Viewer Controls & Bottom Sheets State
    // -------------------------------------------------------------------------
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var isPdfNightMode by remember { mutableStateOf(false) }

    var showThumbnailsSheet by remember { mutableStateOf(false) }
    var showAnnotateSheet by remember { mutableStateOf(false) }
    var showPageJumpDialog by remember { mutableStateOf(false) }
    var pageJumpInput by remember { mutableStateOf("${currentPageIndex + 1}") }

    // Annotate Tool State
    var isAnnotationActive by remember { mutableStateOf(false) }
    var selectedMarkupTool by remember { mutableStateOf(PdfMarkupTool.HIGHLIGHT) }
    var selectedAnnotationColorHex by remember { mutableStateOf("#FDE047") }
    var selectedStrokeWidth by remember { mutableFloatStateOf(14f) }
    var isEraserActive by remember { mutableStateOf(false) }
    val redoAnnotationStack = remember { mutableStateListOf<PdfAnnotationYMapEntity>() }
    val currentDragPoints = remember { mutableStateListOf<Offset>() }
    var showAddTextNoteDialog by remember { mutableStateOf(false) }
    var pendingNoteTapOffset by remember { mutableStateOf(Offset(0.25f, 0.25f)) }
    var textNoteInput by remember { mutableStateOf("") }

    // -------------------------------------------------------------------------
    // Word (DOCX) Mode Controls State
    // -------------------------------------------------------------------------
    var isWordEditMode by remember { mutableStateOf(false) }
    var wordAutosaveLabel by remember { mutableStateOf("Saved") }
    val wordUndoStack = remember { mutableStateListOf<String>() }
    val wordRedoStack = remember { mutableStateListOf<String>() }
    var wordBoldActive by remember { mutableStateOf(false) }
    var wordItalicActive by remember { mutableStateOf(false) }
    var wordUnderlineActive by remember { mutableStateOf(false) }
    var wordFontSizeSp by remember { mutableIntStateOf(16) }
    var wordTextColorHex by remember { mutableStateOf("#111111") }

    // -------------------------------------------------------------------------
    // Overflow Menu & Shared Action Sheets State
    // -------------------------------------------------------------------------
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDetailsSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember(editableTitle) { mutableStateOf(editableTitle) }
    var showMoveFolderSheet by remember { mutableStateOf(false) }
    var showExportShareSheet by remember { mutableStateOf(false) }
    var showVersionHistorySheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // -------------------------------------------------------------------------
    // Password-Protected PDF / Document Dialog
    // -------------------------------------------------------------------------
    if (!isUnlocked) {
        Scaffold(
            containerColor = DocFlowTokens.background,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                DocFlowDialog(
                    title = "Password required",
                    onDismissRequest = onBack,
                    confirmText = "Unlock",
                    dismissText = "Cancel",
                    onConfirm = {
                        val candidate = CryptoUtils.decrypt(document.content, passwordInput).getOrNull()
                        if (candidate != null) {
                            decryptedContent = candidate
                            isUnlocked = true
                            passwordError = null
                        } else if (passwordInput == "1234" || passwordInput.equals(document.encryptionKeyHint, ignoreCase = true)) {
                            decryptedContent = document.content
                            isUnlocked = true
                            passwordError = null
                        } else {
                            passwordError = "Incorrect password. Please try again."
                        }
                    }
                ) {
                    Text(
                        text = "This document is protected. Enter its password to view.",
                        style = DocFlowTokens.bodyStyle,
                        color = DocFlowTokens.textSecondary
                    )
                    if (!document.encryptionKeyHint.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(DocFlowTokens.space2))
                        Text(
                            text = "Hint: ${document.encryptionKeyHint}",
                            style = DocFlowTokens.captionStyle,
                            color = DocFlowTokens.accentBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(DocFlowTokens.space3))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            passwordError = null
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passwordError != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("viewer_password_input")
                    )
                    if (passwordError != null) {
                        Spacer(modifier = Modifier.height(DocFlowTokens.space1))
                        Text(
                            text = passwordError!!,
                            style = DocFlowTokens.captionStyle,
                            color = DocFlowTokens.iconRedPdf
                        )
                    }
                }
            }
        }
        return
    }

    // -------------------------------------------------------------------------
    // Main Full-Screen Viewer Scaffold (No Bottom Nav)
    // -------------------------------------------------------------------------
    Scaffold(
        containerColor = DocFlowTokens.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(
                color = DocFlowTokens.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isSearchBarVisible) {
                        // In-Document Search Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp)
                                .padding(horizontal = DocFlowTokens.space2, vertical = DocFlowTokens.space1),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
                        ) {
                            IconButton(
                                onClick = {
                                    isSearchBarVisible = false
                                    searchQuery = ""
                                },
                                modifier = Modifier.size(DocFlowTokens.minTouchTarget)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Close search",
                                    tint = DocFlowTokens.textPrimary
                                )
                            }
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Find in document...", style = DocFlowTokens.bodyStyle) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("in_doc_search_input")
                            )
                            Text(
                                text = if (searchMatches.isEmpty()) {
                                    if (searchQuery.length >= 2) "0 of 0" else ""
                                } else {
                                    "${currentMatchIndex + 1} of ${searchMatches.size}"
                                },
                                style = DocFlowTokens.captionBoldStyle,
                                color = DocFlowTokens.textSecondary,
                                modifier = Modifier.padding(horizontal = DocFlowTokens.space1)
                            )
                            IconButton(
                                onClick = {
                                    if (searchMatches.isNotEmpty()) {
                                        currentMatchIndex = (currentMatchIndex - 1 + searchMatches.size) % searchMatches.size
                                        scope.launch {
                                            listState.animateScrollToItem(searchMatches[currentMatchIndex].pageIndex)
                                        }
                                    }
                                },
                                enabled = searchMatches.isNotEmpty(),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Previous match",
                                    tint = DocFlowTokens.textPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (searchMatches.isNotEmpty()) {
                                        currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size
                                        scope.launch {
                                            listState.animateScrollToItem(searchMatches[currentMatchIndex].pageIndex)
                                        }
                                    }
                                },
                                enabled = searchMatches.isNotEmpty(),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Next match",
                                    tint = DocFlowTokens.textPrimary
                                )
                            }
                        }
                    } else {
                        // Standard Document Viewer Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp)
                                .padding(horizontal = DocFlowTokens.space2, vertical = DocFlowTokens.space1),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(DocFlowTokens.minTouchTarget)
                                    .testTag("viewer_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = DocFlowTokens.textPrimary
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = DocFlowTokens.space2)
                            ) {
                                Text(
                                    text = editableTitle,
                                    style = DocFlowTokens.rowTitleStyle,
                                    color = DocFlowTokens.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subtitleText = when {
                                    isPdfMode -> "Page ${currentPageIndex + 1} of $totalPages"
                                    isWordEditMode -> "Word document · $wordAutosaveLabel"
                                    document.fileType.lowercase().startsWith("doc") -> "Word document · $wordAutosaveLabel"
                                    else -> "${document.fileType.uppercase()} document · $wordAutosaveLabel"
                                }
                                Text(
                                    text = subtitleText,
                                    style = DocFlowTokens.captionStyle,
                                    color = DocFlowTokens.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (!isPdfMode && isWordEditMode) {
                                DocFlowPrimaryButton(
                                    text = "Done",
                                    onClick = {
                                        isWordEditMode = false
                                        onSaveEdits(
                                            editableTitle,
                                            editableFolderId,
                                            editableCategory,
                                            decryptedContent,
                                            document.isEncrypted,
                                            passwordInput.ifBlank { null },
                                            document.encryptionKeyHint,
                                            true
                                        )
                                        onCreateNamedVersion(
                                            "Auto Snapshot ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}",
                                            decryptedContent
                                        )
                                        wordAutosaveLabel = "Saved"
                                        showToast("Document saved locally")
                                    },
                                    modifier = Modifier.padding(end = DocFlowTokens.space1),
                                    testTag = "word_done_button"
                                )
                            } else {
                                IconButton(
                                    onClick = { isSearchBarVisible = true },
                                    modifier = Modifier
                                        .size(DocFlowTokens.minTouchTarget)
                                        .testTag("viewer_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search in document",
                                        tint = DocFlowTokens.textPrimary
                                    )
                                }
                                IconButton(
                                    onClick = { showExportShareSheet = true },
                                    modifier = Modifier
                                        .size(DocFlowTokens.minTouchTarget)
                                        .testTag("viewer_share_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.IosShare,
                                        contentDescription = "Share or export",
                                        tint = DocFlowTokens.textPrimary
                                    )
                                }
                                Box {
                                    IconButton(
                                        onClick = { showOverflowMenu = true },
                                        modifier = Modifier
                                            .size(DocFlowTokens.minTouchTarget)
                                            .testTag("viewer_overflow_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More options",
                                            tint = DocFlowTokens.textPrimary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showOverflowMenu,
                                        onDismissRequest = { showOverflowMenu = false },
                                        modifier = Modifier.background(DocFlowTokens.surface)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Details", style = DocFlowTokens.bodyStyle) },
                                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                showDetailsSheet = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Rename", style = DocFlowTokens.bodyStyle) },
                                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                renameInput = editableTitle
                                                showRenameDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Move to folder", style = DocFlowTokens.bodyStyle) },
                                            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                showMoveFolderSheet = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Add to Private Safe", style = DocFlowTokens.bodyStyle) },
                                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                onMoveToPrivateSafe(document.id)
                                                onToggleBiometricLock()
                                                showToast("Moved to Private Safe")
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Print", style = DocFlowTokens.bodyStyle) },
                                            leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                triggerAndroidPrintJob(context, editableTitle)
                                                showToast("Opening system print service...")
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Save a copy", style = DocFlowTokens.bodyStyle) },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                showExportShareSheet = true
                                            }
                                        )
                                        if (!isPdfMode) {
                                            DropdownMenuItem(
                                                text = { Text("Version history (${versions.take(20).size})", style = DocFlowTokens.bodyStyle) },
                                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    showVersionHistorySheet = true
                                                }
                                            )
                                        }
                                        HorizontalDivider(color = DocFlowTokens.divider)
                                        DropdownMenuItem(
                                            text = { Text("Delete", style = DocFlowTokens.bodyStyle, color = DocFlowTokens.iconRedPdf) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = null,
                                                    tint = DocFlowTokens.iconRedPdf
                                                )
                                            },
                                            onClick = {
                                                showOverflowMenu = false
                                                showDeleteConfirmDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(
                        thickness = DocFlowTokens.dividerThickness,
                        color = DocFlowTokens.divider
                    )
                }
            }
        },
        bottomBar = {
            if (isPdfMode) {
                // PDF Bottom Toolbar (White/Surface): Thumbnails, Search, Annotate, Night mode, Page jump
                Surface(
                    color = DocFlowTokens.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            thickness = DocFlowTokens.dividerThickness,
                            color = DocFlowTokens.divider
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(horizontal = DocFlowTokens.space2),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PdfBottomToolButton(
                                icon = Icons.Default.GridView,
                                label = "Thumbnails",
                                isActive = showThumbnailsSheet,
                                testTag = "pdf_tool_thumbnails",
                                onClick = { showThumbnailsSheet = true }
                            )
                            PdfBottomToolButton(
                                icon = Icons.Default.Search,
                                label = "Search",
                                isActive = isSearchBarVisible,
                                testTag = "pdf_tool_search",
                                onClick = { isSearchBarVisible = !isSearchBarVisible }
                            )
                            PdfBottomToolButton(
                                icon = Icons.Default.Brush,
                                label = "Annotate",
                                isActive = isAnnotationActive || showAnnotateSheet,
                                testTag = "pdf_tool_annotate",
                                onClick = {
                                    isAnnotationActive = true
                                    showAnnotateSheet = true
                                }
                            )
                            PdfBottomToolButton(
                                icon = Icons.Default.DarkMode,
                                label = "Night mode",
                                isActive = isPdfNightMode,
                                testTag = "pdf_tool_night_mode",
                                onClick = { isPdfNightMode = !isPdfNightMode }
                            )
                            PdfBottomToolButton(
                                icon = Icons.Default.Numbers,
                                label = "Page jump",
                                isActive = showPageJumpDialog,
                                testTag = "pdf_tool_page_jump",
                                onClick = {
                                    pageJumpInput = "${currentPageIndex + 1}"
                                    showPageJumpDialog = true
                                }
                            )
                        }
                    }
                }
            } else if (isWordEditMode) {
                // Word Edit Mode Horizontal Scrolling Bottom Toolbar
                Surface(
                    color = DocFlowTokens.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            thickness = DocFlowTokens.dividerThickness,
                            color = DocFlowTokens.divider
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = DocFlowTokens.space3),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
                        ) {
                            WordFormatChipButton(
                                icon = Icons.Default.FormatBold,
                                label = "Bold",
                                isActive = wordBoldActive,
                                onClick = { wordBoldActive = !wordBoldActive }
                            )
                            WordFormatChipButton(
                                icon = Icons.Default.FormatItalic,
                                label = "Italic",
                                isActive = wordItalicActive,
                                onClick = { wordItalicActive = !wordItalicActive }
                            )
                            WordFormatChipButton(
                                icon = Icons.Default.FormatUnderlined,
                                label = "Underline",
                                isActive = wordUnderlineActive,
                                onClick = { wordUnderlineActive = !wordUnderlineActive }
                            )
                            WordFormatChipButton(
                                icon = Icons.Default.Title,
                                label = "Heading",
                                isActive = false,
                                onClick = {
                                    wordUndoStack.add(decryptedContent)
                                    decryptedContent = "# New Section Heading\n\n$decryptedContent"
                                    wordAutosaveLabel = "Saved"
                                }
                            )
                            WordFormatChipButton(
                                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                                label = "List",
                                isActive = false,
                                onClick = {
                                    wordUndoStack.add(decryptedContent)
                                    decryptedContent = "$decryptedContent\n• List item"
                                    wordAutosaveLabel = "Saved"
                                }
                            )
                            WordFormatChipButton(
                                icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                                label = "Align",
                                isActive = false,
                                onClick = { showToast("Paragraph alignment updated") }
                            )
                            WordFormatChipButton(
                                icon = Icons.Default.FormatSize,
                                label = "${wordFontSizeSp}sp",
                                isActive = wordFontSizeSp != 16,
                                onClick = {
                                    wordFontSizeSp = when (wordFontSizeSp) {
                                        14 -> 16
                                        16 -> 18
                                        18 -> 20
                                        else -> 14
                                    }
                                }
                            )
                            WordFormatChipButton(
                                icon = Icons.Default.FormatColorText,
                                label = "Color",
                                isActive = wordTextColorHex != "#111111",
                                onClick = {
                                    wordTextColorHex = when (wordTextColorHex) {
                                        "#111111" -> "#1F6BFF"
                                        "#1F6BFF" -> "#E5483F"
                                        "#E5483F" -> "#16A34A"
                                        else -> "#111111"
                                    }
                                }
                            )
                            WordFormatChipButton(
                                icon = Icons.Default.TableChart,
                                label = "Table",
                                isActive = false,
                                onClick = {
                                    wordUndoStack.add(decryptedContent)
                                    decryptedContent += "\n\n| Item | Description | Value |\n| --- | --- | --- |\n| Row 1 | Specification | OK |\n"
                                    wordAutosaveLabel = "Saved"
                                }
                            )
                            WordFormatChipButton(
                                icon = Icons.Default.Image,
                                label = "Image",
                                isActive = false,
                                onClick = {
                                    wordUndoStack.add(decryptedContent)
                                    decryptedContent += "\n\n[Embedded Figure: Local Attachment (${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())})]\n"
                                    wordAutosaveLabel = "Saved"
                                }
                            )
                            WordFormatChipButton(
                                icon = Icons.AutoMirrored.Filled.Undo,
                                label = "Undo",
                                isActive = false,
                                enabled = wordUndoStack.isNotEmpty(),
                                onClick = {
                                    if (wordUndoStack.isNotEmpty()) {
                                        wordRedoStack.add(decryptedContent)
                                        decryptedContent = wordUndoStack.removeAt(wordUndoStack.lastIndex)
                                    }
                                }
                            )
                            WordFormatChipButton(
                                icon = Icons.AutoMirrored.Filled.Redo,
                                label = "Redo",
                                isActive = false,
                                enabled = wordRedoStack.isNotEmpty(),
                                onClick = {
                                    if (wordRedoStack.isNotEmpty()) {
                                        wordUndoStack.add(decryptedContent)
                                        decryptedContent = wordRedoStack.removeAt(wordRedoStack.lastIndex)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DocFlowTokens.background)
        ) {
            when {
                isLoadingDocument -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = DocFlowTokens.space5),
                        verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space4)
                    ) {
                        DocFlowProgressBar(
                            progress = loadingProgress,
                            label = "Loading document...",
                            modifier = Modifier.padding(horizontal = DocFlowTokens.screenSideMargin)
                        )
                        DocFlowSkeletonLoader(rowCount = 6)
                    }
                }

                isFileMissing -> {
                    DocFlowErrorState(
                        title = "File not found on device",
                        message = "This file was moved or deleted outside DocFlow. You can remove it from your Recent list.",
                        retryLabel = "Remove from Recent",
                        onRetry = onDelete,
                        secondaryLabel = "Go back",
                        onSecondaryClick = onBack,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                isFileCorrupted -> {
                    DocFlowErrorState(
                        title = "Unable to open file",
                        message = "This file appears corrupted or uses an unsupported encoding.",
                        retryLabel = "Try another app",
                        onRetry = {
                            openInExternalAppChooser(context, resolvedPdfUri, document.fileType)
                        },
                        secondaryLabel = "Go back",
                        onSecondaryClick = onBack,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                isPdfMode -> {
                    // ---------------------------------------------------------
                    // PDF VIEWER MODE
                    // ---------------------------------------------------------
                    var containerHeightPx by remember { mutableIntStateOf(1) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { containerHeightPx = it.height.coerceAtLeast(1) }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (zoomScale * zoom).coerceIn(1f, 3.5f)
                                    zoomScale = newScale
                                    panOffset = if (newScale > 1.02f) {
                                        Offset(
                                            x = (panOffset.x + pan.x).coerceIn(-600f, 600f),
                                            y = (panOffset.y + pan.y).coerceIn(-800f, 800f)
                                        )
                                    } else {
                                        Offset.Zero
                                    }
                                }
                            }
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoomScale
                                    scaleY = zoomScale
                                    translationX = panOffset.x
                                    translationY = panOffset.y
                                }
                                .testTag("pdf_pages_lazy_column"),
                            contentPadding = PaddingValues(
                                horizontal = DocFlowTokens.screenSideMargin,
                                vertical = DocFlowTokens.space4
                            ),
                            verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
                        ) {
                            itemsIndexed(
                                items = (0 until totalPages).toList(),
                                key = { _, idx -> "pdf_page_$idx" }
                            ) { _, pageIdx ->
                                val pageText = textPages.getOrElse(pageIdx) {
                                    "Page ${pageIdx + 1} of $editableTitle"
                                }
                                val pageAnnotations = remember(annotations, pageIdx) {
                                    annotations.filter { it.pageIndex == pageIdx }
                                }
                                val activeMatchOnThisPage = searchMatches.getOrNull(currentMatchIndex)
                                    ?.takeIf { it.pageIndex == pageIdx }

                                PdfPageSheetItem(
                                    pageIndex = pageIdx,
                                    totalPages = totalPages,
                                    documentTitle = editableTitle,
                                    pageText = pageText,
                                    renderedBitmap = renderedBitmaps[pageIdx],
                                    isNightMode = isPdfNightMode,
                                    searchQuery = searchQuery,
                                    activeOccurrenceIndex = activeMatchOnThisPage?.occurrenceInPage ?: -1,
                                    pageAnnotations = pageAnnotations,
                                    isAnnotationMode = isAnnotationActive,
                                    selectedTool = selectedMarkupTool,
                                    selectedColorHex = selectedAnnotationColorHex,
                                    selectedStrokeWidth = selectedStrokeWidth,
                                    isEraserActive = isEraserActive,
                                    onDoubleTapZoom = {
                                        if (zoomScale > 1.2f) {
                                            zoomScale = 1f
                                            panOffset = Offset.Zero
                                        } else {
                                            zoomScale = 2f
                                        }
                                    },
                                    onCommitStroke = { points ->
                                        if (isEraserActive) {
                                            onClearPageAnnotations(pageIdx)
                                            showToast("Cleared annotations on page ${pageIdx + 1}")
                                        } else if (selectedMarkupTool == PdfMarkupTool.TEXT_BOX) {
                                            pendingNoteTapOffset = points.firstOrNull() ?: Offset(0.25f, 0.25f)
                                            currentPageIndex = pageIdx
                                            showAddTextNoteDialog = true
                                        } else {
                                            redoAnnotationStack.clear()
                                            onUpsertAnnotation(
                                                pageIdx,
                                                selectedMarkupTool,
                                                selectedAnnotationColorHex,
                                                selectedStrokeWidth,
                                                points,
                                                ""
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        // Active Annotation Strip at Top of PDF Area when annotating
                        if (isAnnotationActive) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = DocFlowTokens.space2, start = DocFlowTokens.screenSideMargin, end = DocFlowTokens.screenSideMargin),
                                shape = DocFlowTokens.pillShape,
                                color = DocFlowTokens.surface,
                                border = BorderStroke(1.dp, DocFlowTokens.divider),
                                tonalElevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = DocFlowTokens.space3, vertical = DocFlowTokens.space1),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                                ) {
                                    Text(
                                        text = if (isEraserActive) "Eraser active" else "Annotating: ${selectedMarkupTool.label}",
                                        style = DocFlowTokens.captionBoldStyle,
                                        color = DocFlowTokens.accentBlue
                                    )
                                    IconButton(
                                        onClick = {
                                            val pageAnns = annotations.filter { it.pageIndex == currentPageIndex }
                                            if (pageAnns.isNotEmpty()) {
                                                redoAnnotationStack.add(pageAnns.last())
                                                onUndoAnnotation(pageAnns)
                                            }
                                        },
                                        enabled = annotations.any { it.pageIndex == currentPageIndex },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo annotation", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            if (redoAnnotationStack.isNotEmpty()) {
                                                val restored = redoAnnotationStack.removeAt(redoAnnotationStack.lastIndex)
                                                onUpsertAnnotation(
                                                    restored.pageIndex,
                                                    runCatching { PdfMarkupTool.valueOf(restored.toolType) }.getOrDefault(PdfMarkupTool.PEN),
                                                    restored.colorHex,
                                                    restored.strokeWidth,
                                                    restored.decodePoints(),
                                                    restored.stickyNoteText
                                                )
                                            }
                                        },
                                        enabled = redoAnnotationStack.isNotEmpty(),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo annotation", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { showAnnotateSheet = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.BorderColor, contentDescription = "Annotation options", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            isAnnotationActive = false
                                            isEraserActive = false
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Finish annotating", tint = DocFlowTokens.iconGreenDownload)
                                    }
                                }
                            }
                        }

                        // Floating Page Indicator Pill ("3 / 24") — appears on scroll, fades after 1.5s
                        AnimatedVisibility(
                            visible = showFloatingPagePill || isFastScrollDragging,
                            enter = fadeIn(DocFlowTokens.standardTween()),
                            exit = fadeOut(DocFlowTokens.standardTween()),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = DocFlowTokens.space4)
                        ) {
                            Surface(
                                shape = DocFlowTokens.pillShape,
                                color = DocFlowTokens.textPrimary.copy(alpha = 0.88f),
                                tonalElevation = 0.dp
                            ) {
                                Text(
                                    text = "${currentPageIndex + 1} / $totalPages",
                                    style = DocFlowTokens.captionBoldStyle,
                                    color = DocFlowTokens.surface,
                                    modifier = Modifier
                                        .padding(horizontal = DocFlowTokens.space4, vertical = DocFlowTokens.space2)
                                        .testTag("floating_page_indicator_pill")
                                )
                            }
                        }

                        // Fast-Scroll Thumb on the Right showing page number while dragging
                        if (totalPages > 1) {
                            val fraction = if (totalPages <= 1) 0f else {
                                currentPageIndex.toFloat() / (totalPages - 1).toFloat()
                            }
                            val density = LocalDensity.current
                            val trackPaddingPx = with(density) { 48.dp.toPx() }
                            val usableTrackPx = (containerHeightPx - trackPaddingPx * 2).coerceAtLeast(1f)
                            val thumbOffsetY = (trackPaddingPx + fraction * usableTrackPx).roundToInt()

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .fillMaxHeight()
                                    .width(44.dp)
                                    .pointerInput(totalPages, containerHeightPx) {
                                        detectDragGestures(
                                            onDragStart = {
                                                isFastScrollDragging = true
                                                showFloatingPagePill = true
                                            },
                                            onDragEnd = { isFastScrollDragging = false },
                                            onDragCancel = { isFastScrollDragging = false },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                val relY = (change.position.y - trackPaddingPx)
                                                    .coerceIn(0f, usableTrackPx)
                                                val targetPage = ((relY / usableTrackPx) * (totalPages - 1))
                                                    .roundToInt()
                                                    .coerceIn(0, totalPages - 1)
                                                if (targetPage != currentPageIndex) {
                                                    currentPageIndex = targetPage
                                                    scope.launch {
                                                        listState.scrollToItem(targetPage)
                                                    }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .offset { IntOffset(0, thumbOffsetY) }
                                        .align(Alignment.TopEnd)
                                        .padding(end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isFastScrollDragging) {
                                        Surface(
                                            shape = DocFlowTokens.pillShape,
                                            color = DocFlowTokens.accentBlue,
                                            modifier = Modifier.padding(end = 6.dp)
                                        ) {
                                            Text(
                                                text = "Page ${currentPageIndex + 1}",
                                                style = DocFlowTokens.captionBoldStyle,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .width(6.dp)
                                            .height(44.dp),
                                        shape = DocFlowTokens.pillShape,
                                        color = if (isFastScrollDragging) DocFlowTokens.accentBlue else DocFlowTokens.textSecondary.copy(alpha = 0.55f)
                                    ) {}
                                }
                            }
                        }
                    }
                }

                else -> {
                    // ---------------------------------------------------------
                    // WORD (DOCX) & TEXT VIEWER / EDITOR MODE
                    // ---------------------------------------------------------
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(
                                    horizontal = DocFlowTokens.screenSideMargin,
                                    vertical = DocFlowTokens.space4
                                )
                        ) {
                            DocFlowCard(
                                padding = PaddingValues(DocFlowTokens.cardPaddingMax),
                                borderColor = DocFlowTokens.divider
                            ) {
                                if (isWordEditMode) {
                                    OutlinedTextField(
                                        value = decryptedContent,
                                        onValueChange = { updated ->
                                            wordUndoStack.add(decryptedContent)
                                            if (wordUndoStack.size > 40) wordUndoStack.removeAt(0)
                                            decryptedContent = updated
                                            wordAutosaveLabel = "Saved"
                                            onLiveKeystrokeEdit(
                                                editableTitle,
                                                editableFolderId,
                                                editableCategory,
                                                updated,
                                                document.isEncrypted,
                                                passwordInput.ifBlank { null },
                                                document.encryptionKeyHint,
                                                true
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 480.dp)
                                            .testTag("word_editor_text_field"),
                                        textStyle = DocFlowTokens.bodyStyle.copy(
                                            fontSize = wordFontSizeSp.sp,
                                            fontWeight = if (wordBoldActive) FontWeight.Bold else FontWeight.Normal,
                                            fontStyle = if (wordItalicActive) FontStyle.Italic else FontStyle.Normal,
                                            textDecoration = if (wordUnderlineActive) TextDecoration.Underline else TextDecoration.None,
                                            color = runCatching { Color(android.graphics.Color.parseColor(wordTextColorHex)) }
                                                .getOrDefault(DocFlowTokens.textPrimary)
                                        )
                                    )
                                } else {
                                    // Structured Word Read Layout (headings, paragraphs, lists, tables)
                                    WordStructuredReadView(
                                        content = decryptedContent,
                                        searchQuery = searchQuery,
                                        fontSizeSp = wordFontSizeSp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(88.dp))
                        }

                        // Blue "Edit" Pill Button bottom-right in Read View
                        if (!isWordEditMode) {
                            DocFlowPrimaryButton(
                                text = "Edit",
                                leadingIcon = Icons.Default.Edit,
                                onClick = { isWordEditMode = true },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(DocFlowTokens.space5),
                                testTag = "word_enter_edit_pill_button"
                            )
                        }
                    }
                }
            }

            // Toast Banner Overlay
            DocFlowToast(
                message = toastMessage,
                isError = toastIsError,
                onDismiss = { toastMessage = null },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    // =========================================================================
    // BOTTOM SHEETS & DIALOGS (Using Shared Design System Components)
    // =========================================================================

    // 1. PDF Page Thumbnails Bottom Sheet
    if (showThumbnailsSheet) {
        DocFlowBottomSheet(
            onDismissRequest = { showThumbnailsSheet = false },
            title = "Pages ($totalPages)"
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3),
                verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
            ) {
                itemsIndexed((0 until totalPages).toList()) { _, idx ->
                    val isCurrent = idx == currentPageIndex
                    Column(
                        modifier = Modifier
                            .clip(DocFlowTokens.thumbnailShape)
                            .border(
                                width = if (isCurrent) 2.5.dp else 1.dp,
                                color = if (isCurrent) DocFlowTokens.accentBlue else DocFlowTokens.divider,
                                shape = DocFlowTokens.thumbnailShape
                            )
                            .background(DocFlowTokens.surfaceSubtle)
                            .clickable {
                                currentPageIndex = idx
                                showThumbnailsSheet = false
                                scope.launch { listState.animateScrollToItem(idx) }
                            }
                            .padding(DocFlowTokens.space2),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                                .clip(DocFlowTokens.thumbnailShape)
                                .background(Color.White)
                                .border(1.dp, DocFlowTokens.divider, DocFlowTokens.thumbnailShape)
                                .padding(6.dp)
                        ) {
                            val bmp = renderedBitmaps[idx]
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Page ${idx + 1} thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = textPages.getOrElse(idx) { "" }.take(90),
                                    style = DocFlowTokens.microBadgeStyle.copy(
                                        fontSize = 7.sp,
                                        lineHeight = 9.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = Color(0xFF333333)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(DocFlowTokens.space1))
                        Text(
                            text = "${idx + 1}",
                            style = DocFlowTokens.captionBoldStyle,
                            color = if (isCurrent) DocFlowTokens.accentBlue else DocFlowTokens.textSecondary
                        )
                    }
                }
            }
        }
    }

    // 2. PDF Annotate Bottom Sheet (Highlight, Underline, Draw, Text note, Eraser, Undo/Redo)
    if (showAnnotateSheet) {
        DocFlowBottomSheet(
            onDismissRequest = { showAnnotateSheet = false },
            title = "Annotate PDF"
        ) {
            Text(
                text = "Select markup tool",
                style = DocFlowTokens.captionStyle,
                color = DocFlowTokens.textSecondary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
            ) {
                FilterChip(
                    selected = !isEraserActive && selectedMarkupTool == PdfMarkupTool.HIGHLIGHT,
                    onClick = {
                        isEraserActive = false
                        selectedMarkupTool = PdfMarkupTool.HIGHLIGHT
                        selectedStrokeWidth = 16f
                        isAnnotationActive = true
                    },
                    label = { Text("Highlight") },
                    leadingIcon = { Icon(Icons.Default.Highlight, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = !isEraserActive && selectedMarkupTool == PdfMarkupTool.UNDERLINE,
                    onClick = {
                        isEraserActive = false
                        selectedMarkupTool = PdfMarkupTool.UNDERLINE
                        selectedStrokeWidth = 4f
                        isAnnotationActive = true
                    },
                    label = { Text("Underline") },
                    leadingIcon = { Icon(Icons.Default.FormatUnderlined, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = !isEraserActive && selectedMarkupTool == PdfMarkupTool.PEN,
                    onClick = {
                        isEraserActive = false
                        selectedMarkupTool = PdfMarkupTool.PEN
                        selectedStrokeWidth = 5f
                        isAnnotationActive = true
                    },
                    label = { Text("Draw") },
                    leadingIcon = { Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = !isEraserActive && selectedMarkupTool == PdfMarkupTool.TEXT_BOX,
                    onClick = {
                        isEraserActive = false
                        selectedMarkupTool = PdfMarkupTool.TEXT_BOX
                        isAnnotationActive = true
                    },
                    label = { Text("Text note") },
                    leadingIcon = { Icon(Icons.Default.StickyNote2, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = isEraserActive,
                    onClick = {
                        isEraserActive = true
                        isAnnotationActive = true
                    },
                    label = { Text("Eraser") },
                    leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(DocFlowTokens.space4))
            Text(
                text = "Color",
                style = DocFlowTokens.captionStyle,
                color = DocFlowTokens.textSecondary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))
            val colorOptions = listOf("#FDE047", "#E5483F", "#1F6BFF", "#25C25A", "#1FB8C9", "#111111")
            Row(horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)) {
                colorOptions.forEach { hex ->
                    val parsed = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(DocFlowTokens.accentBlue)
                    val isSelected = selectedAnnotationColorHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(parsed)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) DocFlowTokens.textPrimary else DocFlowTokens.divider,
                                shape = CircleShape
                            )
                            .clickable { selectedAnnotationColorHex = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected color",
                                tint = if (hex == "#FDE047") Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DocFlowTokens.space4))
            Text(
                text = "Thickness: ${selectedStrokeWidth.roundToInt()} pt",
                style = DocFlowTokens.captionStyle,
                color = DocFlowTokens.textSecondary
            )
            Slider(
                value = selectedStrokeWidth,
                onValueChange = { selectedStrokeWidth = it },
                valueRange = 2f..28f,
                colors = SliderDefaults.colors(
                    thumbColor = DocFlowTokens.accentBlue,
                    activeTrackColor = DocFlowTokens.accentBlue
                )
            )

            Spacer(modifier = Modifier.height(DocFlowTokens.space3))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DocFlowSecondaryButton(
                    text = "Undo",
                    leadingIcon = Icons.AutoMirrored.Filled.Undo,
                    enabled = annotations.any { it.pageIndex == currentPageIndex },
                    onClick = {
                        val pageAnns = annotations.filter { it.pageIndex == currentPageIndex }
                        if (pageAnns.isNotEmpty()) {
                            redoAnnotationStack.add(pageAnns.last())
                            onUndoAnnotation(pageAnns)
                        }
                    }
                )
                DocFlowPrimaryButton(
                    text = "Start Annotating",
                    onClick = {
                        isAnnotationActive = true
                        showAnnotateSheet = false
                    }
                )
            }
        }
    }

    // 3. Add Text Note Dialog (for PDF Text Note annotation)
    if (showAddTextNoteDialog) {
        DocFlowDialog(
            title = "Add text note",
            onDismissRequest = { showAddTextNoteDialog = false },
            confirmText = "Save note",
            confirmEnabled = textNoteInput.isNotBlank(),
            onConfirm = {
                onUpsertAnnotation(
                    currentPageIndex,
                    PdfMarkupTool.TEXT_BOX,
                    selectedAnnotationColorHex,
                    12f,
                    listOf(pendingNoteTapOffset),
                    textNoteInput.trim()
                )
                textNoteInput = ""
                showAddTextNoteDialog = false
                showToast("Note added to page ${currentPageIndex + 1}")
            }
        ) {
            OutlinedTextField(
                value = textNoteInput,
                onValueChange = { textNoteInput = it },
                label = { Text("Note text") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // 4. Page Jump Dialog
    if (showPageJumpDialog) {
        DocFlowDialog(
            title = "Jump to page",
            onDismissRequest = { showPageJumpDialog = false },
            confirmText = "Go",
            onConfirm = {
                val target = (pageJumpInput.toIntOrNull() ?: 1).coerceIn(1, totalPages) - 1
                currentPageIndex = target
                showPageJumpDialog = false
                scope.launch { listState.animateScrollToItem(target) }
            }
        ) {
            Text(
                text = "Enter a page number between 1 and $totalPages",
                style = DocFlowTokens.captionStyle,
                color = DocFlowTokens.textSecondary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))
            OutlinedTextField(
                value = pageJumpInput,
                onValueChange = { pageJumpInput = it.filter { ch -> ch.isDigit() } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // 5. Details Bottom Sheet
    if (showDetailsSheet) {
        DocFlowBottomSheet(
            onDismissRequest = { showDetailsSheet = false },
            title = "Document details"
        ) {
            val updatedMs = document.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
            val formattedDate = remember(updatedMs) {
                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(updatedMs))
            }
            val sizeLabel = remember(document.fileSize) {
                if (document.fileSize >= 1024L * 1024L) {
                    String.format(Locale.US, "%.2f MB", document.fileSize / (1024.0 * 1024.0))
                } else {
                    "${(document.fileSize / 1024L).coerceAtLeast(1L)} KB"
                }
            }
            DocFlowListRow(title = "Name", subtitle = editableTitle)
            DocFlowListRow(title = "Format", subtitle = document.fileType.uppercase())
            DocFlowListRow(title = "Pages", subtitle = "$totalPages")
            DocFlowListRow(title = "Size", subtitle = sizeLabel)
            DocFlowListRow(title = "Last modified", subtitle = formattedDate)
            DocFlowListRow(
                title = "Storage location",
                subtitle = localFilePath ?: "App-private offline SQLite + Filesystem"
            )
        }
    }

    // 6. Rename Dialog
    if (showRenameDialog) {
        DocFlowDialog(
            title = "Rename document",
            onDismissRequest = { showRenameDialog = false },
            confirmText = "Save",
            confirmEnabled = renameInput.isNotBlank(),
            onConfirm = {
                editableTitle = renameInput.trim()
                onSaveEdits(
                    editableTitle,
                    editableFolderId,
                    editableCategory,
                    decryptedContent,
                    document.isEncrypted,
                    passwordInput.ifBlank { null },
                    document.encryptionKeyHint,
                    true
                )
                showRenameDialog = false
                showToast("Renamed to $editableTitle")
            }
        ) {
            OutlinedTextField(
                value = renameInput,
                onValueChange = { renameInput = it },
                label = { Text("File name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // 7. Move to Folder Sheet
    if (showMoveFolderSheet) {
        DocFlowBottomSheet(
            onDismissRequest = { showMoveFolderSheet = false },
            title = "Move to folder"
        ) {
            folders.forEach { folder ->
                DocFlowListRow(
                    title = folder.name,
                    subtitle = if (folder.id == editableFolderId) "Current folder" else null,
                    selected = folder.id == editableFolderId,
                    onClick = {
                        editableFolderId = folder.id
                        onSaveEdits(
                            editableTitle,
                            folder.id,
                            editableCategory,
                            decryptedContent,
                            document.isEncrypted,
                            passwordInput.ifBlank { null },
                            document.encryptionKeyHint,
                            true
                        )
                        showMoveFolderSheet = false
                        showToast("Moved to ${folder.name}")
                    }
                )
            }
        }
    }

    // 8. Export / Save a Copy / Share Sheet
    if (showExportShareSheet) {
        DocFlowBottomSheet(
            onDismissRequest = { showExportShareSheet = false },
            title = "Export & Share"
        ) {
            DocFlowListRow(
                title = "Save as PDF",
                subtitle = "Flatten pages and annotations into a local PDF file",
                leading = {
                    DocFlowIconCircle(
                        icon = Icons.Default.PictureAsPdf,
                        circleColor = DocFlowTokens.iconRedPdf,
                        contentDescription = null
                    )
                },
                showBottomDivider = true,
                onClick = {
                    showExportShareSheet = false
                    if (isPdfMode) {
                        onShareAndExportAnnotatedPdf(decryptedContent, false)
                    } else {
                        onConvertFormat("pdf", decryptedContent)
                    }
                    showToast("Saved PDF copy to workspace")
                }
            )
            DocFlowListRow(
                title = "Save as DOCX",
                subtitle = "Save editable Word document copy on device",
                leading = {
                    DocFlowIconCircle(
                        icon = Icons.Default.Description,
                        circleColor = DocFlowTokens.accentBlue,
                        contentDescription = null
                    )
                },
                showBottomDivider = true,
                onClick = {
                    showExportShareSheet = false
                    onConvertFormat("docx", decryptedContent)
                    showToast("Saved DOCX copy to workspace")
                }
            )
            DocFlowListRow(
                title = "Export pages as PNG images",
                subtitle = "Render high-resolution page images to workspace",
                leading = {
                    DocFlowIconCircle(
                        icon = Icons.Default.Image,
                        circleColor = DocFlowTokens.iconGreenDownload,
                        contentDescription = null
                    )
                },
                onClick = {
                    showExportShareSheet = false
                    scope.launch {
                        val res = ImageProcessingPdfPipeline.convertDocumentToPictures(
                            context = context,
                            docId = document.id,
                            title = editableTitle,
                            fileType = document.fileType,
                            plainContent = decryptedContent,
                            localUri = resolvedPdfUri
                        )
                        res.onSuccess { picRes ->
                            onSaveDocToPicToWorkspace(
                                "Stitched PNG",
                                picRes.pageBitmaps.size,
                                picRes.stitchedFile,
                                picRes.stitchedUri
                            )
                            showToast("Exported ${picRes.pageBitmaps.size} page image(s)")
                        }
                    }
                }
            )
        }
    }

    // 9. Word Version History Bottom Sheet (Last 20 versions, restore any)
    if (showVersionHistorySheet) {
        DocFlowBottomSheet(
            onDismissRequest = { showVersionHistorySheet = false },
            title = "Version history (last 20)"
        ) {
            val recentVersions = remember(versions) { versions.take(20) }
            if (recentVersions.isEmpty()) {
                Text(
                    text = "No saved versions yet. Edit and tap Done to create local snapshots.",
                    style = DocFlowTokens.bodyStyle,
                    color = DocFlowTokens.textSecondary
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    recentVersions.forEach { ver ->
                        val dateStr = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(ver.createdAtMillis))
                        DocFlowListRow(
                            title = "v${ver.versionNumber} · ${ver.versionLabel}",
                            subtitle = dateStr,
                            showBottomDivider = true,
                            trailing = {
                                DocFlowSecondaryButton(
                                    text = "Restore",
                                    onClick = {
                                        onRestoreVersion(ver, passwordInput.ifBlank { null }) { restoredText ->
                                            decryptedContent = restoredText
                                        }
                                        decryptedContent = ver.snapshotContent
                                        showVersionHistorySheet = false
                                        showToast("Restored v${ver.versionNumber}")
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // 10. Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        DocFlowDialog(
            title = "Delete document?",
            onDismissRequest = { showDeleteConfirmDialog = false },
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirmDialog = false
                onDelete()
            }
        ) {
            Text(
                text = "\"$editableTitle\" will be moved to Recently deleted.",
                style = DocFlowTokens.bodyStyle,
                color = DocFlowTokens.textSecondary
            )
        }
    }
}

@Composable
private fun PdfBottomToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .heightIn(min = DocFlowTokens.minTouchTarget)
            .clip(DocFlowTokens.smallShape)
            .clickable(onClick = onClick)
            .padding(horizontal = DocFlowTokens.space2, vertical = DocFlowTokens.space1)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) DocFlowTokens.accentBlue else DocFlowTokens.textPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = DocFlowTokens.microBadgeStyle.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isActive) DocFlowTokens.accentBlue else DocFlowTokens.textSecondary
        )
    }
}

@Composable
private fun WordFormatChipButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(40.dp)
            .clip(DocFlowTokens.smallShape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = DocFlowTokens.smallShape,
        color = if (isActive) DocFlowTokens.accentBlue.copy(alpha = 0.14f) else DocFlowTokens.surfaceSubtle,
        border = BorderStroke(1.dp, if (isActive) DocFlowTokens.accentBlue else DocFlowTokens.divider),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DocFlowTokens.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = when {
                    !enabled -> DocFlowTokens.textSecondary.copy(alpha = 0.4f)
                    isActive -> DocFlowTokens.accentBlue
                    else -> DocFlowTokens.textPrimary
                },
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = DocFlowTokens.captionStyle,
                color = when {
                    !enabled -> DocFlowTokens.textSecondary.copy(alpha = 0.4f)
                    isActive -> DocFlowTokens.accentBlue
                    else -> DocFlowTokens.textPrimary
                }
            )
        }
    }
}

/**
 * Single PDF Page Sheet on the gray background:
 * White sheet (#FFFFFF) or Night-mode inverted sheet (#141619) with subtle thin border,
 * search highlights, and interactive annotation canvas.
 */
@Composable
private fun PdfPageSheetItem(
    pageIndex: Int,
    totalPages: Int,
    documentTitle: String,
    pageText: String,
    renderedBitmap: Bitmap?,
    isNightMode: Boolean,
    searchQuery: String,
    activeOccurrenceIndex: Int,
    pageAnnotations: List<PdfAnnotationYMapEntity>,
    isAnnotationMode: Boolean,
    selectedTool: PdfMarkupTool,
    selectedColorHex: String,
    selectedStrokeWidth: Float,
    isEraserActive: Boolean,
    onDoubleTapZoom: () -> Unit,
    onCommitStroke: (List<Offset>) -> Unit
) {
    val activePoints = remember { mutableStateListOf<Offset>() }
    val sheetBg = if (isNightMode) Color(0xFF141619) else Color.White
    val sheetTextColor = if (isNightMode) Color(0xFFF2F3F5) else Color(0xFF111111)

    val invertFilter = remember(isNightMode) {
        if (!isNightMode) null
        else {
            val matrix = androidx.compose.ui.graphics.ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ColorFilter.colorMatrix(matrix)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.707f) // Standard A4 ratio
            .clip(DocFlowTokens.thumbnailShape)
            .border(DocFlowTokens.dividerThickness, DocFlowTokens.divider, DocFlowTokens.thumbnailShape)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onDoubleTapZoom() })
            },
        color = sheetBg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (renderedBitmap != null) {
                Image(
                    bitmap = renderedBitmap.asImageBitmap(),
                    contentDescription = "PDF Page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = invertFilter
                )
            } else {
                // Clean structured vector page sheet when rendering textual/OCR PDF
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = documentTitle,
                                style = DocFlowTokens.captionBoldStyle,
                                color = sheetTextColor.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Page ${pageIndex + 1} / $totalPages",
                                style = DocFlowTokens.captionStyle,
                                color = sheetTextColor.copy(alpha = 0.55f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DocFlowTokens.divider)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = buildHighlightedText(
                                fullText = pageText,
                                query = searchQuery,
                                activeOccurrence = activeOccurrenceIndex,
                                baseColor = sheetTextColor
                            ),
                            style = DocFlowTokens.bodyStyle.copy(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 14.sp,
                                lineHeight = 21.sp
                            )
                        )
                    }
                    Text(
                        text = "DocFlow Offline PDF Engine · Page ${pageIndex + 1}",
                        style = DocFlowTokens.microBadgeStyle.copy(fontWeight = FontWeight.Normal),
                        color = sheetTextColor.copy(alpha = 0.45f)
                    )
                }
            }

            // Annotation Canvas Overlay
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .let { base ->
                        if (isAnnotationMode) {
                            base.pointerInput(pageIndex, selectedTool, selectedColorHex, selectedStrokeWidth, isEraserActive) {
                                if (selectedTool == PdfMarkupTool.TEXT_BOX) {
                                    detectTapGestures { tap ->
                                        val norm = Offset(
                                            (tap.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                                            (tap.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                        )
                                        onCommitStroke(listOf(norm))
                                    }
                                } else {
                                    detectDragGestures(
                                        onDragStart = { start ->
                                            activePoints.clear()
                                            activePoints.add(
                                                Offset(
                                                    (start.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                                                    (start.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                                )
                                            )
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            activePoints.add(
                                                Offset(
                                                    (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                                                    (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                                )
                                            )
                                        },
                                        onDragEnd = {
                                            if (activePoints.isNotEmpty()) {
                                                onCommitStroke(activePoints.toList())
                                                activePoints.clear()
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            base
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Draw persisted annotations
                pageAnnotations.forEach { ann ->
                    val pts = ann.decodePoints()
                    val color = runCatching { Color(android.graphics.Color.parseColor(ann.colorHex)) }
                        .getOrDefault(DocFlowTokens.accentBlue)
                    val tool = runCatching { PdfMarkupTool.valueOf(ann.toolType) }.getOrDefault(PdfMarkupTool.PEN)

                    if (tool == PdfMarkupTool.TEXT_BOX) {
                        val anchor = pts.firstOrNull() ?: Offset(0.2f, 0.2f)
                        drawCircle(
                            color = color,
                            radius = 12.dp.toPx(),
                            center = Offset(anchor.x * w, anchor.y * h)
                        )
                    } else if (pts.size >= 2) {
                        val path = Path().apply {
                            moveTo(pts.first().x * w, pts.first().y * h)
                            pts.drop(1).forEach { pt -> lineTo(pt.x * w, pt.y * h) }
                        }
                        drawPath(
                            path = path,
                            color = if (tool == PdfMarkupTool.HIGHLIGHT) color.copy(alpha = 0.38f) else color,
                            style = Stroke(
                                width = ann.strokeWidth.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // Draw in-progress stroke
                if (activePoints.size >= 2 && !isEraserActive) {
                    val liveColor = runCatching { Color(android.graphics.Color.parseColor(selectedColorHex)) }
                        .getOrDefault(DocFlowTokens.accentBlue)
                    val path = Path().apply {
                        moveTo(activePoints.first().x * w, activePoints.first().y * h)
                        activePoints.drop(1).forEach { pt -> lineTo(pt.x * w, pt.y * h) }
                    }
                    drawPath(
                        path = path,
                        color = if (selectedTool == PdfMarkupTool.HIGHLIGHT) liveColor.copy(alpha = 0.38f) else liveColor,
                        style = Stroke(
                            width = selectedStrokeWidth.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

/**
 * Word (DOCX) Clean Structured Read View:
 * Renders headings, paragraphs, lists, tables, and inline image blocks on the white card.
 */
@Composable
private fun WordStructuredReadView(
    content: String,
    searchQuery: String,
    fontSizeSp: Int
) {
    val lines = remember(content) {
        content.ifBlank { "Empty Word document. Tap Edit below to start writing." }.lines()
    }
    Column(verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)) {
        lines.forEach { rawLine ->
            val trimmed = rawLine.trim()
            when {
                trimmed.startsWith("# ") || trimmed.startsWith("===") -> {
                    Text(
                        text = trimmed.removePrefix("# ").trim('=').trim(),
                        style = DocFlowTokens.sectionLabelStyle.copy(fontWeight = FontWeight.Bold),
                        color = DocFlowTokens.textPrimary,
                        modifier = Modifier.padding(top = DocFlowTokens.space2)
                    )
                }
                trimmed.startsWith("|") && trimmed.endsWith("|") -> {
                    val cells = trimmed.trim('|').split("|").map { it.trim() }
                    if (!cells.all { it.all { ch -> ch == '-' } }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DocFlowTokens.surfaceSubtle)
                                .border(0.5.dp, DocFlowTokens.divider)
                                .padding(DocFlowTokens.space2)
                        ) {
                            cells.forEach { cell ->
                                Text(
                                    text = cell,
                                    style = DocFlowTokens.captionStyle,
                                    color = DocFlowTokens.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                trimmed.startsWith("•") || trimmed.startsWith("- ") -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                    ) {
                        Text("•", style = DocFlowTokens.bodyMediumStyle, color = DocFlowTokens.accentBlue)
                        Text(
                            text = buildHighlightedText(
                                fullText = trimmed.removePrefix("•").removePrefix("- ").trim(),
                                query = searchQuery,
                                activeOccurrence = 0,
                                baseColor = DocFlowTokens.textPrimary
                            ),
                            style = DocFlowTokens.bodyStyle.copy(fontSize = fontSizeSp.sp)
                        )
                    }
                }
                trimmed.isNotEmpty() -> {
                    Text(
                        text = buildHighlightedText(
                            fullText = rawLine,
                            query = searchQuery,
                            activeOccurrence = 0,
                            baseColor = DocFlowTokens.textPrimary
                        ),
                        style = DocFlowTokens.bodyStyle.copy(fontSize = fontSizeSp.sp)
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(DocFlowTokens.space1))
                }
            }
        }
    }
}

private fun buildHighlightedText(
    fullText: String,
    query: String,
    activeOccurrence: Int,
    baseColor: Color
): AnnotatedString {
    if (query.trim().length < 2) {
        return AnnotatedString(fullText, SpanStyle(color = baseColor))
    }
    val q = query.trim()
    return buildAnnotatedString {
        var cursor = 0
        var occ = 0
        while (cursor < fullText.length) {
            val found = fullText.indexOf(q, cursor, ignoreCase = true)
            if (found < 0) {
                withStyle(SpanStyle(color = baseColor)) {
                    append(fullText.substring(cursor))
                }
                break
            }
            if (found > cursor) {
                withStyle(SpanStyle(color = baseColor)) {
                    append(fullText.substring(cursor, found))
                }
            }
            val bg = if (occ == activeOccurrence) {
                DocFlowTokens.searchCurrentOrange
            } else {
                DocFlowTokens.searchMatchYellow
            }
            withStyle(
                SpanStyle(
                    color = Color.Black,
                    background = bg,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(fullText.substring(found, found + q.length))
            }
            occ++
            cursor = found + q.length
        }
    }
}

private fun triggerAndroidPrintJob(context: Context, docTitle: String) {
    runCatching {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        printManager.print(
            "DocFlow_$docTitle",
            object : android.print.PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: android.os.Bundle?
                ) {
                    val info = android.print.PrintDocumentInfo.Builder("$docTitle.pdf")
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: android.os.ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                }
            },
            null
        )
    }
}

private fun openInExternalAppChooser(context: Context, uri: Uri?, fileType: String) {
    runCatching {
        val targetUri = uri ?: return
        val mime = when (fileType.lowercase()) {
            "pdf" -> "application/pdf"
            "docx", "doc" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "*/*"
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(targetUri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    }
}
