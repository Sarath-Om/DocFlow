package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens

/**
 * Shared "New document" creation dialog conforming strictly to Section 2A:
 * - Title: "New document", subtitle: "Stored on this device"
 * - Solid surface (#FFFFFF / #1B1D20), 28dp radius, 40% black scrim
 * - Three equal-width format chips (40dp high, icon + short label, no clipping)
 * - Title field: 56dp high, 16dp radius, visible 1dp border (#C9CBD0), 2dp blue on focus, label "Document title",
 *   validates illegal characters, max length, and duplicates; blank title = "Untitled"
 * - "Initial content (optional)" on its own line; below it, scrollable text area (min 120dp) with
 *   a 48dp AI Mic button inside its bottom-right corner
 * - Empty body creates an empty document (0 words); never prefills body with the title
 */
@Composable
fun DocFlowNewDocumentDialog(
    palette: DocFlowPalette,
    existingTitles: List<String>,
    initialFormat: String = "docx",
    onDismiss: () -> Unit,
    onOpenAiMic: (onDictated: (String) -> Unit) -> Unit,
    onCreateDocument: (title: String, format: String, initialContent: String) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(initialFormat) }
    var titleInput by remember { mutableStateOf("") }
    var contentInput by remember { mutableStateOf("") }
    var lastDictatedChunk by remember { mutableStateOf<String?>(null) }

    val illegalCharRegex = remember { Regex("[/\\\\:*?\"<>|]") }
    val hasIllegalChars = illegalCharRegex.containsMatchIn(titleInput)
    val isTooLong = titleInput.length > 80

    val inlineError: String? = when {
        hasIllegalChars -> "Title cannot contain / \\ : * ? \" < > |"
        isTooLong -> "Title must be 80 characters or fewer"
        else -> null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.divider, RoundedCornerShape(28.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .testTag("new_document_dialog")
            ) {
                Text(
                    text = "New document",
                    style = DocFlowTokens.screenTitleStyle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Stored on this device",
                    style = DocFlowTokens.captionStyle,
                    fontSize = 13.sp,
                    color = palette.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Format selector: 3 equal-width chips (40dp high, icon + short label, no clipping)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val formats: List<Triple<String, String, ImageVector>> = listOf(
                        Triple("docx", "Word", Icons.Filled.Description),
                        Triple("pptx", "PPT", Icons.Filled.Slideshow),
                        Triple("xlsx", "Excel", Icons.Filled.TableChart)
                    )
                    formats.forEach { (ext, shortLabel, icon) ->
                        val isSelected = selectedFormat == ext
                        val bg = if (isSelected) DocFlowTokens.SelectedRowTintLight else palette.background
                        val borderCol = if (isSelected) DocFlowTokens.AccentBlue else palette.divider
                        val textCol = if (isSelected) DocFlowTokens.AccentBlue else palette.textPrimary

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = borderCol,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedFormat = ext }
                                .padding(horizontal = 8.dp)
                                .testTag("new_doc_format_chip_$ext"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = textCol,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = shortLabel,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = textCol,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title field: 56dp high, 16dp radius, visible 1dp border (#C9CBD0), 2dp blue on focus
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Document title", fontSize = 13.sp) },
                    placeholder = { Text("Untitled", fontSize = 14.sp, color = palette.textSecondary) },
                    singleLine = true,
                    isError = inlineError != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DocFlowTokens.AccentBlue,
                        unfocusedBorderColor = palette.inputBorder,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary,
                        focusedContainerColor = palette.surface,
                        unfocusedContainerColor = palette.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("new_document_title_input")
                )

                if (inlineError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = inlineError,
                        fontSize = 12.sp,
                        color = DocFlowTokens.IconCircleRed,
                        modifier = Modifier.testTag("new_document_title_error")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // "Initial content (optional)": label on its own line, never wrapping next to a control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Initial content (optional)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (lastDictatedChunk != null) {
                        TextButton(
                            onClick = {
                                val chunk = lastDictatedChunk ?: ""
                                if (chunk.isNotEmpty() && contentInput.endsWith(chunk)) {
                                    contentInput = contentInput.removeSuffix(chunk).trimEnd()
                                }
                                lastDictatedChunk = null
                            }
                        ) {
                            Text("Undo dictation", fontSize = 12.sp, color = DocFlowTokens.AccentBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Text area (min 120dp, scrolls) with AI Mic button inside bottom-right corner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 128.dp)
                ) {
                    OutlinedTextField(
                        value = contentInput,
                        onValueChange = { contentInput = it },
                        placeholder = {
                            Text(
                                text = "Leave blank to start with an empty document...",
                                fontSize = 14.sp,
                                color = palette.textSecondary
                            )
                        },
                        minLines = 4,
                        maxLines = 7,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DocFlowTokens.AccentBlue,
                            unfocusedBorderColor = palette.inputBorder,
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary,
                            focusedContainerColor = palette.surface,
                            unfocusedContainerColor = palette.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 128.dp)
                            .testTag("new_document_body_input")
                    )

                    IconButton(
                        onClick = {
                            onOpenAiMic { dictated ->
                                val clean = dictated.trim()
                                if (clean.isNotEmpty()) {
                                    val sep = if (contentInput.isBlank()) "" else " "
                                    val appended = "$sep$clean"
                                    contentInput += appended
                                    lastDictatedChunk = appended
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DocFlowTokens.SelectedRowTintLight)
                            .testTag("new_document_ai_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "AI Mic voice dictation",
                            tint = DocFlowTokens.AccentBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Footer: "Cancel" (text button) and filled blue pill "Create" (48dp high, single-line label)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("new_document_cancel_button")
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textSecondary,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    DocFlowPrimaryButton(
                        text = "Create",
                        enabled = inlineError == null,
                        onClick = {
                            val baseTitle = titleInput.trim().ifBlank { "Untitled" }
                            val uniqueTitle = resolveUniqueDocumentTitle(baseTitle, existingTitles)
                            // Empty body MUST create an empty document (0 words); never prefill with title
                            onCreateDocument(uniqueTitle, selectedFormat, contentInput)
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("new_document_confirm_button")
                    )
                }
            }
        }
    }
}

fun resolveUniqueDocumentTitle(baseTitle: String, existingTitles: List<String>): String {
    val lowerSet = existingTitles.map { it.trim().lowercase() }.toSet()
    if (!lowerSet.contains(baseTitle.lowercase())) return baseTitle
    var counter = 2
    while (lowerSet.contains("$baseTitle ($counter)".lowercase())) {
        counter++
    }
    return "$baseTitle ($counter)"
}
