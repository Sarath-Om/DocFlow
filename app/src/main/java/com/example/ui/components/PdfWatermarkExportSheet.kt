package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.OnDeviceAiAndBackupEngine
import com.example.data.util.PdfWatermarkConfig
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PdfWatermarkExportSheet(
    palette: DocFlowPalette,
    documentTitle: String,
    previewTextSnippet: String,
    initialEnabled: Boolean = true,
    confirmLabel: String = "Export PDF (Stored on this device)",
    onDismiss: () -> Unit,
    onConfirmExport: (PdfWatermarkConfig) -> Unit
) {
    val context = LocalContext.current
    val saved = remember { OnDeviceAiAndBackupEngine.getSavedWatermarkConfig(context) }

    var watermarkEnabled by remember { mutableStateOf(initialEnabled) }
    var watermarkText by remember { mutableStateOf(saved.text.ifBlank { "Confidential" }) }
    var opacityPercent by remember { mutableFloatStateOf(saved.opacityPercent.toFloat()) }
    var positionIndex by remember { mutableIntStateOf(saved.positionIndex) }
    var rotationDegrees by remember { mutableFloatStateOf(saved.rotationDegrees.toFloat()) }
    var fontSizeSp by remember { mutableFloatStateOf(saved.fontSizeSp.toFloat()) }
    var colorHex by remember { mutableStateOf(saved.colorHex) }
    var pageRangeMode by remember { mutableStateOf(saved.pageRangeMode) }
    var customPageRange by remember { mutableStateOf(saved.customPageRange) }
    var layerBehindContent by remember { mutableStateOf(saved.layerBehindContent) }

    val presets = listOf(
        "Confidential",
        "Draft",
        "Do not copy",
        "Sample",
        "Internal use",
        "Urgent",
        "गोपनीय"
    )
    val swatches = listOf(
        "#8A8D93" to "Gray",
        "#E5483F" to "Red",
        "#1F6BFF" to "Blue",
        "#111111" to "Black",
        "#25C25A" to "Green"
    )
    val gridPositions = listOf(
        0 to "Top-L",
        1 to "Top-C",
        2 to "Top-R",
        3 to "Mid-L",
        4 to "Center",
        5 to "Mid-R",
        6 to "Bot-L",
        7 to "Bot-C",
        8 to "Bot-R",
        9 to "Tile across page"
    )

    val currentConfig = PdfWatermarkConfig(
        enabled = watermarkEnabled,
        text = watermarkText.take(40),
        opacityPercent = opacityPercent.toInt().coerceIn(5, 100),
        positionIndex = positionIndex,
        rotationDegrees = rotationDegrees.toInt().coerceIn(-90, 90),
        fontSizeSp = fontSizeSp.toInt().coerceIn(14, 72),
        colorHex = colorHex,
        pageRangeMode = pageRangeMode,
        customPageRange = customPageRange,
        layerBehindContent = layerBehindContent
    )

    DocFlowBottomSheet(
        onDismissRequest = onDismiss,
        palette = palette
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DocFlowIconCircle(
                        icon = Icons.Filled.PictureAsPdf,
                        circleColor = DocFlowTokens.IconCircleRed,
                        size = 44.dp
                    )
                    Column {
                        Text(
                            text = "Export PDF & Watermark",
                            style = DocFlowTokens.sectionLabelStyle,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "$documentTitle • Stored on this device",
                            style = DocFlowTokens.captionStyle,
                            color = palette.textSecondary
                        )
                    }
                }
                Switch(
                    checked = watermarkEnabled,
                    onCheckedChange = { watermarkEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DocFlowTokens.AccentBlue),
                    modifier = Modifier.testTag("watermark_enable_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live First-Page Preview Canvas
            Text(
                text = "Live First-Page Preview",
                style = DocFlowTokens.captionStyle,
                fontWeight = FontWeight.SemiBold,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(DocFlowTokens.buttonShape)
                    .background(Color(0xFFF0F1F3))
                    .border(1.dp, palette.divider, DocFlowTokens.buttonShape)
                    .padding(12.dp)
                    .testTag("watermark_live_preview"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .width(120.dp)
                        .height(156.dp)
                        .clip(DocFlowTokens.thumbnailShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFDADCE0), DocFlowTokens.thumbnailShape)
                ) {
                    val pw = size.width
                    val ph = size.height

                    fun drawPreviewWatermark() {
                        if (!currentConfig.enabled || currentConfig.text.isBlank()) return
                        val parsed = runCatching { android.graphics.Color.parseColor(currentConfig.colorHex) }
                            .getOrDefault(android.graphics.Color.GRAY)
                        val alpha = ((currentConfig.opacityPercent / 100f) * 255f).toInt().coerceIn(15, 255)
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.argb(
                                alpha,
                                android.graphics.Color.red(parsed),
                                android.graphics.Color.green(parsed),
                                android.graphics.Color.blue(parsed)
                            )
                            textSize = (currentConfig.fontSizeSp * 0.36f).coerceIn(8f, 26f)
                            typeface = android.graphics.Typeface.create(
                                android.graphics.Typeface.DEFAULT,
                                android.graphics.Typeface.BOLD
                            )
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        val nc = drawContext.canvas.nativeCanvas
                        if (currentConfig.positionIndex == 9) {
                            val stepX = pw / 2.2f
                            val stepY = ph / 3.0f
                            var y = stepY * 0.55f
                            while (y < ph - 8f) {
                                var x = stepX * 0.5f
                                while (x < pw - 8f) {
                                    nc.save()
                                    nc.translate(x, y)
                                    nc.rotate(currentConfig.rotationDegrees.toFloat())
                                    nc.drawText(currentConfig.text.take(16), 0f, 0f, paint)
                                    nc.restore()
                                    x += stepX
                                }
                                y += stepY
                            }
                        } else {
                            val col = currentConfig.positionIndex.coerceIn(0, 8) % 3
                            val row = currentConfig.positionIndex.coerceIn(0, 8) / 3
                            val tx = when (col) {
                                0 -> pw * 0.26f
                                2 -> pw * 0.74f
                                else -> pw * 0.50f
                            }
                            val ty = when (row) {
                                0 -> ph * 0.20f
                                2 -> ph * 0.80f
                                else -> ph * 0.50f
                            }
                            nc.save()
                            nc.translate(tx, ty)
                            nc.rotate(currentConfig.rotationDegrees.toFloat())
                            nc.drawText(currentConfig.text.take(20), 0f, 0f, paint)
                            nc.restore()
                        }
                    }

                    if (currentConfig.layerBehindContent) {
                        drawPreviewWatermark()
                    }

                    // Simulated document lines on the preview page
                    val linePaint = Color(0xFF1F2937)
                    drawLine(linePaint, Offset(12f, 18f), Offset(pw - 24f, 18f), strokeWidth = 4f)
                    for (lineIdx in 0..6) {
                        val y = 34f + lineIdx * 14f
                        val endX = if (lineIdx % 3 == 2) pw - 32f else pw - 14f
                        drawLine(Color(0xFF9CA3AF), Offset(12f, y), Offset(endX, y), strokeWidth = 2.5f)
                    }

                    if (!currentConfig.layerBehindContent) {
                        drawPreviewWatermark()
                    }
                }
            }

            if (watermarkEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = watermarkText,
                    onValueChange = { if (it.length <= 40) watermarkText = it },
                    label = { Text("Watermark text (max 40 chars • English / हिन्दी)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("watermark_text_input")
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Preset Chips",
                    style = DocFlowTokens.captionStyle,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = watermarkText.equals(preset, ignoreCase = true),
                            onClick = { watermarkText = preset },
                            label = { Text(preset, fontSize = 12.sp) },
                            modifier = Modifier.testTag("watermark_preset_${preset.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Opacity Slider (5% - 100%, default 20%)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Opacity", style = DocFlowTokens.captionStyle, color = palette.textPrimary)
                    Text("${opacityPercent.toInt()}%", style = DocFlowTokens.captionStyle, fontWeight = FontWeight.Bold, color = DocFlowTokens.AccentBlue)
                }
                Slider(
                    value = opacityPercent,
                    onValueChange = { opacityPercent = it },
                    valueRange = 5f..100f,
                    modifier = Modifier.testTag("watermark_opacity_slider")
                )

                // Rotation Slider (-90° to +90°, with -45° diagonal shortcut)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rotation (${rotationDegrees.toInt()}°)", style = DocFlowTokens.captionStyle, color = palette.textPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "45° Diagonal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DocFlowTokens.AccentBlue,
                            modifier = Modifier.clickable { rotationDegrees = -45f }
                        )
                        Text(
                            text = "0° Horizontal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DocFlowTokens.AccentBlue,
                            modifier = Modifier.clickable { rotationDegrees = 0f }
                        )
                    }
                }
                Slider(
                    value = rotationDegrees,
                    onValueChange = { rotationDegrees = it },
                    valueRange = -90f..90f,
                    modifier = Modifier.testTag("watermark_rotation_slider")
                )

                // Font Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Font Size", style = DocFlowTokens.captionStyle, color = palette.textPrimary)
                    Text("${fontSizeSp.toInt()} pt", style = DocFlowTokens.captionStyle, fontWeight = FontWeight.Bold, color = DocFlowTokens.AccentBlue)
                }
                Slider(
                    value = fontSizeSp,
                    onValueChange = { fontSizeSp = it },
                    valueRange = 16f..68f,
                    modifier = Modifier.testTag("watermark_size_slider")
                )

                // 3x3 Position Grid + Tile Across Page
                Text(
                    text = "Position (3×3 Grid or Tile Across Page)",
                    style = DocFlowTokens.captionStyle,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    gridPositions.forEach { (idx, label) ->
                        FilterChip(
                            selected = positionIndex == idx,
                            onClick = { positionIndex = idx },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.testTag("watermark_pos_$idx")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Color Swatches
                Text(
                    text = "Watermark Color",
                    style = DocFlowTokens.captionStyle,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    swatches.forEach { (hex, label) ->
                        val parsed = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = colorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parsed)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) DocFlowTokens.AccentBlue else palette.divider,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex }
                                .testTag("watermark_color_${label.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = label,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Page Range & Layer Toggle
                Text(
                    text = "Page Range & Layer",
                    style = DocFlowTokens.captionStyle,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = pageRangeMode == "ALL",
                        onClick = { pageRangeMode = "ALL" },
                        label = { Text("All pages", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = pageRangeMode == "FIRST",
                        onClick = { pageRangeMode = "FIRST" },
                        label = { Text("First page only", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = pageRangeMode == "CUSTOM",
                        onClick = { pageRangeMode = "CUSTOM" },
                        label = { Text("Custom range ($customPageRange)", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = layerBehindContent,
                        onClick = { layerBehindContent = true },
                        label = { Text("Behind content", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = !layerBehindContent,
                        onClick = { layerBehindContent = false },
                        label = { Text("Over content", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DocFlowSecondaryButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                DocFlowPrimaryButton(
                    text = confirmLabel,
                    onClick = {
                        OnDeviceAiAndBackupEngine.saveWatermarkConfig(context, currentConfig)
                        onConfirmExport(currentConfig)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("watermark_confirm_export_button")
                )
            }
        }
    }
}
