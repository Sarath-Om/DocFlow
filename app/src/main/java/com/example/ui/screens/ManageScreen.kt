package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilterChip
import com.example.data.local.CachedDocumentEntity
import com.example.data.local.DocumentCommentEntity
import com.example.data.model.DocEntity
import com.example.data.model.FolderEntity
import com.example.data.util.OnDeviceAiAndBackupEngine
import com.example.ui.components.AiMicDictationBottomSheet
import com.example.ui.components.DocFlowCard
import com.example.ui.components.DocFlowDivider
import com.example.ui.components.DocFlowHeaderAction
import com.example.ui.components.DocFlowIconCircle
import com.example.ui.components.DocFlowListRow
import com.example.ui.components.DocFlowPrimaryButton
import com.example.ui.components.DocFlowScreenHeader
import com.example.ui.components.DocFlowSecondaryButton
import com.example.ui.components.DocFlowSectionLabel
import com.example.ui.components.MySharedLinksBottomSheet
import com.example.ui.components.ShareNoteWithLinkBottomSheet
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens
import com.example.ui.viewmodel.DocFlowViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(
    viewModel: DocFlowViewModel,
    palette: DocFlowPalette,
    onOpenDocument: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onRequestBiometricUnlock: (onSuccess: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val foldersState by viewModel.foldersState.collectAsState()
    val deletedDocs by viewModel.deletedDocuments.collectAsState()
    val privateSafeDocs by viewModel.privateSafeDocuments.collectAsState()
    val createdByMeDocs by viewModel.createdByMeDocuments.collectAsState()
    val cachedMap by viewModel.cachedDocumentsMap.collectAsState()

    val folders = (foldersState as? UiState.Success<List<FolderEntity>>)?.data ?: emptyList()

    var sourceFolderStates by remember {
        mutableStateOf(OnDeviceAiAndBackupEngine.getAllSourceFolderStates(context))
    }
    var pendingSourceKey by remember { mutableStateOf<String?>(null) }

    // Sub-screens / Sheets
    var showCreatedByMeSheet by remember { mutableStateOf(false) }
    var showStandaloneNotesSheet by remember { mutableStateOf(false) }
    var showMySharedLinksSheet by remember { mutableStateOf(false) }
    var showPrivateSafePinDialog by remember { mutableStateOf(false) }
    var showPrivateSafeSheet by remember { mutableStateOf(false) }
    var showRecentlyDeletedSheet by remember { mutableStateOf(false) }
    var activeFolderSheet by remember { mutableStateOf<FolderEntity?>(null) }
    var activeSourceSheetKey by remember { mutableStateOf<String?>(null) }

    // Standalone Notes state
    val standaloneNotesDocId = "standalone_notes_hub"
    val standaloneNotes by viewModel.observeDocumentComments(standaloneNotesDocId).collectAsState(initial = emptyList())
    var noteSearchQuery by remember { mutableStateOf("") }
    var newNoteTitle by remember { mutableStateOf("") }
    var newNoteBody by remember { mutableStateOf("") }
    var newNoteColorHex by remember { mutableStateOf("#FFFFFF") }
    var newNotePinned by remember { mutableStateOf(false) }
    var showNoteAiMic by remember { mutableStateOf(false) }
    var shareLinkTargetNote by remember { mutableStateOf<Pair<String, String>?>(null) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    val safTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        val key = pendingSourceKey
        if (treeUri != null && key != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            OnDeviceAiAndBackupEngine.saveSourceFolderUri(context, key, treeUri)
            sourceFolderStates = OnDeviceAiAndBackupEngine.getAllSourceFolderStates(context)

            val uris = OnDeviceAiAndBackupEngine.listFilesInGrantedTreeUri(context, treeUri, maxFiles = 12)
            uris.forEach { fileUri ->
                viewModel.importDocumentFromUri(
                    context = context,
                    uri = fileUri,
                    folderId = "root",
                    category = key.replaceFirstChar { it.uppercase() },
                    isEncrypted = false,
                    password = "",
                    encryptionKeyHint = null
                )
            }
            activeSourceSheetKey = key
        }
        pendingSourceKey = null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("manage_screen_list"),
        contentPadding = PaddingValues(bottom = 104.dp)
    ) {
        item {
            DocFlowScreenHeader(
                title = "Manage",
                subtitle = "Stored on this device",
                palette = palette,
                actions = listOf(
                    DocFlowHeaderAction(
                        icon = Icons.Filled.Search,
                        contentDescription = "Search",
                        testTag = "manage_search_button",
                        onClick = onOpenSearch
                    ),
                    DocFlowHeaderAction(
                        icon = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        testTag = "manage_settings_button",
                        onClick = onOpenSettings
                    )
                )
            )
        }

        // Section 1: File sources (WhatsApp, Download, Bluetooth)
        item {
            DocFlowSectionLabel(text = "File sources", palette = palette)
            DocFlowCard(
                palette = palette,
                contentPadding = PaddingValues(vertical = DocFlowTokens.space1)
            ) {
                ManageRowItem(
                    icon = Icons.Filled.Chat,
                    iconBg = DocFlowTokens.IconCircleGreen,
                    title = "WhatsApp",
                    subtitle = if (sourceFolderStates["whatsapp"] != null) {
                        "Linked • Stored on this device"
                    } else {
                        "Tap to grant folder access via Android SAF"
                    },
                    palette = palette,
                    testTag = "manage_source_whatsapp",
                    onClick = {
                        if (sourceFolderStates["whatsapp"] != null) {
                            activeSourceSheetKey = "whatsapp"
                        } else {
                            pendingSourceKey = "whatsapp"
                            safTreeLauncher.launch(null)
                        }
                    }
                )
                DocFlowDivider(
                    palette = palette,
                    modifier = Modifier.padding(start = 72.dp, end = DocFlowTokens.cardPaddingMin)
                )
                ManageRowItem(
                    icon = Icons.Filled.Download,
                    iconBg = DocFlowTokens.IconCircleGreen,
                    title = "Download",
                    subtitle = if (sourceFolderStates["download"] != null) {
                        "Linked • Stored on this device"
                    } else {
                        "Tap to grant Downloads folder access"
                    },
                    palette = palette,
                    testTag = "manage_source_download",
                    onClick = {
                        if (sourceFolderStates["download"] != null) {
                            activeSourceSheetKey = "download"
                        } else {
                            pendingSourceKey = "download"
                            safTreeLauncher.launch(null)
                        }
                    }
                )
                DocFlowDivider(
                    palette = palette,
                    modifier = Modifier.padding(start = 72.dp, end = DocFlowTokens.cardPaddingMin)
                )
                ManageRowItem(
                    icon = Icons.Filled.Bluetooth,
                    iconBg = DocFlowTokens.AccentBlue,
                    title = "Bluetooth",
                    subtitle = if (sourceFolderStates["bluetooth"] != null) {
                        "Linked • Stored on this device"
                    } else {
                        "Tap to grant Bluetooth folder access"
                    },
                    palette = palette,
                    testTag = "manage_source_bluetooth",
                    onClick = {
                        if (sourceFolderStates["bluetooth"] != null) {
                            activeSourceSheetKey = "bluetooth"
                        } else {
                            pendingSourceKey = "bluetooth"
                            safTreeLauncher.launch(null)
                        }
                    }
                )
            }
        }

        // Section 2: Personal (Notes, Created by me, Private Safe, Recently deleted, My shared links)
        item {
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))
            DocFlowSectionLabel(text = "Personal", palette = palette)
            DocFlowCard(
                palette = palette,
                contentPadding = PaddingValues(vertical = DocFlowTokens.space1)
            ) {
                ManageRowItem(
                    icon = Icons.Filled.Notes,
                    iconBg = DocFlowTokens.AccentBlue,
                    title = "Notes",
                    subtitle = "${standaloneNotes.size} note(s) • Checklists, AI Mic, PDF export & Share with link",
                    palette = palette,
                    testTag = "manage_personal_notes",
                    onClick = { showStandaloneNotesSheet = true }
                )
                DocFlowDivider(
                    palette = palette,
                    modifier = Modifier.padding(start = 72.dp, end = DocFlowTokens.cardPaddingMin)
                )
                ManageRowItem(
                    icon = Icons.Filled.Link,
                    iconBg = DocFlowTokens.IconCircleTeal,
                    title = "My shared links",
                    subtitle = "${OnDeviceAiAndBackupEngine.getSavedSharedLinks(context).size} link(s) • Local list stored on this device",
                    palette = palette,
                    testTag = "manage_personal_shared_links",
                    onClick = { showMySharedLinksSheet = true }
                )
                DocFlowDivider(
                    palette = palette,
                    modifier = Modifier.padding(start = 72.dp, end = DocFlowTokens.cardPaddingMin)
                )
                ManageRowItem(
                    icon = Icons.Filled.Person,
                    iconBg = DocFlowTokens.IconCircleTeal,
                    title = "Created by me",
                    subtitle = "${createdByMeDocs.size} file(s) • Scans, edited docs & conversions stored on this device",
                    palette = palette,
                    testTag = "manage_personal_created_by_me",
                    onClick = { showCreatedByMeSheet = true }
                )
                DocFlowDivider(
                    palette = palette,
                    modifier = Modifier.padding(start = 72.dp, end = DocFlowTokens.cardPaddingMin)
                )
                ManageRowItem(
                    icon = Icons.Filled.Lock,
                    iconBg = DocFlowTokens.IconCircleBlue,
                    title = "Private Safe",
                    subtitle = "${privateSafeDocs.size} encrypted file(s) • AES-256-GCM + Biometric unlock",
                    palette = palette,
                    testTag = "manage_personal_private_safe",
                    onClick = {
                        pinInput = ""
                        pinError = null
                        showPrivateSafePinDialog = true
                    }
                )
                DocFlowDivider(
                    palette = palette,
                    modifier = Modifier.padding(start = 72.dp, end = DocFlowTokens.cardPaddingMin)
                )
                ManageRowItem(
                    icon = Icons.Filled.Delete,
                    iconBg = DocFlowTokens.IconCircleGray,
                    title = "Recently deleted",
                    subtitle = "${deletedDocs.size} item(s) • Auto-purged after 30 days",
                    palette = palette,
                    testTag = "manage_personal_recently_deleted",
                    onClick = { showRecentlyDeletedSheet = true }
                )
            }
        }

        // Section 3: My Folders (Custom Local Folder Tree)
        item {
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.screenHorizontalMargin, vertical = DocFlowTokens.space2),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My folders",
                    style = DocFlowTokens.sectionLabelStyle,
                    color = palette.textPrimary
                )
                Row(
                    modifier = Modifier
                        .clip(DocFlowTokens.pillButtonShape)
                        .clickable {
                            newFolderName = ""
                            showCreateFolderDialog = true
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("manage_new_folder_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CreateNewFolder,
                        contentDescription = "New folder",
                        tint = DocFlowTokens.AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "New folder",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DocFlowTokens.AccentBlue
                    )
                }
            }

            DocFlowCard(
                palette = palette,
                contentPadding = PaddingValues(vertical = DocFlowTokens.space1)
            ) {
                if (folders.isEmpty()) {
                    Text(
                        text = "No folders yet. Tap 'New folder' to organize your documents on this device.",
                        style = DocFlowTokens.captionStyle,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(DocFlowTokens.cardPaddingMax)
                    )
                } else {
                    folders.forEachIndexed { idx, folder ->
                        val count = cachedMap.values.count {
                            !it.isDeleted && !it.isPrivateSafe && it.folderId == folder.id
                        }
                        ManageRowItem(
                            icon = Icons.Filled.Folder,
                            iconBg = DocFlowTokens.AccentBlue,
                            title = folder.name,
                            subtitle = "$count item(s) • Stored on this device",
                            palette = palette,
                            testTag = "manage_folder_${folder.id}",
                            onClick = { activeFolderSheet = folder }
                        )
                        if (idx < folders.lastIndex) {
                            DocFlowDivider(
                                palette = palette,
                                modifier = Modifier.padding(start = 72.dp, end = DocFlowTokens.cardPaddingMin)
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create new folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newFolderName.trim()
                        if (name.isNotEmpty()) {
                            viewModel.createFolder(name, "#1F6BFF", "folder")
                        }
                        showCreateFolderDialog = false
                    }
                ) {
                    Text("Create", fontWeight = FontWeight.Bold, color = DocFlowTokens.AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Private Safe PIN / Biometric Unlock Dialog
    if (showPrivateSafePinDialog) {
        val hasExistingPin = OnDeviceAiAndBackupEngine.hasPrivateSafePin(context)
        AlertDialog(
            onDismissRequest = { showPrivateSafePinDialog = false },
            title = {
                Text(if (hasExistingPin) "Unlock Private Safe" else "Set Private Safe PIN")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)) {
                    Text(
                        text = if (hasExistingPin) {
                            "Enter your PIN or use Biometric unlock to decrypt your Private Safe vault (AES-256-GCM • Stored on this device)."
                        } else {
                            "Choose a 4–8 digit PIN to derive your local AES-256-GCM encryption key (default: 1234)."
                        },
                        style = DocFlowTokens.captionStyle,
                        color = palette.textSecondary
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it.filter { ch -> ch.isDigit() }.take(8)
                            pinError = null
                        },
                        label = { Text("PIN (4–8 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError != null) {
                        Text(text = pinError!!, color = DocFlowTokens.IconCircleRed, style = DocFlowTokens.captionStyle)
                    }
                    TextButton(
                        onClick = {
                            onRequestBiometricUnlock {
                                showPrivateSafePinDialog = false
                                showPrivateSafeSheet = true
                            }
                        }
                    ) {
                        Text("Unlock with Fingerprint / Biometrics", color = DocFlowTokens.AccentBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val candidate = pinInput.ifBlank { "1234" }
                        if (!hasExistingPin) {
                            OnDeviceAiAndBackupEngine.setPrivateSafePin(context, candidate)
                            showPrivateSafePinDialog = false
                            showPrivateSafeSheet = true
                        } else if (OnDeviceAiAndBackupEngine.verifyPrivateSafePin(context, candidate)) {
                            showPrivateSafePinDialog = false
                            showPrivateSafeSheet = true
                        } else {
                            pinError = "Incorrect PIN. Try again or use Biometrics."
                        }
                    }
                ) {
                    Text("Unlock", fontWeight = FontWeight.Bold, color = DocFlowTokens.AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivateSafePinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Private Safe Vault Sheet
    if (showPrivateSafeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPrivateSafeSheet = false },
            containerColor = palette.surface,
            shape = DocFlowTokens.sheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.cardPaddingMax, vertical = DocFlowTokens.space2)
            ) {
                Text(
                    text = "Private Safe (AES-256-GCM)",
                    style = DocFlowTokens.sectionLabelStyle,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = "Hidden from Recent & search • Stored on this device",
                    style = DocFlowTokens.captionStyle,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                if (privateSafeDocs.isEmpty()) {
                    Text(
                        text = "Your Private Safe is empty. Long-press any document in Recent and tap 'Move to Private Safe'.",
                        style = DocFlowTokens.captionStyle,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(vertical = DocFlowTokens.space4)
                    )
                } else {
                    privateSafeDocs.forEach { safeDoc ->
                        DocFlowListRow(
                            title = safeDoc.title,
                            subtitle = "${safeDoc.fileType.uppercase()} • AES-256-GCM Encrypted • Stored on this device",
                            palette = palette,
                            onClick = {
                                showPrivateSafeSheet = false
                                onOpenDocument(safeDoc.id)
                            },
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Filled.Lock,
                                    circleColor = DocFlowTokens.IconCircleBlue
                                )
                            },
                            trailing = {
                                IconButton(
                                    onClick = {
                                        viewModel.removeFromPrivateSafe(safeDoc.id, pinInput.ifBlank { "1234" })
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LockOpen,
                                        contentDescription = "Remove from Private Safe",
                                        tint = DocFlowTokens.AccentBlue
                                    )
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(DocFlowTokens.space5))
            }
        }
    }

    // Created By Me Sheet
    if (showCreatedByMeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreatedByMeSheet = false },
            containerColor = palette.surface,
            shape = DocFlowTokens.sheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.cardPaddingMax, vertical = DocFlowTokens.space2)
            ) {
                Text(
                    text = "Created by me (${createdByMeDocs.size})",
                    style = DocFlowTokens.sectionLabelStyle,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = "Scans, created docs, conversions, summaries & translations stored on this device",
                    style = DocFlowTokens.captionStyle,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                if (createdByMeDocs.isEmpty()) {
                    Text(
                        text = "No locally authored or converted files yet.",
                        style = DocFlowTokens.captionStyle,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(vertical = DocFlowTokens.space4)
                    )
                } else {
                    createdByMeDocs.forEach { doc ->
                        DocFlowListRow(
                            title = doc.title,
                            subtitle = "${doc.fileType.uppercase()} • ${doc.fileName} • Stored on this device",
                            palette = palette,
                            onClick = {
                                showCreatedByMeSheet = false
                                onOpenDocument(doc.id)
                            },
                            showChevron = true,
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Filled.Description,
                                    circleColor = DocFlowTokens.AccentBlue
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(DocFlowTokens.space5))
            }
        }
    }

    // Recently Deleted Sheet
    if (showRecentlyDeletedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRecentlyDeletedSheet = false },
            containerColor = palette.surface,
            shape = DocFlowTokens.sheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.cardPaddingMax, vertical = DocFlowTokens.space2)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Recently deleted (${deletedDocs.size})",
                            style = DocFlowTokens.sectionLabelStyle,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Items are automatically purged after 30 days",
                            style = DocFlowTokens.captionStyle,
                            color = palette.textSecondary
                        )
                    }
                    if (deletedDocs.isNotEmpty()) {
                        TextButton(onClick = { viewModel.emptyRecentlyDeleted() }) {
                            Text("Empty trash", color = DocFlowTokens.IconCircleRed, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                if (deletedDocs.isEmpty()) {
                    Text(
                        text = "Recently deleted is empty.",
                        style = DocFlowTokens.captionStyle,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(vertical = DocFlowTokens.space4)
                    )
                } else {
                    deletedDocs.forEach { delDoc ->
                        DocFlowListRow(
                            title = delDoc.title,
                            subtitle = "${delDoc.fileType.uppercase()} • ${delDoc.fileName}",
                            palette = palette,
                            onClick = {},
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Filled.Delete,
                                    circleColor = DocFlowTokens.IconCircleGray
                                )
                            },
                            trailing = {
                                Row {
                                    IconButton(onClick = { viewModel.restoreDeletedDocument(delDoc.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.RestoreFromTrash,
                                            contentDescription = "Restore",
                                            tint = DocFlowTokens.AccentBlue
                                        )
                                    }
                                    IconButton(onClick = { viewModel.permanentlyDeleteDocument(delDoc.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteForever,
                                            contentDescription = "Delete forever",
                                            tint = DocFlowTokens.IconCircleRed
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(DocFlowTokens.space5))
            }
        }
    }

    // Folder Detail Sheet
    activeFolderSheet?.let { folder ->
        val folderDocs = cachedMap.values.filter {
            !it.isDeleted && !it.isPrivateSafe && it.folderId == folder.id
        }
        ModalBottomSheet(
            onDismissRequest = { activeFolderSheet = null },
            containerColor = palette.surface,
            shape = DocFlowTokens.sheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.cardPaddingMax, vertical = DocFlowTokens.space2)
            ) {
                Text(
                    text = folder.name,
                    style = DocFlowTokens.sectionLabelStyle,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = "${folderDocs.size} document(s) • Stored on this device",
                    style = DocFlowTokens.captionStyle,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                if (folderDocs.isEmpty()) {
                    Text(
                        text = "This folder is empty.",
                        style = DocFlowTokens.captionStyle,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(vertical = DocFlowTokens.space4)
                    )
                } else {
                    folderDocs.forEach { doc ->
                        DocFlowListRow(
                            title = doc.title,
                            subtitle = "${doc.fileType.uppercase()} • ${doc.fileName}",
                            palette = palette,
                            onClick = {
                                activeFolderSheet = null
                                onOpenDocument(doc.id)
                            },
                            showChevron = true,
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Filled.Description,
                                    circleColor = DocFlowTokens.AccentBlue
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(DocFlowTokens.space5))
            }
        }
    }

    // SAF Source Folder Sheet (WhatsApp, Download, Bluetooth)
    activeSourceSheetKey?.let { key ->
        val displayTitle = when (key) {
            "whatsapp" -> "WhatsApp Documents"
            "download" -> "Download Folder"
            else -> "Bluetooth Received Files"
        }
        val matchingDocs = cachedMap.values.filter {
            !it.isDeleted && !it.isPrivateSafe &&
                (it.sourceFolderKey?.equals(key, ignoreCase = true) == true ||
                    it.category.equals(key, ignoreCase = true))
        }
        ModalBottomSheet(
            onDismissRequest = { activeSourceSheetKey = null },
            containerColor = palette.surface,
            shape = DocFlowTokens.sheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.cardPaddingMax, vertical = DocFlowTokens.space2)
            ) {
                Text(
                    text = displayTitle,
                    style = DocFlowTokens.sectionLabelStyle,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = "Android SAF folder access • Stored on this device",
                    style = DocFlowTokens.captionStyle,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                Row(horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)) {
                    DocFlowSecondaryButton(
                        text = "Change Folder",
                        onClick = {
                            pendingSourceKey = key
                            activeSourceSheetKey = null
                            safTreeLauncher.launch(null)
                        },
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                    DocFlowPrimaryButton(
                        text = "Rescan Folder",
                        onClick = {
                            val savedUri = OnDeviceAiAndBackupEngine.getSourceFolderUri(context, key)
                            if (savedUri != null) {
                                val uris = OnDeviceAiAndBackupEngine.listFilesInGrantedTreeUri(context, savedUri)
                                uris.forEach { fileUri ->
                                    viewModel.importDocumentFromUri(
                                        context = context,
                                        uri = fileUri,
                                        folderId = "root",
                                        category = key.replaceFirstChar { it.uppercase() },
                                        isEncrypted = false,
                                        password = "",
                                        encryptionKeyHint = null
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                matchingDocs.take(8).forEach { doc ->
                    DocFlowListRow(
                        title = doc.title,
                        subtitle = "${doc.fileType.uppercase()} • ${doc.fileName}",
                        palette = palette,
                        onClick = {
                            activeSourceSheetKey = null
                            onOpenDocument(doc.id)
                        },
                        showChevron = true,
                        leading = {
                            DocFlowIconCircle(
                                icon = Icons.Filled.Description,
                                circleColor = DocFlowTokens.IconCircleGreen
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(DocFlowTokens.space5))
            }
        }
    }

    // Standalone Notes Sheet (Manage > Notes)
    if (showStandaloneNotesSheet) {
        val virtualNotesHostDoc = remember {
            DocEntity(
                id = standaloneNotesDocId,
                userId = viewModel.currentUserId,
                folderId = "root",
                title = "DocFlow Standalone Notes",
                category = "Notes",
                fileType = "note",
                fileName = "standalone_notes.txt",
                fileSize = 1024L,
                content = ""
            )
        }
        val filteredStandalone = remember(standaloneNotes, noteSearchQuery) {
            val q = noteSearchQuery.trim()
            val list = if (q.isEmpty()) standaloneNotes else standaloneNotes.filter {
                it.body.contains(q, ignoreCase = true) || it.anchorLineOrText.contains(q, ignoreCase = true)
            }
            list.sortedByDescending { it.anchorLineOrText.contains("[PINNED]") }
        }

        ModalBottomSheet(
            onDismissRequest = { showStandaloneNotesSheet = false },
            containerColor = palette.surface,
            shape = DocFlowTokens.sheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.cardPaddingMax, vertical = DocFlowTokens.space2)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notes (${standaloneNotes.size})",
                            style = DocFlowTokens.sectionLabelStyle,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Stored on this device • Export PDF or Share with link",
                            style = DocFlowTokens.captionStyle,
                            color = palette.textSecondary
                        )
                    }
                    TextButton(
                        onClick = {
                            showStandaloneNotesSheet = false
                            showMySharedLinksSheet = true
                        }
                    ) {
                        Text("Shared links", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DocFlowTokens.AccentBlue)
                    }
                }

                Spacer(modifier = Modifier.height(DocFlowTokens.space2))

                OutlinedTextField(
                    value = noteSearchQuery,
                    onValueChange = { noteSearchQuery = it },
                    placeholder = { Text("Search inside notes...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manage_notes_search_input")
                )

                Spacer(modifier = Modifier.height(DocFlowTokens.space2))

                // New Note Composer Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(DocFlowTokens.cardShape)
                        .background(palette.background)
                        .border(1.dp, palette.divider, DocFlowTokens.cardShape)
                        .padding(DocFlowTokens.space3),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newNoteTitle,
                        onValueChange = { newNoteTitle = it },
                        placeholder = { Text("Note title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manage_note_title_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Color:", fontSize = 12.sp, color = palette.textSecondary)
                            listOf("#FFFFFF", "#FFF8E1", "#E8F0FF", "#E8F5E9", "#FCE4EC").forEach { hex ->
                                val c = Color(android.graphics.Color.parseColor(hex))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (newNoteColorHex == hex) 2.dp else 1.dp,
                                            color = if (newNoteColorHex == hex) DocFlowTokens.AccentBlue else palette.divider,
                                            shape = CircleShape
                                        )
                                        .clickable { newNoteColorHex = hex }
                                )
                            }
                        }

                        FilterChip(
                            selected = newNotePinned,
                            onClick = { newNotePinned = !newNotePinned },
                            label = { Text(if (newNotePinned) "Pinned" else "Pin", fontSize = 12.sp) }
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newNoteBody,
                            onValueChange = { newNoteBody = it },
                            placeholder = { Text("Write note or checklist items ('- [ ] item')...") },
                            minLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manage_note_body_input")
                        )
                        IconButton(
                            onClick = { showNoteAiMic = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DocFlowTokens.AccentBlue.copy(alpha = 0.12f))
                                .testTag("manage_note_ai_mic_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Dictate note with AI Mic",
                                tint = DocFlowTokens.AccentBlue
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                newNoteBody = if (newNoteBody.isBlank()) "- [ ] " else "${newNoteBody.trimEnd()}\n- [ ] "
                            }
                        ) {
                            Text("+ Checklist", fontSize = 13.sp, color = DocFlowTokens.AccentBlue, fontWeight = FontWeight.SemiBold)
                        }

                        DocFlowPrimaryButton(
                            text = "Save Note",
                            enabled = newNoteBody.isNotBlank() || newNoteTitle.isNotBlank(),
                            onClick = {
                                val resolvedTitle = newNoteTitle.trim().ifBlank { "Untitled Note" }
                                val pinPrefix = if (newNotePinned) "[PINNED] " else ""
                                val bodyText = if (newNoteTitle.isNotBlank()) {
                                    "# ${newNoteTitle.trim()}\n${newNoteBody.trim()}"
                                } else {
                                    newNoteBody.trim()
                                }
                                viewModel.addDocumentComment(
                                    doc = virtualNotesHostDoc,
                                    anchorLineOrText = "$pinPrefix$resolvedTitle • Color:$newNoteColorHex",
                                    body = bodyText,
                                    isSuggestion = false,
                                    suggestedReplacement = ""
                                )
                                newNoteTitle = ""
                                newNoteBody = ""
                                newNotePinned = false
                            },
                            modifier = Modifier.testTag("manage_save_note_button")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(DocFlowTokens.space2))

                if (filteredStandalone.isEmpty()) {
                    Text(
                        text = "No standalone notes yet. Create a note above or use AI Mic to dictate.",
                        style = DocFlowTokens.captionStyle,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(vertical = DocFlowTokens.space3)
                    )
                } else {
                    filteredStandalone.forEach { comment ->
                        val extractedTitle = comment.body.lines().firstOrNull()?.removePrefix("# ")?.trim()?.ifBlank { "Note" } ?: "Note"
                        DocFlowListRow(
                            title = extractedTitle,
                            subtitle = "${comment.body.take(80)} • ${comment.anchorLineOrText}",
                            palette = palette,
                            onClick = {},
                            trailing = {
                                Row {
                                    IconButton(
                                        onClick = {
                                            showStandaloneNotesSheet = false
                                            shareLinkTargetNote = extractedTitle to comment.body
                                        },
                                        modifier = Modifier.testTag("manage_share_note_link_${comment.id}")
                                    ) {
                                        Icon(Icons.Filled.Link, contentDescription = "Share with link", tint = DocFlowTokens.AccentBlue)
                                    }
                                    IconButton(
                                        onClick = {
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, extractedTitle)
                                                putExtra(Intent.EXTRA_TEXT, comment.body)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share note as text"))
                                        }
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = "Share note as text", tint = palette.textPrimary)
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.exportCommentOrNoteAsPdf(context, virtualNotesHostDoc, comment)
                                            showStandaloneNotesSheet = false
                                        }
                                    ) {
                                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export Note as PDF", tint = DocFlowTokens.IconCircleRed)
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteComment(comment.id) }
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete note", tint = palette.textSecondary)
                                    }
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(DocFlowTokens.space5))
            }
        }
    }

    if (showNoteAiMic) {
        AiMicDictationBottomSheet(
            palette = palette,
            initialText = "",
            confirmButtonLabel = "Insert into Note",
            onDismiss = { showNoteAiMic = false },
            onConfirmCleanText = { cleaned ->
                newNoteBody = if (newNoteBody.isBlank()) cleaned else "${newNoteBody.trimEnd()}\n$cleaned"
                showNoteAiMic = false
            }
        )
    }

    shareLinkTargetNote?.let { (noteTitle, noteBody) ->
        ShareNoteWithLinkBottomSheet(
            noteTitle = noteTitle,
            noteBody = noteBody,
            palette = palette,
            onDismiss = { shareLinkTargetNote = null }
        )
    }

    if (showMySharedLinksSheet) {
        MySharedLinksBottomSheet(
            palette = palette,
            onDismiss = { showMySharedLinksSheet = false }
        )
    }
}

@Composable
private fun ManageRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    palette: DocFlowPalette,
    testTag: String,
    onClick: () -> Unit
) {
    DocFlowListRow(
        title = title,
        subtitle = subtitle,
        palette = palette,
        onClick = onClick,
        showChevron = true,
        modifier = Modifier.testTag(testTag),
        leading = {
            DocFlowIconCircle(
                icon = icon,
                circleColor = iconBg
            )
        }
    )
}
