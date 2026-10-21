package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tag
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CachedDocumentEntity
import com.example.data.local.SyncLogEntity
import com.example.data.local.SyncState
import com.example.data.model.DocEntity
import com.example.data.model.FolderEntity
import com.example.ui.components.CrystalGlassBackground
import com.example.ui.components.CrystalGlassCard
import com.example.ui.components.DocFlowBrandLogo
import com.example.ui.components.SyncStatusVisualIndicator
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
    onSeedSampleWorkspace: () -> Unit,
    onClearSyncHistory: () -> Unit,
    onSignOut: () -> Unit
) {
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewDocDialog by remember { mutableStateOf(false) }
    var showSyncMonitorDialog by remember { mutableStateOf(false) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportFileUri(uri)
        }
    }

    val folders = (foldersState as? UiState.Success)?.data ?: emptyList()
    val categories = listOf("All", "Invoices", "Contracts", "Receipts", "Work", "Personal", "Legal", "Medical", "Notes")

    // Collect distinct tags across all Room-cached documents for instant 1-tap tag filtering
    val allAvailableTags = remember(cachedDocumentsMap) {
        cachedDocumentsMap.values
            .flatMap { it.parsedTags() }
            .distinct()
            .take(10)
    }

    CrystalGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DocFlowBrandLogo(
                                iconSize = 40.dp,
                                showWordmark = false
                            )
                            Column {
                                DocFlowBrandLogo(
                                    iconSize = 0.dp,
                                    showWordmark = true,
                                    wordmarkSize = 21.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isOfflineMode) Color(0xFFF59E0B) else Color(0xFF10B981)
                                            )
                                    )
                                    Text(
                                        text = if (isOfflineMode) {
                                            "Local Only • Room FTS4 Active"
                                        } else {
                                            "Cloud Synced • Crystal Glass UI"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSyncMonitorDialog = true },
                            modifier = Modifier.testTag("sync_monitor_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (pendingSyncCount > 0) {
                                        Badge { Text("$pendingSyncCount") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Room Sync & Cache Monitor"
                                )
                            }
                        }

                        IconButton(
                            onClick = onToggleOfflineNetworkMode,
                            modifier = Modifier.testTag("offline_mode_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudSync,
                                contentDescription = if (isOfflineMode) "Switch to Online Sync" else "Switch to Offline Mode",
                                tint = if (isOfflineMode) Color(0xFFF59E0B) else Color(0xFF007BFF)
                            )
                        }

                        IconButton(
                            onClick = onToggleThemeMode,
                            modifier = Modifier.testTag("theme_mode_toggle_button")
                        ) {
                            Icon(
                                imageVector = when (themeMode) {
                                    ThemeMode.DARK -> Icons.Default.LightMode
                                    ThemeMode.LIGHT -> Icons.Default.DarkMode
                                    ThemeMode.SYSTEM -> Icons.Default.DarkMode
                                },
                                contentDescription = "Toggle Dark Mode"
                            )
                        }

                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Status Toast / Crystal Banner
                item {
                    AnimatedVisibility(visible = statusBannerMessage != null) {
                        CrystalGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp,
                            tintColor = Color(0xFF007BFF)
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
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onDismissStatusBanner,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Primary Action Row: Open PDF/Word, Scan Physical Doc, Create New Doc
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                openDocumentLauncher.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/msword",
                                        "text/plain"
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("open_file_picker_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0066FF),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open PDF/Word", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        }

                        Button(
                            onClick = onOpenScanner,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("open_scanner_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5E17EB),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan to PDF", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { showNewDocDialog = true },
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("create_doc_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Create New Document")
                        }
                    }
                }

                // Room Full-Text Search Bar (Title, Content Preview, or Tags) + Quick Tag Pills
                item {
                    CrystalGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = {
                                    Text("Room FTS4 Search: title, content preview, or #tags...")
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Full-Text Search",
                                        tint = Color(0xFF007BFF)
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchQueryChange("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_documents_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF007BFF),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            )

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
                                        contentDescription = "Quick Tags",
                                        tint = Color(0xFF007BFF),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "FTS Tags:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    allAvailableTags.forEach { tag ->
                                        val isTagSelected = searchQuery.equals(tag, ignoreCase = true)
                                        Surface(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable {
                                                    if (isTagSelected) {
                                                        onSearchQueryChange("")
                                                    } else {
                                                        onSearchQueryChange(tag)
                                                    }
                                                }
                                                .testTag("quick_tag_$tag"),
                                            color = if (isTagSelected) {
                                                Color(0xFF007BFF)
                                            } else {
                                                Color(0xFF007BFF).copy(alpha = 0.12f)
                                            },
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isTagSelected) Color.White else Color(0xFF007BFF),
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Personalized Folder Organization Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Personalized Crystal Folders",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { showNewFolderDialog = true },
                                modifier = Modifier.testTag("add_folder_button")
                            ) {
                                Icon(
                                    Icons.Default.CreateNewFolder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Folder")
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FolderPillCard(
                                name = "All Files",
                                colorHex = "#2563EB",
                                iconName = "folder",
                                isSelected = selectedFolderId == "all",
                                onClick = { onSelectFolder("all") }
                            )
                            FolderPillCard(
                                name = "Main Workspace",
                                colorHex = "#0891B2",
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
                    }
                }

                // File Format & Offline Filter Chips
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "All" to "All Formats",
                                "pdf" to "PDF Files",
                                "docx" to "Word (.docx)",
                                "scanned" to "Scanned Docs"
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
                                label = { Text("Local Offline Pinned") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.OfflinePin,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }

                        // Category Filter Row
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
                    }
                }

                // Sort & Count Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredDocuments.size} Document(s) • Room FTS4 Indexed",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SortOption.entries.forEach { option ->
                                FilterChip(
                                    selected = sortOption == option,
                                    onClick = { onSortOptionChange(option) },
                                    label = {
                                        Text(
                                            text = option.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Document List Content
                when (documentsState) {
                    is UiState.Loading -> {
                        if (filteredDocuments.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
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

                if (filteredDocuments.isEmpty() && documentsState !is UiState.Loading) {
                    item {
                        CrystalGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            cornerRadius = 26.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DocFlowBrandLogo(iconSize = 64.dp, showWordmark = false)
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        "No FTS4 Matches for \"$searchQuery\""
                                    } else {
                                        "Your Crystal Glass DocFlow Workspace is Ready"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Open a local PDF or Word (.docx) file, scan a physical document with CameraX, or load starter documents to test Room FTS4 search, visual sync badges, and AES-256 encryption.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onSeedSampleWorkspace,
                                    modifier = Modifier.testTag("seed_sample_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0066FF),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Load Sample PDF, Word & Encrypted Docs")
                                }
                            }
                        }
                    }
                } else {
                    items(filteredDocuments, key = { it.id }) { doc ->
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
                            hasLocalPdf = !cachedEntity?.localFilePath.isNullOrBlank(),
                            onClick = { onOpenDocument(doc) },
                            onTagClick = { clickedTag -> onSearchQueryChange(clickedTag) },
                            onToggleOffline = { onToggleDocumentOffline(doc) }
                        )
                    }
                }
            }
        }
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
            .getOrDefault(Color(0xFF2563EB))
    }

    CrystalGlassCard(
        cornerRadius = 18.dp,
        tintColor = if (isSelected) parsedColor else null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(parsedColor.copy(alpha = 0.24f)),
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
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (onDelete != null && isSelected) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Folder",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(15.dp)
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
    hasLocalPdf: Boolean,
    onClick: () -> Unit,
    onTagClick: (String) -> Unit,
    onToggleOffline: () -> Unit
) {
    val badgeColor = when (doc.fileType.lowercase()) {
        "pdf" -> Color(0xFFDC2626)
        "docx", "doc" -> Color(0xFF0066FF)
        "scanned" -> Color(0xFF6200EA)
        else -> Color(0xFF059669)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Glossy Crystal File Type Icon Box
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(badgeColor.copy(alpha = 0.16f))
                        .border(
                            width = 1.dp,
                            color = badgeColor.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = when (doc.fileType.lowercase()) {
                                "pdf" -> Icons.Default.PictureAsPdf
                                "scanned" -> Icons.Default.DocumentScanner
                                else -> Icons.Default.Description
                            },
                            contentDescription = doc.fileType,
                            tint = badgeColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = doc.fileType.uppercase().take(4),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColor
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = doc.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (doc.isEncrypted) {
                            Surface(
                                color = Color(0xFF6200EA).copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "AES-256 Encrypted",
                                        tint = Color(0xFF6200EA),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "AES-256",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6200EA)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (doc.isEncrypted) {
                            "🔒 Encrypted payload • Unlock with passphrase to view or edit"
                        } else {
                            doc.content.replace("\n", " ").take(100)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Visual Sync Status Indicator + Metadata Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        SyncStatusVisualIndicator(
                            syncStateName = syncStateName,
                            isOfflineMode = isOfflineMode,
                            isOfflineCached = doc.isOfflineCached
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = doc.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "• $folderName • ${doc.pageCount}p",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )

                        if (hasLocalPdf) {
                            Text(
                                text = "• PDF Ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF0066FF),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onToggleOffline,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("toggle_offline_${doc.id}")
                    ) {
                        Icon(
                            imageVector = if (doc.isOfflineCached) Icons.Default.OfflinePin else Icons.Default.CloudDone,
                            contentDescription = if (doc.isOfflineCached) "Pinned Locally for Offline" else "Cloud Stream Only",
                            tint = if (doc.isOfflineCached) {
                                Color(0xFF007BFF)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Room FTS4 Searchable Tags Row
            if (tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.take(6).forEach { tag ->
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onTagClick(tag) },
                            color = Color(0xFF007BFF).copy(alpha = 0.10f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF007BFF),
                                fontWeight = FontWeight.Medium,
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
                            .getOrDefault(Color.Blue)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == hex) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
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
    var category by remember { mutableStateOf("Work") }
    var folderId by remember { mutableStateOf(initialFolderId) }
    var tagsInput by remember { mutableStateOf("urgent, workflow, cloud") }
    var content by remember {
        mutableStateOf("Enter your PDF or Word document content here. All changes synchronize in real-time across connected devices.")
    }
    var isEncrypted by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var keyHint by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create PDF or Word Document", fontWeight = FontWeight.Bold) },
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = fileType == "pdf",
                        onClick = { fileType = "pdf" },
                        label = { Text("PDF (.pdf)") }
                    )
                    FilterChip(
                        selected = fileType == "docx",
                        onClick = { fileType = "docx" },
                        label = { Text("Word (.docx)") }
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Work", "Invoices", "Contracts", "Personal", "Notes").forEach { cat ->
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
                Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFF007BFF))
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
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
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
                                                color = Color(0xFF007BFF)
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
