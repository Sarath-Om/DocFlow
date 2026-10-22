package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DocEntity
import com.example.data.model.StorageDashboardSummary
import com.example.ui.components.DocFlowCard
import com.example.ui.components.DocFlowEmptyState
import com.example.ui.components.DocFlowFileFormatBadge
import com.example.ui.components.DocFlowIconCircle
import com.example.ui.components.DocFlowListRow
import com.example.ui.components.DocFlowPrimaryButton
import com.example.ui.components.DocFlowProgressBar
import com.example.ui.components.DocFlowScreenHeader
import com.example.ui.components.DocFlowSecondaryButton
import com.example.ui.components.DocFlowSectionLabel
import com.example.ui.theme.DocFlowTokens
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun StorageDashboardScreen(
    storageSummary: StorageDashboardSummary,
    allDocuments: List<DocEntity>,
    activeFormatFilter: String,
    onBackToWorkspace: () -> Unit,
    onOpenBatchStudio: () -> Unit,
    onFilterWorkspaceByFormat: (String) -> Unit,
    onOpenDocument: (DocEntity) -> Unit,
    onCompactSnapshotsAndCache: () -> Unit,
    onSeedSampleWorkspace: () -> Unit
) {
    BackHandler { onBackToWorkspace() }

    val configuration = LocalConfiguration.current
    val horizontalMargin = DocFlowTokens.adaptiveScreenMargin(configuration.screenWidthDp)

    var selectedSliceKey by remember {
        mutableStateOf(
            if (activeFormatFilter.equals("All", ignoreCase = true)) {
                storageSummary.slices.firstOrNull { it.bytesUsed > 0L }?.formatKey ?: "pdf"
            } else {
                activeFormatFilter.lowercase()
            }
        )
    }

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }
    val sweepProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = DocFlowTokens.MOTION_DURATION_MS, easing = FastOutSlowInEasing),
        label = "donut_sweep"
    )

    val selectedSlice = remember(storageSummary.slices, selectedSliceKey) {
        storageSummary.slices.find { it.formatKey.equals(selectedSliceKey, ignoreCase = true) }
            ?: storageSummary.slices.firstOrNull()
    }

    val matchingDocsForSlice = remember(allDocuments, selectedSliceKey) {
        allDocuments.filter { doc ->
            val ft = doc.fileType.lowercase()
            when (selectedSliceKey.lowercase()) {
                "pdf" -> ft == "pdf" || ft == "scanned"
                "docx" -> ft == "docx" || ft == "doc"
                "pptx" -> ft == "pptx" || ft == "ppt"
                "xlsx" -> ft == "xlsx" || ft == "xls" || ft == "csv"
                "pic" -> ft == "pic" || ft == "png" || ft == "jpg"
                else -> true
            }
        }.sortedByDescending { it.fileSize }
    }

    Scaffold(
        containerColor = DocFlowTokens.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DocFlowTokens.background)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = DocFlowTokens.maxContentWidth),
                contentPadding = PaddingValues(bottom = DocFlowTokens.space8),
                verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
            ) {
                item {
                    DocFlowScreenHeader(
                        title = "Storage Stats",
                        subtitle = "${formatStorageBytes(storageSummary.totalBytesUsed)} used across ${allDocuments.size} local files",
                        onNavigateBack = onBackToWorkspace,
                        backTestTag = "storage_back_button"
                    )
                }

                // Interactive Donut Chart Card
                item {
                    DocFlowCard(
                        modifier = Modifier
                            .padding(horizontal = horizontalMargin)
                            .testTag("storage_donut_chart_card")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                                ) {
                                    DocFlowIconCircle(
                                        icon = Icons.Default.PieChart,
                                        circleColor = DocFlowTokens.accentBlue,
                                        contentDescription = null,
                                        size = 36.dp,
                                        iconSize = 18.dp
                                    )
                                    Text(
                                        text = "Format Storage Breakdown",
                                        style = DocFlowTokens.rowTitleStyle.copy(fontWeight = FontWeight.Bold),
                                        color = DocFlowTokens.textPrimary
                                    )
                                }
                                Surface(
                                    color = DocFlowTokens.accentBlue.copy(alpha = 0.12f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${allDocuments.size} Files",
                                        style = DocFlowTokens.microBadgeStyle,
                                        color = DocFlowTokens.accentBlue,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (allDocuments.isEmpty()) {
                                DocFlowEmptyState(
                                    icon = Icons.Default.Description,
                                    title = "No local documents indexed yet",
                                    subtitle = "Import or scan documents to inspect storage usage by file format.",
                                    actionLabel = "Back to Workspace",
                                    onActionClick = onBackToWorkspace
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(210.dp)
                                        .testTag("interactive_donut_canvas"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val slices = storageSummary.slices
                                    val trackColor = DocFlowTokens.divider
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp)
                                            .pointerInput(slices) {
                                                detectTapGestures { tapOffset ->
                                                    val center = Offset(size.width / 2f, size.height / 2f)
                                                    val dx = tapOffset.x - center.x
                                                    val dy = tapOffset.y - center.y
                                                    val dist = hypot(dx, dy)
                                                    val radius = minOf(size.width, size.height).toFloat() / 2f
                                                    if (dist in (radius * 0.42f)..(radius * 1.15f)) {
                                                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                                        angle = (angle + 90f + 360f) % 360f
                                                        var cumulative = 0f
                                                        for (slice in slices) {
                                                            val sweep = (slice.percentage / 100f) * 360f
                                                            if (angle >= cumulative && angle <= cumulative + sweep) {
                                                                selectedSliceKey = slice.formatKey
                                                                break
                                                            }
                                                            cumulative += sweep
                                                        }
                                                    }
                                                }
                                            }
                                    ) {
                                        val strokeWidth = 30.dp.toPx()
                                        val selectedStrokeWidth = 38.dp.toPx()
                                        val diameter = size.minDimension - selectedStrokeWidth
                                        val topLeft = Offset(
                                            (size.width - diameter) / 2f,
                                            (size.height - diameter) / 2f
                                        )
                                        val arcSize = Size(diameter, diameter)

                                        drawArc(
                                            color = trackColor,
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = arcSize,
                                            style = Stroke(width = strokeWidth)
                                        )

                                        var startAngle = -90f
                                        slices.forEach { slice ->
                                            val rawSweep = (slice.percentage / 100f) * 360f * sweepProgress
                                            if (rawSweep > 0.5f) {
                                                val isSelected = slice.formatKey.equals(selectedSliceKey, ignoreCase = true)
                                                val gap = if (slices.count { it.bytesUsed > 0 } > 1) 2.5f else 0f
                                                val effectiveSweep = (rawSweep - gap).coerceAtLeast(1f)
                                                drawArc(
                                                    color = Color(slice.colorHex),
                                                    startAngle = startAngle + (gap / 2f),
                                                    sweepAngle = effectiveSweep,
                                                    useCenter = false,
                                                    topLeft = topLeft,
                                                    size = arcSize,
                                                    style = Stroke(
                                                        width = if (isSelected) selectedStrokeWidth else strokeWidth,
                                                        cap = StrokeCap.Butt
                                                    )
                                                )
                                                startAngle += rawSweep
                                            }
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (selectedSlice != null) {
                                            Text(
                                                text = selectedSlice.displayLabel,
                                                style = DocFlowTokens.captionBoldStyle,
                                                color = Color(selectedSlice.colorHex)
                                            )
                                            Text(
                                                text = formatStorageBytes(selectedSlice.bytesUsed),
                                                style = DocFlowTokens.rowTitleStyle.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = DocFlowTokens.textPrimary
                                            )
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", selectedSlice.percentage)}% • ${selectedSlice.documentCount} file(s)",
                                                style = DocFlowTokens.captionStyle,
                                                color = DocFlowTokens.textSecondary
                                            )
                                        } else {
                                            Text(
                                                text = "Total Used",
                                                style = DocFlowTokens.captionStyle,
                                                color = DocFlowTokens.textSecondary
                                            )
                                            Text(
                                                text = formatStorageBytes(storageSummary.totalBytesUsed),
                                                style = DocFlowTokens.rowTitleStyle.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = DocFlowTokens.textPrimary
                                            )
                                        }
                                    }
                                }

                                selectedSlice?.let { activeSlice ->
                                    DocFlowPrimaryButton(
                                        text = "Filter Workspace: ${activeSlice.displayLabel}",
                                        leadingIcon = Icons.Default.FilterList,
                                        modifier = Modifier.fillMaxWidth(),
                                        testTag = "filter_workspace_by_slice_button",
                                        onClick = { onFilterWorkspaceByFormat(activeSlice.formatKey) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Format Breakdown Cards
                item {
                    DocFlowSectionLabel(text = "Storage by Document Format")
                }

                items(storageSummary.slices, key = { it.formatKey }) { slice ->
                    val isSelected = slice.formatKey.equals(selectedSliceKey, ignoreCase = true)
                    val sliceColor = Color(slice.colorHex)

                    DocFlowCard(
                        modifier = Modifier
                            .padding(horizontal = horizontalMargin)
                            .testTag("storage_slice_card_${slice.formatKey}"),
                        borderColor = if (isSelected) sliceColor else null,
                        onClick = { selectedSliceKey = slice.formatKey }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(sliceColor.copy(alpha = 0.14f))
                                            .border(1.dp, sliceColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (slice.formatKey) {
                                                "pdf" -> Icons.Default.PictureAsPdf
                                                "docx" -> Icons.Default.Description
                                                "pptx" -> Icons.Default.Slideshow
                                                "xlsx" -> Icons.Default.TableChart
                                                else -> Icons.Default.Image
                                            },
                                            contentDescription = slice.displayLabel,
                                            tint = sliceColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = slice.displayLabel,
                                            style = DocFlowTokens.bodyMediumStyle.copy(fontWeight = FontWeight.Bold),
                                            color = DocFlowTokens.textPrimary
                                        )
                                        Text(
                                            text = "${slice.subtitle} • ${slice.documentCount} file(s)",
                                            style = DocFlowTokens.captionStyle,
                                            color = DocFlowTokens.textSecondary
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatStorageBytes(slice.bytesUsed),
                                        style = DocFlowTokens.bodyMediumStyle.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = sliceColor
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", slice.percentage)}%",
                                        style = DocFlowTokens.captionStyle,
                                        color = DocFlowTokens.textSecondary
                                    )
                                }
                            }

                            DocFlowProgressBar(
                                progress = (slice.percentage / 100f).coerceIn(0f, 1f)
                            )
                        }
                    }
                }

                // Files in Selected Format Slice
                if (matchingDocsForSlice.isNotEmpty()) {
                    item {
                        DocFlowSectionLabel(
                            text = "Top ${selectedSlice?.displayLabel ?: ""} Files"
                        )
                    }
                    item {
                        DocFlowCard(
                            modifier = Modifier.padding(horizontal = horizontalMargin),
                            padding = PaddingValues(vertical = DocFlowTokens.space1)
                        ) {
                            val topDocs = matchingDocsForSlice.take(5)
                            topDocs.forEachIndexed { idx, doc ->
                                DocFlowListRow(
                                    title = doc.title,
                                    subtitle = "${doc.fileName} • ${doc.pageCount} pg",
                                    leading = {
                                        DocFlowFileFormatBadge(fileType = doc.fileType)
                                    },
                                    trailing = {
                                        Text(
                                            text = formatStorageBytes(doc.fileSize),
                                            style = DocFlowTokens.captionBoldStyle.copy(
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = DocFlowTokens.accentBlue
                                        )
                                    },
                                    showBottomDivider = idx < topDocs.lastIndex,
                                    onClick = { onOpenDocument(doc) }
                                )
                            }
                        }
                    }
                }

                // Local Cache & Snapshot Compaction Card
                item {
                    DocFlowSectionLabel(text = "Database & Cache Optimization")
                }

                item {
                    DocFlowCard(
                        modifier = Modifier.padding(horizontal = horizontalMargin)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                            ) {
                                StorageStatPill(
                                    icon = Icons.Default.OfflinePin,
                                    label = "Offline Files",
                                    value = "${storageSummary.offlinePinnedCount} (${formatStorageBytes(storageSummary.offlinePinnedBytes)})",
                                    color = DocFlowTokens.accentBlue,
                                    modifier = Modifier.weight(1f)
                                )
                                StorageStatPill(
                                    icon = Icons.Default.EnhancedEncryption,
                                    label = "AES-256 Safe",
                                    value = "${storageSummary.encryptedVaultCount} protected",
                                    color = DocFlowTokens.iconGreenDarkExcel,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            HorizontalDivider(
                                thickness = DocFlowTokens.dividerThickness,
                                color = DocFlowTokens.divider
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Snapshot & Cache Compaction",
                                        style = DocFlowTokens.bodyMediumStyle.copy(fontWeight = FontWeight.Bold),
                                        color = DocFlowTokens.textPrimary
                                    )
                                    Text(
                                        text = "${storageSummary.versionSnapshotsCount} saved version checkpoint(s) in Room",
                                        style = DocFlowTokens.captionStyle,
                                        color = DocFlowTokens.textSecondary
                                    )
                                }
                                DocFlowSecondaryButton(
                                    text = "Compact",
                                    leadingIcon = Icons.Default.CleaningServices,
                                    testTag = "compact_snapshots_button",
                                    onClick = onCompactSnapshotsAndCache
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageStatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Column {
                Text(
                    text = label,
                    style = DocFlowTokens.captionStyle,
                    color = DocFlowTokens.textSecondary
                )
                Text(
                    text = value,
                    style = DocFlowTokens.captionBoldStyle,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun formatStorageBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    return if (kb < 1024.0) {
        String.format(Locale.US, "%.1f KB", kb.coerceAtLeast(0.5))
    } else {
        val mb = kb / 1024.0
        if (mb < 1024.0) {
            String.format(Locale.US, "%.2f MB", mb)
        } else {
            val gb = mb / 1024.0
            String.format(Locale.US, "%.2f GB", gb)
        }
    }
}
