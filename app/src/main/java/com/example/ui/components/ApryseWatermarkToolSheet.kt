package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DocEntity
import com.example.data.util.ApryseWatermarkOptions
import com.example.data.util.ApryseWatermarkPosition
import com.example.data.util.FileParserUtils
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens
import kotlinx.coroutines.launch

private val WatermarkPresets = listOf(
    "CONFIDENTIAL",
    "DRAFT",
    "DO NOT COPY",
    "URGENT",
    "APPROVED",
    "SAMPLE",
    "INTERNAL ONLY"
)

private val WatermarkColors = listOf(
    Pair("#E53935", "Crimson Red"),
    Pair("#212121", "Dark Charcoal"),
    Pair("#1E88E5", "Ocean Blue"),
    Pair("#2E7D32", "Forest Green"),
    Pair("#F57F17", "Amber Gold"),
    Pair("#757575", "Neutral Gray")
)

private val RotationChips = listOf(
    Pair(-45.0, "-45°"),
    Pair(0.0, "0° (Flat)"),
    Pair(30.0, "+30°"),
    Pair(45.0, "+45°"),
    Pair(90.0, "+90°")
)

private val FontSizes = listOf(
    Pair(24.0, "Small (24pt)"),
    Pair(36.0, "Medium (36pt)"),
    Pair(48.0, "Large (48pt)"),
    Pair(64.0, "Extra (64pt)")
)

