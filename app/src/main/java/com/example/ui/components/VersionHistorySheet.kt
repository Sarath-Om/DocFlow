package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DocumentVersionEntity
import com.example.data.model.DocEntity
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated Version History UI bottom sheet component.
 * Displays a list of file snapshots, allowing users to inspect previous states,
 * restore any snapshot non-destructively, or export individual versions as standalone PDFs.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VersionHistorySheet(
    palette: DocFlowPalette,
    document: DocEntity,
    versions: List<DocumentVersionEntity>,
    canRestore: Boolean = true,
    onDismiss: () -> Unit,
    onCreateNamedVersion: (String) -> Unit,
    onRestoreVersion: (DocumentVersionEntity) -> Unit,
    onExportVersionAsPdf: (DocumentVersionEntity) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var newVersionLabelInput by remember { mutableStateOf("") }
    var expandedVersionId by remember { mutableStateOf<String?>(null) }
    var pendingRestoreVersion by remember { mutableStateOf<DocumentVersionEntity?>(null) }
    var exportingVersionId by remember { mutableStateOf<String?>(null) }

    val sortedVersions = remember(versions) {
        versions.sortedByDescending { it.versionNumber }
    }

    val filteredVersions = remember(sortedVersions, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedVersions
        } else {
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            sortedVersions.filter { ver ->
                ver.versionLabel.lowercase(Locale.getDefault()).contains(q) ||
                ver.authorName.lowercase(Locale.getDefault()).contains(q) ||
                ver.snapshotContent.lowercase(Locale.getDefault()).contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .testTag("version_history_sheet")
        ) {
            // Header Bar
            Surface(
                color = palette.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DocFlowTokens.space3, vertical = DocFlowTokens.space2),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                    ) {
                        DocFlowIconCircle(
                            icon = Icons.Default.History,
                            circleColor = DocFlowTokens.AccentBlue
                        )
                        Column {
                            Text(
                                text = "Version History",
                                style = DocFlowTokens.rowTitleStyle,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "${document.title} • ${versions.size} snapshot(s)",
                                style = DocFlowTokens.captionStyle,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("version_history_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Version History",
                            tint = palette.textSecondary
                        )
                    }
                }
            }

            // Create New Named Snapshot Box
            Surface(
                color = palette.surfaceSubtle,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DocFlowTokens.space3, vertical = DocFlowTokens.space2)
                ) {
                    Text(
                        text = "Create Named Snapshot",
                        style = DocFlowTokens.captionBoldStyle,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(DocFlowTokens.space1))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newVersionLabelInput,
                            onValueChange = { newVersionLabelInput = it },
                            placeholder = { Text("e.g. Approved Legal Draft, Final Polish") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("version_history_label_input")
                        )
                        DocFlowPrimaryButton(
                            text = "Save",
                            leadingIcon = Icons.Default.Add,
                            onClick = {
                                val label = newVersionLabelInput.trim().ifBlank { "Manual Snapshot" }
                                onCreateNamedVersion(label)
                                newVersionLabelInput = ""
                                Toast.makeText(context, "Saved version snapshot '$label'", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("version_history_save_button")
                        )
                    }
                }
            }

            // Search Filter
            if (sortedVersions.size > 2) {
                Box(modifier = Modifier.padding(horizontal = DocFlowTokens.space3, vertical = DocFlowTokens.space1)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter snapshots by label, author, or keyword...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = palette.textSecondary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("version_history_search_input")
                    )
                }
            }

            // Versions List
            if (filteredVersions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(DocFlowTokens.space4),
                    contentAlignment = Alignment.Center
                ) {
                    if (versions.isEmpty()) {
                        DocFlowEmptyState(
                            icon = Icons.Default.History,
                            title = "No snapshots recorded yet",
                            subtitle = "Save a named snapshot above, or edit document content to record historical checkpoints.",
                            palette = palette
                        )
                    } else {
                        DocFlowEmptyState(
                            icon = Icons.Default.Search,
                            title = "No matching snapshots",
                            subtitle = "Try adjusting your search query.",
                            palette = palette
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = DocFlowTokens.space3),
                    contentPadding = PaddingValues(top = DocFlowTokens.space2, bottom = DocFlowTokens.space4),
                    verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                ) {
                    itemsIndexed(filteredVersions, key = { _, item -> item.id }) { index, version ->
                        val isLatest = index == 0 && searchQuery.isEmpty()
                        val isExpanded = expandedVersionId == version.id
                        val formattedDate = remember(version.createdAtMillis) {
                            SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(version.createdAtMillis))
                        }

                        DocFlowCard(
                            palette = palette,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .testTag("version_card_${version.versionNumber}")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
                            ) {
                                // Top status row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = if (isLatest) DocFlowTokens.AccentBlue else palette.surfaceSubtle,
                                            shape = RoundedCornerShape(DocFlowTokens.smallRadius)
                                        ) {
                                            Text(
                                                text = "v${version.versionNumber}",
                                                style = DocFlowTokens.captionBoldStyle,
                                                color = if (isLatest) Color.White else palette.textPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (isLatest) {
                                            Surface(
                                                color = DocFlowTokens.AccentBlue.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(DocFlowTokens.smallRadius)
                                            ) {
                                                Text(
                                                    text = "Current Live",
                                                    style = DocFlowTokens.captionBoldStyle,
                                                    color = DocFlowTokens.AccentBlue,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "${(version.byteSize / 1024L).coerceAtLeast(1)} KB",
                                        style = DocFlowTokens.captionStyle.copy(fontFamily = FontFamily.Monospace),
                                        color = palette.textSecondary
                                    )
                                }

                                // Title / Label
                                Text(
                                    text = version.versionLabel,
                                    style = DocFlowTokens.rowTitleStyle,
                                    color = palette.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Metadata (Author & Timestamp)
                                Text(
                                    text = "By ${version.authorName} • $formattedDate",
                                    style = DocFlowTokens.captionStyle,
                                    color = palette.textSecondary
                                )

                                // Snippet / Content Preview
                                Surface(
                                    color = palette.surfaceSubtle,
                                    shape = RoundedCornerShape(DocFlowTokens.thumbnailRadius),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedVersionId = if (isExpanded) null else version.id
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(DocFlowTokens.space2)) {
                                        val previewText = version.snapshotContent.replace(Regex("\\s+"), " ").trim()
                                        Text(
                                            text = if (isExpanded) version.snapshotContent.ifBlank { "(Empty Content Snapshot)" } else previewText.take(160).ifBlank { "(Empty Content Snapshot)" },
                                            style = if (isExpanded) DocFlowTokens.captionStyle.copy(fontFamily = FontFamily.Monospace) else DocFlowTokens.captionStyle,
                                            color = palette.textSecondary,
                                            maxLines = if (isExpanded) 14 else 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (isExpanded) "Collapse content" else "View snapshot preview",
                                                style = DocFlowTokens.captionBoldStyle,
                                                color = DocFlowTokens.AccentBlue
                                            )
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = DocFlowTokens.AccentBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(DocFlowTokens.space1))

                                // Action Buttons: Restore & Export as PDF
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                                ) {
                                    // Export as PDF button
                                    DocFlowSecondaryButton(
                                        text = if (exportingVersionId == version.id) "Exporting..." else "Export PDF",
                                        leadingIcon = Icons.Default.PictureAsPdf,
                                        onClick = {
                                            exportingVersionId = version.id
                                            onExportVersionAsPdf(version)
                                            exportingVersionId = null
                                            Toast.makeText(
                                                context,
                                                "Successfully exported version v${version.versionNumber} ('${version.versionLabel}') as PDF • Stored on this device",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        },
                                        palette = palette,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("export_version_pdf_button_${version.versionNumber}")
                                    )

                                    // Restore Version button
                                    if (canRestore) {
                                        DocFlowPrimaryButton(
                                            text = if (isLatest) "Re-apply v${version.versionNumber}" else "Restore v${version.versionNumber}",
                                            leadingIcon = Icons.Default.Restore,
                                            onClick = {
                                                pendingRestoreVersion = version
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("restore_version_button_${version.versionNumber}")
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

    // Restore Confirmation Dialog
    if (pendingRestoreVersion != null) {
        val verToRestore = pendingRestoreVersion!!
        AlertDialog(
            onDismissRequest = { pendingRestoreVersion = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    tint = DocFlowTokens.AccentBlue,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Restore Version v${verToRestore.versionNumber}?",
                    style = DocFlowTokens.rowTitleStyle
                )
            },
            text = {
                Text(
                    text = "This will restore the document text to '${verToRestore.versionLabel}'. A new snapshot will be saved so your current progress is completely preserved.",
                    style = DocFlowTokens.bodyMediumStyle,
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = verToRestore
                        pendingRestoreVersion = null
                        onRestoreVersion(selected)
                        onDismiss()
                        Toast.makeText(context, "Restored v${selected.versionNumber} to document", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.testTag("confirm_restore_version_dialog_btn")
                ) {
                    Text("Restore Snapshot", fontWeight = FontWeight.Bold, color = DocFlowTokens.AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreVersion = null }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            }
        )
    }
}
