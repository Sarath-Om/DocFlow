package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.MlKitOcrScanResult
import com.example.data.util.MlKitTextBlock
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens
import kotlinx.coroutines.delay

/**
 * Live Bounding Box Overlay rendered directly on top of the image-based PDF page in the document viewer.
 * Highlights detected ML Kit text blocks, enabling direct tap-to-copy functionality on any page area.
 */
@Composable
fun MlKitOcrPageBoundingBoxesOverlay(
    detailedBlocks: List<MlKitTextBlock>,
    modifier: Modifier = Modifier,
    onBlockCopied: (String) -> Unit
) {
    val context = LocalContext.current
    var lastCopiedSnippet by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lastCopiedSnippet) {
        if (lastCopiedSnippet != null) {
            delay(2200)
            lastCopiedSnippet = null
        }
    }

    Box(modifier = modifier) {
        // Draw interactive clickable boxes for each text block
        detailedBlocks.forEach { block ->
            val box = block.normalizedBoundingBox
            if (box != null) {
                val boxWidthFraction = (box.right - box.left).coerceIn(0.01f, 1f)
                val boxHeightFraction = (box.bottom - box.top).coerceIn(0.01f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = (box.left * 100).coerceAtLeast(0f).dp, // conceptual positioning if absolute
                            top = (box.top * 100).coerceAtLeast(0f).dp
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(boxWidthFraction)
                            .height(28.dp) // interactive touch slab
                            .clip(RoundedCornerShape(4.dp))
                            .background(DocFlowTokens.AccentBlue.copy(alpha = 0.15f))
                            .border(1.dp, DocFlowTokens.AccentBlue.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Extracted Text", block.text))
                                Toast.makeText(context, "Copied text block", Toast.LENGTH_SHORT).show()
                                lastCopiedSnippet = block.text.take(30)
                                onBlockCopied(block.text)
                            }
                    )
                }
            }
        }

        // Floating Toast when a block is tapped
        AnimatedVisibility(
            visible = lastCopiedSnippet != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            Surface(
                color = Color(0xFF1F2937),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Copied: \"${lastCopiedSnippet}\"...",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Comprehensive ML Kit Text Recognition Bottom Sheet & Overlay.
 * Allows users to view extracted text, copy all text with 1-tap,
 * search inside extracted content, add to document notes, or copy individual paragraphs.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MlKitExtractedTextOverlaySheet(
    palette: DocFlowPalette,
    scanResult: MlKitOcrScanResult,
    currentPageIndex: Int = 0,
    onDismiss: () -> Unit,
    onAddToNotes: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var copiedConfirmation by remember { mutableStateOf(false) }

    val currentPageBlock = scanResult.pages.find { it.pageNumber == currentPageIndex + 1 }
        ?: scanResult.pages.firstOrNull()

    val rawText = currentPageBlock?.rawText?.ifBlank { scanResult.fullExtractedText }.orEmpty()
    val allParagraphs = remember(rawText) {
        rawText.split("\n\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    val filteredParagraphs = remember(allParagraphs, searchQuery) {
        if (searchQuery.isBlank()) allParagraphs
        else allParagraphs.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    val wordCount = remember(rawText) {
        rawText.split(Regex("\\s+")).count { it.isNotBlank() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        shape = DocFlowTokens.sheetShape,
        modifier = Modifier.testTag("ml_kit_ocr_overlay_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DocFlowTokens.space4)
                .padding(bottom = DocFlowTokens.space5)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
                ) {
                    DocFlowIconCircle(
                        icon = Icons.Default.DocumentScanner,
                        circleColor = DocFlowTokens.AccentBlue,
                        size = 40.dp
                    )
                    Column {
                        Text(
                            text = "Extracted Text (Google ML Kit)",
                            style = DocFlowTokens.sectionLabelStyle,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = DocFlowTokens.AccentBlue,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "100% On-Device • Page ${currentPageIndex + 1}",
                                style = DocFlowTokens.captionStyle,
                                color = DocFlowTokens.AccentBlue
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(DocFlowTokens.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = palette.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(DocFlowTokens.space2))
            DocFlowDivider(palette = palette)
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))

            // Metadata Chips Row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2),
                verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
            ) {
                val detected = scanResult.languageDetection
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text("${detected.detectedLanguage.displayName} (${detected.scriptFamily})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DocFlowTokens.AccentBlue.copy(alpha = 0.12f),
                        selectedLabelColor = DocFlowTokens.AccentBlue
                    )
                )

                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text("$wordCount words • ${rawText.length} chars", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = palette.surfaceSubtle,
                        labelColor = palette.textSecondary
                    )
                )

                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text("${currentPageBlock?.blockCount ?: 0} blocks", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = palette.surfaceSubtle,
                        labelColor = palette.textSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            // Primary Actions: Copy All & Add to Notes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
            ) {
                DocFlowPrimaryButton(
                    text = if (copiedConfirmation) "Copied to Clipboard!" else "Copy All Text",
                    leadingIcon = if (copiedConfirmation) Icons.Default.Check else Icons.Default.ContentCopy,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ML Kit Extracted Text", rawText))
                        Toast.makeText(context, "All extracted text copied to clipboard", Toast.LENGTH_SHORT).show()
                        copiedConfirmation = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ocr_copy_all_button")
                )

                DocFlowSecondaryButton(
                    text = "Add to Notes",
                    leadingIcon = Icons.Default.Notes,
                    onClick = {
                        onAddToNotes(rawText)
                        Toast.makeText(context, "Added to document notes", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ocr_add_to_notes_button")
                )
            }

            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            // Search Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter extracted text...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DocFlowTokens.AccentBlue,
                    unfocusedBorderColor = palette.divider,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary
                ),
                shape = DocFlowTokens.rectButtonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("ocr_search_input")
            )

            Spacer(modifier = Modifier.height(DocFlowTokens.space2))

            // Scrollable List of Extracted Paragraphs
            if (filteredParagraphs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DocFlowEmptyState(
                        icon = Icons.Default.DocumentScanner,
                        title = if (rawText.isBlank()) "No text detected on this page" else "No matching lines",
                        subtitle = if (rawText.isBlank()) "This page appears to have no machine-readable or handwritten text." else "Try a different search term.",
                        palette = palette
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                ) {
                    itemsIndexed(filteredParagraphs) { index, paragraph ->
                        DocFlowCard(
                            palette = palette,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ocr_paragraph_card_$index")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(DocFlowTokens.space3),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Paragraph ${index + 1}",
                                        style = DocFlowTokens.captionBoldStyle,
                                        color = DocFlowTokens.AccentBlue
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = paragraph,
                                        style = DocFlowTokens.bodyStyle,
                                        color = palette.textPrimary,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Paragraph ${index + 1}", paragraph))
                                        Toast.makeText(context, "Copied paragraph ${index + 1}", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(DocFlowTokens.minTouchTarget)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Paragraph",
                                        tint = DocFlowTokens.AccentBlue,
                                        modifier = Modifier.size(18.dp)
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