/**
 * Dedicated Watermark Tool Sheet for the Tools module using the Apryse SDK.
 * Allows users to choose a document, configure text string, opacity, position preset,
 * rotation, fine-tuning offsets, and layer, and executes programmatically on a background thread.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ApryseWatermarkToolSheet(
    palette: DocFlowPalette,
    availableDocuments: List<DocEntity>,
    initialDocument: DocEntity?,
    onResolveDocUri: (DocEntity) -> Uri?,
    onDismiss: () -> Unit,
    onApplyWatermark: (
        doc: DocEntity?,
        externalUri: Uri?,
        title: String,
        options: ApryseWatermarkOptions,
        onFinished: (Boolean) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Document State
    var selectedDoc by remember {
        mutableStateOf<DocEntity?>(
            initialDocument ?: availableDocuments.firstOrNull { it.fileType.equals("pdf", ignoreCase = true) }
        )
    }
    var selectedExternalUri by remember { mutableStateOf<Uri?>(null) }
    var selectedExternalTitle by remember { mutableStateOf<String?>(null) }
    var showDocPickerList by remember { mutableStateOf(false) }

    // Watermark Configuration State
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var opacityPercent by remember { mutableIntStateOf(30) } // 5..100
    var selectedPosition by remember { mutableStateOf(ApryseWatermarkPosition.CENTER_DIAGONAL) }
    var customRotation by remember { mutableStateOf<Double?>(null) }
    var xOffsetPt by remember { mutableDoubleStateOf(0.0) }
    var yOffsetPt by remember { mutableDoubleStateOf(0.0) }
    var fontSizePt by remember { mutableDoubleStateOf(42.0) }
    var selectedColorHex by remember { mutableStateOf("#E53935") }
    var asBackground by remember { mutableStateOf(false) }

    // Execution & Progress State
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Live page preview thumbnail
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val externalPdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedExternalUri = uri
            selectedDoc = null
            val resolvedName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Document.pdf"
            selectedExternalTitle = resolvedName
            coroutineScope.launch {
                previewBitmap = FileParserUtils.renderPdfPage(context, uri, 0)
            }
        }
    }

    LaunchedEffect(selectedDoc) {
        val doc = selectedDoc
        if (doc != null) {
            selectedExternalUri = null
            selectedExternalTitle = null
            coroutineScope.launch {
                val inputUri = onResolveDocUri(doc)
                if (inputUri != null) {
                    previewBitmap = FileParserUtils.renderPdfPage(context, inputUri, 0)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isProcessing) onDismiss()
        },
        sheetState = sheetState,
        containerColor = palette.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .testTag("apryse_watermark_sheet")
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
                            icon = Icons.Default.BrandingWatermark,
                            circleColor = DocFlowTokens.AccentBlue
                        )
                        Column {
                            Text(
                                text = "Apryse PDF Watermark",
                                style = DocFlowTokens.rowTitleStyle,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Background Thread • Apryse Stamper Engine",
                                style = DocFlowTokens.captionStyle,
                                color = palette.textSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = { if (!isProcessing) onDismiss() },
                        enabled = !isProcessing,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Watermark Tool",
                            tint = palette.textSecondary
                        )
                    }
                }
            }

            // Scrollable Settings Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.space3),
                verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2),
                contentPadding = PaddingValues(top = DocFlowTokens.space2, bottom = DocFlowTokens.space4)
            ) {
                // Section 1: Target Document Selection
                item {
                    DocFlowSectionLabel(text = "Target PDF Document", palette = palette)

                    val docTitle = selectedDoc?.title ?: selectedExternalTitle ?: "No Document Selected"
                    val docSub = selectedDoc?.let { "${it.category} • ${it.fileType.uppercase()}" }
                        ?: (if (selectedExternalUri != null) "External PDF from storage" else "Select a PDF file to watermark")

                    DocFlowCard(
                        palette = palette,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                        ) {
                            DocFlowIconCircle(
                                icon = Icons.Default.Description,
                                circleColor = DocFlowTokens.IconCircleRed
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = docTitle,
                                    style = DocFlowTokens.rowTitleStyle,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = docSub,
                                    style = DocFlowTokens.captionStyle,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(DocFlowTokens.space2))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                        ) {
                            val pdfCount = availableDocuments.count { it.fileType.equals("pdf", ignoreCase = true) }
                            TextButton(
                                onClick = { showDocPickerList = !showDocPickerList },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("watermark_select_from_app_btn")
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (showDocPickerList) "Hide List" else "From App ($pdfCount)")
                            }

                            TextButton(
                                onClick = { externalPdfPicker.launch(arrayOf("application/pdf")) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("watermark_pick_external_btn")
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick PDF File")
                            }
                        }

                        // App Document List (if expanded)
                        AnimatedVisibility(visible = showDocPickerList) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = DocFlowTokens.space2)
                            ) {
                                val pdfs = availableDocuments.filter { it.fileType.equals("pdf", ignoreCase = true) }
                                if (pdfs.isEmpty()) {
                                    Text(
                                        text = "No PDF files stored in DocFlow yet. Pick an external PDF above.",
                                        style = DocFlowTokens.captionStyle,
                                        color = palette.textSecondary,
                                        modifier = Modifier.padding(DocFlowTokens.space2)
                                    )
                                } else {
                                    pdfs.forEach { itemDoc ->
                                        val isSelected = selectedDoc?.id == itemDoc.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(DocFlowTokens.buttonRadius))
                                                .background(if (isSelected) DocFlowTokens.AccentBlue.copy(alpha = 0.1f) else Color.Transparent)
                                                .clickable {
                                                    selectedDoc = itemDoc
                                                    selectedExternalUri = null
                                                    showDocPickerList = false
                                                }
                                                .padding(horizontal = DocFlowTokens.space2, vertical = DocFlowTokens.space1),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = itemDoc.title,
                                                style = DocFlowTokens.bodyMediumStyle,
                                                color = if (isSelected) DocFlowTokens.AccentBlue else palette.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
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

                // Section 2: Watermark Text & Presets
                item {
                    DocFlowSectionLabel(text = "Watermark Text String", palette = palette)
                    DocFlowCard(palette = palette) {
                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = { watermarkText = it },
                            label = { Text("Watermark Text") },
                            placeholder = { Text("e.g. CONFIDENTIAL, DRAFT") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("watermark_text_input"),
                            trailingIcon = {
                                if (watermarkText.isNotEmpty()) {
                                    IconButton(onClick = { watermarkText = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(DocFlowTokens.space2))

                        Text(
                            text = "Quick Presets",
                            style = DocFlowTokens.captionBoldStyle,
                            color = palette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(DocFlowTokens.space1))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space1),
                            verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
                        ) {
                            WatermarkPresets.forEach { preset ->
                                val isSelected = watermarkText.equals(preset, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { watermarkText = preset },
                                    label = { Text(preset, style = DocFlowTokens.captionBoldStyle) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DocFlowTokens.AccentBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 3: Opacity Settings (User Defined)
                item {
                    DocFlowSectionLabel(text = "Opacity Setting ($opacityPercent%)", palette = palette)
                    DocFlowCard(palette = palette) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Watermark Transparency",
                                style = DocFlowTokens.rowTitleStyle,
                                color = palette.textPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(DocFlowTokens.buttonRadius),
                                color = DocFlowTokens.AccentBlue.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "$opacityPercent% Opacity",
                                    style = DocFlowTokens.captionBoldStyle,
                                    color = DocFlowTokens.AccentBlue,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(DocFlowTokens.space1))

                        Slider(
                            value = opacityPercent.toFloat(),
                            onValueChange = { opacityPercent = it.toInt() },
                            valueRange = 5f..100f,
                            steps = 18,
                            colors = SliderDefaults.colors(
                                thumbColor = DocFlowTokens.AccentBlue,
                                activeTrackColor = DocFlowTokens.AccentBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("watermark_opacity_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("5% (Very Subtle)", style = DocFlowTokens.captionStyle, color = palette.textSecondary)
                            Text("50% (Balanced)", style = DocFlowTokens.captionStyle, color = palette.textSecondary)
                            Text("100% (Solid)", style = DocFlowTokens.captionStyle, color = palette.textSecondary)
                        }

                        Spacer(modifier = Modifier.height(DocFlowTokens.space1))

                        // Quick Opacity Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
                        ) {
                            listOf(15 to "15% Subtle", 30 to "30% Standard", 50 to "50% Medium", 80 to "80% Bold").forEach { (valOp, label) ->
                                val isSelected = opacityPercent == valOp
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { opacityPercent = valOp },
                                    label = { Text(label, style = DocFlowTokens.captionStyle) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Section 4: Position Settings (User Defined)
                item {
                    DocFlowSectionLabel(text = "Position & Alignment Settings", palette = palette)
                    DocFlowCard(palette = palette) {
                        Text(
                            text = "Preset Alignment: ${selectedPosition.label}",
                            style = DocFlowTokens.rowTitleStyle,
                            color = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.height(DocFlowTokens.space1))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space1),
                            verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
                        ) {
                            ApryseWatermarkPosition.values().forEach { pos ->
                                val isSelected = selectedPosition == pos
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedPosition = pos
                                        customRotation = null
                                    },
                                    label = { Text(pos.label, style = DocFlowTokens.captionBoldStyle) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DocFlowTokens.AccentBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(DocFlowTokens.space2))

                        // Rotation Angle
                        val currentAngle = customRotation ?: selectedPosition.defaultRotation
                        Text(
                            text = "Rotation Angle: ${currentAngle.toInt()}°",
                            style = DocFlowTokens.captionBoldStyle,
                            color = palette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(DocFlowTokens.space1))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
                        ) {
                            RotationChips.forEach { (angle, label) ->
                                val isSelected = (customRotation ?: selectedPosition.defaultRotation) == angle
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { customRotation = angle },
                                    label = { Text(label, style = DocFlowTokens.captionStyle) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(DocFlowTokens.space2))

                        // X & Y Offset Fine Tuning
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Horizontal Offset: ${xOffsetPt.toInt()} pt",
                                    style = DocFlowTokens.captionStyle,
                                    color = palette.textSecondary
                                )
                                Slider(
                                    value = xOffsetPt.toFloat(),
                                    onValueChange = { xOffsetPt = it.toDouble() },
                                    valueRange = -100f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = DocFlowTokens.AccentBlue,
                                        activeTrackColor = DocFlowTokens.AccentBlue
                                    )
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Vertical Offset: ${yOffsetPt.toInt()} pt",
                                    style = DocFlowTokens.captionStyle,
                                    color = palette.textSecondary
                                )
                                Slider(
                                    value = yOffsetPt.toFloat(),
                                    onValueChange = { yOffsetPt = it.toDouble() },
                                    valueRange = -100f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = DocFlowTokens.AccentBlue,
                                        activeTrackColor = DocFlowTokens.AccentBlue
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 5: Appearance, Color & Layer
                item {
                    DocFlowSectionLabel(text = "Appearance & Color", palette = palette)
                    DocFlowCard(palette = palette) {
                        Text(
                            text = "Watermark Color",
                            style = DocFlowTokens.captionBoldStyle,
                            color = palette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(DocFlowTokens.space1))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WatermarkColors.forEach { (hex, name) ->
                                val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) DocFlowTokens.AccentBlue else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = name,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(DocFlowTokens.space2))

                        // Font Size
                        Text(
                            text = "Font Size: ${fontSizePt.toInt()} pt",
                            style = DocFlowTokens.captionBoldStyle,
                            color = palette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(DocFlowTokens.space1))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
                        ) {
                            FontSizes.forEach { (sz, label) ->
                                val isSelected = fontSizePt == sz
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { fontSizePt = sz },
                                    label = { Text(label, style = DocFlowTokens.captionStyle) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(DocFlowTokens.space2))

                        // Layer Setting: Overlay vs Underlay
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (asBackground) "Background Layer (Under Text)" else "Foreground Overlay (Over Content)",
                                    style = DocFlowTokens.rowTitleStyle,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = if (asBackground) "Renders behind text & imagery" else "Renders on top of document content",
                                    style = DocFlowTokens.captionStyle,
                                    color = palette.textSecondary
                                )
                            }
                            Switch(
                                checked = !asBackground,
                                onCheckedChange = { asBackground = !it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DocFlowTokens.AccentBlue,
                                    checkedTrackColor = DocFlowTokens.AccentBlue.copy(alpha = 0.35f)
                                )
                            )
                        }
                    }
                }

                // Section 6: Live Interactive Preview Box
                item {
                    DocFlowSectionLabel(text = "Live Page Preview", palette = palette)
                    DocFlowCard(
                        palette = palette,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Previewing Page 1 with Apryse Stamper parameters",
                            style = DocFlowTokens.captionStyle,
                            color = palette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(DocFlowTokens.space1))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(DocFlowTokens.buttonRadius))
                                .background(Color.White)
                                .border(1.dp, palette.divider, RoundedCornerShape(DocFlowTokens.buttonRadius)),
                            contentAlignment = Alignment.Center
                        ) {
                            // If we have real PDF page bitmap, render it
                            if (previewBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = previewBitmap!!.asImageBitmap(),
                                    contentDescription = "Page 1 Preview",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // Fallback mock page layout lines
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val lineYSpacing = 18f
                                    var currY = 40f
                                    while (currY < size.height - 30f) {
                                        drawLine(
                                            color = Color(0xFFEEEEEE),
                                            start = Offset(40f, currY),
                                            end = Offset(size.width - 40f, currY),
                                            strokeWidth = 6f
                                        )
                                        currY += lineYSpacing
                                    }
                                }
                            }

                            // Dynamic Watermark Overlay Canvas
                            val activeAngle = (customRotation ?: selectedPosition.defaultRotation).toFloat()
                            val parsedColor = try {
                                Color(android.graphics.Color.parseColor(selectedColorHex))
                            } catch (_: Exception) {
                                Color.Red
                            }
                            val watermarkAlpha = (opacityPercent / 100f).coerceIn(0.05f, 1f)

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val centerX = size.width / 2f + xOffsetPt.toFloat()
                                val centerY = size.height / 2f - yOffsetPt.toFloat() // Invert Y for graphics

                                val (targetX, targetY) = when (selectedPosition) {
                                    ApryseWatermarkPosition.CENTER_DIAGONAL,
                                    ApryseWatermarkPosition.CENTER_HORIZONTAL -> Pair(centerX, centerY)
                                    ApryseWatermarkPosition.TOP_CENTER -> Pair(centerX, 50f - yOffsetPt.toFloat())
                                    ApryseWatermarkPosition.BOTTOM_CENTER -> Pair(centerX, size.height - 50f - yOffsetPt.toFloat())
                                    ApryseWatermarkPosition.TOP_LEFT -> Pair(80f + xOffsetPt.toFloat(), 50f - yOffsetPt.toFloat())
                                    ApryseWatermarkPosition.TOP_RIGHT -> Pair(size.width - 80f + xOffsetPt.toFloat(), 50f - yOffsetPt.toFloat())
                                    ApryseWatermarkPosition.BOTTOM_LEFT -> Pair(80f + xOffsetPt.toFloat(), size.height - 50f - yOffsetPt.toFloat())
                                    ApryseWatermarkPosition.BOTTOM_RIGHT -> Pair(size.width - 80f + xOffsetPt.toFloat(), size.height - 50f - yOffsetPt.toFloat())
                                }

                                rotate(degrees = activeAngle, pivot = Offset(targetX, targetY)) {
                                    val paint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.argb(
                                            (watermarkAlpha * 255).toInt(),
                                            (parsedColor.red * 255).toInt(),
                                            (parsedColor.green * 255).toInt(),
                                            (parsedColor.blue * 255).toInt()
                                        )
                                        textSize = (fontSizePt * 0.75f).toFloat()
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isFakeBoldText = true
                                        isAntiAlias = true
                                    }
                                    drawIntoCanvas { canvas ->
                                        canvas.nativeCanvas.drawText(
                                            watermarkText.ifBlank { "CONFIDENTIAL" },
                                            targetX,
                                            targetY + (paint.textSize / 3),
                                            paint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (statusMessage != null) {
                    item {
                        Surface(
                            color = DocFlowTokens.AccentBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(DocFlowTokens.buttonRadius),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(DocFlowTokens.space2),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = DocFlowTokens.AccentBlue
                                )
                                Text(
                                    text = statusMessage!!,
                                    style = DocFlowTokens.captionBoldStyle,
                                    color = DocFlowTokens.AccentBlue
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Sticky Action Bar
            Surface(
                color = palette.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(DocFlowTokens.space3)
                ) {
                    val hasTarget = selectedDoc != null || selectedExternalUri != null

                    DocFlowPrimaryButton(
                        text = if (isProcessing) "Applying with Apryse SDK on background thread..." else "Apply Watermark (Apryse SDK)",
                        leadingIcon = if (isProcessing) null else Icons.Default.BrandingWatermark,
                        onClick = {
                            if (!hasTarget || isProcessing) return@DocFlowPrimaryButton
                            isProcessing = true
                            statusMessage = "Apryse Stamper processing on Dispatchers.IO background thread..."

                            val options = ApryseWatermarkOptions(
                                text = watermarkText.ifBlank { "CONFIDENTIAL" },
                                opacityPercent = opacityPercent,
                                position = selectedPosition,
                                customRotationDegrees = customRotation,
                                xOffsetPt = xOffsetPt,
                                yOffsetPt = yOffsetPt,
                                fontSizePt = fontSizePt,
                                colorHex = selectedColorHex,
                                asBackground = asBackground
                            )

                            val targetTitle = selectedDoc?.title ?: selectedExternalTitle ?: "Watermarked Document"

                            onApplyWatermark(
                                selectedDoc,
                                selectedExternalUri,
                                targetTitle,
                                options
                            ) { success ->
                                isProcessing = false
                                statusMessage = null
                                if (success) {
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apryse_apply_watermark_button"),
                        enabled = hasTarget && !isProcessing
                    )
                }
            }
        }
    }
}
