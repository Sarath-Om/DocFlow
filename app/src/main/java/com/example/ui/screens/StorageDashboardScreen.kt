package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocEntity
import com.example.data.model.FormatStorageSlice
import com.example.data.model.StorageDashboardSummary
import com.example.ui.components.CrystalGlassBackground
import com.example.ui.components.CrystalGlassCard
import kotlin.math.atan2
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3Api::class)
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
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
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

    CrystalGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(
                            onClick = onBackToWorkspace,
                            modifier = Modifier.testTag("storage_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Workspace"
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = "Visual Storage Intelligence",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Interactive Donut Breakdown • Room & Encrypted Cloud Vault",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            },
            bottomBar = {
                WorkspaceBottomNavBar(
                    activeTab = "storage",
                    onSelectWorkspace = onBackToWorkspace,
                    onSelectStorage = {},
                    onSelectBatchStudio = onOpenBatchStudio
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Interactive Donut Chart & Center Telemetry Card
                item {
                    CrystalGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("storage_donut_chart_card"),
                        cornerRadius = 24.dp,
                        tintColor = selectedSlice?.let { Color(it.colorHex) } ?: Color(0xFF0066FF)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PieChart,
                                        contentDescription = null,
                                        tint = Color(0xFF0066FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Format Storage Donut Chart",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    color = Color(0xFF0066FF).copy(alpha = 0.14f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${allDocuments.size} Total Files",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0066FF),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (allDocuments.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "No real documents indexed yet in your storage vault.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = onBackToWorkspace,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                                    ) {
                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Workspace & Import Documents")
                                    }
                                }
                            } else {
                                // Interactive Canvas Donut Chart
                                Box(
                                    modifier = Modifier
                                        .size(230.dp)
                                        .testTag("interactive_donut_canvas"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val slices = storageSummary.slices
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                            .pointerInput(slices) {
                                                detectTapGestures { tapOffset ->
                                                    val center = Offset(size.width / 2f, size.height / 2f)
                                                    val dx = tapOffset.x - center.x
                                                    val dy = tapOffset.y - center.y
                                                    val dist = hypot(dx, dy)
                                                    val radius = minOf(size.width, size.height).toFloat() / 2f
                                                    if (dist in (radius * 0.42f)..(radius * 1.15f)) {
                                                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                                        // Shift from -90 deg start
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
                                        val strokeWidth = 34.dp.toPx()
                                        val selectedStrokeWidth = 42.dp.toPx()
                                        val diameter = size.minDimension - selectedStrokeWidth
                                        val topLeft = Offset(
                                            (size.width - diameter) / 2f,
                                            (size.height - diameter) / 2f
                                        )
                                        val arcSize = Size(diameter, diameter)

                                        // Subtle background ring
                                        drawArc(
                                            color = Color.Gray.copy(alpha = 0.14f),
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

                                    // Center Readout inside Donut Hole
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (selectedSlice != null) {
                                            Text(
                                                text = selectedSlice.displayLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(selectedSlice.colorHex)
                                            )
                                            Text(
                                                text = formatStorageBytes(selectedSlice.bytesUsed),
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = "${String.format("%.1f", selectedSlice.percentage)}% • ${selectedSlice.documentCount} file(s)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            Text(
                                                text = "Total Used",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = formatStorageBytes(storageSummary.totalBytesUsed),
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Tap any format slice on the donut chart or legend below to inspect & filter",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Selected Slice Action Row
                                selectedSlice?.let { activeSlice ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = { onFilterWorkspaceByFormat(activeSlice.formatKey) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("filter_workspace_by_slice_button"),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(activeSlice.colorHex),
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Filter Workspace: ${activeSlice.displayLabel}",
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Format Breakdown Interactive Legend Cards (PDF, DOCX, PPTX, XLSX, PIC)
                item {
                    Text(
                        text = "Storage Breakdown by Document Type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(storageSummary.slices, key = { it.formatKey }) { slice ->
                    val isSelected = slice.formatKey.equals(selectedSliceKey, ignoreCase = true)
                    val sliceColor = Color(slice.colorHex)

                    CrystalGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("storage_slice_card_${slice.formatKey}"),
                        cornerRadius = 18.dp,
                        tintColor = if (isSelected) sliceColor else null,
                        onClick = { selectedSliceKey = slice.formatKey }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(sliceColor.copy(alpha = 0.18f))
                                            .border(1.dp, sliceColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
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
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${slice.subtitle} • ${slice.documentCount} document(s)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatStorageBytes(slice.bytesUsed),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = sliceColor
                                    )
                                    Text(
                                        text = "${String.format("%.1f", slice.percentage)}% of used",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { (slice.percentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = sliceColor,
                                trackColor = sliceColor.copy(alpha = 0.14f)
                            )
                        }
                    }
                }

                // Files in Selected Format Slice
                if (matchingDocsForSlice.isNotEmpty()) {
                    item {
                        CrystalGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Top ${selectedSlice?.displayLabel ?: ""} Files by Footprint",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                matchingDocsForSlice.take(4).forEach { doc ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onOpenDocument(doc) }
                                            .padding(vertical = 6.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = doc.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${doc.fileName} • ${doc.pageCount} pg",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = formatStorageBytes(doc.fileSize),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Offline Cache, E2E Encrypted Vault & Version Compaction Card
                item {
                    CrystalGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp,
                        tintColor = Color(0xFF059669)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = Color(0xFF059669)
                                )
                                Text(
                                    text = "Room Offline Cache & CRDT Snapshot Health",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StorageStatPill(
                                    icon = Icons.Default.OfflinePin,
                                    label = "Offline Pinned",
                                    value = "${storageSummary.offlinePinnedCount} files (${formatStorageBytes(storageSummary.offlinePinnedBytes)})",
                                    color = Color(0xFF0066FF),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StorageStatPill(
                                    icon = Icons.Default.EnhancedEncryption,
                                    label = "AES-256 E2EE",
                                    value = "${storageSummary.encryptedVaultCount} protected",
                                    color = Color(0xFF5E17EB),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "CRDT Snapshot Compaction",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${storageSummary.versionSnapshotsCount} saved version checkpoint(s) in Room",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = onCompactSnapshotsAndCache,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("compact_snapshots_button")
                                ) {
                                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Compact", style = MaterialTheme.typography.labelSmall)
                                }
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
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun WorkspaceBottomNavBar(
    activeTab: String,
    onSelectWorkspace: () -> Unit,
    onSelectStorage: () -> Unit,
    onSelectBatchStudio: () -> Unit,
    onSelectSettings: () -> Unit = {}
) {
    Surface(
        color = Color(0xFF0C0E17).copy(alpha = 0.88f),
        tonalElevation = 8.dp,
        shadowElevation = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WorkspaceNavTabItem(
                    label = "Documents",
                    icon = Icons.Default.Description,
                    selected = activeTab == "workspace",
                    accentColor = Color(0xFF00F4FE),
                    testTag = "nav_tab_workspace",
                    onClick = onSelectWorkspace
                )
                WorkspaceNavTabItem(
                    label = "Shared",
                    icon = Icons.Default.PieChart,
                    selected = activeTab == "storage",
                    accentColor = Color(0xFF00F4FE),
                    testTag = "nav_tab_storage",
                    onClick = onSelectStorage
                )
                WorkspaceNavTabItem(
                    label = "Workflows",
                    icon = Icons.Default.Layers,
                    selected = activeTab == "batch",
                    accentColor = Color(0xFF00F4FE),
                    testTag = "nav_tab_batch",
                    onClick = onSelectBatchStudio
                )
                WorkspaceNavTabItem(
                    label = "Settings",
                    icon = Icons.Default.History,
                    selected = activeTab == "settings",
                    accentColor = Color(0xFF00F4FE),
                    testTag = "nav_tab_settings",
                    onClick = onSelectSettings
                )
            }
            Box(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .width(110.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    }
}

@Composable
private fun WorkspaceNavTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) accentColor.copy(alpha = 0.14f) else Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) accentColor else Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accentColor else Color.White.copy(alpha = 0.55f)
            )
        }
    }
}

fun formatStorageBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    return if (kb < 1024.0) {
        String.format("%.1f KB", kb.coerceAtLeast(0.5))
    } else {
        val mb = kb / 1024.0
        String.format("%.2f MB", mb)
    }
}
