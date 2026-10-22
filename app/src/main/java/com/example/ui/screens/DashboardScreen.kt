package com.example.ui.screens

import android.net.Uri
import android.text.format.DateUtils
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CachedDocumentEntity
import com.example.data.local.SyncLogEntity
import com.example.data.model.ActiveCollaborator
import com.example.data.model.DocEntity
import com.example.data.model.FolderEntity
import com.example.data.sync.BackgroundSyncWorkerState
import com.example.data.util.OtaInstallPhase
import com.example.ui.theme.BadgeDocBlue
import com.example.ui.theme.BadgeOfdPurple
import com.example.ui.theme.BadgePdfRed
import com.example.ui.theme.BadgePptOrange
import com.example.ui.theme.BadgeTxtSlate
import com.example.ui.theme.BadgeXlsGreen
import com.example.ui.theme.DocFlowAccentBlue
import com.example.ui.viewmodel.SortOption
import com.example.ui.viewmodel.ThemeMode
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class FormatTabSpec(
    val label: String,
    val filterKey: String
)

private val FORMAT_FILTER_TABS = listOf(
    FormatTabSpec("All", "All"),
    FormatTabSpec("DOC", "docx"),
    FormatTabSpec("XLS", "xlsx"),
    FormatTabSpec("PPT", "pptx"),
    FormatTabSpec("PDF", "pdf"),
    FormatTabSpec("OFD", "ofd"),
    FormatTabSpec("TXT", "txt")
)

