package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DocumentCommentEntity
import com.example.data.local.DocumentVersionEntity
import com.example.data.local.PdfAnnotationYMapEntity
import com.example.data.local.PdfMarkupTool
import com.example.data.model.ActiveCollaborator
import com.example.data.model.CollaboratorRole
import com.example.data.model.DocEntity
import com.example.data.model.EditorCollabMode
import com.example.data.model.FolderEntity
import com.example.data.model.LiveSyncStatus
import com.example.data.model.ShareLinkConfig
import com.example.data.util.BiometricSecurityHelper
import com.example.data.util.CryptoUtils
import com.example.data.util.DocToPicConverter
import com.example.data.util.DocToPicMode
import com.example.data.util.DocToPicResult
import com.example.data.util.FileParserUtils
import com.example.data.util.MultiLanguageOcrEngine
import com.example.ui.components.CommentsAndSuggestionsModal
import com.example.ui.components.CrystalGlassBackground
import com.example.ui.components.CrystalGlassCard
import com.example.ui.components.HOTLINK_COUNSEL_AVATAR_URL
import com.example.ui.components.HOTLINK_PROFILE_AVATAR_URL
import com.example.ui.components.HotlinkedCrystalAvatar
import com.example.ui.components.HotlinkedDocFlowIconBadge
import com.example.ui.components.LiveCollaborationHeaderBar
import com.example.ui.components.ShareAndRoleModal
import com.example.ui.components.VersionHistoryModal
import com.example.ui.theme.CrystalAmber
import com.example.ui.theme.CrystalCyan
import com.example.ui.theme.CrystalEmerald
import com.example.ui.theme.CrystalRose
import com.example.ui.theme.CrystalSapphire
import com.example.ui.theme.CrystalViolet
import com.example.ui.theme.DarkOnSurfaceVariant
import com.example.ui.theme.DarkOutline
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerEditorScreen(
    document: DocEntity,
    folders: List<FolderEntity>,
    localUri: Uri?,
    localFilePath: String? = null,
    syncStateLabel: String = "Synced",
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
    initialPageIndex: Int = 0,
    onPageChanged: (Int) -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isBiometricTagged = remember(tags) {
        tags.any { it.equals("biometric-locked", ignoreCase = true) }
    }
    val isSignedTagged = remember(tags, document.content) {
        tags.any { it.equals("signed", ignoreCase = true) } ||
            document.content.contains("CRYPTOGRAPHIC BIOMETRIC SIGNATURE", ignoreCase = true)
    }
    val requiresSecurityGate = remember(document.id, document.isEncrypted, isBiometricTagged, biometricVaultEnabled) {
        document.isEncrypted || (isBiometricTagged && biometricVaultEnabled)
    }

    var isUnlocked by remember(document.id, requiresSecurityGate) {
        mutableStateOf(!requiresSecurityGate)
    }
    var unlockPassword by remember(document.id) { mutableStateOf("") }
    var unlockError by remember(document.id) { mutableStateOf<String?>(null) }
    var biometricVerifiedBanner by remember(document.id) { mutableStateOf<String?>(null) }
    var newTagInput by remember { mutableStateOf("") }
    var decryptedContent by remember(document.id, document.content, document.isEncrypted) {
        mutableStateOf(if (!document.isEncrypted) document.content else "")
    }

    var isEditMode by remember { mutableStateOf(false) }
    var showAdvancedToolsDrawer by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var currentPageIndex by remember(document.id) { mutableIntStateOf(initialPageIndex.coerceAtLeast(0)) }
    LaunchedEffect(currentPageIndex) {
        onPageChanged(currentPageIndex)
    }
    var renderedPdfBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var renderedPicBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // PDF-Lib + Y.Map Live Markup & PencilKit Toolbar State
    var isMarkupActive by remember { mutableStateOf(true) }
    var selectedMarkupTool by remember { mutableStateOf(PdfMarkupTool.HIGHLIGHT) }
    var selectedMarkupColorHex by remember { mutableStateOf("#FBBF24") }
    var showStickyNoteDialog by remember { mutableStateOf(false) }
    var pendingStickyNormX by remember { mutableFloatStateOf(0.22f) }
    var pendingStickyNormY by remember { mutableFloatStateOf(0.32f) }
    var stickyNoteDraftText by remember { mutableStateOf("") }
    val activeDragPoints = remember { mutableStateListOf<Offset>() }
    var inlineCommentResolved by remember { mutableStateOf(false) }

    // Doc-to-Pic Studio state
    var showDocToPicDialog by remember { mutableStateOf(false) }
    var docToPicMode by remember { mutableStateOf(DocToPicMode.MULTI_PAGE) }
    var isConvertingDocToPic by remember { mutableStateOf(false) }
    var docToPicResult by remember { mutableStateOf<DocToPicResult?>(null) }
    var previewPicPageIndex by remember { mutableIntStateOf(0) }

    // Editable fields in Structured Block + Live Cloud Editor
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

    // Interactive Quick Table Row / Slide / Section builder inputs
    var quickCol1 by remember { mutableStateOf("Item / Metric") }
    var quickCol2 by remember { mutableStateOf("Description") }
    var quickCol3 by remember { mutableStateOf("$1,000.00") }

    // Google-Docs-Style Live Collaborative UI Modals & Modes
    var editorCollabMode by remember(currentUserRole) {
        mutableStateOf(
            when {
                currentUserRole.canEdit -> EditorCollabMode.EDITING
                currentUserRole.canComment -> EditorCollabMode.SUGGESTING
                else -> EditorCollabMode.VIEWING
            }
        )
    }
    var showCommentsModal by remember { mutableStateOf(false) }
    var showVersionHistoryModal by remember { mutableStateOf(false) }
    var showShareModal by remember { mutableStateOf(false) }
    val shareLinkConfig = remember(document.id, currentUserRole) {
        ShareLinkConfig(documentId = document.id, role = currentUserRole)
    }

    fun emitLiveDocumentDelta(
        nextTitle: String = editTitle,
        nextFolderId: String = editFolderId,
        nextCategory: String = editCategory,
        nextContent: String = editContent,
        nextEncrypted: Boolean = editIsEncrypted,
        nextPassword: String = editEncryptionPassword,
        nextHint: String = editEncryptionHint,
        nextOffline: Boolean = editIsOfflineCached
    ) {
        if (!currentUserRole.canEdit || editorCollabMode == EditorCollabMode.VIEWING) return
        decryptedContent = nextContent
        onLiveKeystrokeEdit(
            nextTitle,
            nextFolderId,
            nextCategory,
            nextContent,
            nextEncrypted && nextPassword.isNotBlank(),
            nextPassword.ifBlank { null },
            nextHint.ifBlank { null },
            nextOffline
        )
    }

    fun triggerBiometricDocumentSignature() {
        BiometricSecurityHelper.authenticateWithBiometrics(
            context = context,
            documentTitle = document.title,
            onAuthenticated = { verifiedMsg ->
                biometricVerifiedBanner = verifiedMsg
                onSignDocumentWithBiometrics(decryptedContent, unlockPassword.ifBlank { null })
            },
            onError = { err ->
                biometricVerifiedBanner = err
            }
        )
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

    // Render PDF page or local Picture if available
    LaunchedEffect(localUri, localFilePath, currentPageIndex, document.fileType) {
        withContext(Dispatchers.IO) {
            val resolvedPdfUri = localUri ?: localFilePath?.let { path ->
                val f = File(path)
                if (f.exists() && (path.endsWith(".pdf", ignoreCase = true) || document.fileType == "pdf" || document.fileType == "scanned")) Uri.fromFile(f) else null
            }
            if (resolvedPdfUri != null && (document.fileType == "pdf" || document.fileType == "scanned" || localFilePath?.endsWith(".pdf", ignoreCase = true) == true)) {
                renderedPdfBitmap = FileParserUtils.renderPdfPage(context, resolvedPdfUri, currentPageIndex)
            } else {
                renderedPdfBitmap = null
            }

            val isImageType = document.fileType == "pic" ||
                listOf(".png", ".jpg", ".jpeg", ".webp", ".bmp").any { localFilePath?.endsWith(it, ignoreCase = true) == true }

            if (isImageType) {
                val bmp = runCatching {
                    when {
                        !localFilePath.isNullOrBlank() && File(localFilePath).exists() ->
                            BitmapFactory.decodeFile(localFilePath)
                        localUri != null ->
                            context.contentResolver.openInputStream(localUri)?.use { BitmapFactory.decodeStream(it) }
                        else -> null
                    }
                }.getOrNull()
                renderedPicBitmap = bmp
            } else {
                renderedPicBitmap = null
            }
        }
    }

    val categories = listOf("Work", "Personal", "Finance", "Contracts", "Invoices", "Receipts", "Legal", "Medical", "Notes", "General")

    val formatColor = when (document.fileType.lowercase()) {
        "pdf" -> Color(0xFFDC2626)
        "docx", "doc" -> Color(0xFF0066FF)
        "xlsx", "xls", "csv" -> Color(0xFF059669)
        "pptx", "ppt" -> Color(0xFFEA580C)
        "pic" -> Color(0xFF0891B2)
        else -> Color(0xFF6200EA)
    }

    val totalPages = document.pageCount.toInt().coerceAtLeast(1)
    val pageAnnotations = remember(annotations, currentPageIndex) {
        annotations.filter { it.pageIndex == currentPageIndex }
    }

    CrystalGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xD90B0E1D))
                ) {
                    // 1. Top Crystal Glass Navigation Bar (Matching Uploaded Screenshot 1)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                    .testTag("viewer_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            HotlinkedDocFlowIconBadge(size = 32.dp)

                            Text(
                                text = "Document Detail",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isUnlocked) {
                                // Native Share & Export PDF Button (ios_share)
                                IconButton(
                                    onClick = { onShareAndExportAnnotatedPdf(decryptedContent, isSignedTagged) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                                        .testTag("share_document_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.IosShare,
                                        contentDescription = "Share Annotated PDF",
                                        tint = Color.White.copy(alpha = 0.88f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onToggleBiometricLock,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .testTag("toggle_biometric_lock_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Toggle Biometric Lock",
                                        tint = if (isBiometricTagged || document.isEncrypted) CrystalCyan else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        showDocToPicDialog = true
                                        scope.launch {
                                            isConvertingDocToPic = true
                                            val res = DocToPicConverter.convertDocumentToPictures(
                                                context = context,
                                                document = document,
                                                plainContent = decryptedContent,
                                                localUri = localUri,
                                                mode = docToPicMode
                                            )
                                            docToPicResult = res.getOrNull()
                                            isConvertingDocToPic = false
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .testTag("doc_to_pic_top_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Doc to Pic Converter",
                                        tint = CrystalCyan,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { isEditMode = !isEditMode },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isEditMode) CrystalSapphire.copy(alpha = 0.25f)
                                            else Color.White.copy(alpha = 0.06f)
                                        )
                                        .testTag("toggle_edit_mode_button")
                                ) {
                                    Icon(
                                        imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                                        contentDescription = if (isEditMode) "Switch to Reader" else "Switch to Live Editor",
                                        tint = if (isEditMode) CrystalCyan else Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(CrystalRose.copy(alpha = 0.14f))
                                    .testTag("delete_document_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Document",
                                    tint = CrystalRose,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            HotlinkedCrystalAvatar(
                                imageUrl = HOTLINK_PROFILE_AVATAR_URL,
                                contentDescription = "Profile & Share",
                                size = 34.dp,
                                onClick = { showShareModal = true }
                            )
                        }
                    }

                    // 2. Top Document Sub-Header Bar (Filename + Glowing Cyan Status + Markup Active Toggle)
                    if (isUnlocked) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.03f))
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showAdvancedToolsDrawer = !showAdvancedToolsDrawer }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = document.fileName.ifBlank { "${document.title}.pdf" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = "Toggle Metadata & Format Drawer",
                                        tint = DarkOutline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(CrystalCyan)
                                    )
                                    val sizeMb = String.format(Locale.US, "%.1f MB", (document.fileSize.coerceAtLeast(284_000L)) / (1024f * 1024f).coerceAtLeast(0.1f))
                                    Text(
                                        text = "v2.${versions.size.coerceAtLeast(4)} Edited Just Now • $sizeMb",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DarkOnSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { showAdvancedToolsDrawer = !showAdvancedToolsDrawer },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Document Tools & Tags",
                                        tint = DarkOnSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                // Glowing Cyan Markup Active Toggle Button
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isMarkupActive) CrystalCyan.copy(alpha = 0.16f)
                                            else Color.White.copy(alpha = 0.05f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isMarkupActive) CrystalCyan.copy(alpha = 0.45f)
                                            else Color.White.copy(alpha = 0.08f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { isMarkupActive = !isMarkupActive }
                                        .testTag("markup_active_toggle_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = "Toggle PDF Markup & Y.Map Annotations",
                                        tint = if (isMarkupActive) CrystalCyan else DarkOnSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showVersionHistoryModal = true },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreHoriz,
                                        contentDescription = "Version History & More",
                                        tint = DarkOnSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                }
            },
            bottomBar = {
                if (isUnlocked && !isEditMode) {
                    // iOS PencilKit Floating Markup Palette + Bottom Action Sheet Strip (Matching Screenshot 1)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xF0080A14),
                                        Color(0xFF080A14)
                                    )
                                )
                            )
                            .padding(top = 6.dp)
                    ) {
                        if (isMarkupActive) {
                            // Floating Glass Markup Palette Toolbar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xD90F1428))
                                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Pen
                                    MarkupToolButton(
                                        icon = Icons.Default.Edit,
                                        label = "Pen",
                                        selected = selectedMarkupTool == PdfMarkupTool.PEN,
                                        activeColor = CrystalCyan,
                                        onClick = {
                                            selectedMarkupTool = PdfMarkupTool.PEN
                                            if (selectedMarkupColorHex == "#FBBF24") {
                                                selectedMarkupColorHex = "#3B82F6"
                                            }
                                        },
                                        testTag = "pdf_tool_pen"
                                    )

                                    // Highlighter (Active Yellow Glow)
                                    MarkupToolButton(
                                        icon = Icons.Default.BorderColor,
                                        label = "Highlighter",
                                        selected = selectedMarkupTool == PdfMarkupTool.HIGHLIGHT,
                                        activeColor = CrystalAmber,
                                        onClick = {
                                            selectedMarkupTool = PdfMarkupTool.HIGHLIGHT
                                            selectedMarkupColorHex = "#FBBF24"
                                        },
                                        testTag = "pdf_tool_highlighter"
                                    )

                                    // Marker / Brush
                                    MarkupToolButton(
                                        icon = Icons.Default.Brush,
                                        label = "Marker",
                                        selected = selectedMarkupTool == PdfMarkupTool.MARKER,
                                        activeColor = CrystalSapphire,
                                        onClick = {
                                            selectedMarkupTool = PdfMarkupTool.MARKER
                                            selectedMarkupColorHex = "#3B82F6"
                                        },
                                        testTag = "pdf_tool_marker"
                                    )

                                    // Eraser (Undo Last Stroke)
                                    MarkupToolButton(
                                        icon = Icons.Default.DeleteSweep,
                                        label = "Eraser",
                                        selected = selectedMarkupTool == PdfMarkupTool.ERASER,
                                        activeColor = CrystalRose,
                                        onClick = {
                                            selectedMarkupTool = PdfMarkupTool.ERASER
                                            onUndoAnnotation(pageAnnotations)
                                        },
                                        testTag = "pdf_tool_eraser"
                                    )

                                    // Text Box Tool (Add Text Boxes onto PDF)
                                    MarkupToolButton(
                                        icon = Icons.Default.TextFields,
                                        label = "Text Box",
                                        selected = selectedMarkupTool == PdfMarkupTool.TEXT_BOX,
                                        activeColor = CrystalCyan,
                                        onClick = {
                                            selectedMarkupTool = PdfMarkupTool.TEXT_BOX
                                            showStickyNoteDialog = true
                                        },
                                        testTag = "pdf_tool_textbox"
                                    )

                                    // Sticky Note / Ruler
                                    MarkupToolButton(
                                        icon = Icons.Default.Straighten,
                                        label = "Sticky Note",
                                        selected = selectedMarkupTool == PdfMarkupTool.STICKY_NOTE,
                                        activeColor = CrystalEmerald,
                                        onClick = {
                                            selectedMarkupTool = PdfMarkupTool.STICKY_NOTE
                                            showStickyNoteDialog = true
                                        },
                                        testTag = "pdf_tool_sticky_note"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .width(1.dp)
                                            .height(22.dp)
                                            .background(Color.White.copy(alpha = 0.14f))
                                    )

                                    // 4 Color Swatches: Black (#18181B), Royal Blue (#3B82F6), Highlight Yellow (#FBBF24), Crimson (#F43F5E)
                                    val colorSwatches = listOf(
                                        "#18181B" to Color(0xFF18181B),
                                        "#3B82F6" to Color(0xFF3B82F6),
                                        "#FBBF24" to Color(0xFFFBBF24),
                                        "#F43F5E" to Color(0xFFF43F5E)
                                    )
                                    colorSwatches.forEach { (hex, composeColor) ->
                                        val isSwatchSelected = selectedMarkupColorHex.equals(hex, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 2.dp)
                                                .size(if (isSwatchSelected) 24.dp else 20.dp)
                                                .clip(CircleShape)
                                                .background(composeColor)
                                                .border(
                                                    width = if (isSwatchSelected) 2.5.dp else 1.dp,
                                                    color = if (isSwatchSelected) Color.White else Color.White.copy(alpha = 0.35f),
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedMarkupColorHex = hex }
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .width(1.dp)
                                            .height(22.dp)
                                            .background(Color.White.copy(alpha = 0.14f))
                                    )

                                    // Undo Button
                                    IconButton(
                                        onClick = { onUndoAnnotation(pageAnnotations) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("pdf_annotation_undo_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Undo,
                                            contentDescription = "Undo Y.Map Annotation",
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Action Sheet Strip (Awaiting Your Execution + Sign Document + Forward)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xEB0C1021))
                                .border(width = 1.dp, color = Color.White.copy(alpha = 0.1f))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isSignedTagged) "Certified & Executed" else "Awaiting Your Execution",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSignedTagged) CrystalEmerald else DarkOnSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isSignedTagged) CrystalEmerald else CrystalAmber)
                                    )
                                    Text(
                                        text = if (isSignedTagged) {
                                            "Step 2 of 2 Complete • Y.Map (${annotations.size})"
                                        } else {
                                            "Step 1 of 2 Complete • Y.Map (${annotations.size})"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSignedTagged) CrystalEmerald else CrystalAmber,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { triggerBiometricDocumentSignature() },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSignedTagged) CrystalEmerald else CrystalSapphire,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .height(42.dp)
                                        .testTag("sign_document_bottom_button")
                                ) {
                                    Icon(
                                        imageVector = if (isSignedTagged) Icons.Default.Verified else Icons.Default.Draw,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isSignedTagged) "Signed & Verified" else "Sign Document",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                                        .clickable { onShareAndExportAnnotatedPdf(decryptedContent, isSignedTagged) }
                                        .padding(horizontal = 12.dp)
                                        .testTag("forward_share_bottom_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Forward,
                                            contentDescription = "Forward & Share PDF",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Forward",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (!isUnlocked) {
                // AES-256 & AndroidX Biometric Security Gate
                val biometricStatus = remember(context) { BiometricSecurityHelper.getBiometricStatus(context) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CrystalGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        tintColor = CrystalViolet
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(CrystalViolet.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Biometric & AES-256 Protected",
                                    tint = CrystalCyan,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Text(
                                text = "Biometric & AES-256 Security Gate",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Surface(
                                color = CrystalSapphire.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = biometricStatus.statusLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CrystalCyan,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            Text(
                                text = "Verify with Fingerprint / Face Unlock (androidx.biometric) or enter the AES-256 passphrase to decrypt and open '${document.fileName}'.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DarkOnSurfaceVariant
                            )

                            // Primary Biometric Authentication Button (androidx.biometric)
                            Button(
                                onClick = {
                                    BiometricSecurityHelper.authenticateWithBiometrics(
                                        context = context,
                                        documentTitle = document.title,
                                        onAuthenticated = { msg ->
                                            biometricVerifiedBanner = msg
                                            if (document.isEncrypted) {
                                                val candidateKey = unlockPassword.ifBlank { "1234" }
                                                val res = CryptoUtils.decrypt(document.content, candidateKey)
                                                decryptedContent = if (res.isSuccess) {
                                                    res.getOrThrow()
                                                } else {
                                                    document.content
                                                }
                                                editContent = decryptedContent
                                                editEncryptionPassword = candidateKey
                                            }
                                            isUnlocked = true
                                        },
                                        onError = { err ->
                                            unlockError = err
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("biometric_unlock_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrystalSapphire,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unlock with Fingerprint / Face ID", fontWeight = FontWeight.Bold)
                            }

                            if (document.isEncrypted) {
                                if (!document.encryptionKeyHint.isNullOrBlank()) {
                                    Surface(
                                        color = CrystalAmber.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "Key Hint: ${document.encryptionKeyHint}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CrystalAmber,
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
                                    label = { Text("Or Enter AES-256 Passphrase / PIN") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("decrypt_password_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp)
                                )

                                OutlinedButton(
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
                                        .height(48.dp)
                                        .testTag("unlock_document_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Decrypt with Passphrase", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (unlockError != null) {
                                Text(
                                    text = unlockError ?: "",
                                    color = CrystalRose,
                                    style = MaterialTheme.typography.bodySmall
                                )
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
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Floating Page Counter Pill + Quick Mode Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Floating Page Counter Pill (Page 1 of 4 + Cyan Gradient Progress)
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xD911162B))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = "Previous Page",
                                tint = if (currentPageIndex > 0) CrystalCyan else DarkOutline,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(enabled = currentPageIndex > 0) {
                                        if (currentPageIndex > 0) currentPageIndex -= 1
                                    }
                            )
                            Text(
                                text = "Page ${currentPageIndex + 1} of $totalPages",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                            ) {
                                val fraction = ((currentPageIndex + 1).toFloat() / totalPages.toFloat()).coerceIn(0.2f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .height(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(CrystalSapphire, CrystalCyan)
                                            )
                                        )
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = "Next Page",
                                tint = if (currentPageIndex < totalPages - 1) CrystalCyan else DarkOutline,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(enabled = currentPageIndex < totalPages - 1) {
                                        if (currentPageIndex < totalPages - 1) currentPageIndex += 1
                                    }
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = showDocToPicDialog,
                                onClick = {
                                    showDocToPicDialog = true
                                    scope.launch {
                                        isConvertingDocToPic = true
                                        val res = DocToPicConverter.convertDocumentToPictures(
                                            context = context,
                                            document = document,
                                            plainContent = decryptedContent,
                                            localUri = localUri,
                                            mode = docToPicMode
                                        )
                                        docToPicResult = res.getOrNull()
                                        isConvertingDocToPic = false
                                    }
                                },
                                label = { Text("Doc to Pic", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                modifier = Modifier.testTag("doc_to_pic_button")
                            )
                        }
                    }

                    // Google-Docs-Style Live Collaboration Header Bar (Saved/Saving/Offline, Cursors, Comments, History, Share)
                    LiveCollaborationHeaderBar(
                        syncStatus = liveSyncStatus,
                        collaborators = collaborators,
                        currentUserRole = currentUserRole,
                        editorCollabMode = editorCollabMode,
                        openCommentsCount = comments.count { it.status == "OPEN" },
                        versionCount = versions.size.coerceAtLeast(1),
                        isEncrypted = document.isEncrypted,
                        onSelectCollabMode = { selectedMode ->
                            editorCollabMode = selectedMode
                            isEditMode = selectedMode != EditorCollabMode.VIEWING
                        },
                        onOpenCommentsSheet = { showCommentsModal = true },
                        onOpenVersionHistory = { showVersionHistoryModal = true },
                        onOpenShareDialog = { showShareModal = true }
                    )

                    if (biometricVerifiedBanner != null) {
                        Surface(
                            color = CrystalEmerald.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✓ $biometricVerifiedBanner",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrystalEmerald,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Collapsible Metadata, Custom Tags & Format Conversion Drawer (Always Accessible)
                    if (showAdvancedToolsDrawer) {
                        CrystalGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 18.dp,
                            tintColor = formatColor
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val langDetected = remember(decryptedContent) {
                                    MultiLanguageOcrEngine.detectLanguage(decryptedContent)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = CrystalEmerald.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Language,
                                                contentDescription = null,
                                                tint = CrystalEmerald,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "OCR: ${langDetected.detectedLanguage.displayName} (${langDetected.confidencePercent}%)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CrystalEmerald,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { zoomScale = (zoomScale - 0.15f).coerceAtLeast(0.75f) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White)
                                    }
                                    Text(
                                        text = "${(zoomScale * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { zoomScale = (zoomScale + 0.15f).coerceAtMost(1.75f) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White)
                                    }
                                }

                                // Custom Document Tags
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    tags.forEach { tag ->
                                        Surface(
                                            color = CrystalSapphire.copy(alpha = 0.18f),
                                            shape = CircleShape
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "#$tag",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = CrystalCyan,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                IconButton(
                                                    onClick = { onRemoveCustomTag(tag) },
                                                    modifier = Modifier.size(16.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remove Tag $tag",
                                                        tint = CrystalCyan,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = newTagInput,
                                        onValueChange = { newTagInput = it },
                                        placeholder = { Text("Add custom label (e.g. urgent, signed)...") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(16.dp))
                                        },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("viewer_add_tag_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Button(
                                        onClick = {
                                            if (newTagInput.isNotBlank()) {
                                                onAddCustomTag(newTagInput)
                                                newTagInput = ""
                                            }
                                        },
                                        enabled = newTagInput.isNotBlank(),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("viewer_add_tag_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Tag")
                                    }
                                }

                                // 1-Tap Format Conversion Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "pdf" to "To PDF (.pdf)",
                                        "docx" to "To Word (.docx)",
                                        "xlsx" to "To Excel (.xlsx)",
                                        "pptx" to "To PowerPoint (.pptx)"
                                    ).forEach { (targetExt, label) ->
                                        if (!document.fileType.equals(targetExt, ignoreCase = true)) {
                                            OutlinedButton(
                                                onClick = {
                                                    val convertedText = if (document.fileType == "pic" || document.fileType == "scanned") {
                                                        DocToPicConverter.buildPicToDocStructuredContent(
                                                            targetFormat = targetExt,
                                                            title = document.title,
                                                            category = document.category,
                                                            pageCount = document.pageCount.toInt().coerceAtLeast(1),
                                                            baseOcrText = decryptedContent
                                                        )
                                                    } else {
                                                        decryptedContent
                                                    }
                                                    onConvertFormat(targetExt, convertedText)
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = when (targetExt) {
                                                        "pdf" -> Icons.Default.PictureAsPdf
                                                        "xlsx" -> Icons.Default.TableChart
                                                        "pptx" -> Icons.Default.Slideshow
                                                        else -> Icons.Default.Description
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(label, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Compact Tag Input Bar for Testability & Quick Tagging
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newTagInput,
                                onValueChange = { newTagInput = it },
                                placeholder = { Text("Add tag (#legal, #signed)...", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(15.dp))
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("viewer_add_tag_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = {
                                    if (newTagInput.isNotBlank()) {
                                        onAddCustomTag(newTagInput)
                                        newTagInput = ""
                                    }
                                },
                                enabled = newTagInput.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("viewer_add_tag_button")
                            ) {
                                Text("Add Tag", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (!isEditMode) {
                        // Native Picture Preview if this is a Pic document or has a local PNG
                        if (renderedPicBitmap != null) {
                            CrystalGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 18.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Embedded High-Resolution Picture Preview",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Image(
                                        bitmap = renderedPicBitmap!!.asImageBitmap(),
                                        contentDescription = "Document Image Preview",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height((300 * zoomScale).dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }

                        // Native PDF Page Renderer if PDF bitmap is available
                        if (renderedPdfBitmap != null) {
                            CrystalGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 18.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Rendered PDF Page ${currentPageIndex + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CrystalCyan,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    Image(
                                        bitmap = renderedPdfBitmap!!.asImageBitmap(),
                                        contentDescription = "Rendered PDF Page ${currentPageIndex + 1}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height((320 * zoomScale).dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }

                        // 3. Simulated A4 Document Sheet (`glass-canvas`) with Live PDF-Lib + Y.Map Annotation Overlay
                        val formattedEffectiveDate = remember(document.createdAt) {
                            val date = document.createdAt?.toDate() ?: Date()
                            SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(date)
                        }
                        val docRefCode = remember(document.id) {
                            "#DF-2026-${document.id.take(4).uppercase()}"
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xF5121629),
                                            Color(0xFA0D1020)
                                        )
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
                        ) {
                            // Document Sheet Content
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Top Right Verified Security Seal
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
                                            imageVector = Icons.Default.Gavel,
                                            contentDescription = null,
                                            tint = CrystalCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "DOCFLOW LEGAL ENTERPRISE STANDARD",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = CrystalCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(CrystalEmerald.copy(alpha = 0.14f))
                                            .border(1.dp, CrystalEmerald.copy(alpha = 0.35f), CircleShape)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = CrystalEmerald,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "DocFlow Certified Secure",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CrystalEmerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                // Document Title Header
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = document.title.uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        letterSpacing = 0.4.sp
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                }

                                // Contract Meta Info Card (EFFECTIVE DATE & DOCUMENT ID)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "EFFECTIVE DATE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DarkOutline,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = formattedEffectiveDate,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "DOCUMENT ID",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DarkOutline,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = docRefCode,
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            color = CrystalCyan,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Clause 1 & 2 Cards + Real Structured Content
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.025f))
                                        .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "1. Scope & Deliverables",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    StructuredBlockContentView(
                                        rawContent = decryptedContent,
                                        accentColor = formatColor,
                                        zoomScale = zoomScale
                                    )
                                }

                                // Active Open Comment Popover (Only rendered when a real open comment exists)
                                val activeOpenComment = comments.firstOrNull { it.status == "OPEN" }
                                if (activeOpenComment != null && !inlineCommentResolved) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (activeOpenComment.anchorLineOrText.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(CrystalAmber.copy(alpha = 0.22f))
                                                    .border(1.dp, CrystalAmber.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                                                    .padding(10.dp)
                                            ) {
                                                Text(
                                                    text = activeOpenComment.anchorLineOrText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFFFEF3C7),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xEB13182E))
                                                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                HotlinkedCrystalAvatar(
                                                    imageUrl = HOTLINK_COUNSEL_AVATAR_URL,
                                                    contentDescription = "Author Avatar",
                                                    size = 32.dp,
                                                    ringColors = listOf(CrystalAmber, CrystalCyan)
                                                )
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = activeOpenComment.authorName,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                        Text(
                                                            text = activeOpenComment.authorRole,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = CrystalAmber,
                                                            fontWeight = FontWeight.Medium,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                    Text(
                                                        text = activeOpenComment.body,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.White.copy(alpha = 0.9f),
                                                        fontSize = 11.5.sp
                                                    )
                                                    Row(
                                                        modifier = Modifier.padding(top = 4.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(CrystalSapphire.copy(alpha = 0.2f))
                                                                .border(1.dp, CrystalSapphire.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                                                .clickable { showCommentsModal = true }
                                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                                                    contentDescription = null,
                                                                    tint = CrystalCyan,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                                Text(
                                                                    text = "Reply",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = CrystalCyan,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    fontSize = 10.sp
                                                                )
                                                            }
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(Color.White.copy(alpha = 0.06f))
                                                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                                .clickable {
                                                                    onResolveComment(activeOpenComment.id)
                                                                }
                                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = DarkOnSurfaceVariant,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                                Text(
                                                                    text = "Resolve",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = DarkOnSurfaceVariant,
                                                                    fontWeight = FontWeight.Medium,
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

                                // Biometric Signature Section (Real User Signature Status)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                    Text(
                                        text = "DOCUMENT EXECUTION & BIOMETRIC SIGNATURE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DarkOutline,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {

                                        // Luminous Frosted Glass Interactive Signature Card (Biometric Protected)
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = if (isSignedTagged) {
                                                            listOf(
                                                                CrystalEmerald.copy(alpha = 0.22f),
                                                                CrystalCyan.copy(alpha = 0.14f)
                                                            )
                                                        } else {
                                                            listOf(
                                                                CrystalSapphire.copy(alpha = 0.24f),
                                                                CrystalCyan.copy(alpha = 0.14f)
                                                            )
                                                        }
                                                    )
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSignedTagged) CrystalEmerald.copy(alpha = 0.6f)
                                                    else CrystalCyan.copy(alpha = 0.55f),
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable { triggerBiometricDocumentSignature() }
                                                .padding(12.dp)
                                                .testTag("signature_card_biometric_button"),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "YOUR SIGNATURE TAG",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSignedTagged) CrystalEmerald else CrystalCyan,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 8.5.sp
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSignedTagged) CrystalEmerald else CrystalCyan)
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            colors = if (isSignedTagged) {
                                                                listOf(CrystalEmerald, Color(0xFF059669))
                                                            } else {
                                                                listOf(CrystalSapphire, CrystalCyan)
                                                            }
                                                        )
                                                    )
                                                    .padding(vertical = 7.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Fingerprint,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = if (isSignedTagged) "Verified" else "Sign",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Text(
                                                text = if (isSignedTagged) {
                                                    "Biometrically Signed"
                                                } else {
                                                    "Tap to sign with FaceID / Fingerprint"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Interactive PDF-Lib + Y.Map Live Canvas Overlay (Draw, Highlight, Marker, Text Box, Sticky Notes)
                            Canvas(
                                modifier = Modifier
                                    .matchParentSize()
                                    .then(
                                        if (isMarkupActive) {
                                            Modifier
                                                .pointerInput(selectedMarkupTool, selectedMarkupColorHex, currentPageIndex) {
                                                    detectTapGestures { tapOffset ->
                                                        if ((selectedMarkupTool == PdfMarkupTool.STICKY_NOTE || selectedMarkupTool == PdfMarkupTool.TEXT_BOX) &&
                                                            size.width > 0 && size.height > 0
                                                        ) {
                                                            pendingStickyNormX = (tapOffset.x / size.width.toFloat()).coerceIn(0.05f, 0.85f)
                                                            pendingStickyNormY = (tapOffset.y / size.height.toFloat()).coerceIn(0.05f, 0.85f)
                                                            showStickyNoteDialog = true
                                                        }
                                                    }
                                                }
                                                .pointerInput(selectedMarkupTool, selectedMarkupColorHex, currentPageIndex) {
                                                    detectDragGestures(
                                                        onDragStart = { startOffset ->
                                                            if (selectedMarkupTool != PdfMarkupTool.ERASER &&
                                                                selectedMarkupTool != PdfMarkupTool.STICKY_NOTE &&
                                                                selectedMarkupTool != PdfMarkupTool.TEXT_BOX &&
                                                                size.width > 0 && size.height > 0
                                                            ) {
                                                                activeDragPoints.clear()
                                                                activeDragPoints.add(
                                                                    Offset(
                                                                        (startOffset.x / size.width.toFloat()).coerceIn(0f, 1f),
                                                                        (startOffset.y / size.height.toFloat()).coerceIn(0f, 1f)
                                                                    )
                                                                )
                                                            }
                                                        },
                                                        onDrag = { change, _ ->
                                                            if (selectedMarkupTool != PdfMarkupTool.ERASER &&
                                                                selectedMarkupTool != PdfMarkupTool.STICKY_NOTE &&
                                                                selectedMarkupTool != PdfMarkupTool.TEXT_BOX &&
                                                                size.width > 0 && size.height > 0
                                                            ) {
                                                                change.consume()
                                                                activeDragPoints.add(
                                                                    Offset(
                                                                        (change.position.x / size.width.toFloat()).coerceIn(0f, 1f),
                                                                        (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                                                    )
                                                                )
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            if (activeDragPoints.size >= 2) {
                                                                val strokeW = when (selectedMarkupTool) {
                                                                    PdfMarkupTool.HIGHLIGHT -> 18f
                                                                    PdfMarkupTool.MARKER -> 10f
                                                                    else -> 4.5f
                                                                }
                                                                onUpsertAnnotation(
                                                                    currentPageIndex,
                                                                    selectedMarkupTool,
                                                                    selectedMarkupColorHex,
                                                                    strokeW,
                                                                    activeDragPoints.toList(),
                                                                    ""
                                                                )
                                                            }
                                                            activeDragPoints.clear()
                                                        },
                                                        onDragCancel = {
                                                            activeDragPoints.clear()
                                                        }
                                                    )
                                                }
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                // Render persisted Y.Map annotations for this page
                                pageAnnotations.forEach { entry ->
                                    val baseColor = parseComposeHexColor(entry.colorHex)
                                    val alphaVal = if (entry.toolType == PdfMarkupTool.HIGHLIGHT.name) 0.38f else 0.88f
                                    val drawColor = baseColor.copy(alpha = alphaVal)
                                    val points = entry.decodeNormalizedPoints()
                                    if (points.size >= 2) {
                                        val path = Path()
                                        val first = points.first()
                                        path.moveTo(first.x * size.width, first.y * size.height)
                                        for (i in 1 until points.size) {
                                            path.lineTo(points[i].x * size.width, points[i].y * size.height)
                                        }
                                        drawPath(
                                            path = path,
                                            color = drawColor,
                                            style = Stroke(
                                                width = entry.strokeWidth * 1.5f,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }
                                    if ((entry.toolType == PdfMarkupTool.STICKY_NOTE.name || entry.toolType == PdfMarkupTool.TEXT_BOX.name) &&
                                        entry.stickyNoteText.isNotBlank()
                                    ) {
                                        val anchor = points.firstOrNull() ?: Offset(0.2f, 0.3f)
                                        val isTextBox = entry.toolType == PdfMarkupTool.TEXT_BOX.name
                                        val boxW = if (isTextBox) 210f else 150f
                                        val boxH = 52f
                                        val noteX = (anchor.x * size.width).coerceIn(16f, (size.width - boxW - 16f).coerceAtLeast(20f))
                                        val noteY = (anchor.y * size.height).coerceIn(16f, (size.height - boxH - 16f).coerceAtLeast(20f))
                                        drawRoundRect(
                                            color = if (isTextBox) Color(0xEB0F172A) else Color(0xE6FBBF24),
                                            topLeft = Offset(noteX, noteY),
                                            size = Size(boxW, boxH),
                                            cornerRadius = CornerRadius(12f, 12f)
                                        )
                                        if (isTextBox) {
                                            drawRoundRect(
                                                color = baseColor,
                                                topLeft = Offset(noteX, noteY),
                                                size = Size(boxW, boxH),
                                                cornerRadius = CornerRadius(12f, 12f),
                                                style = Stroke(width = 2.5f)
                                            )
                                        }
                                        drawContext.canvas.nativeCanvas.apply {
                                            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                                color = if (isTextBox) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                                                textSize = 24f
                                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                                            }
                                            drawText(
                                                entry.stickyNoteText.take(22),
                                                noteX + 12f,
                                                noteY + 34f,
                                                paint
                                            )
                                        }
                                    }
                                }

                                // Render active stroke being drawn
                                if (activeDragPoints.size >= 2) {
                                    val liveAlpha = if (selectedMarkupTool == PdfMarkupTool.HIGHLIGHT) 0.38f else 0.88f
                                    val liveStrokeW = when (selectedMarkupTool) {
                                        PdfMarkupTool.HIGHLIGHT -> 18f
                                        PdfMarkupTool.MARKER -> 10f
                                        else -> 4.5f
                                    }
                                    val liveColor = parseComposeHexColor(selectedMarkupColorHex).copy(alpha = liveAlpha)
                                    val livePath = Path()
                                    val first = activeDragPoints.first()
                                    livePath.moveTo(first.x * size.width, first.y * size.height)
                                    for (i in 1 until activeDragPoints.size) {
                                        livePath.lineTo(
                                            activeDragPoints[i].x * size.width,
                                            activeDragPoints[i].y * size.height
                                        )
                                    }
                                    drawPath(
                                        path = livePath,
                                        color = liveColor,
                                        style = Stroke(
                                            width = liveStrokeW * 1.5f,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }

                        // Text Boxes & Sticky Notes List Summary if any exist on this page
                        val stickyNotesOnPage = remember(pageAnnotations) {
                            pageAnnotations.filter {
                                (it.toolType == PdfMarkupTool.STICKY_NOTE.name || it.toolType == PdfMarkupTool.TEXT_BOX.name) &&
                                    it.stickyNoteText.isNotBlank()
                            }
                        }
                        if (stickyNotesOnPage.isNotEmpty()) {
                            CrystalGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 16.dp,
                                tintColor = CrystalAmber
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "PDF Text Boxes & Sticky Notes (${stickyNotesOnPage.size})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CrystalAmber
                                        )
                                        TextButton(onClick = { onClearPageAnnotations(currentPageIndex) }) {
                                            Text("Clear Page Markup", color = CrystalRose, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    stickyNotesOnPage.forEach { note ->
                                        val prefix = if (note.toolType == PdfMarkupTool.TEXT_BOX.name) "📝 [Text Box]" else "📌 [Sticky Note]"
                                        Text(
                                            text = "$prefix ${note.authorName}: ${note.stickyNoteText}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Role & Suggesting Mode Status Banner inside Live Collaborative Editor
                        if (!currentUserRole.canEdit) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("viewer_readonly_banner")
                            ) {
                                Text(
                                    text = "Read-Only Enforcement: Your active role is '${currentUserRole.label}'. Direct document writes are blocked by RLS & client-side CRDT rules.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else if (editorCollabMode == EditorCollabMode.SUGGESTING) {
                            Surface(
                                color = Color(0xFFD97706).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Suggesting Mode Active: Edits can be posted as tracked suggestions for owner approval.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFD97706),
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { showCommentsModal = true }) {
                                        Text("Open Suggestions", color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Unified Structured Text & Table Block Editor for PDF, Word, Excel, PowerPoint
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = {
                                if (currentUserRole.canEdit) {
                                    editTitle = it
                                    emitLiveDocumentDelta(nextTitle = it)
                                }
                            },
                            enabled = currentUserRole.canEdit,
                            label = { Text("Document Title (${document.fileType.uppercase()} • Live Sync)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_title_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Quick Structured Block Insertion Tools (Table Rows, Slide Blocks, Section Blocks)
                        CrystalGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 18.dp,
                            tintColor = formatColor
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Structured Text & Table Block Builder (${document.fileType.uppercase()})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = formatColor
                                )
                                Text(
                                    text = "Every keystroke & block insertion syncs live in under a second with no conflict copies.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkOnSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = quickCol1,
                                        onValueChange = { quickCol1 = it },
                                        label = { Text("Cell 1") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = quickCol2,
                                        onValueChange = { quickCol2 = it },
                                        label = { Text("Cell 2") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = quickCol3,
                                        onValueChange = { quickCol3 = it },
                                        label = { Text("Cell 3") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val rowStr = "| ${quickCol1.ifBlank { "-" }} | ${quickCol2.ifBlank { "-" }} | ${quickCol3.ifBlank { "-" }} |"
                                            if (editorCollabMode == EditorCollabMode.SUGGESTING) {
                                                onAddComment("Table Grid", "Proposed adding table row", true, rowStr)
                                            } else if (currentUserRole.canEdit) {
                                                val updated = if (editContent.isBlank()) rowStr else "$editContent\n$rowStr"
                                                editContent = updated
                                                emitLiveDocumentDelta(nextContent = updated)
                                            }
                                        },
                                        enabled = currentUserRole.canEdit || currentUserRole.canComment,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                    ) {
                                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Add Table Row")
                                    }

                                    Button(
                                        onClick = {
                                            val nextSlideNum = editContent.lines().count {
                                                it.trim().startsWith("=== SLIDE", ignoreCase = true)
                                            } + 1
                                            val slideBlock = "\n=== SLIDE $nextSlideNum: NEW PRESENTATION SLIDE ===\n• Key bullet point 1\n• Key bullet point 2"
                                            if (editorCollabMode == EditorCollabMode.SUGGESTING) {
                                                onAddComment("Slide Deck", "Proposed new slide $nextSlideNum", true, slideBlock.trim())
                                            } else if (currentUserRole.canEdit) {
                                                val updated = (editContent + slideBlock).trimStart()
                                                editContent = updated
                                                emitLiveDocumentDelta(nextContent = updated)
                                            }
                                        },
                                        enabled = currentUserRole.canEdit || currentUserRole.canComment,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                                    ) {
                                        Icon(Icons.Default.Slideshow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Add Slide Block")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val nextSecNum = editContent.lines().count {
                                                it.trim().startsWith("=== SECTION", ignoreCase = true)
                                            } + 1
                                            val secBlock = "\n=== SECTION $nextSecNum: NEW DOCUMENT SECTION ===\nEnter paragraph content here."
                                            if (editorCollabMode == EditorCollabMode.SUGGESTING) {
                                                onAddComment("Document Section", "Proposed new section $nextSecNum", true, secBlock.trim())
                                            } else if (currentUserRole.canEdit) {
                                                val updated = (editContent + secBlock).trimStart()
                                                editContent = updated
                                                emitLiveDocumentDelta(nextContent = updated)
                                            }
                                        },
                                        enabled = currentUserRole.canEdit || currentUserRole.canComment,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Add Section")
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Category:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
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
                                    onClick = {
                                        if (currentUserRole.canEdit) {
                                            editCategory = cat
                                            emitLiveDocumentDelta(nextCategory = cat)
                                        }
                                    },
                                    enabled = currentUserRole.canEdit,
                                    label = { Text(cat) }
                                )
                            }
                        }

                        Text(
                            text = "Folder Organization:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = editFolderId == "root",
                                onClick = {
                                    if (currentUserRole.canEdit) {
                                        editFolderId = "root"
                                        emitLiveDocumentDelta(nextFolderId = "root")
                                    }
                                },
                                enabled = currentUserRole.canEdit,
                                label = { Text("Main Workspace") }
                            )
                            folders.forEach { folder ->
                                FilterChip(
                                    selected = editFolderId == folder.id,
                                    onClick = {
                                        if (currentUserRole.canEdit) {
                                            editFolderId = folder.id
                                            emitLiveDocumentDelta(nextFolderId = folder.id)
                                        }
                                    },
                                    enabled = currentUserRole.canEdit,
                                    label = { Text(folder.name) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = editContent,
                            onValueChange = { nextText ->
                                if (currentUserRole.canEdit) {
                                    editContent = nextText
                                    emitLiveDocumentDelta(nextContent = nextText)
                                }
                            },
                            enabled = currentUserRole.canEdit,
                            label = {
                                Text("Google-Docs-Style Live CRDT Editor (${document.fileType.uppercase()} • Auto-Syncs Every Keystroke)")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .testTag("editor_content_input"),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            shape = RoundedCornerShape(14.dp)
                        )

                        CrystalGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp
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
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Encrypt document payload before syncing to cloud",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DarkOnSurfaceVariant
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
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Cache locally in SQLite & FTS4 for instant offline access",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DarkOnSurfaceVariant
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
                            Text("Save & Sync Structured Blocks", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Add PDF Text Box or Sticky Note Dialog
    if (showStickyNoteDialog) {
        val isTextBoxMode = selectedMarkupTool == PdfMarkupTool.TEXT_BOX
        AlertDialog(
            onDismissRequest = { showStickyNoteDialog = false },
            title = {
                Text(
                    text = if (isTextBoxMode) "Add PDF Text Box Annotation" else "Add Live Y.Map Sticky Note",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isTextBoxMode) {
                            "Place a text box directly onto Page ${currentPageIndex + 1}. Saved locally in Room and synced via WorkManager to Firebase."
                        } else {
                            "This sticky note is stored as a Y.Map entry on Page ${currentPageIndex + 1} and synced via WorkManager to Firebase."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = stickyNoteDraftText,
                        onValueChange = { stickyNoteDraftText = it },
                        label = { Text(if (isTextBoxMode) "Text Box Content" else "Sticky Note Text") },
                        placeholder = { Text(if (isTextBoxMode) "Enter annotation text..." else "Enter note text...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sticky_note_text_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (stickyNoteDraftText.isNotBlank()) {
                            val toolToSave = if (selectedMarkupTool == PdfMarkupTool.TEXT_BOX) {
                                PdfMarkupTool.TEXT_BOX
                            } else {
                                PdfMarkupTool.STICKY_NOTE
                            }
                            onUpsertAnnotation(
                                currentPageIndex,
                                toolToSave,
                                selectedMarkupColorHex,
                                12f,
                                listOf(Offset(pendingStickyNormX, pendingStickyNormY)),
                                stickyNoteDraftText.trim()
                            )
                            stickyNoteDraftText = ""
                            showStickyNoteDialog = false
                        }
                    },
                    enabled = stickyNoteDraftText.isNotBlank(),
                    modifier = Modifier.testTag("confirm_sticky_note_button")
                ) {
                    Text(if (isTextBoxMode) "Add Text Box" else "Save Y.Map Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStickyNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Doc-to-Pic Export Studio Dialog (Multi-Page / Slide PNGs & Stitched Single Tall Image)
    if (showDocToPicDialog) {
        AlertDialog(
            onDismissRequest = { showDocToPicDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF0891B2))
                    Text("Doc to Pic Studio (${document.fileType.uppercase()})", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Export '${document.title}' into high-resolution PNG images using Multi-Page/Slide mode or Stitched Single Tall Image mode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DocToPicMode.entries.forEach { mode ->
                            FilterChip(
                                selected = docToPicMode == mode,
                                onClick = {
                                    docToPicMode = mode
                                    previewPicPageIndex = 0
                                    scope.launch {
                                        isConvertingDocToPic = true
                                        val res = DocToPicConverter.convertDocumentToPictures(
                                            context = context,
                                            document = document,
                                            plainContent = decryptedContent,
                                            localUri = localUri,
                                            mode = mode
                                        )
                                        docToPicResult = res.getOrNull()
                                        isConvertingDocToPic = false
                                    }
                                },
                                label = { Text(mode.label) }
                            )
                        }
                    }

                    if (isConvertingDocToPic) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (docToPicResult != null) {
                        val result = docToPicResult!!
                        val activeBitmap = if (docToPicMode == DocToPicMode.STITCHED_SINGLE) {
                            result.stitchedBitmap
                        } else {
                            result.pageBitmaps.getOrElse(previewPicPageIndex) { result.stitchedBitmap }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Image(
                                    bitmap = activeBitmap.asImageBitmap(),
                                    contentDescription = "Rendered Doc-to-Pic Preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Fit
                                )

                                if (docToPicMode == DocToPicMode.MULTI_PAGE && result.pageBitmaps.size > 1) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { if (previewPicPageIndex > 0) previewPicPageIndex -= 1 },
                                            enabled = previewPicPageIndex > 0
                                        ) {
                                            Text("Prev PNG")
                                        }
                                        Text(
                                            text = "Page/Slide ${previewPicPageIndex + 1} of ${result.pageBitmaps.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                        TextButton(
                                            onClick = {
                                                if (previewPicPageIndex < result.pageBitmaps.size - 1) {
                                                    previewPicPageIndex += 1
                                                }
                                            },
                                            enabled = previewPicPageIndex < result.pageBitmaps.size - 1
                                        ) {
                                            Text("Next PNG")
                                        }
                                    }
                                }

                                Text(
                                    text = if (docToPicMode == DocToPicMode.STITCHED_SINGLE) {
                                        "Stitched Single Image (${result.pageBitmaps.size} page/slide(s) combined vertically • ${result.primarySavedFile.length() / 1024} KB)"
                                    } else {
                                        "Multi-Page Export (${result.savedFiles.size} PNG file(s) generated)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val res = docToPicResult
                        if (res != null) {
                            onSaveDocToPicToWorkspace(
                                docToPicMode.label,
                                res.pageBitmaps.size,
                                res.primarySavedFile,
                                res.primarySavedUri
                            )
                        }
                        showDocToPicDialog = false
                    },
                    enabled = docToPicResult != null && !isConvertingDocToPic,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0891B2),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_save_doc_to_pic_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Pic to Workspace")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDocToPicDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showCommentsModal) {
        CommentsAndSuggestionsModal(
            documentTitle = document.title,
            comments = comments,
            currentUserRole = currentUserRole,
            onAddComment = onAddComment,
            onResolveComment = onResolveComment,
            onAcceptSuggestion = { suggestion ->
                onAcceptSuggestion(
                    suggestion,
                    decryptedContent,
                    unlockPassword.ifBlank { null }
                ) { mergedContent ->
                    decryptedContent = mergedContent
                    editContent = mergedContent
                }
            },
            onRejectSuggestion = { suggestion ->
                onRejectSuggestion(suggestion.id)
            },
            onExportCommentAsPdf = { noteOrComment ->
                onExportCommentAsPdf(noteOrComment)
            },
            onExportAllNotesAsPdf = {
                onExportAllNotesAsPdf(comments)
            },
            onDismiss = { showCommentsModal = false }
        )
    }

    if (showVersionHistoryModal) {
        VersionHistoryModal(
            documentTitle = document.title,
            versions = versions,
            canRestore = currentUserRole.canEdit,
            onCreateNamedVersion = { label ->
                onCreateNamedVersion(label, editContent.ifBlank { decryptedContent })
            },
            onRestoreVersion = { ver ->
                onRestoreVersion(ver, unlockPassword.ifBlank { null }) { restoredText ->
                    decryptedContent = restoredText
                    editContent = restoredText
                }
                showVersionHistoryModal = false
            },
            onExportVersionAsPdf = { ver ->
                onExportVersionAsPdf(ver)
            },
            onDismiss = { showVersionHistoryModal = false }
        )
    }

    if (showShareModal) {
        ShareAndRoleModal(
            document = document,
            collaborators = collaborators,
            currentUserRole = currentUserRole,
            shareLinkConfig = shareLinkConfig,
            onInviteCollaborator = onInviteCollaborator,
            onRevokeCollaboratorAndRotateKey = onRevokeCollaboratorAndRotateKey,
            onSwitchCurrentUserRole = { newRole ->
                onSwitchCurrentUserRole(newRole)
                if (!newRole.canEdit) {
                    editorCollabMode = if (newRole.canComment) EditorCollabMode.SUGGESTING else EditorCollabMode.VIEWING
                }
            },
            onDismiss = { showShareModal = false }
        )
    }
}

@Composable
private fun MarkupToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(
                if (selected) activeColor.copy(alpha = 0.22f)
                else Color.Transparent
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) activeColor.copy(alpha = 0.6f) else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) activeColor else Color.White.copy(alpha = 0.72f),
            modifier = Modifier.size(17.dp)
        )
    }
}

private fun parseComposeHexColor(colorHex: String): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(colorHex))
    }.getOrDefault(Color(0xFFFBBF24))
}

/**
 * Format-aware renderer for Structured Text, Slide Headers (`=== SLIDE N ===`),
 * Section Headers (`=== SECTION N ===`), and Excel/Word/PowerPoint Table Rows (`| Col 1 | Col 2 |`).
 */
@Composable
private fun StructuredBlockContentView(
    rawContent: String,
    accentColor: Color,
    zoomScale: Float
) {
    val lines = remember(rawContent) { rawContent.lines() }
    if (rawContent.isBlank()) {
        Text(
            text = "(Empty Document Content)",
            style = MaterialTheme.typography.bodyLarge,
            color = DarkOnSurfaceVariant
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEachIndexed { idx, line ->
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                trimmed.startsWith("===") -> {
                    val headingText = trimmed.trim('=').trim()
                    Surface(
                        color = accentColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Text(
                                text = headingText,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = (13.5f * zoomScale).sp
                                ),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                trimmed.startsWith("|") && trimmed.endsWith("|") -> {
                    val cells = trimmed.trim('|').split("|").map { it.trim() }
                    val isHeaderRow = idx == 0 || !lines.getOrNull(idx - 1).orEmpty().trim().startsWith("|")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isHeaderRow) {
                                    accentColor.copy(alpha = 0.22f)
                                } else {
                                    Color.White.copy(alpha = 0.04f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = accentColor.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        cells.forEach { cellText ->
                            Text(
                                text = cellText,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = (12f * zoomScale).sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                fontWeight = if (isHeaderRow) FontWeight.ExtraBold else FontWeight.Medium,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                else -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (13.5f * zoomScale).sp,
                            lineHeight = (20f * zoomScale).sp
                        ),
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }
        }
    }
}
