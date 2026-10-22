package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CachedDocumentEntity
import com.example.data.model.DocEntity
import com.example.data.model.FolderEntity
import com.example.data.util.BiometricSecurityHelper
import com.example.data.util.OnDeviceAiAndBackupEngine
import com.example.ui.theme.DocFlowAccentBlue
import com.example.ui.theme.DocFlowGrayTxt
import com.example.ui.theme.DocFlowGreenExcel
import com.example.ui.theme.DocFlowOrangePpt
import com.example.ui.theme.DocFlowPurpleOfd
import com.example.ui.theme.DocFlowRedPdf
import com.example.ui.theme.FolderColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(
    allDocuments: List<DocEntity>,
    cachedDocumentsMap: Map<String, CachedDocumentEntity>,
    createdByMeDocs: List<CachedDocumentEntity>,
    privateSafeDocs: List<CachedDocumentEntity>,
    deletedDocs: List<CachedDocumentEntity>,
    folders: List<FolderEntity>,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDocument: (DocEntity) -> Unit,
    onImportFileUri: (Uri) -> Unit,
    onCreateFolder: (name: String, color: String, icon: String, parentFolderId: String?) -> Unit,
    onRenameOrUpdateFolder: (folderId: String, newName: String, newColor: String, parentFolderId: String?) -> Unit,
    onDeleteFolder: (folderId: String) -> Unit,
    onMoveToPrivateSafe: (docId: String, pinPassword: String) -> Unit,
    onRemoveFromPrivateSafe: (docId: String, pinPassword: String) -> Unit,
    onRestoreDeletedDocument: (docId: String) -> Unit,
    onPermanentlyDeleteDocument: (docId: String) -> Unit,
    onEmptyRecentlyDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showCreatedByMeSheet by remember { mutableStateOf(false) }
    var activeSourceSheet by remember { mutableStateOf<String?>(null) } // "whatsapp", "download", "bluetooth"
    var showPrivateSafeSheet by remember { mutableStateOf(false) }
    var showRecentlyDeletedSheet by remember { mutableStateOf(false) }
    var showFolderEditorSheet by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<FolderEntity?>(null) }
    var viewingFolder by remember { mutableStateOf<FolderEntity?>(null) }

    // Private Safe state
    var isPrivateSafeUnlocked by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    val hasConfiguredPin = remember(showPrivateSafeSheet) {
        OnDeviceAiAndBackupEngine.hasPrivateSafePin(context)
    }

    // SAF Folder Access state
    var sourceUriState by remember {
        mutableStateOf(OnDeviceAiAndBackupEngine.getAllSourceFolderStates(context))
    }
    var pendingSafSourceKey by remember { mutableStateOf<String?>(null) }

    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        val key = pendingSafSourceKey
        if (treeUri != null && key != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            OnDeviceAiAndBackupEngine.saveSourceFolderUri(context, key, treeUri)
            sourceUriState = OnDeviceAiAndBackupEngine.getAllSourceFolderStates(context)
            // Import any discovered supported files from the granted folder
            val discoveredUris = OnDeviceAiAndBackupEngine.listFilesInGrantedTreeUri(context, treeUri)
            discoveredUris.forEach { fileUri ->
                onImportFileUri(fileUri)
            }
        }
    }

    val effectiveCreatedByMe = if (createdByMeDocs.isNotEmpty()) {
        createdByMeDocs
    } else {
        cachedDocumentsMap.values.filter { !it.isDeleted && !it.isPrivateSafe }.take(6)
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val headerFontSize = when {
        screenWidthDp < 350 -> 30.sp
        screenWidthDp < 390 -> 34.sp
        else -> 38.sp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp)
            .padding(horizontal = 16.dp)
            .testTag("manage_screen_list"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header: Large Bold "Manage" (~40sp) + Search & Settings icons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manage",
                    fontSize = headerFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("manage_screen_title")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("manage_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search documents",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("manage_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        // Card 1: "Created by me" with three thumbnail previews + chevron
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { showCreatedByMeSheet = true }
                    .testTag("manage_created_by_me_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Created by me",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${effectiveCreatedByMe.size}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View all Created by me",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (effectiveCreatedByMe.isEmpty()) {
                        Text(
                            text = "Documents you create, scan, or convert appear here.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            effectiveCreatedByMe.take(3).forEach { item ->
                                CreatedByMeThumbnailTile(
                                    doc = item,
                                    onClick = { onOpenDocument(item.toDocEntity()) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat((3 - effectiveCreatedByMe.take(3).size).coerceAtLeast(0)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Section 2: "Sources" — one card with dividers & chevrons: WhatsApp, Download, Bluetooth
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Sources",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .testTag("manage_sources_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ManageRowItem(
                            title = "WhatsApp",
                            subtitle = if (sourceUriState["whatsapp"] != null) "Folder access granted" else "Tap to browse or grant folder access",
                            icon = Icons.Default.Chat,
                            iconCircleColor = DocFlowGreenExcel,
                            testTag = "source_whatsapp_row",
                            onClick = { activeSourceSheet = "whatsapp" }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp, end = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        ManageRowItem(
                            title = "Download",
                            subtitle = if (sourceUriState["download"] != null) "Folder access granted" else "Tap to browse or grant folder access",
                            icon = Icons.Default.Download,
                            iconCircleColor = DocFlowGreenExcel,
                            testTag = "source_download_row",
                            onClick = { activeSourceSheet = "download" }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp, end = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        ManageRowItem(
                            title = "Bluetooth",
                            subtitle = if (sourceUriState["bluetooth"] != null) "Folder access granted" else "Tap to browse or grant folder access",
                            icon = Icons.Default.Bluetooth,
                            iconCircleColor = DocFlowAccentBlue,
                            testTag = "source_bluetooth_row",
                            onClick = { activeSourceSheet = "bluetooth" }
                        )
                    }
                }
            }
        }

        // Card 3: Private Safe & Recently deleted
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("manage_security_and_trash_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ManageRowItem(
                        title = "Private Safe",
                        subtitle = if (isPrivateSafeUnlocked) "${privateSafeDocs.size} encrypted items (Unlocked)" else "PIN & Biometric AES-256-GCM vault",
                        icon = Icons.Default.Security,
                        iconCircleColor = DocFlowAccentBlue,
                        testTag = "manage_private_safe_row",
                        onClick = {
                            pinError = null
                            pinInput = ""
                            showPrivateSafeSheet = true
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 76.dp, end = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    ManageRowItem(
                        title = "Recently deleted",
                        subtitle = "${deletedDocs.size} items",
                        icon = Icons.Default.Delete,
                        iconCircleColor = DocFlowGrayTxt,
                        testTag = "manage_recently_deleted_row",
                        onClick = { showRecentlyDeletedSheet = true }
                    )
                }
            }
        }

        // Section 4: Personalized folders (create, rename, color, nest folders, move files)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Personalized folders",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = {
                            editingFolder = null
                            showFolderEditorSheet = true
                        },
                        modifier = Modifier.testTag("manage_new_folder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            tint = DocFlowAccentBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New folder", color = DocFlowAccentBlue, fontWeight = FontWeight.SemiBold)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .testTag("manage_personalized_folders_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    if (folders.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No personalized folders yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Create custom colored or nested folders to organize your files offline.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            folders.forEachIndexed { index, folder ->
                                val parentFolder = folders.find { it.id == folder.parentFolderId }
                                val docCount = allDocuments.count { it.folderId == folder.id }
                                val folderColor = runCatching {
                                    Color(android.graphics.Color.parseColor(folder.color))
                                }.getOrDefault(DocFlowAccentBlue)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewingFolder = folder }
                                        .padding(horizontal = 20.dp, vertical = 16.dp)
                                        .testTag("folder_row_${folder.id}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(folderColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = folderColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (parentFolder != null) "${parentFolder.name} / ${folder.name}" else folder.name,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$docCount item${if (docCount == 1) "" else "s"}",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            editingFolder = folder
                                            showFolderEditorSheet = true
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit folder",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (index < folders.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 80.dp, end = 20.dp),
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

    // Sheet 1: Full "Created by me" list
    if (showCreatedByMeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreatedByMeSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Created by me",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${effectiveCreatedByMe.size} document(s) created, scanned, or converted in DocFlow",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showCreatedByMeSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (effectiveCreatedByMe.isEmpty()) {
                    Text(
                        text = "No files created yet. Use the + button on Recent or Tools to create, scan, or convert a file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    effectiveCreatedByMe.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCreatedByMeSheet = false
                                    onOpenDocument(item.toDocEntity())
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FormatBadgeCircle(fileType = item.fileType)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.fileName} • ${item.category}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }

    // Sheet 2: Source Folder Sheet (WhatsApp / Download / Bluetooth via SAF)
    val currentSourceKey = activeSourceSheet
    if (currentSourceKey != null) {
        val sourceTitle = when (currentSourceKey) {
            "whatsapp" -> "WhatsApp Documents"
            "download" -> "Download Folder"
            else -> "Bluetooth Received Files"
        }
        val grantedUriStr = sourceUriState[currentSourceKey]
        val matchingDocs = cachedDocumentsMap.values.filter {
            !it.isDeleted && !it.isPrivateSafe && it.sourceFolderKey == currentSourceKey
        }

        ModalBottomSheet(
            onDismissRequest = { activeSourceSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = sourceTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Uses Android Storage Access Framework (no broad storage permission needed)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { activeSourceSheet = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (grantedUriStr != null) {
                                "Granted SAF Folder URI:\n$grantedUriStr"
                            } else {
                                "Folder access has not been granted yet. Tap below to select your $sourceTitle directory once."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                pendingSafSourceKey = currentSourceKey
                                safFolderLauncher.launch(null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("saf_grant_access_button_$currentSourceKey"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DocFlowAccentBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (grantedUriStr != null) "Change Granted Folder" else "Grant Access to $sourceTitle",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = "Indexed files from $sourceTitle (${matchingDocs.size}):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                if (matchingDocs.isEmpty()) {
                    Text(
                        text = "No documents indexed from this source yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    matchingDocs.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeSourceSheet = null
                                    onOpenDocument(item.toDocEntity())
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FormatBadgeCircle(fileType = item.fileType)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.fileName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }

    // Sheet 3: Private Safe (PIN / Biometric Lock + AES-256-GCM Encrypted Vault)
    if (showPrivateSafeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPrivateSafeSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DocFlowAccentBlue.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPrivateSafeUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                tint = DocFlowAccentBlue
                            )
                        }
                        Column {
                            Text(
                                text = "Private Safe",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "AES-256-GCM encrypted on device • Hidden from Recent & Search",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { showPrivateSafeSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (!isPrivateSafeUnlocked) {
                    Text(
                        text = if (hasConfiguredPin) {
                            "Enter your Private Safe PIN (or use Biometric Unlock) to decrypt and view your protected files."
                        } else {
                            "Set a 4+ digit Private Safe PIN (default demo PIN is 1234). Your PIN derives an AES-256-GCM key via PBKDF2."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            pinError = null
                        },
                        label = { Text(if (hasConfiguredPin) "Private Safe PIN" else "Create Private Safe PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("private_safe_pin_input")
                    )

                    if (pinError != null) {
                        Text(
                            text = pinError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                BiometricSecurityHelper.authenticateForAppEntry(
                                    context = context,
                                    onAuthenticated = {
                                        isPrivateSafeUnlocked = true
                                        pinError = null
                                    },
                                    onError = { err ->
                                        pinError = err
                                    }
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("private_safe_biometric_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Biometrics", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                val candidate = pinInput.trim()
                                if (candidate.length < 4) {
                                    pinError = "PIN must be at least 4 characters"
                                    return@Button
                                }
                                if (!hasConfiguredPin) {
                                    OnDeviceAiAndBackupEngine.setPrivateSafePin(context, candidate)
                                    isPrivateSafeUnlocked = true
                                } else if (OnDeviceAiAndBackupEngine.verifyPrivateSafePin(context, candidate)) {
                                    isPrivateSafeUnlocked = true
                                } else {
                                    pinError = "Incorrect PIN. Try again."
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("private_safe_unlock_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DocFlowAccentBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Text(if (hasConfiguredPin) "Unlock Safe" else "Set PIN & Open", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Unlocked Private Safe View
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Encrypted files in Safe (${privateSafeDocs.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { isPrivateSafeUnlocked = false }) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lock Now")
                        }
                    }

                    if (privateSafeDocs.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Private Safe is empty. Tap any file below to encrypt and move it into Private Safe.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        privateSafeDocs.forEach { safeDoc ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showPrivateSafeSheet = false
                                            onOpenDocument(safeDoc.toDocEntity())
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FormatBadgeCircle(fileType = safeDoc.fileType)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = safeDoc.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "AES-256-GCM Encrypted • ${safeDoc.fileName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DocFlowAccentBlue
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onRemoveFromPrivateSafe(safeDoc.id, pinInput.ifBlank { "1234" })
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Move Out", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    val outsideDocs = allDocuments.filter { doc ->
                        cachedDocumentsMap[doc.id]?.isPrivateSafe != true
                    }
                    if (outsideDocs.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = "Add document to Private Safe:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(outsideDocs, key = { it.id }) { doc ->
                                OutlinedButton(
                                    onClick = {
                                        onMoveToPrivateSafe(doc.id, pinInput.ifBlank { "1234" })
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(doc.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sheet 4: Recently deleted (Restore or Delete forever + 30-day auto-purge)
    if (showRecentlyDeletedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRecentlyDeletedSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Recently deleted",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${deletedDocs.size} item(s) • Automatically purged after 30 days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (deletedDocs.isNotEmpty()) {
                        TextButton(
                            onClick = onEmptyRecentlyDeleted,
                            modifier = Modifier.testTag("empty_recently_deleted_button")
                        ) {
                            Text("Empty All", color = DocFlowRedPdf, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (deletedDocs.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Recently deleted is empty.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else {
                    deletedDocs.forEach { delDoc ->
                        val deletedMs = delDoc.deletedAtMillis ?: System.currentTimeMillis()
                        val elapsedDays = ((System.currentTimeMillis() - deletedMs) / (24L * 60L * 60L * 1000L)).toInt()
                        val daysLeft = (30 - elapsedDays).coerceIn(1, 30)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FormatBadgeCircle(fileType = delDoc.fileType)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = delDoc.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$daysLeft days left • ${delDoc.fileName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onRestoreDeletedDocument(delDoc.id) },
                                    modifier = Modifier.testTag("restore_doc_${delDoc.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestoreFromTrash,
                                        contentDescription = "Restore",
                                        tint = DocFlowAccentBlue
                                    )
                                }
                                IconButton(
                                    onClick = { onPermanentlyDeleteDocument(delDoc.id) },
                                    modifier = Modifier.testTag("delete_forever_doc_${delDoc.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = "Delete forever",
                                        tint = DocFlowRedPdf
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sheet 5: Create / Rename / Color / Nest Folder
    if (showFolderEditorSheet) {
        val currentEdit = editingFolder
        var folderName by remember(currentEdit) { mutableStateOf(currentEdit?.name ?: "") }
        var selectedColor by remember(currentEdit) { mutableStateOf(currentEdit?.color ?: FolderColors.first()) }
        var selectedParentId by remember(currentEdit) { mutableStateOf<String?>(currentEdit?.parentFolderId) }

        ModalBottomSheet(
            onDismissRequest = { showFolderEditorSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentEdit == null) "New personalized folder" else "Edit folder",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentEdit != null) {
                        TextButton(
                            onClick = {
                                onDeleteFolder(currentEdit.id)
                                showFolderEditorSheet = false
                            }
                        ) {
                            Text("Delete", color = DocFlowRedPdf, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_name_input")
                )

                Text(
                    text = "Folder color",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(FolderColors) { hex ->
                        val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(DocFlowAccentBlue)
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                val candidateParents = folders.filter { it.id != currentEdit?.id }
                if (candidateParents.isNotEmpty()) {
                    Text(
                        text = "Parent folder (optional nesting)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedParentId == null,
                                onClick = { selectedParentId = null },
                                label = { Text("Top level (None)") }
                            )
                        }
                        items(candidateParents, key = { it.id }) { parent ->
                            FilterChip(
                                selected = selectedParentId == parent.id,
                                onClick = { selectedParentId = parent.id },
                                label = { Text(parent.name) }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (folderName.isBlank()) return@Button
                        if (currentEdit == null) {
                            onCreateFolder(folderName.trim(), selectedColor, "folder", selectedParentId)
                        } else {
                            onRenameOrUpdateFolder(currentEdit.id, folderName.trim(), selectedColor, selectedParentId)
                        }
                        showFolderEditorSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_folder_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DocFlowAccentBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (currentEdit == null) "Create Folder" else "Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Sheet 6: Viewing contents of a specific Personalized Folder
    val activeFolder = viewingFolder
    if (activeFolder != null) {
        val docsInFolder = allDocuments.filter { it.folderId == activeFolder.id }
        val childFolders = folders.filter { it.parentFolderId == activeFolder.id }
        ModalBottomSheet(
            onDismissRequest = { viewingFolder = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = activeFolder.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${docsInFolder.size} document(s) • ${childFolders.size} subfolder(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewingFolder = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (childFolders.isNotEmpty()) {
                    Text(
                        text = "Subfolders",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(childFolders, key = { it.id }) { sub ->
                            FilterChip(
                                selected = false,
                                onClick = { viewingFolder = sub },
                                label = { Text(sub.name) }
                            )
                        }
                    }
                }

                if (docsInFolder.isEmpty()) {
                    Text(
                        text = "This folder is empty.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    docsInFolder.forEach { doc ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewingFolder = null
                                    onOpenDocument(doc)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FormatBadgeCircle(fileType = doc.fileType)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = doc.fileName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatedByMeThumbnailTile(
    doc: CachedDocumentEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeColor = formatColorFor(doc.fileType)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = badgeColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = doc.fileType.uppercase().take(4),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${doc.pageCount}p",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = doc.contentPreview.ifBlank { doc.title },
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = doc.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ManageRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconCircleColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconCircleColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconCircleColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FormatBadgeCircle(
    fileType: String,
    modifier: Modifier = Modifier
) {
    val color = formatColorFor(fileType)
    val icon = when (fileType.lowercase()) {
        "pdf", "scanned" -> Icons.Default.PictureAsPdf
        "xlsx", "xls", "csv" -> Icons.Default.TableChart
        "pptx", "ppt" -> Icons.Default.Slideshow
        else -> Icons.Default.Description
    }
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = fileType.uppercase(),
            tint = color,
            modifier = Modifier.size(22.dp)
        )
    }
}

fun formatColorFor(fileType: String): Color {
    return when (fileType.lowercase()) {
        "pdf", "scanned" -> DocFlowRedPdf
        "docx", "doc" -> DocFlowAccentBlue
        "xlsx", "xls", "csv" -> DocFlowGreenExcel
        "pptx", "ppt" -> DocFlowOrangePpt
        "ofd" -> DocFlowPurpleOfd
        else -> DocFlowGrayTxt
    }
}
