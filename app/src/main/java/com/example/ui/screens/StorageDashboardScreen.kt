package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CachedDocumentEntity
import com.example.data.util.OnDeviceAiAndBackupEngine
import com.example.ui.components.DocFlowCard
import com.example.ui.components.DocFlowDivider
import com.example.ui.components.DocFlowEmptyState
import com.example.ui.components.DocFlowIconCircle
import com.example.ui.components.DocFlowListRow
import com.example.ui.components.DocFlowSectionLabel
import com.example.ui.components.DocFlowToast
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens
import com.example.ui.viewmodel.DocFlowViewModel

private data class DeviceCategoryRow(
    val key: String,
    val label: String,
    val bytes: Long,
    val color: Color,
    val docs: List<CachedDocumentEntity>
)

@Composable
fun StorageDashboardScreen(
    viewModel: DocFlowViewModel,
    palette: DocFlowPalette,
    onBack: () -> Unit,
    onOpenDocument: (String) -> Unit
) {
    val context = LocalContext.current
    val cachedMap by viewModel.cachedDocumentsMap.collectAsState()
    val privateSafeDocs by viewModel.privateSafeDocuments.collectAsState()
    val deletedDocs by viewModel.deletedDocuments.collectAsState()

    var cacheBytes by remember {
        mutableLongStateOf(OnDeviceAiAndBackupEngine.calculateCacheAndTempBytes(context))
    }
    var selectedCategoryKey by remember { mutableStateOf<String?>(null) }
    var toastText by remember { mutableStateOf<String?>(null) }

    val allDocs = cachedMap.values.toList()
    val normalDocs = allDocs.filter { !it.isDeleted && !it.isPrivateSafe }

    val pdfDocs = normalDocs.filter { it.fileType.equals("pdf", true) || it.fileType.equals("ofd", true) }
    val wordDocs = normalDocs.filter {
        val ft = it.fileType.lowercase()
        ft in setOf("docx", "doc", "txt", "md", "note")
    }
    val officeDocs = normalDocs.filter {
        val ft = it.fileType.lowercase()
        ft in setOf("xlsx", "xls", "csv", "pptx", "ppt")
    }
    val imageDocs = normalDocs.filter {
        val ft = it.fileType.lowercase()
        ft in setOf("pic", "scanned", "png", "jpg", "jpeg", "webp", "heic")
    }

    val categories = listOf(
        DeviceCategoryRow(
            key = "pdf",
            label = "PDF",
            bytes = pdfDocs.sumOf { it.fileSize.coerceAtLeast(1024L) },
            color = DocFlowTokens.IconCircleRed,
            docs = pdfDocs.sortedByDescending { it.fileSize }
        ),
        DeviceCategoryRow(
            key = "word",
            label = "Word & Notes",
            bytes = wordDocs.sumOf { it.fileSize.coerceAtLeast(1024L) },
            color = DocFlowTokens.AccentBlue,
            docs = wordDocs.sortedByDescending { it.fileSize }
        ),
        DeviceCategoryRow(
            key = "office",
            label = "Excel & PowerPoint",
            bytes = officeDocs.sumOf { it.fileSize.coerceAtLeast(1024L) },
            color = DocFlowTokens.IconCircleExcelGreen,
            docs = officeDocs.sortedByDescending { it.fileSize }
        ),
        DeviceCategoryRow(
            key = "images",
            label = "Images / Scans",
            bytes = imageDocs.sumOf { it.fileSize.coerceAtLeast(1024L) },
            color = DocFlowTokens.IconCircleTeal,
            docs = imageDocs.sortedByDescending { it.fileSize }
        ),
        DeviceCategoryRow(
            key = "safe",
            label = "Private Safe",
            bytes = privateSafeDocs.sumOf { it.fileSize.coerceAtLeast(1024L) },
            color = DocFlowTokens.IconCircleBlue,
            docs = privateSafeDocs.sortedByDescending { it.fileSize }
        ),
        DeviceCategoryRow(
            key = "deleted",
            label = "Recently deleted",
            bytes = deletedDocs.sumOf { it.fileSize.coerceAtLeast(1024L) },
            color = DocFlowTokens.IconCircleGray,
            docs = deletedDocs.sortedByDescending { it.fileSize }
        )
    )

    val totalUsedBytes = categories.sumOf { it.bytes } + cacheBytes
    val freeDeviceBytes = remember(totalUsedBytes) {
        OnDeviceAiAndBackupEngine.getDeviceFreeSpaceBytes(context)
    }

    val activeCategory = categories.find { it.key == selectedCategoryKey }
    val largestFilesToShow = activeCategory?.docs ?: allDocs.sortedByDescending { it.fileSize }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("storage_dashboard_list"),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        // Top Header with Back Arrow and 40sp Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = DocFlowTokens.space2,
                        end = DocFlowTokens.screenHorizontalMargin,
                        top = DocFlowTokens.space3,
                        bottom = DocFlowTokens.space2
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(DocFlowTokens.minTouchTarget)
                        .clip(CircleShape)
                        .clickable(onClick = onBack)
                        .testTag("storage_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(DocFlowTokens.space1))
                Column {
                    Text(
                        text = "On this device",
                        style = DocFlowTokens.largeTitleStyle,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Stored on this device • Device storage & file accounting",
                        style = DocFlowTokens.captionStyle,
                        color = palette.textSecondary
                    )
                }
            }
        }

        if (toastText != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DocFlowTokens.screenHorizontalMargin, vertical = DocFlowTokens.space1)
                ) {
                    DocFlowToast(message = toastText!!)
                }
            }
        }

        // Main "On this device" Card with Donut Chart, Breakdown Bar, and Free Space
        item {
            DocFlowSectionLabel(text = "Device Storage Overview", palette = palette)
            DocFlowCard(palette = palette) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space4)
                ) {
                    // Donut Chart
                    Box(
                        modifier = Modifier
                            .size(124.dp)
                            .testTag("storage_donut_chart"),
                        contentAlignment = Alignment.Center
                    ) {
                        val safeTotal = totalUsedBytes.coerceAtLeast(1L).toFloat()
                        Canvas(modifier = Modifier.size(112.dp)) {
                            val strokeWidth = 18.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                            val arcSize = Size(diameter, diameter)

                            drawArc(
                                color = Color(0xFFE3E4E7),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth)
                            )

                            var startAngle = -90f
                            categories.forEach { cat ->
                                val fraction = (cat.bytes.toFloat() / safeTotal).coerceIn(0f, 1f)
                                val sweep = (fraction * 360f).coerceAtLeast(if (cat.bytes > 0L) 6f else 0f)
                                if (sweep > 0f) {
                                    drawArc(
                                        color = cat.color,
                                        startAngle = startAngle,
                                        sweepAngle = (sweep - 2f).coerceAtLeast(2f),
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweep
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formatBytes(totalUsedBytes),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "${allDocs.size} files",
                                style = DocFlowTokens.captionStyle,
                                color = palette.textSecondary
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                    ) {
                        Text(
                            text = "On this device",
                            style = DocFlowTokens.rowTitleStyle,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Used by DocFlow: ${formatBytes(totalUsedBytes)}",
                            style = DocFlowTokens.bodyStyle,
                            color = DocFlowTokens.AccentBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Free on device: ${formatBytes(freeDeviceBytes)}",
                            style = DocFlowTokens.captionStyle,
                            color = palette.textSecondary
                        )
                        Text(
                            text = "Stored on this device",
                            style = DocFlowTokens.captionStyle,
                            color = DocFlowTokens.IconCircleGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(DocFlowTokens.space4))

                // Multi-color Breakdown Bar
                val barTotal = totalUsedBytes.coerceAtLeast(1L).toFloat()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(DocFlowTokens.pillButtonShape)
                        .background(palette.divider)
                        .testTag("storage_breakdown_bar")
                ) {
                    categories.forEach { cat ->
                        val weightVal = (cat.bytes.toFloat() / barTotal).coerceAtLeast(if (cat.bytes > 0L) 0.04f else 0f)
                        if (weightVal > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(weightVal)
                                    .height(10.dp)
                                    .background(cat.color)
                            )
                        }
                    }
                }
            }
        }

        // Category Rows (Tap a row to filter largest files in that category)
        item {
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))
            DocFlowSectionLabel(text = "Categories • Tap to inspect largest files", palette = palette)
            DocFlowCard(palette = palette, contentPadding = PaddingValues(vertical = DocFlowTokens.space1)) {
                categories.forEachIndexed { idx, cat ->
                    if (idx > 0) {
                        DocFlowDivider(
                            palette = palette,
                            modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin)
                        )
                    }
                    val isSelected = selectedCategoryKey == cat.key
                    DocFlowListRow(
                        title = cat.label,
                        subtitle = "${formatBytes(cat.bytes)} • ${cat.docs.size} file(s) • Stored on this device",
                        palette = palette,
                        onClick = {
                            selectedCategoryKey = if (isSelected) null else cat.key
                        },
                        showChevron = true,
                        modifier = Modifier.testTag("storage_slice_${cat.key}"),
                        leading = {
                            DocFlowIconCircle(
                                icon = when (cat.key) {
                                    "pdf" -> Icons.Filled.PictureAsPdf
                                    "word" -> Icons.Filled.Description
                                    "office" -> Icons.Filled.TableChart
                                    "images" -> Icons.Filled.Image
                                    "safe" -> Icons.Filled.Lock
                                    else -> Icons.Filled.DeleteOutline
                                },
                                circleColor = cat.color
                            )
                        },
                        trailing = {
                            Text(
                                text = if (isSelected) "Filtered" else formatBytes(cat.bytes),
                                style = DocFlowTokens.captionStyle,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) DocFlowTokens.AccentBlue else palette.textSecondary
                            )
                        }
                    )
                }

                DocFlowDivider(
                    palette = palette,
                    modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin)
                )

                // Clear Cache row (never deletes user documents)
                DocFlowListRow(
                    title = "Cache (${formatBytes(cacheBytes)})",
                    subtitle = "Temporary thumbnails • Clear cache never deletes user documents",
                    palette = palette,
                    onClick = {
                        val freed = OnDeviceAiAndBackupEngine.clearCacheAndTempFiles(context)
                        viewModel.clearThumbnailAndTempCache(context)
                        cacheBytes = OnDeviceAiAndBackupEngine.calculateCacheAndTempBytes(context)
                        toastText = "Cleared ${formatBytes(freed)} cache (documents untouched)"
                    },
                    modifier = Modifier.testTag("storage_clear_cache_row"),
                    leading = {
                        DocFlowIconCircle(
                            icon = Icons.Filled.CleaningServices,
                            circleColor = DocFlowTokens.IconCircleGreen
                        )
                    },
                    trailing = {
                        Text(
                            text = "Clear cache",
                            style = DocFlowTokens.captionStyle,
                            fontWeight = FontWeight.Bold,
                            color = DocFlowTokens.AccentBlue
                        )
                    }
                )
            }
        }

        // Largest Files in Selected Category (or All Categories) with Delete Option
        item {
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))
            DocFlowSectionLabel(
                text = if (activeCategory != null) {
                    "Largest Files in ${activeCategory.label} (${largestFilesToShow.size})"
                } else {
                    "Largest Files On This Device (${largestFilesToShow.size})"
                },
                palette = palette
            )
        }

        if (largestFilesToShow.isEmpty()) {
            item {
                DocFlowCard(palette = palette) {
                    DocFlowEmptyState(
                        icon = Icons.Filled.Devices,
                        title = "No files in this category",
                        subtitle = "Documents stored on this device will appear here with size and delete options.",
                        palette = palette
                    )
                }
            }
        } else {
            item {
                DocFlowCard(palette = palette, contentPadding = PaddingValues(vertical = DocFlowTokens.space1)) {
                    largestFilesToShow.forEachIndexed { idx, doc ->
                        if (idx > 0) {
                            DocFlowDivider(
                                palette = palette,
                                modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin)
                            )
                        }
                        DocFlowListRow(
                            title = doc.title,
                            subtitle = "${doc.fileType.uppercase()} • ${formatBytes(doc.fileSize)} • Stored on this device",
                            palette = palette,
                            onClick = { onOpenDocument(doc.id) },
                            modifier = Modifier.testTag("storage_file_${doc.id}"),
                            leading = {
                                DocFlowIconCircle(
                                    icon = when (doc.fileType.lowercase()) {
                                        "pdf" -> Icons.Filled.PictureAsPdf
                                        "xlsx", "xls", "csv" -> Icons.Filled.TableChart
                                        "pptx", "ppt" -> Icons.Filled.Slideshow
                                        "pic", "scanned" -> Icons.Filled.Image
                                        else -> Icons.Filled.Description
                                    },
                                    circleColor = when (doc.fileType.lowercase()) {
                                        "pdf" -> DocFlowTokens.IconCircleRed
                                        "xlsx", "xls", "csv" -> DocFlowTokens.IconCircleExcelGreen
                                        "pptx", "ppt" -> DocFlowTokens.IconCircleOrange
                                        "pic", "scanned" -> DocFlowTokens.IconCircleTeal
                                        else -> DocFlowTokens.AccentBlue
                                    }
                                )
                            },
                            trailing = {
                                Box(
                                    modifier = Modifier
                                        .size(DocFlowTokens.minTouchTarget)
                                        .clip(CircleShape)
                                        .clickable {
                                            if (doc.isDeleted) {
                                                viewModel.permanentlyDeleteDocument(doc.id)
                                                toastText = "Permanently deleted '${doc.title}'"
                                            } else {
                                                viewModel.softDeleteDocument(doc.id)
                                                toastText = "Moved '${doc.title}' to Recently deleted"
                                            }
                                        }
                                        .testTag("storage_delete_file_${doc.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete ${doc.title}",
                                        tint = DocFlowTokens.IconCircleRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format("%.2f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}
