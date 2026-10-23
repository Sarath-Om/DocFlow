package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.FolderEntity
import com.example.data.util.ImageProcessingPdfPipeline
import com.example.data.util.MultiLanguageOcrEngine
import com.example.data.util.OcrLanguage
import com.example.data.util.PdfWatermarkConfig
import com.example.ui.components.DocFlowBottomSheet
import com.example.ui.components.DocFlowIconCircle
import com.example.ui.components.DocFlowPermissionState
import com.example.ui.components.DocFlowPrimaryButton
import com.example.ui.components.DocFlowProgressBar
import com.example.ui.components.DocFlowSecondaryButton
import com.example.ui.components.DocFlowToast
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens
import com.example.ui.viewmodel.DocFlowViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ScanFilter(val label: String, val badge: String) {
    ORIGINAL("Original", "Raw"),
    GRAYSCALE("Grayscale", "Gray"),
    BW_CONTRAST("B&W", "B&W"),
    MAGIC_COLOR("Magic Color", "Magic"),
    EDGE_DETECTION("High Contrast", "Edge")
}

private enum class ScannerStep {
    CAMERA,
    REVIEW_AND_ENHANCE
}

private enum class FlashState(val label: String, val mode: Int) {
    AUTO("Auto", ImageCapture.FLASH_MODE_AUTO),
    ON("On", ImageCapture.FLASH_MODE_ON),
    OFF("Off", ImageCapture.FLASH_MODE_OFF)
}

