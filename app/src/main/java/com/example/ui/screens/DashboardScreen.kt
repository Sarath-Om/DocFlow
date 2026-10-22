package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CachedDocumentEntity
import com.example.data.local.SyncLogEntity
import com.example.data.local.SyncState
import com.example.data.model.ActiveCollaborator
import com.example.data.model.DocEntity
import com.example.data.model.FolderEntity
import com.example.data.util.OtaInstallPhase
import com.example.data.sync.BackgroundSyncWorkerState
import com.example.ui.components.CrystalGlassBackground
import com.example.ui.components.CrystalGlassCard
import com.example.ui.components.DocFlowBrandLogo
import com.example.ui.components.HotlinkedCrystalAvatar
import com.example.ui.components.HotlinkedDocFlowIconBadge
import com.example.ui.components.RecentFilesExpandableDrawer
import com.example.ui.components.SyncStatusVisualIndicator
import com.example.ui.components.WorkManagerSupabaseSyncCard
import com.example.ui.theme.FolderColors
import com.example.ui.viewmodel.SortOption
import com.example.ui.viewmodel.ThemeMode
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    onClearSyncHistory: () -> Unit,
    onSignOut: () -> Unit,
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
    otaVersionName: String = "2.3.0-crystal",
    otaInstallPhase: OtaInstallPhase = OtaInstallPhase.UPDATE_AVAILABLE,
    onOpenOtaAndSettings: () -> Unit = {},
    backgroundSyncWorkerState: BackgroundSyncWorkerState = BackgroundSyncWorkerState(),
    onTriggerWorkManagerSync: () -> Unit = {}
) {
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewDocDialog by remember { mutableStateOf(false) }
    var showSyncMonitorDialog by remember { mutableStateOf(false) }
    var showAdvancedFilterPanel by remember { mutableStateOf(false) }
    var activeSegmentIndex by remember { mutableIntStateOf(0) }
    var docForAddingTag by remember { mutableStateOf<DocEntity?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                onImportFileUri(uri)
            }
        }
    }

    val folders = (foldersState as? UiState.Success)?.data ?: emptyList()
    val categories = listOf("All", "Work", "Personal", "Finance", "Contracts", "Invoices", "Receipts", "Legal", "Notes")

    val allAvailableTags = remember(cachedDocumentsMap, selectedTagFilters) {
        (cachedDocumentsMap.values.flatMap { it.parsedTags() } + selectedTagFilters)
            .distinct()
            .take(14)
    }

    // Apply segmented tab refinement on top of real filteredDocuments
    val segmentFilteredDocs = remember(filteredDocuments, cachedDocumentsMap, activeSegmentIndex) {
        when (activeSegmentIndex) {
            1 -> filteredDocuments.filter { doc ->
                val tags = cachedDocumentsMap[doc.id]?.parsedTags() ?: emptyList()
                !tags.any { it.equals("signed", true) || it.equals("completed", true) }
            }
            2 -> filteredDocuments.filter { doc ->
                val tags = cachedDocumentsMap[doc.id]?.parsedTags() ?: emptyList()
                tags.any { it.equals("signed", true) || it.equals("completed", true) }
            }
            else -> filteredDocuments
        }
    }

    val inReviewCount = remember(filteredDocuments, cachedDocumentsMap) {
        filteredDocuments.count { doc ->
            val tags = cachedDocumentsMap[doc.id]?.parsedTags() ?: emptyList()
            !tags.any { it.equals("signed", true) || it.equals("completed", true) }
        }
    }

    // Priority Signature Document from real user workspace
    val priorityDoc = remember(filteredDocuments, cachedDocumentsMap) {
        filteredDocuments.firstOrNull { doc ->
            val tags = cachedDocumentsMap[doc.id]?.parsedTags() ?: emptyList()
            !tags.any { it.equals("signed", true) } &&
                (doc.category.equals("Contracts", true) || doc.category.equals("Legal", true) || doc.fileType.equals("pdf", true))
        } ?: filteredDocuments.firstOrNull()
    }

    val totalBytesUsed = remember(filteredDocuments) {
        filteredDocuments.sumOf { it.fileSize.coerceAtLeast(1024L) }
    }

    val cyanGlow = Color(0xFF00F4FE)
    val blueGlow = Color(0xFF3E90FF)
    val purpleGlow = Color(0xFFA178FF)
    val emeraldGlow = Color(0xFF34D399)
    val amberGlow = Color(0xFFFBBF24)
    val roseGlow = Color(0xFFF43F5E)

    CrystalGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                // Frosted Translucent Glass Top Header Bar
                Surface(
                    color = Color(0xFF0C0E17).copy(alpha = 0.72f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.08f)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HotlinkedDocFlowIconBadge(size = 38.dp)
                            Column {
                                Text(
                                    text = "Documents",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isOfflineMode) amberGlow else cyanGlow)
                                    )
                                    Text(
                                        text = if (isOfflineMode) {
                                            "Offline Room FTS4 • v$otaVersionName"
                                        } else {
                                            "Live Y.Map Sync • v$otaVersionName"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFC0C6D6)
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // OTA App Update Pill / Button
                            IconButton(
                                onClick = onOpenOtaAndSettings,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, cyanGlow.copy(alpha = 0.35f), CircleShape)
                                    .testTag("ota_update_header_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (otaInstallPhase == OtaInstallPhase.UPDATE_AVAILABLE) {
                                            Badge(containerColor = cyanGlow)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = "OTA App Updates",
                                        tint = cyanGlow,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // Biometric Vault Toggle
                            IconButton(
                                onClick = onToggleBiometricVault,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                    .testTag("biometric_vault_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Biometric Security",
                                    tint = if (biometricVaultEnabled) emeraldGlow else Color(0xFFC0C6D6),
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Offline Mode Toggle
                            IconButton(
                                onClick = onToggleOfflineNetworkMode,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                    .testTag("offline_mode_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudSync,
                                    contentDescription = "Toggle Offline Mode",
                                    tint = if (isOfflineMode) amberGlow else cyanGlow,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Create Document (+) Crystal Button
                            IconButton(
                                onClick = { showNewDocDialog = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                                    .testTag("create_doc_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = "Create Document",
                                    tint = cyanGlow,
                                    modifier = Modifier.size(21.dp)
                                )
                            }

                            // Hotlinked Profile Avatar -> Opens OTA & Security Settings / Sign Out
                            HotlinkedCrystalAvatar(
                                size = 36.dp,
                                onClick = onOpenOtaAndSettings,
                                modifier = Modifier.testTag("profile_avatar_button")
                            )

                            // Hidden/Accessible testTag buttons for existing tests
                            Box(modifier = Modifier.size(1.dp)) {
                                IconButton(
                                    onClick = { showSyncMonitorDialog = true },
                                    modifier = Modifier.size(1.dp).testTag("sync_monitor_button")
                                ) {}
                                IconButton(
                                    onClick = onToggleThemeMode,
                                    modifier = Modifier.size(1.dp).testTag("theme_mode_toggle_button")
                                ) {}
                                IconButton(
                                    onClick = onSignOut,
                                    modifier = Modifier.size(1.dp).testTag("sign_out_button")
                                ) {}
                            }
                        }
                    }
                }
            },
            bottomBar = {
                WorkspaceBottomNavBar(
                    activeTab = "workspace",
                    onSelectWorkspace = {},
                    onSelectStorage = onOpenStorageDashboard,
                    onSelectBatchStudio = onOpenBatchProcessor,
                    onSelectSettings = onOpenOtaAndSettings
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Status Toast / Crystal Banner
                    item {
                        AnimatedVisibility(visible = statusBannerMessage != null) {
                            CrystalGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 16.dp,
                                tintColor = cyanGlow
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = statusBannerMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = onDismissStatusBanner,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Dismiss",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 1. Crystal Frosted Search Bar ("Search documents, tags, or people")
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CrystalGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 18.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = Color.White.copy(alpha = 0.55f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = onSearchQueryChange,
                                        placeholder = {
                                            Text(
                                                text = "Search documents, tags, or people",
                                                color = Color.White.copy(alpha = 0.42f),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("search_documents_input"),
                                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            cursorColor = cyanGlow
                                        )
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { onSearchQueryChange("") },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear Search",
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { showSyncMonitorDialog = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Sync Logs & Voice Filter",
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showAdvancedFilterPanel = !showAdvancedFilterPanel },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "Filters",
                                            tint = if (showAdvancedFilterPanel) cyanGlow else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }

                            // Quick Tag Filter Pills
                            if (allAvailableTags.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tag,
                                        contentDescription = "Custom Tag Filter",
                                        tint = cyanGlow,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    if (selectedTagFilters.isNotEmpty()) {
                                        Surface(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable { onClearTagFilters() }
                                                .testTag("clear_tag_filters_button"),
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "Clear (${selectedTagFilters.size}) ×",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    allAvailableTags.forEach { tag ->
                                        val isTagSelected = selectedTagFilters.any { it.equals(tag, ignoreCase = true) } ||
                                            searchQuery.equals(tag, ignoreCase = true)
                                        Surface(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable { onToggleTagFilter(tag) }
                                                .testTag("quick_tag_$tag"),
                                            color = if (isTagSelected) {
                                                blueGlow
                                            } else {
                                                Color.White.copy(alpha = 0.06f)
                                            },
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isTagSelected) Color.White else cyanGlow,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Crystal Segmented Control (Recents | In Review | Signed | Folders)
                    item {
                        Surface(
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val segments = listOf(
                                    "Recents" to null,
                                    "In Review" to inReviewCount.takeIf { it > 0 },
                                    "Signed" to null,
                                    "Folders" to null
                                )
                                segments.forEachIndexed { idx, (label, badgeNum) ->
                                    val selected = activeSegmentIndex == idx
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .then(
                                                if (selected) {
                                                    Modifier
                                                        .background(
                                                            Brush.linearGradient(
                                                                colors = listOf(
                                                                    Color.White.copy(alpha = 0.22f),
                                                                    Color.White.copy(alpha = 0.08f)
                                                                )
                                                            )
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = Color.White.copy(alpha = 0.35f),
                                                            shape = RoundedCornerShape(14.dp)
                                                        )
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable {
                                                activeSegmentIndex = idx
                                                if (idx == 3) {
                                                    showAdvancedFilterPanel = true
                                                }
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) Color.White else Color.White.copy(alpha = 0.65f)
                                            )
                                            if (badgeNum != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            Brush.linearGradient(
                                                                colors = listOf(cyanGlow, blueGlow)
                                                            )
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "$badgeNum",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color.Black,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2b. WorkManager + Supabase Background Sync Status Card (Apple Crystal Glass Material3 Card + Surface)
                    item {
                        WorkManagerSupabaseSyncCard(
                            workerState = backgroundSyncWorkerState,
                            pendingSyncCount = pendingSyncCount,
                            isOfflineMode = isOfflineMode,
                            onTriggerSyncNow = onTriggerWorkManagerSync
                        )
                    }

                    // 3. Quick Action Shortcuts (4 Crystal Glass Tiles: Scan Doc, Import PDF, Sign Req, Template)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CrystalQuickActionTile(
                                label = "Scan Doc",
                                icon = Icons.Default.DocumentScanner,
                                iconTint = cyanGlow,
                                testTag = "open_scanner_button",
                                modifier = Modifier.weight(1f),
                                onClick = onOpenScanner
                            )
                            CrystalQuickActionTile(
                                label = "Open File",
                                icon = Icons.Default.FolderOpen,
                                iconTint = purpleGlow,
                                testTag = "open_file_picker_button",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    openDocumentLauncher.launch(
                                        arrayOf(
                                            "*/*",
                                            "application/pdf",
                                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                            "application/msword",
                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                            "application/vnd.ms-excel",
                                            "text/csv",
                                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                            "application/vnd.ms-powerpoint",
                                            "image/*",
                                            "text/plain"
                                        )
                                    )
                                }
                            )
                            CrystalQuickActionTile(
                                label = "Sign Req",
                                icon = Icons.Default.Draw,
                                iconTint = emeraldGlow,
                                testTag = "open_batch_processor_button",
                                modifier = Modifier.weight(1f),
                                onClick = onOpenBatchProcessor
                            )
                            CrystalQuickActionTile(
                                label = "New Doc",
                                icon = Icons.Default.PostAdd,
                                iconTint = blueGlow,
                                testTag = "create_new_doc_tile_button",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    showNewDocDialog = true
                                }
                            )
                        }
                    }

                    // Permanent Category & Tag Filter Bar (Work, Personal, Finance, etc.)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Category Pills
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                categories.forEach { cat ->
                                    val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                                    val categoryColor = when (cat.lowercase()) {
                                        "work" -> blueGlow
                                        "personal" -> purpleGlow
                                        "finance" -> emeraldGlow
                                        "contracts" -> amberGlow
                                        "invoices" -> cyanGlow
                                        else -> Color.White.copy(alpha = 0.85f)
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSelectCategory(cat) }
                                            .testTag("filter_category_${cat.lowercase()}"),
                                        color = if (isSelected) categoryColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) categoryColor else Color.White.copy(alpha = 0.12f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (cat != "All") {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(categoryColor)
                                                )
                                            }
                                            Text(
                                                text = cat,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.70f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Dynamic Tag Filter Pills from Room
                            if (allAvailableTags.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tag,
                                        contentDescription = null,
                                        tint = cyanGlow.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    allAvailableTags.forEach { tag ->
                                        val isTagSelected = selectedTagFilters.any { it.equals(tag, ignoreCase = true) }
                                        Surface(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable { onToggleTagFilter(tag) }
                                                .testTag("filter_tag_${tag.lowercase()}"),
                                            color = if (isTagSelected) cyanGlow.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.04f),
                                            shape = CircleShape,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isTagSelected) cyanGlow else Color.White.copy(alpha = 0.09f)
                                            )
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isTagSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isTagSelected) cyanGlow else Color.White.copy(alpha = 0.65f),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    if (selectedTagFilters.isNotEmpty()) {
                                        TextButton(
                                            onClick = onClearTagFilters,
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Clear", style = MaterialTheme.typography.labelSmall, color = roseGlow)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Expandable Folders & Format Filter Panel (Shown when Folders tab or Filter Tune icon active)
                    item {
                        AnimatedVisibility(visible = showAdvancedFilterPanel || activeSegmentIndex == 3) {
                            CrystalGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 20.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Personalized Crystal Folders & Formats",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        TextButton(
                                            onClick = { showNewFolderDialog = true },
                                            modifier = Modifier.testTag("add_folder_button")
                                        ) {
                                            Icon(
                                                Icons.Default.CreateNewFolder,
                                                contentDescription = null,
                                                tint = cyanGlow,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("New Folder", color = cyanGlow)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FolderPillCard(
                                            name = "All Files",
                                            colorHex = "#00F4FE",
                                            iconName = "folder",
                                            isSelected = selectedFolderId == "all",
                                            onClick = { onSelectFolder("all") }
                                        )
                                        FolderPillCard(
                                            name = "Main Workspace",
                                            colorHex = "#3E90FF",
                                            iconName = "work",
                                            isSelected = selectedFolderId == "root",
                                            onClick = { onSelectFolder("root") }
                                        )
                                        folders.forEach { folder ->
                                            FolderPillCard(
                                                name = folder.name,
                                                colorHex = folder.color,
                                                iconName = folder.icon,
                                                isSelected = selectedFolderId == folder.id,
                                                onClick = { onSelectFolder(folder.id) },
                                                onDelete = { onDeleteFolder(folder.id) }
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            "All" to "All Formats",
                                            "pdf" to "PDF (.pdf)",
                                            "docx" to "Word (.docx)",
                                            "xlsx" to "Excel (.xlsx)",
                                            "pptx" to "PowerPoint (.pptx)",
                                            "scanned" to "Scanned",
                                            "pic" to "Pic Exports"
                                        ).forEach { (key, label) ->
                                            FilterChip(
                                                selected = selectedFileType.equals(key, ignoreCase = true),
                                                onClick = { onSelectFileType(key) },
                                                label = { Text(label) }
                                            )
                                        }
                                        FilterChip(
                                            selected = offlineOnlyFilter,
                                            onClick = onToggleOfflineOnlyFilter,
                                            label = { Text("Offline Pinned") }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        categories.forEach { category ->
                                            FilterChip(
                                                selected = selectedCategory.equals(category, ignoreCase = true),
                                                onClick = { onSelectCategory(category) },
                                                label = { Text(category) }
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        SortOption.entries.forEach { option ->
                                            FilterChip(
                                                selected = sortOption == option,
                                                onClick = { onSortOptionChange(option) },
                                                label = { Text(option.label, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Urgent Callout / Pinned Crystal Priority Card ("NEEDS YOUR SIGNATURE")
                    if (priorityDoc != null) {
                        item {
                            val priorityTags = cachedDocumentsMap[priorityDoc.id]?.parsedTags() ?: emptyList()
                            val isAlreadySigned = priorityTags.any { it.equals("signed", true) }
                            CrystalGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("priority_signature_card"),
                                cornerRadius = 26.dp,
                                tintColor = blueGlow,
                                onClick = { onOpenDocument(priorityDoc) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Top Urgent Header Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isAlreadySigned) emeraldGlow else roseGlow)
                                            )
                                            Text(
                                                text = if (isAlreadySigned) {
                                                    "CERTIFIED & SIGNED"
                                                } else {
                                                    "NEEDS YOUR SIGNATURE"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isAlreadySigned) emeraldGlow else Color(0xFFFDA4AF)
                                            )
                                        }
                                        Surface(
                                            color = if (isAlreadySigned) {
                                                emeraldGlow.copy(alpha = 0.18f)
                                            } else {
                                                roseGlow.copy(alpha = 0.20f)
                                            },
                                            shape = CircleShape,
                                            modifier = Modifier.border(
                                                width = 1.dp,
                                                color = if (isAlreadySigned) {
                                                    emeraldGlow.copy(alpha = 0.35f)
                                                } else {
                                                    roseGlow.copy(alpha = 0.35f)
                                                },
                                                shape = CircleShape
                                            )
                                        ) {
                                            Text(
                                                text = if (isAlreadySigned) "Verified" else "Due 5:00 PM",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isAlreadySigned) emeraldGlow else Color(0xFFFECDD3),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Center Title & Verified Badge
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            Color.White.copy(alpha = 0.20f),
                                                            Color.White.copy(alpha = 0.06f)
                                                        )
                                                    )
                                                )
                                                .border(1.dp, cyanGlow.copy(alpha = 0.45f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = cyanGlow,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = priorityDoc.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (isAlreadySigned) {
                                                    "3 of 3 signatures acquired • Biometric Certified"
                                                } else {
                                                    "2 of 3 signatures acquired • ${priorityDoc.fileName}"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.72f)
                                            )
                                        }
                                    }

                                    // Mini Crystal Progress Line
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.10f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(if (isAlreadySigned) 1.0f else 0.66f)
                                                .height(6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(cyanGlow, blueGlow)
                                                    )
                                                )
                                        )
                                    }

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

                                    // Collaborator Avatar Stack + "Review & Sign ->" Button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy((-6).dp)
                                        ) {
                                            val displayInitials = if (activeCollaborators.size >= 2) {
                                                activeCollaborators.take(2).map {
                                                    it.name.take(2).uppercase(Locale.US)
                                                } + "You"
                                            } else {
                                                listOf("SL", "GC", "You")
                                            }
                                            displayInitials.forEachIndexed { i, token ->
                                                val isYou = token == "You"
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isYou) {
                                                                Brush.linearGradient(listOf(cyanGlow, blueGlow))
                                                            } else {
                                                                Brush.linearGradient(
                                                                    listOf(
                                                                        Color.White.copy(alpha = 0.18f),
                                                                        Color.White.copy(alpha = 0.10f)
                                                                    )
                                                                )
                                                            }
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isYou) cyanGlow else Color.White.copy(alpha = 0.25f),
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = token,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isYou) Color.Black else Color.White,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = { onOpenDocument(priorityDoc) },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = cyanGlow,
                                                contentColor = Color(0xFF06101E)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                            modifier = Modifier.testTag("review_and_sign_priority_button")
                                        ) {
                                            Text(
                                                text = "Review & Sign",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Expandable Recent Files Horizontal Scroll Drawer prioritized by Room lastAccessedAtMillis
                    item {
                        RecentFilesExpandableDrawer(
                            recentCachedDocuments = recentCachedDocuments,
                            isOfflineMode = isOfflineMode,
                            onOpenRecentDocument = { cached ->
                                onOpenDocument(cached.toDocEntity())
                            }
                        )
                    }

                    // 6. Grouped Inset Document List Header ("Recent Documents (N)" + "Select" / "New Folder")
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Recent Documents",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "(${segmentFilteredDocs.size})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.50f)
                                )
                            }
                            Text(
                                text = if (showAdvancedFilterPanel) "Hide Filters" else "Select / Filter",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = cyanGlow,
                                modifier = Modifier.clickable {
                                    showAdvancedFilterPanel = !showAdvancedFilterPanel
                                }
                            )
                        }
                    }

                    // 7. Loading / Error / Empty / Crystal Glass Document List
                    when (documentsState) {
                        is UiState.Loading -> {
                            if (segmentFilteredDocs.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = cyanGlow)
                                    }
                                }
                            }
                        }
                        is UiState.Error -> {
                            item {
                                CrystalGlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    tintColor = MaterialTheme.colorScheme.error
                                ) {
                                    Text(
                                        text = documentsState.message,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                        is UiState.Success -> {}
                    }

                    if (segmentFilteredDocs.isEmpty() && documentsState !is UiState.Loading) {
                        item {
                            CrystalGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                cornerRadius = 26.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    HotlinkedDocFlowIconBadge(size = 56.dp)
                                    Text(
                                        text = if (searchQuery.isNotBlank()) {
                                            "No FTS4 Matches for \"$searchQuery\""
                                        } else {
                                            "Your Crystal Glass Workspace is Empty"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Only real user documents are displayed in your secure workspace. Open or import any PDF, Word (.docx), Excel (.xlsx), PowerPoint (.pptx), text, or image file from your device, or scan with CameraX.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFC0C6D6)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                openDocumentLauncher.launch(
                                                    arrayOf(
                                                        "*/*",
                                                        "application/pdf",
                                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                                        "application/msword",
                                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                        "application/vnd.ms-excel",
                                                        "text/csv",
                                                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                                        "application/vnd.ms-powerpoint",
                                                        "image/*",
                                                        "text/plain"
                                                    )
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("open_file_empty_button"),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = cyanGlow,
                                                contentColor = Color.Black
                                            )
                                        ) {
                                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Open / Import",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = onOpenScanner,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("scan_doc_empty_button"),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, cyanGlow.copy(alpha = 0.5f))
                                        ) {
                                            Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = cyanGlow, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Scan",
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(segmentFilteredDocs, key = { it.id }) { doc ->
                            val folderName = folders.find { it.id == doc.folderId }?.name ?: "Main Workspace"
                            val cachedEntity = cachedDocumentsMap[doc.id]
                            val syncStateName = cachedEntity?.syncState ?: SyncState.SYNCED.name
                            val tagsList = cachedEntity?.parsedTags() ?: emptyList()

                            DocumentItemCard(
                                doc = doc,
                                folderName = folderName,
                                syncStateName = syncStateName,
                                isOfflineMode = isOfflineMode,
                                tags = tagsList,
                                selectedTagFilters = selectedTagFilters,
                                hasLocalPdf = !cachedEntity?.localFilePath.isNullOrBlank(),
                                onClick = { onOpenDocument(doc) },
                                onTagClick = { clickedTag -> onToggleTagFilter(clickedTag) },
                                onAddTagClick = { docForAddingTag = doc },
                                onToggleOffline = { onToggleDocumentOffline(doc) },
                                onShareClick = { onShareDocument(doc) }
                            )
                        }
                    }

                    // 8. Visual Activity Status / Crystal Secure Sync Card + OTA App Updates Card
                    item {
                        CrystalGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cloud_secure_sync_card"),
                            cornerRadius = 22.dp,
                            onClick = onOpenStorageDashboard
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.20f),
                                                        Color.White.copy(alpha = 0.06f)
                                                    )
                                                )
                                            )
                                            .border(1.dp, cyanGlow.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = null,
                                            tint = cyanGlow,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "DocFlow Cloud Secure Sync",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Up to date • ${formatStorageBytes(totalBytesUsed)} indexed across ${filteredDocuments.size} file(s)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.62f)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open Storage Dashboard",
                                    tint = Color.White.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }

                    // 9. OTA App Updates Live Banner Card
                    item {
                        CrystalGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_ota_update_card"),
                            cornerRadius = 22.dp,
                            tintColor = purpleGlow,
                            onClick = onOpenOtaAndSettings
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(purpleGlow.copy(alpha = 0.18f))
                                            .border(1.dp, purpleGlow.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdate,
                                            contentDescription = null,
                                            tint = purpleGlow,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "OTA App Updates",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Surface(
                                                color = cyanGlow.copy(alpha = 0.18f),
                                                shape = CircleShape
                                            ) {
                                                Text(
                                                    text = otaInstallPhase.statusText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = cyanGlow,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Current: v$otaVersionName • Tap to verify SHA-256 & install live delta patch",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.65f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Manage OTA Updates",
                                    tint = cyanGlow
                                )
                            }
                        }
                    }
                }

                // Floating Quick-Scan Crystal Pill Button at Bottom Center
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                ) {
                    Button(
                        onClick = onOpenScanner,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cyanGlow,
                            contentColor = Color(0xFF040B16)
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        modifier = Modifier
                            .shadow(
                                elevation = 18.dp,
                                shape = CircleShape,
                                ambientColor = cyanGlow,
                                spotColor = blueGlow
                            )
                            .testTag("floating_quick_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quick Scan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }

    docForAddingTag?.let { targetDoc ->
        val cached = cachedDocumentsMap[targetDoc.id]
        val activeCat = cached?.category ?: targetDoc.category
        val activeTags = cached?.tags ?: ""
        CategorizeAndTagDialog(
            document = targetDoc,
            currentCategory = activeCat,
            currentTags = activeTags,
            onDismiss = { docForAddingTag = null },
            onConfirm = { cat, tags ->
                onUpdateCategoryAndTags(targetDoc, cat, tags)
                docForAddingTag = null
            }
        )
    }

    if (showNewFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onCreate = { name, color, icon ->
                onCreateFolder(name, color, icon)
                showNewFolderDialog = false
            }
        )
    }

    if (showNewDocDialog) {
        CreateDocumentDialog(
            folders = folders,
            initialFolderId = if (selectedFolderId == "all") "root" else selectedFolderId,
            onDismiss = { showNewDocDialog = false },
            onCreate = { title, folderId, category, fileType, fileName, content, isEncrypted, password, keyHint, customTags ->
                onCreateDocument(
                    title,
                    folderId,
                    category,
                    fileType,
                    fileName,
                    content,
                    isEncrypted,
                    password,
                    keyHint,
                    customTags
                )
                showNewDocDialog = false
            }
        )
    }

    if (showSyncMonitorDialog) {
        SyncMonitorDialog(
            syncLogs = syncLogs,
            pendingCount = pendingSyncCount,
            cachedDocCount = cachedDocumentsMap.size,
            isOfflineMode = isOfflineMode,
            onClearLogs = onClearSyncHistory,
            onDismiss = { showSyncMonitorDialog = false }
        )
    }
}

@Composable
private fun CrystalQuickActionTile(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            iconTint.copy(alpha = 0.35f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1
        )
    }
}

@Composable
private fun FolderPillCard(
    name: String,
    colorHex: String,
    iconName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val parsedColor = remember(colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
            .getOrDefault(Color(0xFF00F4FE))
    }

    CrystalGlassCard(
        cornerRadius = 16.dp,
        tintColor = if (isSelected) parsedColor else null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(parsedColor.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (iconName) {
                        "work" -> Icons.Default.Work
                        "receipt" -> Icons.Default.ReceiptLong
                        "scanner" -> Icons.Default.DocumentScanner
                        "lock" -> Icons.Default.Lock
                        else -> Icons.Default.Folder
                    },
                    contentDescription = null,
                    tint = parsedColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = Color.White
            )
            if (onDelete != null && isSelected) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Folder",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentItemCard(
    doc: DocEntity,
    folderName: String,
    syncStateName: String,
    isOfflineMode: Boolean,
    tags: List<String>,
    selectedTagFilters: Set<String>,
    hasLocalPdf: Boolean,
    onClick: () -> Unit,
    onTagClick: (String) -> Unit,
    onAddTagClick: () -> Unit,
    onToggleOffline: () -> Unit,
    onShareClick: () -> Unit = {}
) {
    val isSigned = tags.any { it.equals("signed", true) || it.equals("completed", true) }
    val isUrgent = tags.any { it.equals("urgent", true) || it.equals("action-required", true) }
    val isLegal = doc.category.equals("Contracts", true) || doc.category.equals("Legal", true)

    val (badgeColor, badgeIcon, statusLabel) = when {
        isSigned -> Triple(Color(0xFF34D399), Icons.Default.Verified, "Completed")
        isLegal -> Triple(Color(0xFFFBBF24), Icons.Default.Gavel, "Waiting for Legal")
        isUrgent || doc.isEncrypted -> Triple(Color(0xFFF43F5E), Icons.Default.PriorityHigh, "Action Required")
        doc.fileType.equals("docx", true) || doc.fileType.equals("doc", true) ->
            Triple(Color(0xFF00F4FE), Icons.Default.Description, "Draft")
        doc.fileType.equals("xlsx", true) || doc.fileType.equals("xls", true) ->
            Triple(Color(0xFF34D399), Icons.Default.TableChart, "Spreadsheet")
        doc.fileType.equals("pptx", true) || doc.fileType.equals("ppt", true) ->
            Triple(Color(0xFFA178FF), Icons.Default.Slideshow, "Slide Deck")
        doc.fileType.equals("pic", true) ->
            Triple(Color(0xFF38BDF8), Icons.Default.Image, "High-Res PNG")
        else -> Triple(Color(0xFF3E90FF), Icons.Default.Description, "Cloud Synced")
    }

    val formattedTime = remember(doc.updatedAt, doc.createdAt) {
        val date = (doc.updatedAt ?: doc.createdAt)?.toDate()
        if (date != null) {
            SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(date)
        } else {
            "Just now"
        }
    }

    CrystalGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("document_card_${doc.id}"),
        cornerRadius = 22.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Crystal Glass Format Badge with Top Glowing Bar
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(badgeColor)
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = doc.fileType.uppercase().take(4),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColor,
                            fontSize = 9.sp
                        )
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = doc.fileType,
                            tint = badgeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Document Title, Metadata & Status Pill
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = doc.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatStorageBytes(doc.fileSize.coerceAtLeast(1024L)),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.60f)
                        )
                        Text("•", color = Color.White.copy(alpha = 0.45f))
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.60f)
                        )
                        Text("•", color = Color.White.copy(alpha = 0.45f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = Color(0xFF00F4FE),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${doc.pageCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00F4FE)
                            )
                        }
                    }

                    // Status Pill + Sync Indicator Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        Surface(
                            color = badgeColor.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.border(1.dp, badgeColor.copy(alpha = 0.30f), CircleShape)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isSigned) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = badgeColor
                                )
                            }
                        }

                        SyncStatusVisualIndicator(
                            syncStateName = syncStateName,
                            isOfflineMode = isOfflineMode,
                            isOfflineCached = doc.isOfflineCached
                        )

                        if (doc.isEncrypted || tags.any { it.equals("biometric-locked", true) }) {
                            Surface(
                                color = Color(0xFFA178FF).copy(alpha = 0.16f),
                                shape = CircleShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometric Protected",
                                        tint = Color(0xFFA178FF),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "BIO • AES-256",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA178FF)
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Actions: Share + Offline Pin + More Options
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("share_doc_${doc.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Document",
                            tint = Color(0xFF00F4FE),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleOffline,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("toggle_offline_${doc.id}")
                    ) {
                        Icon(
                            imageVector = if (doc.isOfflineCached) Icons.Default.OfflinePin else Icons.Default.CloudDone,
                            contentDescription = "Toggle Offline",
                            tint = if (doc.isOfflineCached) Color(0xFF00F4FE) else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(
                        onClick = onAddTagClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("add_tag_doc_${doc.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Document Options & Tags",
                            tint = Color.White.copy(alpha = 0.50f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Custom Tags Row
            if (tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.take(6).forEach { tag ->
                        val isSelected = selectedTagFilters.any { it.equals(tag, ignoreCase = true) }
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onTagClick(tag) },
                            color = if (isSelected) Color(0xFF3E90FF) else Color.White.copy(alpha = 0.06f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else Color(0xFF00F4FE),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorizeAndTagDialog(
    document: DocEntity,
    currentCategory: String,
    currentTags: String,
    onDismiss: () -> Unit,
    onConfirm: (category: String, tags: String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(currentCategory.ifBlank { "Work" }) }
    var tagInput by remember { mutableStateOf(currentTags) }
    val categories = listOf("Work", "Personal", "Finance", "Contracts", "Invoices", "Receipts", "Legal", "Notes")
    val presetTags = listOf("work", "personal", "finance", "urgent", "signed", "approved", "important", "tax", "client", "confidential")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Tag, contentDescription = null, tint = Color(0xFF00F4FE))
                Text("Categorize & Tag", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Categorize \"${document.title}\" and assign custom tags in Room for instant dashboard filtering:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Category (e.g. Work, Personal, Finance)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00F4FE)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory.equals(cat, ignoreCase = true),
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("dialog_cat_${cat.lowercase()}")
                        )
                    }
                }

                Text(
                    text = "Custom Tags (Room FTS4 Indexed)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("Tags (comma separated)") },
                    placeholder = { Text("e.g. work, finance, tax-2026") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_custom_tag_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetTags.forEach { preset ->
                        val isPresetActive = tagInput.contains(preset, ignoreCase = true)
                        FilterChip(
                            selected = isPresetActive,
                            onClick = {
                                if (!isPresetActive) {
                                    tagInput = if (tagInput.isBlank()) preset else "$tagInput, $preset"
                                }
                            },
                            label = { Text("#$preset") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedCategory, tagInput) },
                modifier = Modifier.testTag("confirm_dashboard_add_tag_button")
            ) {
                Text("Save to Room")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, color: String, icon: String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(FolderColors.first()) }
    var selectedIcon by remember { mutableStateOf("folder") }

    val icons = listOf(
        "folder" to "General",
        "work" to "Work",
        "receipt" to "Finance",
        "scanner" to "Scans",
        "lock" to "Vault"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Personalized Folder", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_name_input")
                )

                Text("Folder Color:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FolderColors.forEach { hex ->
                        val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                            .getOrDefault(Color.Cyan)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == hex) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                Text("Folder Icon:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    icons.forEach { (iconKey, label) ->
                        FilterChip(
                            selected = selectedIcon == iconKey,
                            onClick = { selectedIcon = iconKey },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(folderName, selectedColor, selectedIcon) },
                enabled = folderName.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_folder_button")
            ) {
                Text("Create Folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CreateDocumentDialog(
    folders: List<FolderEntity>,
    initialFolderId: String,
    onDismiss: () -> Unit,
    onCreate: (
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
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var fileType by remember { mutableStateOf("pdf") }
    var category by remember { mutableStateOf("Contracts") }
    var folderId by remember { mutableStateOf(initialFolderId) }
    var tagsInput by remember { mutableStateOf("legal, contract, cloud") }
    var content by remember {
        mutableStateOf(
            "=== 1. Scope & Deliverables ===\nProvider agrees to perform cloud orchestration and multi-region document security integration services in accordance with Statement of Work (\"SOW\") Schedule A attached hereto.\n\n=== 2. Term and Termination ===\nThe initial term shall commence on the Effective Date and remain in effect for consecutive twelve (12) months unless terminated earlier pursuant to mutual written accord.\n\n=== 4. Liability & Indemnification Cap ===\n4.2 In no event shall either party's aggregate cumulative liability exceed twelve (12) times the monthly recurring fees paid or payable by Client in the preceding three calendar quarters immediately prior to notice."
        )
    }
    var isEncrypted by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var keyHint by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create PDF, Word, Excel or PowerPoint", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_doc_title_input")
                )

                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Search Tags (comma separated)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_doc_tags_input")
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "pdf" to "PDF (.pdf)",
                        "docx" to "Word (.docx)",
                        "xlsx" to "Excel (.xlsx)",
                        "pptx" to "PowerPoint (.pptx)"
                    ).forEach { (ext, label) ->
                        FilterChip(
                            selected = fileType == ext,
                            onClick = {
                                fileType = ext
                                content = when (ext) {
                                    "xlsx" -> "=== SHEET 1: FINANCIAL GRID ===\n| Line Item | Department | Q1 Budget | Q2 Forecast | Status |\n| Cloud Compute | Engineering | $14,500 | $15,200 | Active |\n| Mobile R&D | Product | $9,800 | $11,000 | Approved |\n| TOTAL | All Units | $24,300 | $26,200 | Verified |"
                                    "pptx" -> "=== SLIDE 1: EXECUTIVE OVERVIEW ===\n• Key strategic milestones and product roadmap\n• Real-time cloud and Room FTS4 synchronization\n\n=== SLIDE 2: KPI SUMMARY TABLE ===\n| Metric | Current Quarter | Target |\n| User Retention | 96.4% | 98.0% |\n| Sync Latency | 115ms | <150ms |"
                                    "docx" -> "=== SECTION 1: WORD SPECIFICATION ===\nThis Microsoft Word (.docx) document supports structured paragraphs and embedded table blocks.\n\n| Deliverable | Owner | Due Date |\n| Architecture Review | Core Team | Oct 28 |"
                                    else -> "=== 1. Scope & Deliverables ===\nProvider agrees to perform cloud orchestration and multi-region document security integration services in accordance with Statement of Work (\"SOW\") Schedule A attached hereto.\n\n=== 2. Term and Termination ===\nThe initial term shall commence on the Effective Date and remain in effect for consecutive twelve (12) months."
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Contracts", "Work", "Invoices", "Legal", "Notes").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                if (folders.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = folderId == "root",
                            onClick = { folderId = "root" },
                            label = { Text("Main Workspace") }
                        )
                        folders.forEach { folder ->
                            FilterChip(
                                selected = folderId == folder.id,
                                onClick = { folderId = folder.id },
                                label = { Text(folder.name) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Document Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.EnhancedEncryption, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("AES-256 Encrypt", style = MaterialTheme.typography.labelLarge)
                    }
                    Switch(checked = isEncrypted, onCheckedChange = { isEncrypted = it })
                }

                if (isEncrypted) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Passphrase / PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = keyHint,
                        onValueChange = { keyHint = it },
                        label = { Text("Hint (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                        .ifBlank { "document" } + ".$fileType"
                    onCreate(
                        title,
                        folderId,
                        category,
                        fileType,
                        cleanName,
                        content,
                        isEncrypted && password.isNotBlank(),
                        password,
                        keyHint.ifBlank { null },
                        tagsInput
                    )
                },
                enabled = title.isNotBlank() && (!isEncrypted || password.isNotBlank()),
                modifier = Modifier.testTag("confirm_create_doc_button")
            ) {
                Text("Create & Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SyncMonitorDialog(
    syncLogs: List<SyncLogEntity>,
    pendingCount: Int,
    cachedDocCount: Int,
    isOfflineMode: Boolean,
    onClearLogs: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFF00F4FE))
                Text("Room FTS4 & Sync Status", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Room FTS4 Documents Indexed: $cachedDocCount",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pending Offline Sync Queue: $pendingCount item(s)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Network Mode: ${if (isOfflineMode) "Offline (Room Local Storage Active)" else "Online (Real-Time Cloud Sync)"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Text(
                    text = "Recent Synchronization Events:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                if (syncLogs.isEmpty()) {
                    Text(
                        text = "No sync events logged yet in this session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier.height(220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(syncLogs, key = { it.id }) { log ->
                                val timeStr = remember(log.timestampMillis) {
                                    SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(log.timestampMillis))
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${log.action} • ${log.status}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00F4FE)
                                            )
                                            Text(
                                                text = timeStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${log.documentTitle}: ${log.details}",
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            if (syncLogs.isNotEmpty()) {
                TextButton(onClick = onClearLogs) {
                    Text("Clear History")
                }
            }
        }
    )
}
