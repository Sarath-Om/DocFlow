package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocEntity
import com.example.data.model.FolderEntity
import com.example.data.util.CryptoUtils
import com.example.data.util.FileParserUtils
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerEditorScreen(
    document: DocEntity,
    folders: List<FolderEntity>,
    localUri: Uri?,
    syncStateLabel: String = "Synced",
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
    onDelete: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var isUnlocked by remember(document.id, document.isEncrypted) {
        mutableStateOf(!document.isEncrypted)
    }
    var unlockPassword by remember(document.id) { mutableStateOf("") }
    var unlockError by remember(document.id) { mutableStateOf<String?>(null) }
    var decryptedContent by remember(document.id, document.content, document.isEncrypted) {
        mutableStateOf(if (!document.isEncrypted) document.content else "")
    }

    var isEditMode by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var renderedPdfBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Editable fields in Live Editor
    var editTitle by remember(document.id, document.title) { mutableStateOf(document.title) }
    var editFolderId by remember(document.id, document.folderId) { mutableStateOf(document.folderId) }
    var editCategory by remember(document.id, document.category) { mutableStateOf(document.category) }
    var editContent by remember(decryptedContent) { mutableStateOf(decryptedContent) }
    var editIsEncrypted by remember(document.id, document.isEncrypted) { mutableStateOf(document.isEncrypted) }
    var editEncryptionPassword by remember(unlockPassword) { mutableStateOf(unlockPassword) }
    var editEncryptionHint by remember(document.id, document.encryptionKeyHint) {
        mutableStateOf(document.encryptionKeyHint ?: "")
    }
    var editIsOfflineCached by remember(document.id, document.isOfflineCached) {
        mutableStateOf(document.isOfflineCached)
    }

    // Keep live decrypted content synced if remote updates while unlocked
    LaunchedEffect(document.content, document.isEncrypted, isUnlocked) {
        if (!document.isEncrypted) {
            decryptedContent = document.content
            if (!isEditMode) {
                editContent = document.content
            }
        } else if (isUnlocked && unlockPassword.isNotBlank()) {
            val res = CryptoUtils.decrypt(document.content, unlockPassword)
            if (res.isSuccess) {
                decryptedContent = res.getOrThrow()
                if (!isEditMode) {
                    editContent = decryptedContent
                }
            }
        }
    }

    // Render PDF page if localUri is a PDF
    LaunchedEffect(localUri, currentPageIndex, document.fileType) {
        if (localUri != null && (document.fileType == "pdf" || document.fileType == "scanned")) {
            renderedPdfBitmap = FileParserUtils.renderPdfPage(context, localUri, currentPageIndex)
        } else {
            renderedPdfBitmap = null
        }
    }

    val categories = listOf("Invoices", "Contracts", "Receipts", "Work", "Personal", "Legal", "Medical", "Notes", "General")

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = document.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${document.fileType.uppercase()} • ${document.category}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "• Room: $syncStateLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("viewer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isUnlocked) {
                        IconButton(
                            onClick = { isEditMode = !isEditMode },
                            modifier = Modifier.testTag("toggle_edit_mode_button")
                        ) {
                            Icon(
                                imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                                contentDescription = if (isEditMode) "Switch to Reader" else "Switch to Live Editor",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_document_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Document",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!isUnlocked) {
            // AES-256 Decryption Gate
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Encrypted Document",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "AES-256 Encrypted Document",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Enter the encryption passphrase or PIN to decrypt '${document.fileName}' for viewing and live editing.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!document.encryptionKeyHint.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Key Hint: ${document.encryptionKeyHint}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = unlockPassword,
                            onValueChange = {
                                unlockPassword = it
                                unlockError = null
                            },
                            label = { Text("Passphrase / PIN") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("decrypt_password_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        if (unlockError != null) {
                            Text(
                                text = unlockError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                val res = CryptoUtils.decrypt(document.content, unlockPassword)
                                if (res.isSuccess) {
                                    decryptedContent = res.getOrThrow()
                                    editContent = decryptedContent
                                    editEncryptionPassword = unlockPassword
                                    isUnlocked = true
                                } else {
                                    unlockError = "Invalid passphrase. Could not decrypt AES-256 payload."
                                }
                            },
                            enabled = unlockPassword.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("unlock_document_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Decrypt & Open Document", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mode Switcher Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isEditMode,
                            onClick = { isEditMode = false },
                            label = { Text("Reader View") },
                            leadingIcon = {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                        FilterChip(
                            selected = isEditMode,
                            onClick = { isEditMode = true },
                            label = { Text("Live Cloud Editor") },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }

                    if (!isEditMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { zoomScale = (zoomScale - 0.15f).coerceAtLeast(0.75f) }
                            ) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                            }
                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { zoomScale = (zoomScale + 0.15f).coerceAtMost(1.75f) }
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                            }
                        }
                    }
                }

                if (!isEditMode) {
                    // Metadata Status Banner
                    val folderName = folders.find { it.id == document.folderId }?.name ?: "Main Workspace"
                    val formattedDate = remember(document.updatedAt, document.createdAt) {
                        val date = (document.updatedAt ?: document.createdAt)?.toDate()
                        if (date != null) {
                            SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.US).format(date)
                        } else {
                            "Synced just now"
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Real-Time Sync + Room SQLite Cache ($syncStateLabel)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                if (document.isOfflineCached) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OfflinePin,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Offline Ready",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "File: ${document.fileName} • Folder: $folderName • ${document.pageCount} page(s) • ${document.fileSize / 1024} KB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Last Updated: $formattedDate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Native PDF Page Renderer if PDF bitmap is available
                    if (renderedPdfBitmap != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    bitmap = renderedPdfBitmap!!.asImageBitmap(),
                                    contentDescription = "Rendered PDF Page ${currentPageIndex + 1}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((420 * zoomScale).dp),
                                    contentScale = ContentScale.Fit
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { if (currentPageIndex > 0) currentPageIndex -= 1 },
                                        enabled = currentPageIndex > 0
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous Page")
                                        Text("Prev")
                                    }
                                    Text(
                                        text = "Page ${currentPageIndex + 1} of ${document.pageCount.coerceAtLeast(1)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            if (currentPageIndex < document.pageCount - 1) {
                                                currentPageIndex += 1
                                            }
                                        },
                                        enabled = currentPageIndex < document.pageCount - 1
                                    ) {
                                        Text("Next")
                                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next Page")
                                    }
                                }
                            }
                        }
                    }

                    // Document Sheet Reader Surface
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(20.dp)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = when (document.fileType.lowercase()) {
                                            "pdf" -> "PDF DOCUMENT VIEWER"
                                            "docx", "doc" -> "MICROSOFT WORD (.DOCX) VIEWER"
                                            "scanned" -> "SCANNED OCR DOCUMENT"
                                            else -> "DOCUMENT VIEWER"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                if (document.isEncrypted) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EnhancedEncryption,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "AES-256 Unlocked",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = document.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            Text(
                                text = decryptedContent.ifBlank { "(Empty Document Content)" },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = (15f * zoomScale).sp,
                                    lineHeight = (23f * zoomScale).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Button(
                        onClick = { isEditMode = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Document & Sync Across Devices", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Instant Live Editor View
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Document Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_title_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Text(
                        text = "Category:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = editCategory.equals(cat, ignoreCase = true),
                                onClick = { editCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    Text(
                        text = "Folder Organization:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = editFolderId == "root",
                            onClick = { editFolderId = "root" },
                            label = { Text("Main Workspace") }
                        )
                        folders.forEach { folder ->
                            FilterChip(
                                selected = editFolderId == folder.id,
                                onClick = { editFolderId = folder.id },
                                label = { Text(folder.name) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("Document Content (Real-Time Cloud & Room Sync)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .testTag("editor_content_input"),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Default),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "AES-256 Encryption",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Encrypt document payload before syncing to cloud",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = editIsEncrypted,
                                    onCheckedChange = { editIsEncrypted = it }
                                )
                            }

                            if (editIsEncrypted) {
                                OutlinedTextField(
                                    value = editEncryptionPassword,
                                    onValueChange = { editEncryptionPassword = it },
                                    label = { Text("Encryption Passphrase / PIN") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = editEncryptionHint,
                                    onValueChange = { editEncryptionHint = it },
                                    label = { Text("Key Hint (Optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Available Offline in Room DB",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Cache locally in SQLite for instant offline access",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = editIsOfflineCached,
                                    onCheckedChange = { editIsOfflineCached = it }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            decryptedContent = editContent
                            unlockPassword = editEncryptionPassword
                            onSaveEdits(
                                editTitle,
                                editFolderId,
                                editCategory,
                                editContent,
                                editIsEncrypted && editEncryptionPassword.isNotBlank(),
                                editEncryptionPassword.ifBlank { null },
                                editEncryptionHint.ifBlank { null },
                                editIsOfflineCached
                            )
                            isEditMode = false
                        },
                        enabled = editTitle.isNotBlank() && (!editIsEncrypted || editEncryptionPassword.isNotBlank()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("save_document_changes_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Sync Instantly Across Devices", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