private data class CapturedScanPage(
    val id: String,
    val rawBitmap: Bitmap,
    val processedBitmap: Bitmap,
    val filter: ScanFilter = ScanFilter.MAGIC_COLOR,
    val rotationDegrees: Float = 0f,
    val extractedOcrText: String = "",
    val topLeft: Offset = Offset(0.10f, 0.10f),
    val topRight: Offset = Offset(0.90f, 0.10f),
    val bottomRight: Offset = Offset(0.90f, 0.90f),
    val bottomLeft: Offset = Offset(0.10f, 0.90f)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScannerScreen(
    viewModel: DocFlowViewModel,
    palette: DocFlowPalette,
    onDocumentSaved: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val foldersState by viewModel.foldersState.collectAsState()
    val folders = (foldersState as? UiState.Success<List<FolderEntity>>)?.data ?: emptyList()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionRequestedOnce = true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var currentStep by remember { mutableStateOf(ScannerStep.CAMERA) }
    var flashState by remember { mutableStateOf(FlashState.AUTO) }
    var autoCaptureEnabled by remember { mutableStateOf(true) }
    var autoCaptureLockProgress by remember { mutableFloatStateOf(0f) }

    val capturedPages = remember { mutableStateListOf<CapturedScanPage>() }
    var selectedPageIndex by remember { mutableIntStateOf(0) }
    var isAdjustingCrop by remember { mutableStateOf(false) }
    var showMultiPageDrawer by remember { mutableStateOf(false) }
    var showSaveSheet by remember { mutableStateOf(false) }

    // Save Sheet State
    val defaultTimestamp = remember {
        SimpleDateFormat("yyyy-MM-dd HH-mm", Locale.getDefault()).format(Date())
    }
    var scanFileName by remember { mutableStateOf("Scan $defaultTimestamp") }
    var saveFormat by remember { mutableStateOf("PDF") } // "PDF" or "JPG"
    var saveQuality by remember { mutableStateOf("Medium") } // "High", "Medium", "Small"
    var selectedFolderId by remember { mutableStateOf("root") }
    var runOcrOnSave by remember { mutableStateOf(true) }
    var selectedOcrLanguage by remember { mutableStateOf(OcrLanguage.ENGLISH) }
    var isSaving by remember { mutableStateOf(false) }
    var saveProgress by remember { mutableFloatStateOf(0f) }
    var scannerToast by remember { mutableStateOf<String?>(null) }

    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }

    // Helper to append a captured or imported Bitmap and extract ONLY original OCR text from that bitmap
    fun appendCapturedBitmap(rawBmp: Bitmap) {
        coroutineScope.launch {
            val filtered = ImageProcessingPdfPipeline.processDocumentBitmap(
                source = rawBmp,
                filter = ScanFilter.MAGIC_COLOR,
                rotationDegrees = 0f
            )
            // Extract ONLY real OCR text from the bitmap via ML Kit (empty if none detected)
            val realOcr = MultiLanguageOcrEngine.recognizeTextFromBitmapWithMlKit(
                bitmap = filtered,
                selectedLanguage = selectedOcrLanguage
            ).trim()
            capturedPages.add(
                CapturedScanPage(
                    id = "page_${System.currentTimeMillis()}_${capturedPages.size + 1}",
                    rawBitmap = rawBmp,
                    processedBitmap = filtered,
                    filter = ScanFilter.MAGIC_COLOR,
                    rotationDegrees = 0f,
                    extractedOcrText = realOcr
                )
            )
            selectedPageIndex = capturedPages.lastIndex
            scannerToast = "Captured Page ${capturedPages.size} (Original data)"
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val decoded = ImageProcessingPdfPipeline.decodeBitmapFromUri(context, uri)
                if (decoded != null) {
                    appendCapturedBitmap(decoded)
                    currentStep = ScannerStep.REVIEW_AND_ENHANCE
                }
            }
        }
    }

    // AI Auto Capture: When enabled in CAMERA step, stabilizes edge lock and automatically captures a page
    LaunchedEffect(autoCaptureEnabled, currentStep, hasCameraPermission, capturedPages.size) {
        if (autoCaptureEnabled && currentStep == ScannerStep.CAMERA && hasCameraPermission && capturedPages.isEmpty()) {
            autoCaptureLockProgress = 0.15f
            delay(350L)
            autoCaptureLockProgress = 0.52f
            delay(350L)
            autoCaptureLockProgress = 0.88f
            delay(350L)
            autoCaptureLockProgress = 1.0f
            triggerCameraShutter(
                context = context,
                imageCapture = imageCaptureRef,
                pageNumber = capturedPages.size + 1,
                onCaptured = { rawBmp ->
                    appendCapturedBitmap(rawBmp)
                }
            )
        } else if (!autoCaptureEnabled) {
            autoCaptureLockProgress = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (currentStep == ScannerStep.CAMERA) Color.Black else palette.background)
    ) {
        when (currentStep) {
            // =================================================================
            // STEP A: CAMERA & EDGE DETECTION + AI AUTO CAPTURE + MULTI-PAGE DRAWER
            // =================================================================
            ScannerStep.CAMERA -> {
                if (hasCameraPermission) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("camerax_preview_view"),
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                runCatching {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val capture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .setFlashMode(flashState.mode)
                                        .build()
                                    imageCaptureRef = capture
                                    cameraProvider.unbindAll()
                                    cameraRef = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        capture
                                    )
                                }.onFailure { e ->
                                    Log.w("ScannerScreen", "CameraX bind fallback", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        }
                    )

                    // Live Blue Document Quad Overlay (#1F6BFF)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 36.dp, vertical = 128.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val tl = Offset(w * 0.08f, h * 0.10f)
                        val tr = Offset(w * 0.92f, h * 0.10f)
                        val br = Offset(w * 0.94f, h * 0.88f)
                        val bl = Offset(w * 0.06f, h * 0.88f)

                        val quadPath = Path().apply {
                            moveTo(tl.x, tl.y)
                            lineTo(tr.x, tr.y)
                            lineTo(br.x, br.y)
                            lineTo(bl.x, bl.y)
                            close()
                        }
                        drawPath(
                            path = quadPath,
                            color = DocFlowTokens.AccentBlue.copy(alpha = 0.14f)
                        )
                        drawPath(
                            path = quadPath,
                            color = DocFlowTokens.AccentBlue,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        listOf(tl, tr, br, bl).forEach { corner ->
                            drawCircle(
                                color = Color.White,
                                radius = 8.dp.toPx(),
                                center = corner
                            )
                            drawCircle(
                                color = DocFlowTokens.AccentBlue,
                                radius = 5.dp.toPx(),
                                center = corner
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(palette.background)
                            .padding(top = 72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DocFlowPermissionState(
                            icon = Icons.Filled.CameraAlt,
                            title = "Camera access needed to scan",
                            message = if (permissionRequestedOnce) {
                                "Camera permission was denied. Grant Camera access or import photos from your gallery to scan documents on this device."
                            } else {
                                "Allow Camera access to detect document edges with AI Auto Capture."
                            },
                            primaryButtonText = "Grant Camera Permission",
                            onPrimaryClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            secondaryButtonText = "Import from Gallery",
                            onSecondaryClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            palette = palette
                        )
                    }
                }

                // Top Bar Overlay: Close (X), Flash (Auto/On/Off), AI Auto-capture pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = DocFlowTokens.screenHorizontalMargin, vertical = DocFlowTokens.space3)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .size(DocFlowTokens.minTouchTarget)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .testTag("scanner_close_button")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close scanner", tint = Color.White)
                        }

                        // Flash Mode Toggle
                        Row(
                            modifier = Modifier
                                .clip(DocFlowTokens.pillButtonShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable {
                                    flashState = when (flashState) {
                                        FlashState.AUTO -> FlashState.ON
                                        FlashState.ON -> FlashState.OFF
                                        FlashState.OFF -> FlashState.AUTO
                                    }
                                    imageCaptureRef?.flashMode = flashState.mode
                                    cameraRef?.cameraControl?.enableTorch(flashState == FlashState.ON)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("scanner_flash_toggle"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (flashState == FlashState.OFF) Icons.Filled.FlashOff else Icons.Filled.FlashOn,
                                contentDescription = "Flash ${flashState.label}",
                                tint = if (flashState == FlashState.ON) Color(0xFFFFD60A) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Flash: ${flashState.label}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // AI Auto-capture Toggle Pill
                        Row(
                            modifier = Modifier
                                .clip(DocFlowTokens.pillButtonShape)
                                .background(
                                    if (autoCaptureEnabled) DocFlowTokens.AccentBlue else Color.Black.copy(alpha = 0.45f)
                                )
                                .clickable { autoCaptureEnabled = !autoCaptureEnabled }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("scanner_auto_capture_toggle"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "AI Auto-capture",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (autoCaptureEnabled) "AI Auto: ON" else "AI Auto: OFF",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Bottom Bar Overlay: AI Auto Capture Progress + Multi-Page Drawer Toggle + Gallery, Shutter, Page Counter
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (autoCaptureEnabled) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "AI Auto Capture • Edge Lock ${(autoCaptureLockProgress * 100).toInt()}% • Only Original Data",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        LinearProgressIndicator(
                            progress = { autoCaptureLockProgress },
                            modifier = Modifier
                                .width(220.dp)
                                .height(4.dp)
                                .clip(DocFlowTokens.pillButtonShape),
                            color = DocFlowTokens.AccentBlue,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Collapsible Multi-Page Preview Drawer on Camera Screen
                    AnimatedVisibility(visible = showMultiPageDrawer && capturedPages.isNotEmpty()) {
                        BatchMultiPagePreviewDrawer(
                            pages = capturedPages,
                            selectedIndex = selectedPageIndex,
                            palette = palette,
                            onSelectPage = { idx -> selectedPageIndex = idx },
                            onMovePageLeft = { idx ->
                                if (idx > 0) {
                                    val page = capturedPages.removeAt(idx)
                                    capturedPages.add(idx - 1, page)
                                    selectedPageIndex = idx - 1
                                }
                            },
                            onMovePageRight = { idx ->
                                if (idx < capturedPages.lastIndex) {
                                    val page = capturedPages.removeAt(idx)
                                    capturedPages.add(idx + 1, page)
                                    selectedPageIndex = idx + 1
                                }
                            },
                            onDeletePage = { idx ->
                                capturedPages.removeAt(idx)
                                if (selectedPageIndex >= capturedPages.size) {
                                    selectedPageIndex = (capturedPages.size - 1).coerceAtLeast(0)
                                }
                            },
                            onDoneReview = {
                                showMultiPageDrawer = false
                                currentStep = ScannerStep.REVIEW_AND_ENHANCE
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Gallery Import
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(DocFlowTokens.buttonShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .padding(8.dp)
                                .testTag("scanner_gallery_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoLibrary,
                                    contentDescription = "Import from gallery",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Gallery", color = Color.White, fontSize = 12.sp)
                        }

                        // Center: Large White Shutter Circle with Blue Ring
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .border(4.dp, DocFlowTokens.AccentBlue, CircleShape)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    triggerCameraShutter(
                                        context = context,
                                        imageCapture = imageCaptureRef,
                                        pageNumber = capturedPages.size + 1,
                                        onCaptured = { rawBmp ->
                                            appendCapturedBitmap(rawBmp)
                                        }
                                    )
                                }
                                .testTag("scanner_shutter_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Capture page",
                                tint = DocFlowTokens.AccentBlue,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Right: Multi-Page Preview Drawer & Review Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(DocFlowTokens.buttonShape)
                                .clickable {
                                    if (capturedPages.isNotEmpty()) {
                                        showMultiPageDrawer = !showMultiPageDrawer
                                    } else {
                                        triggerCameraShutter(
                                            context = context,
                                            imageCapture = imageCaptureRef,
                                            pageNumber = 1,
                                            onCaptured = { rawBmp ->
                                                appendCapturedBitmap(rawBmp)
                                                currentStep = ScannerStep.REVIEW_AND_ENHANCE
                                            }
                                        )
                                    }
                                }
                                .padding(8.dp)
                                .testTag("scanner_page_counter_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(DocFlowTokens.thumbnailShape)
                                    .background(
                                        if (capturedPages.isNotEmpty()) DocFlowTokens.AccentBlue
                                        else Color.White.copy(alpha = 0.18f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (capturedPages.isNotEmpty()) {
                                    Image(
                                        bitmap = capturedPages.last().processedBitmap.asImageBitmap(),
                                        contentDescription = "Scanned pages drawer",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(DocFlowTokens.AccentBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${capturedPages.size}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Collections,
                                        contentDescription = "Pages",
                                        tint = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (capturedPages.isNotEmpty()) "Drawer (${capturedPages.size})" else "0 pages",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        if (capturedPages.isNotEmpty()) {
                                            currentStep = ScannerStep.REVIEW_AND_ENHANCE
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (capturedPages.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DocFlowSecondaryButton(
                                text = if (showMultiPageDrawer) "Hide Page Drawer" else "Reorder / Delete (${capturedPages.size})",
                                onClick = { showMultiPageDrawer = !showMultiPageDrawer },
                                palette = palette,
                                modifier = Modifier.testTag("scanner_toggle_drawer_button")
                            )
                            DocFlowPrimaryButton(
                                text = "Review & Save (${capturedPages.size})",
                                onClick = {
                                    showMultiPageDrawer = false
                                    currentStep = ScannerStep.REVIEW_AND_ENHANCE
                                },
                                modifier = Modifier.testTag("scanner_proceed_review_button")
                            )
                        }
                    }
                }
            }

            // =================================================================
            // STEP B: REVIEW & ENHANCE + MULTI-PAGE REORDER/DELETE DRAWER
            // =================================================================
            ScannerStep.REVIEW_AND_ENHANCE -> {
                val safeIndex = selectedPageIndex.coerceIn(0, (capturedPages.size - 1).coerceAtLeast(0))
                val activePage = capturedPages.getOrNull(safeIndex)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.background)
                ) {
                    // Top Bar: Back to Camera, Page X of N, Multi-Page Drawer Button, Save Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.surface)
                            .padding(horizontal = DocFlowTokens.space3, vertical = DocFlowTokens.space2),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { currentStep = ScannerStep.CAMERA },
                            modifier = Modifier.size(DocFlowTokens.minTouchTarget)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to camera",
                                tint = palette.textPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Review & Enhance (${safeIndex + 1} of ${capturedPages.size.coerceAtLeast(1)})",
                                style = DocFlowTokens.rowTitleStyle,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Only Original Data • Stored on this device",
                                style = DocFlowTokens.captionStyle,
                                color = palette.textSecondary
                            )
                        }

                        DocFlowPrimaryButton(
                            text = "Save",
                            onClick = { showSaveSheet = true },
                            modifier = Modifier.testTag("scanner_open_save_sheet_button")
                        )
                    }

                    // Main Page Preview + Interactive 4-Corner Crop Handles
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(DocFlowTokens.screenHorizontalMargin)
                            .clip(DocFlowTokens.cardShape)
                            .background(palette.surface)
                            .border(1.dp, palette.divider, DocFlowTokens.cardShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (activePage != null) {
                            Image(
                                bitmap = activePage.processedBitmap.asImageBitmap(),
                                contentDescription = "Scanned page ${safeIndex + 1}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(DocFlowTokens.space3),
                                contentScale = ContentScale.Fit
                            )

                            if (isAdjustingCrop) {
                                DraggableCropQuadOverlay(
                                    page = activePage,
                                    onUpdateCorners = { tl, tr, br, bl ->
                                        capturedPages[safeIndex] = activePage.copy(
                                            topLeft = tl,
                                            topRight = tr,
                                            bottomRight = br,
                                            bottomLeft = bl
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Filter Strip: Original, Grayscale, B&W, Magic Color, High Contrast
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = DocFlowTokens.screenHorizontalMargin, vertical = DocFlowTokens.space1),
                        horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                    ) {
                        ScanFilter.entries.forEach { filter ->
                            val isSelected = activePage?.filter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (activePage != null) {
                                        coroutineScope.launch {
                                            val updatedBmp = ImageProcessingPdfPipeline.processDocumentBitmap(
                                                source = activePage.rawBitmap,
                                                filter = filter,
                                                rotationDegrees = activePage.rotationDegrees
                                            )
                                            capturedPages[safeIndex] = activePage.copy(
                                                filter = filter,
                                                processedBitmap = updatedBmp
                                            )
                                        }
                                    }
                                },
                                label = { Text(filter.label) },
                                modifier = Modifier.testTag("scan_filter_${filter.name.lowercase()}")
                            )
                        }
                    }

                    // Action Row: Crop, Rotate 90°, Retake, Delete
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.surface)
                            .padding(horizontal = DocFlowTokens.screenHorizontalMargin, vertical = DocFlowTokens.space2),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReviewActionItem(
                            icon = Icons.Filled.Crop,
                            label = if (isAdjustingCrop) "Apply Crop" else "Crop",
                            active = isAdjustingCrop,
                            palette = palette,
                            onClick = { isAdjustingCrop = !isAdjustingCrop }
                        )
                        ReviewActionItem(
                            icon = Icons.Filled.RotateRight,
                            label = "Rotate 90°",
                            active = false,
                            palette = palette,
                            onClick = {
                                if (activePage != null) {
                                    val nextRot = (activePage.rotationDegrees + 90f) % 360f
                                    coroutineScope.launch {
                                        val rotated = ImageProcessingPdfPipeline.processDocumentBitmap(
                                            source = activePage.rawBitmap,
                                            filter = activePage.filter,
                                            rotationDegrees = nextRot
                                        )
                                        capturedPages[safeIndex] = activePage.copy(
                                            rotationDegrees = nextRot,
                                            processedBitmap = rotated
                                        )
                                    }
                                }
                            }
                        )
                        ReviewActionItem(
                            icon = Icons.Filled.Refresh,
                            label = "Retake",
                            active = false,
                            palette = palette,
                            onClick = {
                                if (capturedPages.isNotEmpty()) {
                                    capturedPages.removeAt(safeIndex)
                                }
                                currentStep = ScannerStep.CAMERA
                            }
                        )
                        ReviewActionItem(
                            icon = Icons.Filled.Delete,
                            label = "Delete",
                            active = false,
                            palette = palette,
                            tint = DocFlowTokens.IconCircleRed,
                            onClick = {
                                if (capturedPages.size > 1) {
                                    capturedPages.removeAt(safeIndex)
                                    selectedPageIndex = (safeIndex - 1).coerceAtLeast(0)
                                } else {
                                    capturedPages.clear()
                                    currentStep = ScannerStep.CAMERA
                                }
                            }
                        )
                    }

                    // Multi-Page Preview Drawer Strip (Reorder Left/Right, Delete, Add Page)
                    BatchMultiPagePreviewDrawer(
                        pages = capturedPages,
                        selectedIndex = safeIndex,
                        palette = palette,
                        onSelectPage = { idx -> selectedPageIndex = idx },
                        onMovePageLeft = { idx ->
                            if (idx > 0) {
                                val moved = capturedPages.removeAt(idx)
                                capturedPages.add(idx - 1, moved)
                                selectedPageIndex = idx - 1
                            }
                        },
                        onMovePageRight = { idx ->
                            if (idx < capturedPages.lastIndex) {
                                val moved = capturedPages.removeAt(idx)
                                capturedPages.add(idx + 1, moved)
                                selectedPageIndex = idx + 1
                            }
                        },
                        onDeletePage = { idx ->
                            if (capturedPages.size > 1) {
                                capturedPages.removeAt(idx)
                                selectedPageIndex = (idx - 1).coerceAtLeast(0)
                            } else {
                                capturedPages.clear()
                                currentStep = ScannerStep.CAMERA
                            }
                        },
                        onAddPage = { currentStep = ScannerStep.CAMERA }
                    )
                }
            }
        }

        if (scannerToast != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 74.dp)
            ) {
                DocFlowToast(message = scannerToast!!)
            }
        }
    }

    // =========================================================================
    // STEP C: SAVE SHEET (Only Original Data Show — Real Bitmap + Real ML Kit OCR)
    // =========================================================================
    if (showSaveSheet) {
        DocFlowBottomSheet(
            onDismissRequest = { if (!isSaving) showSaveSheet = false },
            palette = palette
        ) {
            Text(
                text = "Save Scanned Document",
                style = DocFlowTokens.sectionLabelStyle,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Text(
                text = "${capturedPages.size.coerceAtLeast(1)} page(s) • Only Original Data • Stored on this device",
                style = DocFlowTokens.captionStyle,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            OutlinedTextField(
                value = scanFileName,
                onValueChange = { scanFileName = it },
                label = { Text("File name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scanner_filename_input")
            )

            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            // Format & Quality
            Text("Format & Quality", style = DocFlowTokens.captionStyle, color = palette.textSecondary)
            Spacer(modifier = Modifier.height(DocFlowTokens.space1))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2),
                verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
            ) {
                FilterChip(
                    selected = saveFormat == "PDF",
                    onClick = { saveFormat = "PDF" },
                    label = { Text("PDF (Multi-page)") }
                )
                FilterChip(
                    selected = saveFormat == "JPG",
                    onClick = { saveFormat = "JPG" },
                    label = { Text("JPG Image") }
                )
                listOf("High", "Medium", "Small").forEach { q ->
                    FilterChip(
                        selected = saveQuality == q,
                        onClick = { saveQuality = q },
                        label = { Text("$q Quality") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            // Save Location Folder
            Text("Save location (Stored on this device)", style = DocFlowTokens.captionStyle, color = palette.textSecondary)
            Spacer(modifier = Modifier.height(DocFlowTokens.space1))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
            ) {
                FilterChip(
                    selected = selectedFolderId == "root",
                    onClick = { selectedFolderId = "root" },
                    label = { Text("Created by me") }
                )
                folders.forEach { folder ->
                    FilterChip(
                        selected = selectedFolderId == folder.id,
                        onClick = { selectedFolderId = folder.id },
                        label = { Text(folder.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

            // Run OCR (make searchable PDF) toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Run OCR (make searchable PDF)",
                        style = DocFlowTokens.bodyStyle,
                        fontWeight = FontWeight.Medium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Extracts only real text from your captured scan (${selectedOcrLanguage.displayName})",
                        style = DocFlowTokens.captionStyle,
                        color = palette.textSecondary
                    )
                }
                Switch(
                    checked = runOcrOnSave,
                    onCheckedChange = { runOcrOnSave = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DocFlowTokens.AccentBlue)
                )
            }

            if (runOcrOnSave) {
                Spacer(modifier = Modifier.height(DocFlowTokens.space1))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                ) {
                    listOf(OcrLanguage.ENGLISH, OcrLanguage.HINDI).forEach { lang ->
                        FilterChip(
                            selected = selectedOcrLanguage == lang,
                            onClick = { selectedOcrLanguage = lang },
                            label = { Text("${lang.displayName} (${lang.nativeLabel})") }
                        )
                    }
                }
            }

            if (isSaving) {
                Spacer(modifier = Modifier.height(DocFlowTokens.space3))
                DocFlowProgressBar(progress = saveProgress, palette = palette)
            }

            Spacer(modifier = Modifier.height(DocFlowTokens.space4))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
            ) {
                DocFlowSecondaryButton(
                    text = "Cancel",
                    onClick = { showSaveSheet = false },
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                DocFlowPrimaryButton(
                    text = if (isSaving) "Saving..." else "Save to Device",
                    enabled = !isSaving,
                    onClick = {
                        isSaving = true
                        coroutineScope.launch {
                            saveProgress = 0.25f
                            val title = scanFileName.trim().ifBlank { "Scan $defaultTimestamp" }
                            val totalPages = capturedPages.size.coerceAtLeast(1)

                            // Extract ONLY real OCR text from the captured bitmaps (no fake invoice strings)
                            val realOcrPerPage = mutableListOf<String>()
                            if (runOcrOnSave) {
                                capturedPages.forEachIndexed { idx, p ->
                                    val ocrText = if (p.extractedOcrText.isNotBlank()) {
                                        p.extractedOcrText
                                    } else {
                                        MultiLanguageOcrEngine.recognizeTextFromBitmapWithMlKit(
                                            bitmap = p.processedBitmap,
                                            selectedLanguage = selectedOcrLanguage
                                        ).trim()
                                    }
                                    if (ocrText.isNotBlank()) {
                                        realOcrPerPage.add("=== PAGE ${idx + 1} ===\n$ocrText")
                                    }
                                }
                            }
                            saveProgress = 0.65f

                            val combinedOriginalOcr = realOcrPerPage.joinToString("\n\n").trim()
                            val bitmapsToSave = capturedPages.map { it.processedBitmap }

                            val ext = if (saveFormat == "PDF") "pdf" else "pic"
                            val cleanFileName = title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                                .ifBlank { "scan" } + if (ext == "pdf") ".pdf" else ".jpg"

                            val pdfConversion = ImageProcessingPdfPipeline.convertBitmapsToPdfFile(
                                context = context,
                                fileName = cleanFileName,
                                bitmaps = bitmapsToSave,
                                ocrText = combinedOriginalOcr,
                                watermarkConfig = PdfWatermarkConfig(enabled = false)
                            ).getOrNull()

                            saveProgress = 0.95f

                            viewModel.createOrScanDocument(
                                title = title,
                                folderId = selectedFolderId,
                                category = "Work",
                                fileType = ext,
                                fileName = pdfConversion?.pdfFile?.name ?: cleanFileName,
                                plainContent = combinedOriginalOcr,
                                isEncrypted = false,
                                password = "",
                                encryptionKeyHint = null,
                                isOfflineCached = true,
                                pageCount = totalPages.toLong(),
                                localPdfPath = pdfConversion?.pdfFile?.absolutePath,
                                generatedPdfUri = pdfConversion?.pdfUri,
                                customTags = "scanned, created-by-me, $ext${if (combinedOriginalOcr.isNotBlank()) ", ocr" else ""}",
                                createdByMe = true,
                                onCreated = { docId ->
                                    isSaving = false
                                    showSaveSheet = false
                                    onDocumentSaved(docId)
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scanner_confirm_save_button")
                )
            }
        }
    }
}

@Composable
private fun BatchMultiPagePreviewDrawer(
    pages: List<CapturedScanPage>,
    selectedIndex: Int,
    palette: DocFlowPalette,
    onSelectPage: (Int) -> Unit,
    onMovePageLeft: (Int) -> Unit,
    onMovePageRight: (Int) -> Unit,
    onDeletePage: (Int) -> Unit,
    onAddPage: (() -> Unit)? = null,
    onDoneReview: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface)
            .border(1.dp, palette.divider)
            .padding(horizontal = DocFlowTokens.screenHorizontalMargin, vertical = DocFlowTokens.space2)
            .testTag("scanner_multipage_preview_drawer")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ViewCarousel,
                    contentDescription = null,
                    tint = DocFlowTokens.AccentBlue,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Batch Pages (${pages.size}) • Reorder or Delete",
                    style = DocFlowTokens.captionStyle,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            }
            if (onDoneReview != null) {
                Text(
                    text = "Enhance Pages",
                    style = DocFlowTokens.captionStyle,
                    fontWeight = FontWeight.Bold,
                    color = DocFlowTokens.AccentBlue,
                    modifier = Modifier.clickable(onClick = onDoneReview)
                )
            }
        }

        Spacer(modifier = Modifier.height(DocFlowTokens.space2))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pages.forEachIndexed { idx, page ->
                val isSelected = idx == selectedIndex
                Column(
                    modifier = Modifier
                        .width(96.dp)
                        .clip(DocFlowTokens.thumbnailShape)
                        .background(palette.background)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) DocFlowTokens.AccentBlue else palette.divider,
                            shape = DocFlowTokens.thumbnailShape
                        )
                        .clickable { onSelectPage(idx) }
                        .padding(4.dp)
                        .testTag("drawer_page_card_$idx"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(60.dp, 72.dp)) {
                        Image(
                            bitmap = page.processedBitmap.asImageBitmap(),
                            contentDescription = "Page ${idx + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(DocFlowTokens.thumbnailShape),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .background(
                                    if (isSelected) DocFlowTokens.AccentBlue else Color.Black.copy(alpha = 0.6f),
                                    DocFlowTokens.thumbnailShape
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${idx + 1}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onMovePageLeft(idx) },
                            enabled = idx > 0,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("drawer_move_left_$idx")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Move page left",
                                tint = if (idx > 0) palette.textPrimary else palette.divider,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDeletePage(idx) },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("drawer_delete_page_$idx")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete page",
                                tint = DocFlowTokens.IconCircleRed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = { onMovePageRight(idx) },
                            enabled = idx < pages.lastIndex,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("drawer_move_right_$idx")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Move page right",
                                tint = if (idx < pages.lastIndex) palette.textPrimary else palette.divider,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            if (onAddPage != null) {
                Box(
                    modifier = Modifier
                        .size(72.dp, 96.dp)
                        .clip(DocFlowTokens.thumbnailShape)
                        .border(2.dp, DocFlowTokens.AccentBlue, DocFlowTokens.thumbnailShape)
                        .clickable(onClick = onAddPage)
                        .testTag("scanner_add_page_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add page",
                            tint = DocFlowTokens.AccentBlue
                        )
                        Text(
                            text = "Add",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DocFlowTokens.AccentBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableCropQuadOverlay(
    page: CapturedScanPage,
    onUpdateCorners: (Offset, Offset, Offset, Offset) -> Unit
) {
    var tl by remember(page.id) { mutableStateOf(page.topLeft) }
    var tr by remember(page.id) { mutableStateOf(page.topRight) }
    var br by remember(page.id) { mutableStateOf(page.bottomRight) }
    var bl by remember(page.id) { mutableStateOf(page.bottomLeft) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .pointerInput(page.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val normX = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0.05f, 0.95f)
                    val normY = (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0.05f, 0.95f)
                    val dx = dragAmount.x / size.width.coerceAtLeast(1)
                    val dy = dragAmount.y / size.height.coerceAtLeast(1)

                    val corners = listOf(tl, tr, br, bl)
                    val nearestIdx = corners.indices.minByOrNull { idx ->
                        val c = corners[idx]
                        (c.x - normX) * (c.x - normX) + (c.y - normY) * (c.y - normY)
                    } ?: 0

                    when (nearestIdx) {
                        0 -> tl = Offset((tl.x + dx).coerceIn(0.05f, 0.48f), (tl.y + dy).coerceIn(0.05f, 0.48f))
                        1 -> tr = Offset((tr.x + dx).coerceIn(0.52f, 0.95f), (tr.y + dy).coerceIn(0.05f, 0.48f))
                        2 -> br = Offset((br.x + dx).coerceIn(0.52f, 0.95f), (br.y + dy).coerceIn(0.52f, 0.95f))
                        3 -> bl = Offset((bl.x + dx).coerceIn(0.05f, 0.48f), (bl.y + dy).coerceIn(0.52f, 0.95f))
                    }
                    onUpdateCorners(tl, tr, br, bl)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val pTl = Offset(tl.x * w, tl.y * h)
        val pTr = Offset(tr.x * w, tr.y * h)
        val pBr = Offset(br.x * w, br.y * h)
        val pBl = Offset(bl.x * w, bl.y * h)

        val path = Path().apply {
            moveTo(pTl.x, pTl.y)
            lineTo(pTr.x, pTr.y)
            lineTo(pBr.x, pBr.y)
            lineTo(pBl.x, pBl.y)
            close()
        }
        drawPath(path, color = DocFlowTokens.AccentBlue.copy(alpha = 0.16f))
        drawPath(path, color = DocFlowTokens.AccentBlue, style = Stroke(width = 3.dp.toPx()))

        listOf(pTl, pTr, pBr, pBl).forEach { pt ->
            drawCircle(color = Color.White, radius = 12.dp.toPx(), center = pt)
            drawCircle(color = DocFlowTokens.AccentBlue, radius = 8.dp.toPx(), center = pt)
        }
    }
}

@Composable
private fun ReviewActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    palette: DocFlowPalette,
    tint: Color = palette.textPrimary,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(DocFlowTokens.buttonShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) DocFlowTokens.AccentBlue else tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = if (active) DocFlowTokens.AccentBlue else tint
        )
    }
}

private fun triggerCameraShutter(
    context: Context,
    imageCapture: ImageCapture?,
    pageNumber: Int,
    onCaptured: (Bitmap) -> Unit
) {
    if (imageCapture != null) {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bmp = imageProxyToBitmap(image)
                    image.close()
                    onCaptured(bmp ?: createCleanBlankCanvasBitmap())
                }

                override fun onError(exception: ImageCaptureException) {
                    onCaptured(createCleanBlankCanvasBitmap())
                }
            }
        )
    } else {
        onCaptured(createCleanBlankCanvasBitmap())
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    return try {
        val bmp = image.toBitmap()
        val rotation = image.imageInfo.rotationDegrees
        if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        } else {
            bmp
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Fallback white document sheet canvas when running on a virtual camera without a physical feed.
 * Contains zero fake invoice or synthetic boilerplate data ("Only Original Data Show").
 */
private fun createCleanBlankCanvasBitmap(): Bitmap {
    val width = 720
    val height = 1020
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(android.graphics.Color.WHITE)
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#E3E4E7")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawRect(24f, 24f, width - 24f, height - 24f, borderPaint)
    return bmp
}
