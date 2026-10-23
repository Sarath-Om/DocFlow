package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CachedDocumentEntity
import com.example.data.model.DocEntity
import com.example.data.model.FolderEntity
import com.example.ui.components.AiMicDictationBottomSheet
import com.example.ui.components.DocFlowCard
import com.example.ui.components.DocFlowDateGroupHeader
import com.example.ui.components.DocFlowDivider
import com.example.ui.components.DocFlowEmptyState
import com.example.ui.components.DocFlowFab
import com.example.ui.components.DocFlowFilterTabs
import com.example.ui.components.DocFlowHeaderAction
import com.example.ui.components.DocFlowIconCircle
import com.example.ui.components.DocFlowListRow
import com.example.ui.components.DocFlowPrimaryButton
import com.example.ui.components.DocFlowScreenHeader
import com.example.ui.components.DocFlowSecondaryButton
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens
import com.example.ui.viewmodel.DocFlowViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val RECENT_FILTER_TABS = listOf("All", "PDF", "Word", "Excel", "PPT", "OFD", "Other")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: DocFlowViewModel,
    palette: DocFlowPalette,
    onOpenDocument: (String) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenBatchProcessor: () -> Unit,
    onOpenStorageDashboard: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit
) {
    val context = LocalContext.current
    val documents by viewModel.filteredDocuments.collectAsState()
    val cachedMap by viewModel.cachedDocumentsMap.collectAsState()
    val foldersState by viewModel.foldersState.collectAsState()

    var activeTypeTab by remember { mutableStateOf("All") }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedDocIds by remember { mutableStateOf(setOf<String>()) }

    // Collapsible date group state (default expanded)
    val collapsedGroups = remember { mutableStateMapOf<String, Boolean>() }

    // FAB Action Sheet & Create Dialogs
    var showFabSheet by remember { mutableStateOf(false) }
    var showCreateDocDialog by remember { mutableStateOf(false) }
    var createFormat by remember { mutableStateOf("docx") }
    var newDocTitle by remember { mutableStateOf("") }
    var newDocBody by remember { mutableStateOf("") }
    var showAiMicForCreate by remember { mutableStateOf(false) }
    var showStandaloneAiMicSheet by remember { mutableStateOf(false) }

    var showMoveFolderDialog by remember { mutableStateOf(false) }
    var showBatchTagDialog by remember { mutableStateOf(false) }
    var batchTagInput by remember { mutableStateOf("") }
    var docForRename by remember { mutableStateOf<DocEntity?>(null) }
    var renameText by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importDocumentFromUri(
                context = context,
                uri = uri,
                folderId = "root",
                category = "Work",
                isEncrypted = false,
                password = "",
                encryptionKeyHint = null,
                onImported = { docId -> onOpenDocument(docId) }
            )
        }
    }

    val folders = (foldersState as? UiState.Success<List<FolderEntity>>)?.data ?: emptyList()

    // Filter documents by selected horizontal tab ("All", "PDF", "Word", "Excel", "PPT", "OFD", "Other")
    val tabFilteredDocs = remember(documents, activeTypeTab) {
        documents.filter { doc ->
            val ft = doc.fileType.lowercase()
            when (activeTypeTab) {
                "All" -> true
                "PDF" -> ft == "pdf" || ft == "scanned"
                "Word" -> ft == "docx" || ft == "doc"
                "Excel" -> ft == "xlsx" || ft == "xls" || ft == "csv"
                "PPT" -> ft == "pptx" || ft == "ppt"
                "OFD" -> ft == "ofd"
                "Other" -> ft !in setOf("pdf", "scanned", "docx", "doc", "xlsx", "xls", "csv", "pptx", "ppt", "ofd")
                else -> true
            }
        }
    }

    // Group documents by date (Today, Yesterday, Earlier this week, or "DD MMM")
    val groupedDocs = remember(tabFilteredDocs, cachedMap) {
        val sorted = tabFilteredDocs.sortedByDescending { doc ->
            val cached = cachedMap[doc.id]
            cached?.lastAccessedAtMillis ?: cached?.updatedAtMillis ?: ((doc.updatedAt?.seconds ?: 0L) * 1000L)
        }
        sorted.groupBy { doc ->
            val cached = cachedMap[doc.id]
            val ts = cached?.lastAccessedAtMillis ?: cached?.updatedAtMillis ?: ((doc.updatedAt?.seconds ?: 0L) * 1000L)
            formatDateGroupHeader(ts)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header ("Recent" ~40sp bold, right icons: Search, Multi-select checklist, Settings)
            DocFlowScreenHeader(
                title = if (isMultiSelectMode) "${selectedDocIds.size} Selected" else "Recent",
                subtitle = "Stored on this device",
                palette = palette,
                actions = listOf(
                    DocFlowHeaderAction(
                        icon = Icons.Filled.Search,
                        contentDescription = "Search documents",
                        testTag = "top_search_button",
                        onClick = onOpenSearch
                    ),
                    DocFlowHeaderAction(
                        icon = if (isMultiSelectMode) Icons.Filled.Close else Icons.Filled.Checklist,
                        contentDescription = if (isMultiSelectMode) "Exit selection" else "Multi-select checklist",
                        testTag = "top_multiselect_button",
                        onClick = {
                            isMultiSelectMode = !isMultiSelectMode
                            if (!isMultiSelectMode) selectedDocIds = emptySet()
                        }
                    ),
                    DocFlowHeaderAction(
                        icon = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        testTag = "top_settings_button",
                        onClick = onOpenSettings
                    )
                )
            )

            // Horizontal Filter Tabs with Blue Underline
            DocFlowFilterTabs(
                tabs = RECENT_FILTER_TABS,
                selectedTab = activeTypeTab,
                onSelectTab = { activeTypeTab = it },
                palette = palette
            )

            // Multi-select Batch Action Bar
            AnimatedVisibility(visible = isMultiSelectMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .padding(horizontal = DocFlowTokens.screenHorizontalMargin, vertical = DocFlowTokens.space2),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            selectedDocIds = if (selectedDocIds.size == tabFilteredDocs.size) {
                                emptySet()
                            } else {
                                tabFilteredDocs.map { it.id }.toSet()
                            }
                        }
                    ) {
                        Text(
                            text = if (selectedDocIds.size == tabFilteredDocs.size) "Deselect all" else "Select all",
                            color = DocFlowTokens.AccentBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)) {
                        IconButton(
                            onClick = {
                                val firstId = selectedDocIds.firstOrNull()
                                val doc = tabFilteredDocs.find { it.id == firstId }
                                if (doc != null) viewModel.shareAnyDocument(context, doc)
                            },
                            enabled = selectedDocIds.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share selected", tint = palette.textPrimary)
                        }
                        IconButton(
                            onClick = { showMoveFolderDialog = true },
                            enabled = selectedDocIds.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.DriveFileMove, contentDescription = "Move to folder", tint = palette.textPrimary)
                        }
                        IconButton(
                            onClick = { showBatchTagDialog = true },
                            enabled = selectedDocIds.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Label, contentDescription = "Tag selected", tint = palette.textPrimary)
                        }
                        IconButton(
                            onClick = {
                                selectedDocIds.forEach { id ->
                                    viewModel.moveToPrivateSafe(id, "1234")
                                }
                                selectedDocIds = emptySet()
                                isMultiSelectMode = false
                            },
                            enabled = selectedDocIds.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = "Move to Private Safe", tint = palette.textPrimary)
                        }
                        IconButton(
                            onClick = {
                                viewModel.softDeleteDocuments(selectedDocIds)
                                selectedDocIds = emptySet()
                                isMultiSelectMode = false
                            },
                            enabled = selectedDocIds.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete selected", tint = DocFlowTokens.IconCircleRed)
                        }
                    }
                }
            }

            // Document List Grouped by Date inside White 24dp Flat Cards
            if (tabFilteredDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        DocFlowEmptyState(
                            icon = Icons.Filled.History,
                            title = "No recent files in $activeTypeTab",
                            subtitle = "Create a Word, PPT, Excel, Note or PDF document, scan with AI Auto Capture, or use AI Mic voice-to-text. All files are stored on this device.",
                            palette = palette
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2),
                            modifier = Modifier.padding(horizontal = DocFlowTokens.screenHorizontalMargin)
                        ) {
                            DocFlowPrimaryButton(
                                text = "Create Doc / PPT / XLS",
                                onClick = {
                                    createFormat = "docx"
                                    newDocTitle = ""
                                    newDocBody = ""
                                    showCreateDocDialog = true
                                },
                                modifier = Modifier.testTag("empty_state_create_doc_button")
                            )
                            DocFlowSecondaryButton(
                                text = "AI Mic Dictate",
                                onClick = { showStandaloneAiMicSheet = true },
                                palette = palette,
                                modifier = Modifier.testTag("empty_state_ai_mic_button")
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("recent_documents_list"),
                    contentPadding = PaddingValues(top = DocFlowTokens.space2, bottom = 104.dp)
                ) {
                    groupedDocs.forEach { (dateLabel, docsInGroup) ->
                        val isCollapsed = collapsedGroups[dateLabel] == true
                        item(key = "header_$dateLabel") {
                            DocFlowDateGroupHeader(
                                dateLabel = dateLabel,
                                isExpanded = !isCollapsed,
                                onToggle = { collapsedGroups[dateLabel] = !isCollapsed },
                                palette = palette
                            )
                        }

                        if (!isCollapsed) {
                            item(key = "card_$dateLabel") {
                                DocFlowCard(
                                    palette = palette,
                                    modifier = Modifier.padding(vertical = DocFlowTokens.space1),
                                    contentPadding = PaddingValues(vertical = DocFlowTokens.space1)
                                ) {
                                    docsInGroup.forEachIndexed { index, doc ->
                                        val cached = cachedMap[doc.id]
                                        val isSelected = selectedDocIds.contains(doc.id)
                                        RecentDocumentFlatRow(
                                            doc = doc,
                                            cached = cached,
                                            palette = palette,
                                            isMultiSelectMode = isMultiSelectMode,
                                            isSelected = isSelected,
                                            onClick = {
                                                if (isMultiSelectMode) {
                                                    selectedDocIds = if (isSelected) {
                                                        selectedDocIds - doc.id
                                                    } else {
                                                        selectedDocIds + doc.id
                                                    }
                                                } else {
                                                    onOpenDocument(doc.id)
                                                }
                                            },
                                            onLongClick = {
                                                isMultiSelectMode = true
                                                selectedDocIds = selectedDocIds + doc.id
                                            },
                                            onShare = { viewModel.shareAnyDocument(context, doc) },
                                            onRename = {
                                                docForRename = doc
                                                renameText = doc.title
                                            },
                                            onMoveToSafe = { viewModel.moveToPrivateSafe(doc.id, "1234") },
                                            onDelete = { viewModel.softDeleteDocument(doc.id) }
                                        )
                                        if (index < docsInGroup.lastIndex) {
                                            DocFlowDivider(
                                                palette = palette,
                                                modifier = Modifier.padding(
                                                    start = 72.dp,
                                                    end = DocFlowTokens.cardPaddingMin
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom-Right Blue Circle FAB (+)
        DocFlowFab(
            onClick = { showFabSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .testTag("recent_fab_button")
        )
    }

    // FAB Action Sheet: Scan, Open file, Create DOC/PPT/XLS/TXT/PDF, AI Mic with Noise Clean
    if (showFabSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFabSheet = false },
            containerColor = palette.surface,
            shape = DocFlowTokens.sheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.cardPaddingMax, vertical = DocFlowTokens.space2)
            ) {
                Text(
                    text = "Add or Create Document",
                    style = DocFlowTokens.sectionLabelStyle,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = "Stored on this device • Opens in full Word / Office UI",
                    style = DocFlowTokens.captionStyle,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                FabSheetRow(
                    icon = Icons.Filled.CameraAlt,
                    iconBg = DocFlowTokens.AccentBlue,
                    title = "Scan document (AI Auto Capture)",
                    subtitle = "CameraX batch scanner with auto-edge lock & multi-page preview drawer",
                    palette = palette,
                    onClick = {
                        showFabSheet = false
                        onOpenScanner()
                    }
                )
                FabSheetRow(
                    icon = Icons.Filled.Mic,
                    iconBg = DocFlowTokens.IconCircleTeal,
                    title = "AI Mic • Sound to Text (Noise Clean)",
                    subtitle = "Convert voice to clean text with hardware DSP noise suppression",
                    palette = palette,
                    onClick = {
                        showFabSheet = false
                        showStandaloneAiMicSheet = true
                    }
                )
                FabSheetRow(
                    icon = Icons.Filled.FolderOpen,
                    iconBg = DocFlowTokens.IconCircleGreen,
                    title = "Open file from device",
                    subtitle = "Import PDF, Word (.docx), Excel (.xlsx), PPT (.pptx), TXT or image",
                    palette = palette,
                    onClick = {
                        showFabSheet = false
                        filePickerLauncher.launch(arrayOf("*/*"))
                    }
                )
                FabSheetRow(
                    icon = Icons.Filled.Description,
                    iconBg = DocFlowTokens.AccentBlue,
                    title = "Create Word Document (.docx)",
                    subtitle = "Open in full MS Word-style ribbon editor (Home, Insert, Layout, Review, View)",
                    palette = palette,
                    onClick = {
                        showFabSheet = false
                        createFormat = "docx"
                        newDocTitle = ""
                        newDocBody = ""
                        showCreateDocDialog = true
                    }
                )
                FabSheetRow(
                    icon = Icons.Filled.Slideshow,
                    iconBg = DocFlowTokens.IconCircleOrange,
                    title = "Create Presentation (.pptx)",
                    subtitle = "Author slide decks with speaker notes & export to PDF",
                    palette = palette,
                    onClick = {
                        showFabSheet = false
                        createFormat = "pptx"
                        newDocTitle = ""
                        newDocBody = ""
                        showCreateDocDialog = true
                    }
                )
                FabSheetRow(
                    icon = Icons.Filled.TableChart,
                    iconBg = DocFlowTokens.IconCircleExcelGreen,
                    title = "Create Spreadsheet (.xlsx)",
                    subtitle = "Build structured tables & financial sheets stored on this device",
                    palette = palette,
                    onClick = {
                        showFabSheet = false
                        createFormat = "xlsx"
                        newDocTitle = ""
                        newDocBody = ""
                        showCreateDocDialog = true
                    }
                )
                FabSheetRow(
                    icon = Icons.Filled.PictureAsPdf,
                    iconBg = DocFlowTokens.IconCircleRed,
                    title = "Create Note / PDF (.txt / .pdf)",
                    subtitle = "Author a clean note or instant PDF document on this device",
                    palette = palette,
                    onClick = {
                        showFabSheet = false
                        createFormat = "txt"
                        newDocTitle = ""
                        newDocBody = ""
                        showCreateDocDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space5))
            }
        }
    }

    // Create Document Dialog (Supports Word .docx, PPT .pptx, Excel .xlsx, Note .txt, PDF .pdf + AI Mic Noise Clean)
    if (showCreateDocDialog) {
        val formatOptions = listOf(
            "docx" to "Word (.docx)",
            "pptx" to "PPT (.pptx)",
            "xlsx" to "Excel (.xlsx)",
            "txt" to "Note (.txt)",
            "pdf" to "PDF (.pdf)"
        )
        AlertDialog(
            onDismissRequest = { showCreateDocDialog = false },
            title = {
                Text(
                    text = "Create Document (Open in Word UI)",
                    style = DocFlowTokens.sectionLabelStyle,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)) {
                    Text(
                        text = "Select format • Stored on this device",
                        style = DocFlowTokens.captionStyle,
                        color = palette.textSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                    ) {
                        formatOptions.forEach { (ext, label) ->
                            FilterChip(
                                selected = createFormat == ext,
                                onClick = { createFormat = ext },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.testTag("create_format_chip_$ext")
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newDocTitle,
                        onValueChange = { newDocTitle = it },
                        label = { Text("Document title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_doc_title_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Initial Content (Optional)",
                            style = DocFlowTokens.captionStyle,
                            color = palette.textSecondary
                        )
                        Row(
                            modifier = Modifier
                                .clip(DocFlowTokens.pillButtonShape)
                                .background(DocFlowTokens.IconCircleTeal.copy(alpha = 0.14f))
                                .clickable { showAiMicForCreate = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("create_doc_ai_mic_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = "AI Mic Noise Clean",
                                tint = DocFlowTokens.IconCircleTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "AI Mic (Noise Clean)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DocFlowTokens.IconCircleTeal
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newDocBody,
                        onValueChange = { newDocBody = it },
                        label = { Text("Type or use AI Mic voice-to-text...") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_doc_body_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = newDocTitle.trim().ifBlank {
                            when (createFormat) {
                                "pptx" -> "Untitled Presentation"
                                "xlsx" -> "Untitled Spreadsheet"
                                "txt" -> "Untitled Note"
                                "pdf" -> "Untitled PDF"
                                else -> "Untitled Word Document"
                            }
                        }
                        val ext = createFormat.lowercase()
                        val cleanName = title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                            .ifBlank { "document" } + ".$ext"
                        val bodyContent = newDocBody.ifBlank {
                            when (ext) {
                                "pptx" -> "=== SLIDE 1: $title ===\n• Key presentation point\n• Stored on this device"
                                "xlsx" -> "=== SHEET 1: $title ===\n| Item | Value | Notes |\n| Row 1 | 100 | Active |"
                                else -> title
                            }
                        }
                        viewModel.createOrScanDocument(
                            title = title,
                            folderId = "root",
                            category = "Work",
                            fileType = ext,
                            fileName = cleanName,
                            plainContent = bodyContent,
                            isEncrypted = false,
                            password = "",
                            encryptionKeyHint = null,
                            isOfflineCached = true,
                            pageCount = 1L,
                            customTags = "created-by-me, $ext",
                            createdByMe = true,
                            onCreated = { createdId ->
                                showCreateDocDialog = false
                                onOpenDocument(createdId)
                            }
                        )
                    },
                    modifier = Modifier.testTag("create_doc_confirm_button")
                ) {
                    Text("Create & Open in Word UI", fontWeight = FontWeight.Bold, color = DocFlowTokens.AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDocDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAiMicForCreate) {
        AiMicDictationBottomSheet(
            palette = palette,
            initialText = newDocBody,
            confirmButtonLabel = "Use Clean Text",
            onDismiss = { showAiMicForCreate = false },
            onConfirmCleanText = { cleaned ->
                newDocBody = cleaned
                if (newDocTitle.isBlank()) {
                    newDocTitle = cleaned.lines().firstOrNull()?.take(36)?.trim() ?: "Voice Dictation Note"
                }
                showAiMicForCreate = false
            }
        )
    }

    if (showStandaloneAiMicSheet) {
        AiMicDictationBottomSheet(
            palette = palette,
            initialText = "",
            confirmButtonLabel = "Create Word Doc from Voice",
            onDismiss = { showStandaloneAiMicSheet = false },
            onConfirmCleanText = { cleaned ->
                val title = cleaned.lines().firstOrNull()?.take(36)?.trim()?.ifBlank { "AI Mic Voice Document" }
                    ?: "AI Mic Voice Document"
                val cleanName = title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                    .ifBlank { "voice_doc" } + ".docx"
                viewModel.createOrScanDocument(
                    title = title,
                    folderId = "root",
                    category = "Work",
                    fileType = "docx",
                    fileName = cleanName,
                    plainContent = cleaned,
                    isEncrypted = false,
                    password = "",
                    encryptionKeyHint = null,
                    isOfflineCached = true,
                    pageCount = 1L,
                    customTags = "ai-mic, noise-clean, voice-to-text, created-by-me, docx",
                    createdByMe = true,
                    onCreated = { docId ->
                        showStandaloneAiMicSheet = false
                        onOpenDocument(docId)
                    }
                )
            }
        )
    }

    // Rename Dialog
    docForRename?.let { targetDoc ->
        AlertDialog(
            onDismissRequest = { docForRename = null },
            title = { Text("Rename document") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedTitle = renameText.trim().ifBlank { targetDoc.title }
                        viewModel.updateDocumentContent(
                            doc = targetDoc,
                            newTitle = updatedTitle,
                            newFolderId = targetDoc.folderId,
                            newCategory = targetDoc.category,
                            plainContent = targetDoc.content,
                            isEncrypted = targetDoc.isEncrypted,
                            password = null,
                            encryptionKeyHint = targetDoc.encryptionKeyHint,
                            isOfflineCached = true
                        )
                        docForRename = null
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = DocFlowTokens.AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { docForRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Move to Folder Dialog
    if (showMoveFolderDialog) {
        AlertDialog(
            onDismissRequest = { showMoveFolderDialog = false },
            title = { Text("Move ${selectedDocIds.size} item(s) to folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)) {
                    folders.forEach { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(DocFlowTokens.buttonShape)
                                .clickable {
                                    viewModel.moveDocumentsToFolder(selectedDocIds, folder.id)
                                    showMoveFolderDialog = false
                                    isMultiSelectMode = false
                                    selectedDocIds = emptySet()
                                }
                                .padding(DocFlowTokens.space3),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
                        ) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = DocFlowTokens.AccentBlue)
                            Text(folder.name, style = DocFlowTokens.rowTitleStyle, color = palette.textPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoveFolderDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Batch Tag Dialog
    if (showBatchTagDialog) {
        AlertDialog(
            onDismissRequest = { showBatchTagDialog = false },
            title = { Text("Tag ${selectedDocIds.size} selected item(s)") },
            text = {
                OutlinedTextField(
                    value = batchTagInput,
                    onValueChange = { batchTagInput = it },
                    label = { Text("Tags (comma separated, e.g. urgent, tax)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.batchTagDocuments(selectedDocIds, batchTagInput)
                        batchTagInput = ""
                        showBatchTagDialog = false
                        isMultiSelectMode = false
                        selectedDocIds = emptySet()
                    }
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold, color = DocFlowTokens.AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchTagDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentDocumentFlatRow(
    doc: DocEntity,
    cached: CachedDocumentEntity?,
    palette: DocFlowPalette,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onMoveToSafe: () -> Unit,
    onDelete: () -> Unit
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    val ft = doc.fileType.lowercase()

    val badgeColor = when (ft) {
        "pdf", "scanned" -> DocFlowTokens.IconCircleRed
        "docx", "doc" -> DocFlowTokens.AccentBlue
        "xlsx", "xls", "csv" -> DocFlowTokens.IconCircleExcelGreen
        "pptx", "ppt" -> DocFlowTokens.IconCircleOrange
        else -> DocFlowTokens.IconCircleTeal
    }

    val badgeIcon = when (ft) {
        "pdf", "scanned" -> Icons.Filled.PictureAsPdf
        "docx", "doc" -> Icons.Filled.Description
        "xlsx", "xls", "csv" -> Icons.Filled.TableChart
        "pptx", "ppt" -> Icons.Filled.Slideshow
        "pic" -> Icons.Filled.Image
        else -> Icons.Filled.Description
    }

    val tsMillis = cached?.lastAccessedAtMillis ?: cached?.updatedAtMillis ?: ((doc.updatedAt?.seconds ?: 0L) * 1000L)
    val timeStr = remember(tsMillis) {
        if (tsMillis > 0L) {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(tsMillis)).lowercase()
        } else {
            "just now"
        }
    }
    val sizeStr = remember(doc.fileSize) {
        val kb = doc.fileSize.coerceAtLeast(1024L) / 1024.0
        if (kb >= 1024.0) String.format("%.1f MB", kb / 1024.0) else String.format("%.0f KB", kb)
    }
    val resumeLabel = if ((cached?.lastPageViewed ?: 0) > 0) {
        " • Page ${(cached?.lastPageViewed ?: 0) + 1}"
    } else {
        ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(
                horizontal = DocFlowTokens.cardPaddingMin,
                vertical = DocFlowTokens.space3
            )
            .testTag("doc_row_${doc.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMultiSelectMode) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) DocFlowTokens.AccentBlue else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) DocFlowTokens.AccentBlue else palette.textSecondary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(DocFlowTokens.space3))
        }

        // File Type Thumbnail / Icon with 8dp token radius
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(DocFlowTokens.thumbnailShape)
                .background(badgeColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = doc.fileType,
                tint = badgeColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(DocFlowTokens.space3))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.title,
                style = DocFlowTokens.rowTitleStyle,
                color = palette.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$timeStr • $sizeStr • ${doc.fileType.uppercase()}$resumeLabel • Stored on this device",
                style = DocFlowTokens.captionStyle,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box {
            IconButton(
                onClick = { showOverflowMenu = true },
                modifier = Modifier.size(DocFlowTokens.minTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Document actions",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = { showOverflowMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = {
                        showOverflowMenu = false
                        onShare()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        showOverflowMenu = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Move to Private Safe") },
                    onClick = {
                        showOverflowMenu = false
                        onMoveToSafe()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = DocFlowTokens.IconCircleRed) },
                    onClick = {
                        showOverflowMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun FabSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    palette: DocFlowPalette,
    onClick: () -> Unit
) {
    DocFlowListRow(
        title = title,
        subtitle = subtitle,
        palette = palette,
        onClick = onClick,
        leading = {
            DocFlowIconCircle(
                icon = icon,
                circleColor = iconBg
            )
        }
    )
}

private fun formatDateGroupHeader(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return "Today"
    val nowCal = Calendar.getInstance()
    val itemCal = Calendar.getInstance().apply { timeInMillis = timestampMillis }

    val sameYear = nowCal.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR)
    val dayDiff = nowCal.get(Calendar.DAY_OF_YEAR) - itemCal.get(Calendar.DAY_OF_YEAR)

    return when {
        sameYear && dayDiff == 0 -> "Today"
        sameYear && dayDiff == 1 -> "Yesterday"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestampMillis))
    }
}
