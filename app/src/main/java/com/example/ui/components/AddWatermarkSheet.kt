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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.PdfWatermarkConfig
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens

/**
 * 'Add Watermark' tool sheet for the PDF editor.
 * Allows users to select or type a text string, choose opacity (5% to 100%),
 * pick rotation angle & color, inspect a real-time live preview, and overlay it
 * onto all pages of the selected document entirely on-device.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddWatermarkSheet(
    palette: DocFlowPalette,
    documentTitle: String,
    initialConfig: PdfWatermarkConfig = PdfWatermarkConfig(enabled = true, text = "CONFIDENTIAL", opacityPercent = 25),
    onDismiss: () -> Unit,
    onApplyWatermark: (PdfWatermarkConfig) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var watermarkText by remember { mutableStateOf(initialConfig.text.ifBlank { "CONFIDENTIAL" }) }
    var opacityPercent by remember { mutableFloatStateOf(initialConfig.opacityPercent.toFloat().coerceIn(5f, 100f)) }
    var rotationDegrees by remember { mutableFloatStateOf(initialConfig.rotationDegrees.toFloat()) }
    var fontSizeSp by remember { mutableFloatStateOf(initialConfig.fontSizeSp.toFloat()) }
    var colorHex by remember { mutableStateOf(initialConfig.colorHex) }

    val presetTexts = listOf(
        "CONFIDENTIAL",
        "DO NOT COPY",
        "DRAFT",
        "SAMPLE",
        "INTERNAL USE",
        "URGENT",
        "COPYRIGHT 2026",
        "PAID",
        "गोपनीय"
    )

    val opacityPresets = listOf(
        15 to "15% (Subtle)",
        25 to "25% (Standard)",
        50 to "50% (Medium)",
        80 to "80% (Bold)"
    )

    val swatches = listOf(
        "#8A8D93" to "Gray",
        "#E5483F" to "Red",
        "#1F6BFF" to "Blue",
        "#111111" to "Black",
        "#25C25A" to "Green"
    )

    val rotationAngles = listOf(
        -45f to "Diagonal (-45°)",
        0f to "Horizontal (0°)",
        45f to "Diagonal (+45°)",
        90f to "Vertical (90°)"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        shape = DocFlowTokens.sheetShape,
        modifier = Modifier.testTag("add_watermark_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DocFlowTokens.space4)
                .padding(bottom = DocFlowTokens.space5)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
        ) {
            // Header Row
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
                        icon = Icons.Default.BrandingWatermark,
                        circleColor = DocFlowTokens.AccentBlue,
                        size = 40.dp
                    )
                    Column {
                        Text(
                            text = "Add Watermark",
                            style = DocFlowTokens.sectionLabelStyle,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Overlay on all pages • Stored on this device",
                            style = DocFlowTokens.captionStyle,
                            color = palette.textSecondary
                        )
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

            DocFlowDivider(palette = palette)

            // Live Interactive Preview Box
            DocFlowCard(
                palette = palette,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(DocFlowTokens.space3),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE PAGE PREVIEW",
                            style = DocFlowTokens.captionBoldStyle,
                            color = palette.textSecondary
                        )
                        Text(
                            text = "${opacityPercent.toInt()}% Opacity • ${rotationDegrees.toInt()}°",
                            style = DocFlowTokens.captionStyle,
                            color = DocFlowTokens.AccentBlue
                        )
                    }

                    // Mini PDF Page Simulation Canvas
                    Box(
                        modifier = Modifier
                            .size(width = 220.dp, height = 150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                            // Simulated page lines
                            val linePaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#E5E7EB")
                                strokeWidth = 3f
                            }
                            var y = 20f
                            while (y < size.height - 15f) {
                                drawLine(
                                    color = Color(0xFFE5E7EB),
                                    start = Offset(15f, y),
                                    end = Offset(size.width - 15f, y),
                                    strokeWidth = 3f
                                )
                                y += 18f
                            }

                            // Watermark Stamp overlay
                            val text = watermarkText.trim().ifBlank { "SAMPLE" }
                            val baseColor = runCatching {
                                android.graphics.Color.parseColor(colorHex)
                            }.getOrDefault(android.graphics.Color.GRAY)
                            val alphaVal = ((opacityPercent.coerceIn(5f, 100f) / 100f) * 255f).toInt().coerceIn(12, 255)

                            val wmPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.argb(
                                    alphaVal,
                                    android.graphics.Color.red(baseColor),
                                    android.graphics.Color.green(baseColor),
                                    android.graphics.Color.blue(baseColor)
                                )
                                textSize = 22f
                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                textAlign = android.graphics.Paint.Align.CENTER
                            }

                            drawContext.canvas.nativeCanvas.save()
                            drawContext.canvas.nativeCanvas.translate(size.width / 2f, size.height / 2f)
                            drawContext.canvas.nativeCanvas.rotate(rotationDegrees)
                            drawContext.canvas.nativeCanvas.drawText(text, 0f, 8f, wmPaint)
                            drawContext.canvas.nativeCanvas.restore()
                        }
                    }
                }
            }

            // 1. Watermark Text String
            Column(verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)) {
                Text(
                    text = "WATERMARK TEXT",
                    style = DocFlowTokens.captionBoldStyle,
                    color = palette.textSecondary
                )

                OutlinedTextField(
                    value = watermarkText,
                    onValueChange = { watermarkText = it },
                    placeholder = { Text("e.g. CONFIDENTIAL") },
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
                        .testTag("watermark_text_input")
                )

                // Quick preset chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2),
                    verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
                ) {
                    presetTexts.forEach { preset ->
                        FilterChip(
                            selected = watermarkText.equals(preset, ignoreCase = true),
                            onClick = { watermarkText = preset },
                            label = { Text(preset, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DocFlowTokens.AccentBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 2. Opacity Control
            Column(verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OPACITY",
                        style = DocFlowTokens.captionBoldStyle,
                        color = palette.textSecondary
                    )
                    Text(
                        text = "${opacityPercent.toInt()}%",
                        style = DocFlowTokens.bodyStyle,
                        fontWeight = FontWeight.Bold,
                        color = DocFlowTokens.AccentBlue
                    )
                }

                Slider(
                    value = opacityPercent,
                    onValueChange = { opacityPercent = it },
                    valueRange = 5f..100f,
                    steps = 18,
                    colors = SliderDefaults.colors(
                        thumbColor = DocFlowTokens.AccentBlue,
                        activeTrackColor = DocFlowTokens.AccentBlue,
                        inactiveTrackColor = palette.divider
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("watermark_opacity_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                ) {
                    opacityPresets.forEach { (pct, label) ->
                        FilterChip(
                            selected = opacityPercent.toInt() == pct,
                            onClick = { opacityPercent = pct.toFloat() },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DocFlowTokens.AccentBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 3. Rotation Angle
            Column(verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)) {
                Text(
                    text = "ROTATION ANGLE",
                    style = DocFlowTokens.captionBoldStyle,
                    color = palette.textSecondary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2),
                    verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
                ) {
                    rotationAngles.forEach { (angle, label) ->
                        FilterChip(
                            selected = rotationDegrees == angle,
                            onClick = { rotationDegrees = angle },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DocFlowTokens.AccentBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 4. Color Swatches
            Column(verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)) {
                Text(
                    text = "COLOR",
                    style = DocFlowTokens.captionBoldStyle,
                    color = palette.textSecondary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    swatches.forEach { (hex, name) ->
                        val swatchColor = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = colorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) DocFlowTokens.AccentBlue else palette.divider,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = name,
                                    tint = if (hex == "#111111" || hex == "#1F6BFF") Color.White else Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DocFlowTokens.space2))

            // Action Buttons
            DocFlowPrimaryButton(
                text = "Apply Watermark to All Pages",
                onClick = {
                    val finalConfig = PdfWatermarkConfig(
                        enabled = true,
                        text = watermarkText.trim().ifBlank { "CONFIDENTIAL" },
                        opacityPercent = opacityPercent.toInt().coerceIn(5, 100),
                        rotationDegrees = rotationDegrees.toInt(),
                        fontSizeSp = fontSizeSp.toInt().coerceIn(14, 72),
                        colorHex = colorHex,
                        positionIndex = 4, // Center position
                        pageRangeMode = "ALL",
                        customPageRange = "",
                        layerBehindContent = false
                    )
                    onApplyWatermark(finalConfig)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apply_watermark_button")
            )

            DocFlowSecondaryButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