private val AUTO_CATEGORIES = listOf(
    "All", "Invoice", "Receipt", "Contract", "ID", "Medical", "Education", "Work", "Personal", "Notes", "Other"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    documentsState: UiState<List<DocEntity>>,
    filteredDocuments: List<DocEntity>,
    foldersState: UiState<List<FolderEntity>>,
    cachedDocumentsMap: Map<String, CachedDocumentEntity>,
    syncLogs: List<SyncLogEntity>,
    pendingSyncCount: Int,
    searchQuery: String,
    selectedFolderId: String,
    selectedCategory: String,
    selectedFileType: String,
    offlineOnlyFilter: Boolean,
    sortOption: SortOption,
    isOfflineMode: Boolean,
    themeMode: ThemeMode,
    statusBannerMessage: String?,
    onSearchQueryChange: (String) -> Unit,
    onSelectFolder: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectFileType: (String) -> Unit,
    onToggleOfflineOnlyFilter: () -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onToggleOfflineNetworkMode: () -> Unit,
    onToggleThemeMode: () -> Unit,
    onDismissStatusBanner: () -> Unit,
    onCreateFolder: (String, String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenDocument: (DocEntity) -> Unit,
    onToggleDocumentOffline: (DocEntity) -> Unit,
    onCreateDocument: (
        title: String,
        folderId: String,
        category: String,
        fileType: String,
        fileName: String,
        content: String,
        isEncrypted: Boolean,
        password: String,
        keyHint: String?,
        customTags: String
    ) -> Unit,
    onImportFileUri: (Uri) -> Unit,
    onSeedSampleWorkspace: () -> Unit = {},
    onShareDocument: (DocEntity) -> Unit = {},
    onClearSyncHistory: () -> Unit = {},
    onSignOut: () -> Unit = {},
    selectedTagFilters: Set<String> = emptySet(),
    biometricVaultEnabled: Boolean = true,
    onToggleTagFilter: (String) -> Unit = {},
    onClearTagFilters: () -> Unit = {},
    onToggleBiometricVault: () -> Unit = {},
    onOpenBatchProcessor: () -> Unit = {},
    onAddCustomTagToDoc: (DocEntity, String) -> Unit = { _, _ -> },
    onUpdateCategoryAndTags: (DocEntity, String, String) -> Unit = { _, _, _ -> },
    recentCachedDocuments: List<CachedDocumentEntity> = emptyList(),
    onOpenStorageDashboard: () -> Unit = {},
    activeCollaborators: List<ActiveCollaborator> = emptyList(),
    otaVersionName: String = "3.0.0-offline",
    otaInstallPhase: OtaInstallPhase = OtaInstallPhase.UP_TO_DATE,
    onOpenOtaAndSettings: () -> Unit = {},
    backgroundSyncWorkerState: BackgroundSyncWorkerState = BackgroundSyncWorkerState(),
    onTriggerWorkManagerSync: () -> Unit = {},
    onBatchSoftDelete: (Set<String>) -> Unit = {},
    onBatchMoveToFolder: (Set<String>, String) -> Unit = { _, _ -> },
    onBatchTagDocuments: (Set<String>, String) -> Unit = { _, _ -> },
    onMoveToPrivateSafe: (String, String) -> Unit = { _, _ -> }
) {
    var showSearchBar by remember { mutableStateOf(searchQuery.isNotBlank()) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedDocIds = remember { mutableStateListOf<String>() }
    val collapsedDateGroups = remember { mutableStateMapOf<String, Boolean>() }

    var showFabCreateSheet by remember { mutableStateOf(false) }
    var showNewDocDialog by remember { mutableStateOf(false) }
    var newDocDefaultFormat by remember { mutableStateOf("docx") }
    var showSortMenuDialog by remember { mutableStateOf(false) }
    var showBatchMoveDialog by remember { mutableStateOf(false) }
    var showBatchTagDialog by remember { mutableStateOf(false) }
    var docForQuickPreviewModal by remember { mutableStateOf<DocEntity?>(null) }
    var docForPrivateSafePrompt by remember { mutableStateOf<DocEntity?>(null) }
    var docForCategorizeModal by remember { mutableStateOf<DocEntity?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri -> onImportFileUri(uri) }
        }
    }

    val folders = (foldersState as? UiState.Success)?.data ?: emptyList()

    // Group documents by date string e.g. "17 Oct"
    val dateFormatter = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val groupedByDate = remember(filteredDocuments) {
        filteredDocuments.groupBy { doc ->
            val ts = doc.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
            dateFormatter.format(Date(ts))
        }
    }

    val allAvailableTags = remember(cachedDocumentsMap, selectedTagFilters) {
        (cachedDocumentsMap.values.flatMap { it.parsedTags() } + selectedTagFilters)
            .distinct()
            .take(12)
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val headerFontSize = when {
        screenWidthDp < 350 -> 30.sp
        screenWidthDp < 390 -> 34.sp
        else -> 38.sp
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (!isMultiSelectMode) {
                FloatingActionButton(
                    onClick = { showFabCreateSheet = true },
                    containerColor = DocFlowAccentBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(60.dp)
                        .testTag("recent_fab_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "New document, scan, or import",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            // Top Flat Header: Title "Recent" + Subtitle "N in total" + Checklist, Search, Settings icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Recent",
                        fontSize = headerFontSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.8).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${filteredDocuments.size} in total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { showSearchBar = !showSearchBar },
                        modifier = Modifier.testTag("recent_search_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search documents",
                            tint = if (showSearchBar || searchQuery.isNotBlank()) {
                                DocFlowAccentBlue
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            }
                        )
                    }

                    IconButton(
                        onClick = {
                            isMultiSelectMode = !isMultiSelectMode
                            if (!isMultiSelectMode) selectedDocIds.clear()
                        },
                        modifier = Modifier.testTag("recent_multiselect_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "Multi-select documents",
                            tint = if (isMultiSelectMode) {
                                DocFlowAccentBlue
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            }
                        )
                    }

                    IconButton(
                        onClick = { showSortMenuDialog = true },
                        modifier = Modifier.testTag("recent_sort_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort documents",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = onOpenOtaAndSettings,
                        modifier = Modifier.testTag("recent_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Status Banner (if any action completed)
            AnimatedVisibility(visible = !statusBannerMessage.isNullOrBlank()) {
                if (!statusBannerMessage.isNullOrBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DocFlowAccentBlue.copy(alpha = 0.12f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = statusBannerMessage,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onDismissStatusBanner,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss notification",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Expandable Search + Category & Tag Filter Bar (FTS4 full-text search across OCR & content)
            AnimatedVisibility(visible = showSearchBar || searchQuery.isNotBlank() || selectedCategory != "All" || selectedTagFilters.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input"),
                        placeholder = {
                            Text("Search filename, OCR text, or #tags...")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Category chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AUTO_CATEGORIES.forEach { cat ->
                            val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectCategory(cat) },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }

                    // Tag chips if available
                    if (allAvailableTags.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            allAvailableTags.forEach { tag ->
                                val active = selectedTagFilters.any { it.equals(tag, ignoreCase = true) }
                                FilterChip(
                                    selected = active,
                                    onClick = { onToggleTagFilter(tag) },
                                    label = { Text("#$tag", fontSize = 12.sp) }
                                )
                            }
                            if (selectedTagFilters.isNotEmpty()) {
                                TextButton(onClick = onClearTagFilters) {
                                    Text("Clear tags", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Horizontal Format Filter Tabs: All, DOC, XLS, PPT, PDF, OFD, TXT with red underline on active
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FORMAT_FILTER_TABS.forEach { tab ->
                    val isSelected = selectedFileType.equals(tab.filterKey, ignoreCase = true)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onSelectFileType(tab.filterKey) }
                            .padding(vertical = 4.dp)
                            .testTag("format_tab_${tab.label.lowercase()}")
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(26.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSelected) BadgePdfRed else Color.Transparent)
                        )
                    }
                }
            }

            // Multi-select Batch Action Bar
            AnimatedVisibility(visible = isMultiSelectMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedDocIds.size} selected",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        if (selectedDocIds.size == filteredDocuments.size) {
                                            selectedDocIds.clear()
                                        } else {
                                            selectedDocIds.clear()
                                            selectedDocIds.addAll(filteredDocuments.map { it.id })
                                        }
                                    }
                                ) {
                                    Text(if (selectedDocIds.size == filteredDocuments.size) "Deselect all" else "Select all")
                                }
                                TextButton(
                                    onClick = {
                                        isMultiSelectMode = false
                                        selectedDocIds.clear()
                                    }
                                ) {
                                    Text("Done")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(
                                enabled = selectedDocIds.isNotEmpty(),
                                onClick = {
                                    val firstDoc = filteredDocuments.firstOrNull { it.id in selectedDocIds }
                                    if (firstDoc != null) onShareDocument(firstDoc)
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 13.sp)
                            }
                            TextButton(
                                enabled = selectedDocIds.isNotEmpty(),
                                onClick = { showBatchMoveDialog = true }
                            ) {
                                Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Move", fontSize = 13.sp)
                            }
                            TextButton(
                                enabled = selectedDocIds.isNotEmpty(),
                                onClick = { showBatchTagDialog = true }
                            ) {
                                Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tag", fontSize = 13.sp)
                            }
                            TextButton(
                                enabled = selectedDocIds.isNotEmpty(),
                                onClick = {
                                    onBatchSoftDelete(selectedDocIds.toSet())
                                    selectedDocIds.clear()
                                    isMultiSelectMode = false
                                }
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = BadgePdfRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", color = BadgePdfRed, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Document List or Empty State
            if (filteredDocuments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(DocFlowAccentBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = DocFlowAccentBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Text(
                                text = "No documents found",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Open a PDF, Word, Excel, PowerPoint, or text file from your phone, or scan a paper document offline.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        openDocumentLauncher.launch(
                                            arrayOf(
                                                "application/pdf",
                                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                                "application/msword",
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                                "text/plain",
                                                "*/*"
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DocFlowAccentBlue,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("empty_state_open_file_button")
                                ) {
                                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open a file", fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = onOpenScanner,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.testTag("empty_state_scan_button")
                                ) {
                                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scan", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("recent_documents_list"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 6.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = groupedByDate.entries.toList(),
                        key = { it.key }
                    ) { (dateLabel, docsInGroup) ->
                        val isCollapsed = collapsedDateGroups[dateLabel] == true
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Date Group Header: "17 Oct | 2 items" + Collapse/Expand chevron
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        collapsedDateGroups[dateLabel] = !isCollapsed
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$dateLabel  |  ${docsInGroup.size} ${if (docsInGroup.size == 1) "item" else "items"}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = if (isCollapsed) "Expand date group" else "Collapse date group",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (!isCollapsed) {
                                // White Card with 24.dp corner radius containing the rows for this date
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        docsInGroup.forEachIndexed { index, doc ->
                                            val isSelected = doc.id in selectedDocIds
                                            val cached = cachedDocumentsMap[doc.id]
                                            val tags = cached?.parsedTags() ?: emptyList()

                                            RecentDocumentFlatRow(
                                                doc = doc,
                                                tags = tags,
                                                isMultiSelectMode = isMultiSelectMode,
                                                isSelected = isSelected,
                                                onClick = {
                                                    if (isMultiSelectMode) {
                                                        if (isSelected) {
                                                            selectedDocIds.remove(doc.id)
                                                        } else {
                                                            selectedDocIds.add(doc.id)
                                                        }
                                                    } else {
                                                        onOpenDocument(doc)
                                                    }
                                                },
                                                onLongClick = {
                                                    docForQuickPreviewModal = doc
                                                },
                                                onMoreClick = {
                                                    docForQuickPreviewModal = doc
                                                }
                                            )

                                            if (index < docsInGroup.lastIndex) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(start = 74.dp, end = 16.dp),
                                                    thickness = 0.7.dp,
                                                    color = MaterialTheme.colorScheme.outlineVariant
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
        }
        }
    }

    // FAB Bottom Sheet: New document (Word/Text), Scan document, Import file
    if (showFabCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFabCreateSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Create or Open",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))

                FabSheetOptionRow(
                    icon = Icons.Default.NoteAdd,
                    iconBgColor = BadgeDocBlue,
                    title = "New document (Word)",
                    subtitle = "Create a formatted .docx or text note offline",
                    testTag = "fab_sheet_new_doc",
                    onClick = {
                        showFabCreateSheet = false
                        newDocDefaultFormat = "docx"
                        showNewDocDialog = true
                    }
                )

                FabSheetOptionRow(
                    icon = Icons.Default.DocumentScanner,
                    iconBgColor = BadgePdfRed,
                    title = "Scan document",
                    subtitle = "Auto edge detect, perspective crop & offline OCR",
                    testTag = "fab_sheet_scan_doc",
                    onClick = {
                        showFabCreateSheet = false
                        onOpenScanner()
                    }
                )

                FabSheetOptionRow(
                    icon = Icons.Default.FileOpen,
                    iconBgColor = BadgeXlsGreen,
                    title = "Import file",
                    subtitle = "Open PDF, Word, Excel, PPT, or TXT from device",
                    testTag = "fab_sheet_import_file",
                    onClick = {
                        showFabCreateSheet = false
                        openDocumentLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                "text/plain",
                                "*/*"
                            )
                        )
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    // Quick-Preview & Metadata Modal (Long-press or More button)
    val previewDoc = docForQuickPreviewModal
    if (previewDoc != null) {
        val cached = cachedDocumentsMap[previewDoc.id]
        val tags = cached?.parsedTags() ?: emptyList()
        val updatedTimeMs = previewDoc.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
        val formattedDate = remember(updatedTimeMs) {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(updatedTimeMs))
        }

        AlertDialog(
            onDismissRequest = { docForQuickPreviewModal = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FlatFileFormatBadge(fileType = previewDoc.fileType, sizeDp = 40)
                    Column {
                        Text(
                            text = previewDoc.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = previewDoc.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Format: ${previewDoc.fileType.uppercase()}  •  Pages: ${previewDoc.pageCount.coerceAtLeast(1L)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Size: ${formatDocumentBytes(previewDoc.fileSize)}  •  Category: ${previewDoc.category}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Last modified: $formattedDate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (tags.isNotEmpty()) {
                                Text(
                                    text = "Tags: ${tags.joinToString(", ") { "#$it" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DocFlowAccentBlue
                                )
                            }
                        }
                    }

                    Text(
                        text = "Extracted Content / OCR Preview",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (previewDoc.isEncrypted) {
                            "AES-256-GCM Encrypted Document"
                        } else {
                            previewDoc.content.take(280).ifBlank { "No text preview available." }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                docForQuickPreviewModal = null
                                docForCategorizeModal = previewDoc
                            }
                        ) {
                            Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tags & Category")
                        }

                        TextButton(
                            onClick = {
                                docForQuickPreviewModal = null
                                docForPrivateSafePrompt = previewDoc
                            }
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Private Safe")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                docForQuickPreviewModal = null
                                onShareDocument(previewDoc)
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }

                        TextButton(
                            onClick = {
                                val id = previewDoc.id
                                docForQuickPreviewModal = null
                                onBatchSoftDelete(setOf(id))
                            }
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = BadgePdfRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", color = BadgePdfRed)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val doc = previewDoc
                        docForQuickPreviewModal = null
                        onOpenDocument(doc)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DocFlowAccentBlue)
                ) {
                    Text("Open Document", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { docForQuickPreviewModal = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Move to Private Safe Dialog
    val safeDoc = docForPrivateSafePrompt
    if (safeDoc != null) {
        var pinInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { docForPrivateSafePrompt = null },
            title = { Text("Move to Private Safe", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "'${safeDoc.title}' will be encrypted with AES-256-GCM and hidden from Recent and Search.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("Private Safe PIN (default 1234)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMoveToPrivateSafe(safeDoc.id, pinInput.ifBlank { "1234" })
                        docForPrivateSafePrompt = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DocFlowAccentBlue)
                ) {
                    Text("Encrypt & Move", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { docForPrivateSafePrompt = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Categorize & Tag Modal
    val catDoc = docForCategorizeModal
    if (catDoc != null) {
        val currentTags = cachedDocumentsMap[catDoc.id]?.parsedTags()?.joinToString(", ") ?: ""
        var selectedCat by remember(catDoc) { mutableStateOf(catDoc.category.ifBlank { "Other" }) }
        var tagsInput by remember(catDoc) { mutableStateOf(currentTags) }

        AlertDialog(
            onDismissRequest = { docForCategorizeModal = null },
            title = { Text("Category & Tags", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AUTO_CATEGORIES.filter { it != "All" }.forEach { cat ->
                            FilterChip(
                                selected = selectedCat.equals(cat, ignoreCase = true),
                                onClick = { selectedCat = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        label = { Text("Tags (comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateCategoryAndTags(catDoc, selectedCat, tagsInput)
                        docForCategorizeModal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DocFlowAccentBlue)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { docForCategorizeModal = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // New Document (Word / Text) Dialog
    if (showNewDocDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var fileType by remember { mutableStateOf(newDocDefaultFormat) }
        var tags by remember { mutableStateOf("created-by-me") }

        AlertDialog(
            onDismissRequest = { showNewDocDialog = false },
            title = { Text("New Document", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Document Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("docx" to "Word (.docx)", "txt" to "Text (.txt)", "pdf" to "PDF (.pdf)").forEach { (ext, label) ->
                            FilterChip(
                                selected = fileType == ext,
                                onClick = { fileType = ext },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Document Content") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Tags (comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalTitle = title.trim().ifBlank { "Untitled Document" }
                        val cleanName = finalTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_') + ".$fileType"
                        onCreateDocument(
                            finalTitle,
                            if (selectedFolderId == "all") "root" else selectedFolderId,
                            "Work",
                            fileType,
                            cleanName,
                            content.ifBlank { finalTitle },
                            false,
                            "",
                            null,
                            tags
                        )
                        showNewDocDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DocFlowAccentBlue)
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewDocDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sort Option Dialog
    if (showSortMenuDialog) {
        AlertDialog(
            onDismissRequest = { showSortMenuDialog = false },
            title = { Text("Sort Documents", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    SortOption.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSortOptionChange(option)
                                    showSortMenuDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                            )
                            if (sortOption == option) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = DocFlowAccentBlue)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortMenuDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Batch Move to Folder Dialog
    if (showBatchMoveDialog) {
        AlertDialog(
            onDismissRequest = { showBatchMoveDialog = false },
            title = { Text("Move ${selectedDocIds.size} item(s) to Folder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (folders.isEmpty()) {
                        Text("No custom folders yet. Create folders in the Manage tab.")
                    } else {
                        folders.forEach { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onBatchMoveToFolder(selectedDocIds.toSet(), folder.id)
                                        selectedDocIds.clear()
                                        isMultiSelectMode = false
                                        showBatchMoveDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = DocFlowAccentBlue)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(folder.name, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBatchMoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Batch Tag Dialog
    if (showBatchTagDialog) {
        var batchTagText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBatchTagDialog = false },
            title = { Text("Tag ${selectedDocIds.size} item(s)", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = batchTagText,
                    onValueChange = { batchTagText = it },
                    label = { Text("Tags (e.g. tax, urgent, 2026)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBatchTagDocuments(selectedDocIds.toSet(), batchTagText)
                        selectedDocIds.clear()
                        isMultiSelectMode = false
                        showBatchTagDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DocFlowAccentBlue)
                ) {
                    Text("Apply Tags", color = Color.White)
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
    tags: List<String>,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val updatedMs = doc.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
    val relativeTime = remember(updatedMs) {
        DateUtils.getRelativeTimeSpanString(
            updatedMs,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("doc_row_${doc.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlatFileFormatBadge(fileType = doc.fileType, sizeDp = 44)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$relativeTime  •  ${formatDocumentBytes(doc.fileSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (doc.category.isNotBlank() && !doc.category.equals("Other", ignoreCase = true)) {
                    Surface(
                        color = DocFlowAccentBlue.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = doc.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DocFlowAccentBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        if (isMultiSelectMode) {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) DocFlowAccentBlue else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "Document options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FlatFileFormatBadge(
    fileType: String,
    sizeDp: Int = 44
) {
    val clean = fileType.lowercase().trim()
    val (bgColor, shortLabel) = when {
        clean.contains("pdf") -> BadgePdfRed to "PDF"
        clean.contains("doc") -> BadgeDocBlue to "W"
        clean.contains("xls") || clean.contains("csv") -> BadgeXlsGreen to "X"
        clean.contains("ppt") -> BadgePptOrange to "P"
        clean.contains("ofd") -> BadgeOfdPurple to "OFD"
        else -> BadgeTxtSlate to "TXT"
    }

    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = shortLabel,
            color = Color.White,
            fontSize = if (shortLabel.length > 2) 12.sp else 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun FabSheetOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBgColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconBgColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDocumentBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(512L)
    return when {
        safe >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", safe / (1024f * 1024f))
        safe >= 1024L -> "${safe / 1024L} KB"
        else -> "$safe B"
    }
}
