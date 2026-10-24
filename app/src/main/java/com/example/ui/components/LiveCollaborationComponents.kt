package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CachedDocumentEntity
import com.example.data.local.DocumentCommentEntity
import com.example.data.local.DocumentVersionEntity
import com.example.data.model.ActiveCollaborator
import com.example.data.model.CollaboratorRole
import com.example.data.model.DocEntity
import com.example.data.model.EditorCollabMode
import com.example.data.model.LiveSyncStatus
import com.example.data.model.ShareLinkConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Expandable "Recent Files" horizontal scroll drawer positioned above the folder bar,
 * prioritized by Room `lastAccessedAtMillis` timestamps.
 */
@Composable
fun RecentFilesExpandableDrawer(
    recentCachedDocuments: List<CachedDocumentEntity>,
    isOfflineMode: Boolean,
    onOpenRecentDocument: (CachedDocumentEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    CrystalGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_files_drawer_card"),
        cornerRadius = 20.dp,
        tintColor = Color(0xFF0066FF)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .testTag("recent_files_drawer_header"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0066FF).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Recent Files",
                            tint = Color(0xFF0066FF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Recent Files",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = Color(0xFF0066FF).copy(alpha = 0.14f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "${recentCachedDocuments.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0066FF),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Prioritized by Room DB last-access timestamp",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("toggle_recent_files_drawer_button")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse Recent Files" else "Expand Recent Files"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                if (recentCachedDocuments.isEmpty()) {
                    Text(
                        text = "Open, scan, or create a document to populate your Room last-access carousel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("recent_files_horizontal_scroll"),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recentCachedDocuments.forEach { item ->
                            val formatColor = when (item.fileType.lowercase()) {
                                "pdf", "scanned" -> Color(0xFFE11D48)
                                "docx", "doc" -> Color(0xFF0066FF)
                                "xlsx", "xls", "csv" -> Color(0xFF059669)
                                "pptx", "ppt" -> Color(0xFFD97706)
                                else -> Color(0xFF5E17EB)
                            }
                            CrystalGlassCard(
                                modifier = Modifier
                                    .width(205.dp)
                                    .testTag("recent_file_card_${item.id}"),
                                cornerRadius = 16.dp,
                                tintColor = formatColor,
                                onClick = { onOpenRecentDocument(item) }
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
                                        Surface(
                                            color = formatColor.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = when (item.fileType.lowercase()) {
                                                        "pdf", "scanned" -> Icons.Default.PictureAsPdf
                                                        "docx", "doc" -> Icons.Default.Description
                                                        "xlsx", "xls", "csv" -> Icons.Default.TableChart
                                                        "pptx", "ppt" -> Icons.Default.Slideshow
                                                        else -> Icons.Default.Image
                                                    },
                                                    contentDescription = null,
                                                    tint = formatColor,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = item.fileType.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = formatColor
                                                )
                                            }
                                        }

                                        SyncStatusVisualIndicator(
                                            syncStateName = item.syncState,
                                            isOfflineMode = isOfflineMode,
                                            isOfflineCached = item.isOfflineCached
                                        )
                                    }

                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatRelativeAccessTime(item.lastAccessedAtMillis),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (item.isEncrypted) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "AES-256 Encrypted",
                                                tint = Color(0xFF5E17EB),
                                                modifier = Modifier.size(13.dp)
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

/**
 * Google-Docs-Style Live Collaboration Header Bar for DocumentViewerEditorScreen.
 * Shows live CRDT sync status (Saved / Saving... / Offline), active collaborator avatar stack,
 * active user role, Editing/Suggesting/Viewing mode selector, and triggers for Comments, Version History, and Sharing.
 */
@Composable
fun LiveCollaborationHeaderBar(
    syncStatus: LiveSyncStatus,
    collaborators: List<ActiveCollaborator>,
    currentUserRole: CollaboratorRole,
    editorCollabMode: EditorCollabMode,
    openCommentsCount: Int,
    versionCount: Int,
    isEncrypted: Boolean,
    onSelectCollabMode: (EditorCollabMode) -> Unit,
    onOpenCommentsSheet: () -> Unit,
    onOpenVersionHistory: () -> Unit,
    onOpenShareDialog: () -> Unit
) {
    val statusColor = when (syncStatus) {
        LiveSyncStatus.SAVED -> Color(0xFF059669)
        LiveSyncStatus.SAVING -> Color(0xFF0066FF)
        LiveSyncStatus.OFFLINE_QUEUED -> Color(0xFFD97706)
    }

    CrystalGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_collaboration_header_bar"),
        cornerRadius = 18.dp,
        tintColor = statusColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Live Sync Status Badge + Active Collaborator Avatars + Share Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.testTag("live_sync_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = when (syncStatus) {
                                LiveSyncStatus.SAVED -> Icons.Default.CloudDone
                                LiveSyncStatus.SAVING -> Icons.Default.CloudSync
                                LiveSyncStatus.OFFLINE_QUEUED -> Icons.Default.CloudOff
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = syncStatus.badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor
                        )
                    }
                }

                // Active Collaborator Avatar Stack + Share Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                        collaborators.take(4).forEach { collab ->
                            val ringColor = runCatching {
                                Color(android.graphics.Color.parseColor(collab.colorHex))
                            }.getOrDefault(Color(0xFF0066FF))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(ringColor)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = collab.name.take(2).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onOpenShareDialog,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0066FF),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("open_share_dialog_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share (${currentUserRole.label})", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Second Row: Editing / Suggesting / Viewing Mode + Comments + Version History
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorCollabMode.entries.forEach { mode ->
                    val enabledForRole = when (mode) {
                        EditorCollabMode.EDITING -> currentUserRole.canEdit
                        EditorCollabMode.SUGGESTING -> currentUserRole.canComment
                        EditorCollabMode.VIEWING -> true
                    }
                    FilterChip(
                        selected = editorCollabMode == mode,
                        onClick = { if (enabledForRole) onSelectCollabMode(mode) },
                        enabled = enabledForRole,
                        label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                imageVector = when (mode) {
                                    EditorCollabMode.EDITING -> Icons.Default.Edit
                                    EditorCollabMode.SUGGESTING -> Icons.Default.RateReview
                                    EditorCollabMode.VIEWING -> Icons.Default.Visibility
                                },
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        modifier = Modifier.testTag("collab_mode_${mode.name.lowercase()}")
                    )
                }

                OutlinedButton(
                    onClick = onOpenCommentsSheet,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("open_comments_sheet_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (openCommentsCount > 0) {
                                Badge { Text("$openCommentsCount") }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Comment, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Comments ($openCommentsCount)", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onOpenVersionHistory,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("open_version_history_button")
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("History (v$versionCount)", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Live Collaborator Cursors & E2EE Key Indicator Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF5E17EB).copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EnhancedEncryption,
                            contentDescription = null,
                            tint = Color(0xFF5E17EB),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isEncrypted) "E2EE AES-256-GCM Key Wrapped" else "Live CRDT Channel Active",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5E17EB)
                        )
                    }
                }

                collaborators.forEach { collab ->
                    val cursorColor = runCatching {
                        Color(android.graphics.Color.parseColor(collab.colorHex))
                    }.getOrDefault(Color(0xFF0066FF))
                    Surface(
                        color = cursorColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(cursorColor)
                            )
                            Text(
                                text = "${collab.name} (${collab.role.label}): ${collab.activeCursorLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = cursorColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog for Inline Threaded Comments, @mentions, and Tracked Suggestions (Accept / Reject).
 */
@Composable
fun CommentsAndSuggestionsModal(
    documentTitle: String,
    comments: List<DocumentCommentEntity>,
    currentUserRole: CollaboratorRole,
    onAddComment: (anchorText: String, body: String, isSuggestion: Boolean, suggestedReplacement: String) -> Unit,
    onResolveComment: (String) -> Unit,
    onAcceptSuggestion: (DocumentCommentEntity) -> Unit,
    onRejectSuggestion: (DocumentCommentEntity) -> Unit,
    onExportCommentAsPdf: (DocumentCommentEntity) -> Unit = {},
    onExportAllNotesAsPdf: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var anchorInput by remember { mutableStateOf("Section 1 / Clause") }
    var commentBodyInput by remember { mutableStateOf("") }
    var isSuggestionToggle by remember { mutableStateOf(false) }
    var replacementInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Comment, contentDescription = null, tint = Color(0xFF0066FF))
                Text("Live Comments & Suggestions", fontWeight = FontWeight.Bold)
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
                    text = "Anchor comments to text blocks with @mentions, propose tracked suggestions, or export individual notes as standalone PDF files.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (comments.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onExportAllNotesAsPdf,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_all_notes_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export All Notes (${comments.size}) as PDF",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (currentUserRole.canComment) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = !isSuggestionToggle,
                                    onClick = { isSuggestionToggle = false },
                                    label = { Text("Comment / @Mention") }
                                )
                                FilterChip(
                                    selected = isSuggestionToggle,
                                    onClick = { isSuggestionToggle = true },
                                    label = { Text("Tracked Suggestion") }
                                )
                            }

                            OutlinedTextField(
                                value = anchorInput,
                                onValueChange = { anchorInput = it },
                                label = { Text("Selected Text / Block Anchor") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("comment_anchor_input")
                            )

                            OutlinedTextField(
                                value = commentBodyInput,
                                onValueChange = { commentBodyInput = it },
                                label = {
                                    Text(
                                        if (isSuggestionToggle) {
                                            "Reason for suggestion (e.g. @alex tighten SLA terms)"
                                        } else {
                                            "Comment or @mention collaborator..."
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("comment_body_input")
                            )

                            if (isSuggestionToggle) {
                                OutlinedTextField(
                                    value = replacementInput,
                                    onValueChange = { replacementInput = it },
                                    label = { Text("Proposed Replacement Text / Block") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("suggestion_replacement_input")
                                )
                            }

                            Button(
                                onClick = {
                                    if (commentBodyInput.isNotBlank()) {
                                        onAddComment(
                                            anchorInput.ifBlank { "Document Body" },
                                            commentBodyInput.trim(),
                                            isSuggestionToggle,
                                            replacementInput.trim()
                                        )
                                        commentBodyInput = ""
                                        replacementInput = ""
                                    }
                                },
                                enabled = commentBodyInput.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_comment_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                            ) {
                                Icon(Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isSuggestionToggle) "Post Tracked Suggestion" else "Post Live Comment")
                            }
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Viewer role active: Commenting and suggesting are disabled by zero-trust role rules.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                HorizontalDivider()

                if (comments.isEmpty()) {
                    Text(
                        text = "No comments or tracked suggestions yet on '$documentTitle'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    comments.forEach { item ->
                        val isResolved = item.status != "OPEN"
                        val badgeColor = when {
                            item.status == "ACCEPTED" -> Color(0xFF059669)
                            item.status == "REJECTED" -> Color(0xFFE11D48)
                            item.isSuggestion -> Color(0xFFD97706)
                            else -> Color(0xFF0066FF)
                        }
                        Surface(
                            color = badgeColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
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
                                        text = "${item.authorName} (${item.authorRole})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.18f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = if (item.isSuggestion) "SUGGESTION • ${item.status}" else item.status,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Anchor: \"${item.anchorLineOrText}\"",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = item.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    textDecoration = if (isResolved) TextDecoration.LineThrough else TextDecoration.None
                                )

                                if (item.isSuggestion && item.suggestedReplacement.isNotBlank()) {
                                    Surface(
                                        color = Color(0xFF059669).copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "+ Proposed: \"${item.suggestedReplacement}\"",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onExportCommentAsPdf(item) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("export_note_pdf_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Export Note PDF", style = MaterialTheme.typography.labelSmall)
                                    }

                                    if (item.status == "OPEN" && currentUserRole.canEdit) {
                                        Row(
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (item.isSuggestion) {
                                                TextButton(
                                                    onClick = { onAcceptSuggestion(item) },
                                                    modifier = Modifier.testTag("accept_suggestion_${item.id}")
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Accept & Merge", color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                                                }
                                                TextButton(
                                                    onClick = { onRejectSuggestion(item) },
                                                    modifier = Modifier.testTag("reject_suggestion_${item.id}")
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Reject", color = Color(0xFFE11D48))
                                                }
                                            } else {
                                                TextButton(
                                                    onClick = { onResolveComment(item.id) },
                                                    modifier = Modifier.testTag("resolve_comment_${item.id}")
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Resolve")
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

/**
 * Dialog for viewing CRDT Version History Snapshots, saving named versions, exporting individual versions as PDF,
 * and non-destructively restoring any snapshot.
 */
@Composable
fun VersionHistoryModal(
    documentTitle: String,
    versions: List<DocumentVersionEntity>,
    canRestore: Boolean,
    onCreateNamedVersion: (String) -> Unit,
    onRestoreVersion: (DocumentVersionEntity) -> Unit,
    onExportVersionAsPdf: (DocumentVersionEntity) -> Unit = {},
    onDismiss: () -> Unit
) {
    var versionNameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF0066FF))
                Text("Version History & Snapshots", fontWeight = FontWeight.Bold)
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
                    text = "Export any individual version snapshot as a standalone PDF file, or restore a snapshot to create a new version checkpoint.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (canRestore) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = versionNameInput,
                            onValueChange = { versionNameInput = it },
                            placeholder = { Text("Named version (e.g. Approved Legal Draft)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("named_version_input")
                        )
                        Button(
                            onClick = {
                                val label = versionNameInput.trim().ifBlank { "Manual Checkpoint" }
                                onCreateNamedVersion(label)
                                versionNameInput = ""
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_named_version_button")
                        ) {
                            Text("Save")
                        }
                    }
                }

                HorizontalDivider()

                if (versions.isEmpty()) {
                    Text(
                        text = "Initial live version active for '$documentTitle'. Edit text or save a named checkpoint above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    versions.forEachIndexed { idx, ver ->
                        val isLatest = idx == 0
                        Surface(
                            color = if (isLatest) {
                                Color(0xFF0066FF).copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "v${ver.versionNumber} • ${ver.versionLabel}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLatest) Color(0xFF0066FF) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${ver.byteSize / 1024} KB",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "Author: ${ver.authorName} • Vector: ${ver.stateVector} • ${formatRelativeAccessTime(ver.createdAtMillis)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = ver.snapshotContent.replace(Regex("\\s+"), " ").trim().take(120),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onExportVersionAsPdf(ver) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("export_version_pdf_${ver.versionNumber}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Export Version PDF", style = MaterialTheme.typography.labelSmall)
                                    }

                                    if (canRestore && !isLatest) {
                                        OutlinedButton(
                                            onClick = { onRestoreVersion(ver) },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("restore_version_${ver.versionNumber}")
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore as New Version", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Dialog for E2E Encrypted Collaborator Sharing, Role Enforcement (Owner / Editor / Commenter / Viewer),
 * Key Rotation on Revoke, and URL Fragment Key Sharing (#key=...).
 */
@Composable
fun ShareAndRoleModal(
    document: DocEntity,
    collaborators: List<ActiveCollaborator>,
    currentUserRole: CollaboratorRole,
    shareLinkConfig: ShareLinkConfig,
    onInviteCollaborator: (email: String, role: CollaboratorRole) -> Unit,
    onRevokeCollaboratorAndRotateKey: (String) -> Unit,
    onSwitchCurrentUserRole: (CollaboratorRole) -> Unit,
    onDismiss: () -> Unit
) {
    var inviteEmail by remember { mutableStateOf("") }
    var inviteRole by remember { mutableStateOf(CollaboratorRole.EDITOR) }
    var copiedBanner by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF0066FF))
                Text("Share & E2EE Access Roles", fontWeight = FontWeight.Bold)
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
                    text = "Document AES-256-GCM key is wrapped with each collaborator's X25519 public key. Revoking access automatically rotates the document key.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Role Simulation / Enforcement Switcher so user can test Viewer read-only enforcement
                Surface(
                    color = Color(0xFF5E17EB).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Active Access Role Enforcement:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5E17EB)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CollaboratorRole.entries.forEach { role ->
                                FilterChip(
                                    selected = currentUserRole == role,
                                    onClick = { onSwitchCurrentUserRole(role) },
                                    label = { Text(role.label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("switch_role_${role.name.lowercase()}")
                                )
                            }
                        }
                    }
                }

                // Invite Collaborator by Email + Role
                OutlinedTextField(
                    value = inviteEmail,
                    onValueChange = { inviteEmail = it },
                    label = { Text("Invite Collaborator by Email") },
                    placeholder = { Text("colleague@company.com") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invite_collaborator_email_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CollaboratorRole.entries.filter { it != CollaboratorRole.OWNER }.forEach { role ->
                        FilterChip(
                            selected = inviteRole == role,
                            onClick = { inviteRole = role },
                            label = { Text(role.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    Button(
                        onClick = {
                            if (inviteEmail.isNotBlank()) {
                                onInviteCollaborator(inviteEmail.trim(), inviteRole)
                                inviteEmail = ""
                            }
                        },
                        enabled = inviteEmail.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("invite_collaborator_submit_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Invite")
                    }
                }

                HorizontalDivider()

                // Active Members List with Revoke & Key Rotation
                Text(
                    text = "Document Members (${collaborators.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                collaborators.forEach { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${member.name} (${member.role.label})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${member.email} • ${member.wrappedKeyFingerprint}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (member.role != CollaboratorRole.OWNER) {
                            TextButton(
                                onClick = { onRevokeCollaboratorAndRotateKey(member.id) }
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Revoke & Rotate Key", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Zero-Knowledge Fragment Link Sharing (#key=...)
                Surface(
                    color = Color(0xFF059669).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Zero-Knowledge Share Link (URL Fragment Key):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                        Text(
                            text = "https://docflow.app/d/${document.id}?role=${shareLinkConfig.role.name.lowercase()}&exp=${shareLinkConfig.expiresInHours}h${shareLinkConfig.fragmentKeyPreview}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        OutlinedButton(
                            onClick = {
                                copiedBanner = "Copied E2EE Share Link with URL fragment #key (Key never touches server)"
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy E2EE Fragment Link", style = MaterialTheme.typography.labelSmall)
                        }
                        if (copiedBanner != null) {
                            Text(
                                text = "✓ $copiedBanner",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

fun formatRelativeAccessTime(timestampMillis: Long): String {
    val diffSec = ((System.currentTimeMillis() - timestampMillis) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60L -> "Accessed just now"
        diffSec < 3600L -> "Accessed ${diffSec / 60L}m ago"
        diffSec < 86400L -> "Accessed ${diffSec / 3600L}h ago"
        else -> SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(timestampMillis))
    }
}
