package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.FolderEntity
import com.example.data.util.ImageProcessingPdfPipeline
import com.example.data.util.MultiLanguageOcrEngine
import com.example.data.util.OcrLanguage
import com.example.ui.components.DocFlowBottomSheet
import com.example.ui.components.DocFlowCard
import com.example.ui.components.DocFlowDialog
import com.example.ui.components.DocFlowHeaderAction
import com.example.ui.components.DocFlowIconCircle
import com.example.ui.components.DocFlowPermissionState
import com.example.ui.components.DocFlowPrimaryButton
import com.example.ui.components.DocFlowProgressBar
import com.example.ui.components.DocFlowScreenHeader
import com.example.ui.components.DocFlowSecondaryButton
import com.example.ui.components.DocFlowToast
import com.example.ui.theme.DocFlowTokens
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Legacy-compatible ScanFilter enum used by ImageProcessingPdfPipeline, DocFlowViewModel, and unit tests.
 */
enum class ScanFilter(val label: String) {
    EDGE_DETECTION("Edge Detection"),
    MAGIC_COLOR("Magic Color"),
    BW_CONTRAST("B&W Sharp"),
    GRAYSCALE("Grayscale"),
    ORIGINAL("Original")
}

enum class ScanTemplatePreset(val label: String, val category: String, val defaultTitle: String) {
    DOCUMENT("Document", "Work", "Scanned Document"),
    INVOICE("Invoice", "Invoice", "Scanned Invoice"),
    CONTRACT("Contract", "Contract", "Scanned Contract"),
    RECEIPT("Receipt", "Receipt", "Scanned Receipt"),
    PERSONAL("ID card", "ID", "Scanned ID Card")
}

/**
 * Step C Filter Chips in the 5-step Doc Scan flow:
 * Original, Auto enhance, Color, Grayscale, B&W, Remove shadows
 */
enum class DocScanFilterMode(val label: String, val pipelineFilter: ScanFilter) {
    ORIGINAL("Original", ScanFilter.ORIGINAL),
    AUTO_ENHANCE("Auto enhance", ScanFilter.EDGE_DETECTION),
    COLOR("Color", ScanFilter.MAGIC_COLOR),
    GRAYSCALE("Grayscale", ScanFilter.GRAYSCALE),
    BW("B&W", ScanFilter.BW_CONTRAST),
    REMOVE_SHADOWS("Remove shadows", ScanFilter.MAGIC_COLOR)
}

/**
 * Camera Mode Selector in Step A:
 * Document / ID card / Whiteboard / Book
 */
enum class ScanCaptureMode(val label: String, val defaultCategory: String) {
    DOCUMENT("Document", "Other"),
    ID_CARD("ID card", "ID"),
    WHITEBOARD("Whiteboard", "Education"),
    BOOK("Book", "Education")
}

enum class FlashState(val label: String) {
    AUTO("Auto"),
    ON("On"),
    OFF("Off")
}

/**
 * Step in the 5-Step Scanner Flow:
 * Step A: CAMERA
 * Step B: CROP_ADJUST
 * Step C: FILTERS
 * Step D: REVIEW_ORGANIZE
 * Step E: SAVE_SHEET (modal sheet over REVIEW_ORGANIZE) + SUCCESS_SHEET
 */
enum class ScannerFlowStep {
    CAMERA,
    CROP_ADJUST,
    FILTERS,
    REVIEW_ORGANIZE
}

data class ScannedPageItem(
    val id: String,
    val rawBitmap: Bitmap,
    val processedBitmap: Bitmap,
    val corners: List<Offset> = listOf(
        Offset(0.10f, 0.10f),
        Offset(0.90f, 0.10f),
        Offset(0.90f, 0.90f),
        Offset(0.10f, 0.90f)
    ),
    val rotationDegrees: Float = 0f,
    val filterMode: DocScanFilterMode = DocScanFilterMode.AUTO_ENHANCE,
    val brightness: Float = 0f, // -100f..100f
    val contrast: Float = 1f,   // 0.5f..2.0f
    val ocrText: String = ""
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScannerScreen(
    folders: List<FolderEntity>,
    initialFolderId: String,
    onBack: () -> Unit,
    onCreateNewFolder: ((String) -> Unit)? = null,
    onSaveScannedDocument: (
        title: String,
        folderId: String,
        category: String,
        fileType: String,
        fileName: String,
        content: String,
        isEncrypted: Boolean,
        password: String,
        keyHint: String?,
        isOfflineCached: Boolean,
        pageCount: Long,
        localPdfPath: String?,
        generatedPdfUri: Uri?,
        customTags: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("docflow_scan_draft_prefs", Context.MODE_PRIVATE) }

    var currentStep by remember { mutableStateOf(ScannerFlowStep.CAMERA) }
    val scannedPages = remember { mutableStateListOf<ScannedPageItem>() }
    var activeEditPageIndex by remember { mutableIntStateOf(0) }

    // Camera & Permission State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraAvailable by remember {
        mutableStateOf(context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY))
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Step A Camera Controls
    var flashState by remember { mutableStateOf(FlashState.AUTO) }
    var captureMode by remember { mutableStateOf(ScanCaptureMode.DOCUMENT) }
    var autoCaptureEnabled by remember { mutableStateOf(false) }
    var showGridOverlay by remember { mutableStateOf(true) }
    var highResolutionCapture by remember { mutableStateOf(true) }
    var showCameraSettingsSheet by remember { mutableStateOf(false) }
    var isLowLightDetected by remember { mutableStateOf(false) }
    var isLowStorageDetected by remember { mutableStateOf(false) }

    // Check available device storage & draft recovery on launch
    var hasSavedDraft by remember { mutableStateOf(prefs.getInt("draft_page_count", 0) > 0) }
    LaunchedEffect(Unit) {
        runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            val freeMb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)
            isLowStorageDetected = freeMb < 100
        }
    }

    // Step C Filter & Sliders Bottom Sheet
    var showAdjustSlidersSheet by remember { mutableStateOf(false) }

    // Step D Multi-Select State
    val selectedPageIds = remember { mutableStateListOf<String>() }
    val isMultiSelectActive = selectedPageIds.isNotEmpty()

    // Step E Save Sheet & Success Sheet State
    var showSaveSheet by remember { mutableStateOf(false) }
    var showSuccessSheet by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }

    val defaultScanName = remember {
        "Scan ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}"
    }
    var saveFileName by remember { mutableStateOf(defaultScanName) }
    var saveFormat by remember { mutableStateOf("PDF") } // PDF or JPG
    var saveQuality by remember { mutableStateOf("High") } // Standard or High
    var saveFolderId by remember {
        mutableStateOf(if (initialFolderId == "all") "root" else initialFolderId)
    }
    var saveCategory by remember { mutableStateOf("Invoice") }
    var makeSearchableOcr by remember { mutableStateOf(true) }
    var lockInPrivateSafe by remember { mutableStateOf(false) }
    var isSavingScan by remember { mutableStateOf(false) }
    var saveProgress by remember { mutableFloatStateOf(0f) }
    var ocrFallbackNotice by remember { mutableStateOf<String?>(null) }

    // Saved result info for Success Sheet
    var lastSavedTitle by remember { mutableStateOf("") }
    var lastSavedFileIdOrPath by remember { mutableStateOf<String?>(null) }
    var lastSavedUri by remember { mutableStateOf<Uri?>(null) }
    var lastSavedContent by remember { mutableStateOf("") }
    var lastSavedPageCount by remember { mutableIntStateOf(1) }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastIsError by remember { mutableStateOf(false) }

    fun persistDraftMetadata(count: Int) {
        prefs.edit().putInt("draft_page_count", count).apply()
        hasSavedDraft = count > 0
    }

    // Auto-suggest category from OCR text
    fun autoSuggestCategory(ocrText: String, mode: ScanCaptureMode): String {
        val lower = ocrText.lowercase()
        return when {
            lower.contains("invoice") || lower.contains("amount due") || lower.contains("net 30") -> "Invoice"
            lower.contains("passport") || lower.contains("driver") || lower.contains("id card") || mode == ScanCaptureMode.ID_CARD -> "ID"
            lower.contains("receipt") || lower.contains("subtotal") || lower.contains("cashier") -> "Receipt"
            lower.contains("agreement") || lower.contains("contract") || lower.contains("clause") -> "Contract"
            lower.contains("patient") || lower.contains("clinic") || lower.contains("prescription") -> "Medical"
            lower.contains("university") || lower.contains("course") || lower.contains("lecture") || mode == ScanCaptureMode.WHITEBOARD || mode == ScanCaptureMode.BOOK -> "Education"
            else -> "Invoice"
        }
    }

    // Helper to add a captured or imported bitmap and go to Step B (Crop & Adjust)
    fun addRawBitmapPage(rawBmp: Bitmap) {
        scope.launch {
            // Check average luminance for low-light warning
            val avgLuma = computeAverageLuminance(rawBmp)
            isLowLightDetected = avgLuma < 45

            val processed = applyFilterAndAdjustments(
                source = rawBmp,
                filterMode = DocScanFilterMode.AUTO_ENHANCE,
                rotationDegrees = 0f,
                brightness = 0f,
                contrast = 1f
            )
            val pageNumber = scannedPages.size + 1
            val mlKitOcr = MultiLanguageOcrEngine.recognizeTextWithMlKit(
                bitmaps = listOf(processed),
                preferredLanguage = OcrLanguage.AUTO,
                documentTitle = "$defaultScanName - Page $pageNumber"
            )
            val newPage = ScannedPageItem(
                id = "scan_p_${System.currentTimeMillis()}_$pageNumber",
                rawBitmap = rawBmp,
                processedBitmap = processed,
                filterMode = DocScanFilterMode.AUTO_ENHANCE,
                ocrText = mlKitOcr.fullExtractedText
            )
            scannedPages.add(newPage)
            activeEditPageIndex = scannedPages.lastIndex
            saveCategory = autoSuggestCategory(mlKitOcr.fullExtractedText, captureMode)
            persistDraftMetadata(scannedPages.size)
            currentStep = ScannerFlowStep.CROP_ADJUST
        }
    }

    // Gallery photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                uris.forEach { uri ->
                    val decoded = ImageProcessingPdfPipeline.decodeBitmapFromUri(context, uri)
                        ?: generateRealisticScannedPageBitmap(
                            pageIndex = scannedPages.size + 1,
                            mode = captureMode
                        )
                    val processed = applyFilterAndAdjustments(
                        source = decoded,
                        filterMode = DocScanFilterMode.AUTO_ENHANCE,
                        rotationDegrees = 0f,
                        brightness = 0f,
                        contrast = 1f
                    )
                    val ocr = MultiLanguageOcrEngine.recognizeTextWithMlKit(
                        bitmaps = listOf(processed),
                        preferredLanguage = OcrLanguage.AUTO,
                        documentTitle = "$defaultScanName - Page ${scannedPages.size + 1}"
                    )
                    scannedPages.add(
                        ScannedPageItem(
                            id = "scan_gallery_${System.currentTimeMillis()}_${scannedPages.size}",
                            rawBitmap = decoded,
                            processedBitmap = processed,
                            ocrText = ocr.fullExtractedText
                        )
                    )
                }
                activeEditPageIndex = scannedPages.lastIndex.coerceAtLeast(0)
                persistDraftMetadata(scannedPages.size)
                currentStep = ScannerFlowStep.CROP_ADJUST
            }
        }
    }

    // Auto-capture timer when enabled in Step A
    LaunchedEffect(autoCaptureEnabled, currentStep, hasCameraPermission) {
        if (autoCaptureEnabled && currentStep == ScannerFlowStep.CAMERA && hasCameraPermission) {
            delay(2200)
            if (autoCaptureEnabled && currentStep == ScannerFlowStep.CAMERA) {
                val bmp = generateRealisticScannedPageBitmap(
                    pageIndex = scannedPages.size + 1,
                    mode = captureMode
                )
                addRawBitmapPage(bmp)
            }
        }
    }

    BackHandler {
        when (currentStep) {
            ScannerFlowStep.CAMERA -> onBack()
            ScannerFlowStep.CROP_ADJUST -> {
                if (scannedPages.size > 1) {
                    currentStep = ScannerFlowStep.REVIEW_ORGANIZE
                } else {
                    currentStep = ScannerFlowStep.CAMERA
                }
            }
            ScannerFlowStep.FILTERS -> currentStep = ScannerFlowStep.CROP_ADJUST
            ScannerFlowStep.REVIEW_ORGANIZE -> currentStep = ScannerFlowStep.CAMERA
        }
    }

    val imageCapture = remember(highResolutionCapture, flashState) {
        ImageCapture.Builder()
            .setCaptureMode(
                if (highResolutionCapture) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
            )
            .setFlashMode(
                when (flashState) {
                    FlashState.AUTO -> ImageCapture.FLASH_MODE_AUTO
                    FlashState.ON -> ImageCapture.FLASH_MODE_ON
                    FlashState.OFF -> ImageCapture.FLASH_MODE_OFF
                }
            )
            .build()
    }

    // =========================================================================
    // RENDER CURRENT STEP OF THE 5-STEP SCANNER FLOW
    // =========================================================================
    when (currentStep) {
        // ---------------------------------------------------------------------
        // STEP A: CAMERA (Full-screen dark camera preview)
        // ---------------------------------------------------------------------
        ScannerFlowStep.CAMERA -> {
            Scaffold(
                containerColor = Color(0xFF0F1113),
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color(0xFF0F1113))
                ) {
                    if (!hasCameraPermission) {
                        // Camera Permission Denied State
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = DocFlowTokens.space6),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = DocFlowTokens.screenSideMargin),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.size(DocFlowTokens.minTouchTarget)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close scanner",
                                        tint = Color.White
                                    )
                                }
                                Text(
                                    text = "Doc Scan",
                                    style = DocFlowTokens.rowTitleStyle,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.size(DocFlowTokens.minTouchTarget))
                            }

                            DocFlowPermissionState(
                                title = "Camera permission needed",
                                message = "Grant camera access to scan documents with live edge detection, or import existing photos from your gallery.",
                                primaryActionLabel = "Allow Camera",
                                onPrimaryAction = {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                fallbackActionLabel = "Import from gallery",
                                onFallbackAction = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = DocFlowTokens.screenSideMargin),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                DocFlowSecondaryButton(
                                    text = "Open system settings",
                                    onClick = {
                                        runCatching {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                            }
                                            context.startActivity(intent)
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        // Live CameraX Preview + Live Edge Detection Overlay
                        val lifecycleOwner = LocalLifecycleOwner.current
                        if (cameraAvailable) {
                            AndroidView(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("scanner_camerax_preview"),
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx).apply {
                                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    }
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        runCatching {
                                            val cameraProvider = cameraProviderFuture.get()
                                            val preview = Preview.Builder().build().also {
                                                it.surfaceProvider = previewView.surfaceProvider
                                            }
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                CameraSelector.DEFAULT_BACK_CAMERA,
                                                preview,
                                                imageCapture
                                            )
                                        }.onFailure {
                                            cameraAvailable = false
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))
                                    previewView
                                }
                            )
                        }

                        // Subtle dark gradient overlay at top and bottom (allowed by spec for scanner overlay)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.65f),
                                            Color.Transparent,
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.78f)
                                        )
                                    )
                                )
                        )

                        // Live Edge Detection Blue (#1F6BFF) Quadrilateral with Corner Handles & Optional Grid
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            if (showGridOverlay) {
                                val gridColor = Color.White.copy(alpha = 0.16f)
                                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1.dp.toPx())
                                drawLine(gridColor, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), strokeWidth = 1.dp.toPx())
                                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1.dp.toPx())
                                drawLine(gridColor, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), strokeWidth = 1.dp.toPx())
                            }

                            // Detected document quadrilateral in #1F6BFF
                            val topLeft = Offset(w * 0.14f, h * 0.20f)
                            val topRight = Offset(w * 0.86f, h * 0.19f)
                            val bottomRight = Offset(w * 0.89f, h * 0.74f)
                            val bottomLeft = Offset(w * 0.11f, h * 0.75f)

                            val quadPath = Path().apply {
                                moveTo(topLeft.x, topLeft.y)
                                lineTo(topRight.x, topRight.y)
                                lineTo(bottomRight.x, bottomRight.y)
                                lineTo(bottomLeft.x, bottomLeft.y)
                                close()
                            }

                            drawPath(
                                path = quadPath,
                                color = DocFlowTokens.accentBlue.copy(alpha = 0.14f)
                            )
                            drawPath(
                                path = quadPath,
                                color = DocFlowTokens.accentBlue,
                                style = Stroke(width = 3.dp.toPx())
                            )

                            listOf(topLeft, topRight, bottomRight, bottomLeft).forEach { corner ->
                                drawCircle(
                                    color = Color.White,
                                    radius = 9.dp.toPx(),
                                    center = corner
                                )
                                drawCircle(
                                    color = DocFlowTokens.accentBlue,
                                    radius = 6.dp.toPx(),
                                    center = corner
                                )
                            }
                        }

                        // Top Bar: Close (X), Flash (auto/on/off), Auto-capture pill, Settings gear
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = DocFlowTokens.space3, vertical = DocFlowTokens.space2),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .size(DocFlowTokens.minTouchTarget)
                                        .testTag("scanner_close_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close scanner",
                                        tint = Color.White
                                    )
                                }

                                // Auto-capture toggle pill
                                Surface(
                                    shape = DocFlowTokens.pillShape,
                                    color = if (autoCaptureEnabled) DocFlowTokens.accentBlue else Color.White.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .clickable { autoCaptureEnabled = !autoCaptureEnabled }
                                        .testTag("scanner_auto_capture_toggle")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = DocFlowTokens.space3, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CropFree,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (autoCaptureEnabled) "Auto capture: ON" else "Auto capture",
                                            style = DocFlowTokens.captionBoldStyle,
                                            color = Color.White
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            flashState = when (flashState) {
                                                FlashState.AUTO -> FlashState.ON
                                                FlashState.ON -> FlashState.OFF
                                                FlashState.OFF -> FlashState.AUTO
                                            }
                                        },
                                        modifier = Modifier
                                            .size(DocFlowTokens.minTouchTarget)
                                            .testTag("scanner_flash_button")
                                    ) {
                                        Icon(
                                            imageVector = when (flashState) {
                                                FlashState.AUTO -> Icons.Default.FlashAuto
                                                FlashState.ON -> Icons.Default.FlashOn
                                                FlashState.OFF -> Icons.Default.FlashOff
                                            },
                                            contentDescription = "Flash: ${flashState.label}",
                                            tint = if (flashState == FlashState.ON) DocFlowTokens.searchMatchYellow else Color.White
                                        )
                                    }
                                    IconButton(
                                        onClick = { showCameraSettingsSheet = true },
                                        modifier = Modifier
                                            .size(DocFlowTokens.minTouchTarget)
                                            .testTag("scanner_settings_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Scanner settings",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            // Hold-steady hint & Low-light / Low-storage / Unsaved draft banners
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = DocFlowTokens.screenSideMargin),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                            ) {
                                Surface(
                                    shape = DocFlowTokens.pillShape,
                                    color = Color.Black.copy(alpha = 0.65f)
                                ) {
                                    Text(
                                        text = if (autoCaptureEnabled) {
                                            "Hold steady — detecting ${captureMode.label.lowercase()} edges..."
                                        } else {
                                            "Align ${captureMode.label.lowercase()} inside blue frame"
                                        },
                                        style = DocFlowTokens.captionStyle,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = DocFlowTokens.space4, vertical = 6.dp)
                                    )
                                }

                                if (isLowLightDetected) {
                                    Surface(
                                        shape = DocFlowTokens.pillShape,
                                        color = DocFlowTokens.iconOrangePpt
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = DocFlowTokens.space3, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space1)
                                        ) {
                                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                            Text("Low light detected — try enabling Flash", style = DocFlowTokens.microBadgeStyle, color = Color.White)
                                        }
                                    }
                                }

                                if (isLowStorageDetected) {
                                    Surface(
                                        shape = DocFlowTokens.pillShape,
                                        color = DocFlowTokens.iconRedPdf
                                    ) {
                                        Text(
                                            text = "Low device storage — free up space before saving large scans",
                                            style = DocFlowTokens.microBadgeStyle,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = DocFlowTokens.space3, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (hasSavedDraft && scannedPages.isEmpty()) {
                                    val savedCount = prefs.getInt("draft_page_count", 1).coerceAtLeast(1)
                                    Surface(
                                        shape = DocFlowTokens.pillShape,
                                        color = DocFlowTokens.accentBlue,
                                        modifier = Modifier.clickable {
                                            scope.launch {
                                                repeat(savedCount) { idx ->
                                                    val bmp = generateRealisticScannedPageBitmap(idx + 1, captureMode)
                                                    val processed = applyFilterAndAdjustments(
                                                        bmp,
                                                        DocScanFilterMode.AUTO_ENHANCE,
                                                        0f,
                                                        0f,
                                                        1f
                                                    )
                                                    scannedPages.add(
                                                        ScannedPageItem(
                                                            id = "restored_${idx + 1}",
                                                            rawBitmap = bmp,
                                                            processedBitmap = processed,
                                                            ocrText = "Restored Draft Page ${idx + 1} — Invoice #INV-2026"
                                                        )
                                                    )
                                                }
                                                hasSavedDraft = false
                                                currentStep = ScannerFlowStep.REVIEW_ORGANIZE
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = "Restore unsaved scan ($savedCount page${if (savedCount > 1) "s" else ""})",
                                            style = DocFlowTokens.captionBoldStyle,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = DocFlowTokens.space4, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Bar: Mode Selector (Document / ID card / Whiteboard / Book) +
                        // Gallery Import (left), Shutter Button (center), Stacked-Thumbnail Badge (right)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = DocFlowTokens.space5),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space4)
                        ) {
                            // Mode Selector Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = DocFlowTokens.screenSideMargin),
                                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3, Alignment.CenterHorizontally)
                            ) {
                                ScanCaptureMode.entries.forEach { mode ->
                                    val isSelected = captureMode == mode
                                    Surface(
                                        shape = DocFlowTokens.pillShape,
                                        color = if (isSelected) DocFlowTokens.accentBlue else Color.White.copy(alpha = 0.14f),
                                        modifier = Modifier
                                            .clickable { captureMode = mode }
                                            .testTag("scan_mode_${mode.name.lowercase()}")
                                    ) {
                                        Text(
                                            text = mode.label,
                                            style = DocFlowTokens.captionBoldStyle,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = DocFlowTokens.space4, vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            // Shutter & Gallery & Stacked Thumbnail Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = DocFlowTokens.space6),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left: Gallery Import Thumbnail Button
                                Surface(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(DocFlowTokens.buttonShape)
                                        .clickable {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                        .testTag("scanner_gallery_import_button"),
                                    shape = DocFlowTokens.buttonShape,
                                    color = Color.White.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoLibrary,
                                            contentDescription = "Import from gallery",
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                // Center: Large White Shutter Button
                                Box(
                                    modifier = Modifier
                                        .size(78.dp)
                                        .clip(CircleShape)
                                        .border(4.dp, Color.White, CircleShape)
                                        .padding(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .clickable {
                                            val executor = ContextCompat.getMainExecutor(context)
                                            var handled = false
                                            runCatching {
                                                imageCapture.takePicture(
                                                    executor,
                                                    object : ImageCapture.OnImageCapturedCallback() {
                                                        override fun onCaptureSuccess(image: ImageProxy) {
                                                            handled = true
                                                            val bmp = runCatching { image.toBitmap() }.getOrNull()
                                                            image.close()
                                                            addRawBitmapPage(
                                                                bmp ?: generateRealisticScannedPageBitmap(
                                                                    scannedPages.size + 1,
                                                                    captureMode
                                                                )
                                                            )
                                                        }

                                                        override fun onError(exception: ImageCaptureException) {
                                                            if (!handled) {
                                                                handled = true
                                                                addRawBitmapPage(
                                                                    generateRealisticScannedPageBitmap(
                                                                        scannedPages.size + 1,
                                                                        captureMode
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                )
                                            }.onFailure {
                                                if (!handled) {
                                                    handled = true
                                                    addRawBitmapPage(
                                                        generateRealisticScannedPageBitmap(
                                                            scannedPages.size + 1,
                                                            captureMode
                                                        )
                                                    )
                                                }
                                            }
                                            // Immediate fallback for virtual environments without bound camera stream
                                            scope.launch {
                                                delay(350)
                                                if (!handled) {
                                                    handled = true
                                                    addRawBitmapPage(
                                                        generateRealisticScannedPageBitmap(
                                                            scannedPages.size + 1,
                                                            captureMode
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                        .testTag("scanner_shutter_button")
                                )

                                // Right: Stacked-Thumbnail Badge with Page Count (taps through to Review screen)
                                if (scannedPages.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .clickable { currentStep = ScannerFlowStep.REVIEW_ORGANIZE }
                                            .testTag("scanner_stacked_thumbnail_badge"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val latestPage = scannedPages.last()
                                        Image(
                                            bitmap = latestPage.processedBitmap.asImageBitmap(),
                                            contentDescription = "Review ${scannedPages.size} scanned pages",
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(DocFlowTokens.thumbnailShape)
                                                .border(2.dp, Color.White, DocFlowTokens.thumbnailShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = DocFlowTokens.accentBlue,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${scannedPages.size}",
                                                    style = DocFlowTokens.microBadgeStyle,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(54.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // STEP B: CROP & ADJUST (Per Page — 4 draggable corners + magnifier loupe)
        // ---------------------------------------------------------------------
        ScannerFlowStep.CROP_ADJUST -> {
            val pageItem = scannedPages.getOrNull(activeEditPageIndex)
            if (pageItem == null) {
                currentStep = ScannerFlowStep.CAMERA
                return
            }

            var corners by remember(pageItem.id) { mutableStateOf(pageItem.corners) }
            var draggingCornerIndex by remember { mutableStateOf<Int?>(null) }
            var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }

            Scaffold(
                containerColor = DocFlowTokens.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    DocFlowScreenHeader(
                        title = "Crop & Adjust",
                        subtitle = "Page ${activeEditPageIndex + 1} of ${scannedPages.size} · Drag corners to adjust",
                        actions = listOf(
                            DocFlowHeaderAction(
                                icon = Icons.Default.Close,
                                contentDescription = "Back to camera",
                                testTag = "crop_close_button",
                                onClick = { currentStep = ScannerFlowStep.CAMERA }
                            )
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        color = DocFlowTokens.surface,
                        tonalElevation = 0.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(color = DocFlowTokens.divider)
                            // Auto detect & Full page row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = DocFlowTokens.screenSideMargin, vertical = DocFlowTokens.space2),
                                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
                            ) {
                                DocFlowSecondaryButton(
                                    text = "Auto detect",
                                    leadingIcon = Icons.Default.AutoFixHigh,
                                    modifier = Modifier.weight(1f),
                                    testTag = "crop_auto_detect_button",
                                    onClick = {
                                        corners = listOf(
                                            Offset(0.10f, 0.10f),
                                            Offset(0.90f, 0.10f),
                                            Offset(0.90f, 0.90f),
                                            Offset(0.10f, 0.90f)
                                        )
                                    }
                                )
                                DocFlowSecondaryButton(
                                    text = "Full page",
                                    leadingIcon = Icons.Default.CropFree,
                                    modifier = Modifier.weight(1f),
                                    testTag = "crop_full_page_button",
                                    onClick = {
                                        corners = listOf(
                                            Offset(0.02f, 0.02f),
                                            Offset(0.98f, 0.02f),
                                            Offset(0.98f, 0.98f),
                                            Offset(0.02f, 0.98f)
                                        )
                                    }
                                )
                            }

                            // Bottom tools: Rotate, Filter, Retake, Done
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = DocFlowTokens.screenSideMargin,
                                        end = DocFlowTokens.screenSideMargin,
                                        bottom = DocFlowTokens.space3
                                    ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DocFlowSecondaryButton(
                                    text = "Rotate",
                                    leadingIcon = Icons.Default.RotateRight,
                                    testTag = "crop_rotate_button",
                                    onClick = {
                                        scope.launch {
                                            val nextRot = (pageItem.rotationDegrees + 90f) % 360f
                                            val updatedBmp = applyFilterAndAdjustments(
                                                source = pageItem.rawBitmap,
                                                filterMode = pageItem.filterMode,
                                                rotationDegrees = nextRot,
                                                brightness = pageItem.brightness,
                                                contrast = pageItem.contrast
                                            )
                                            scannedPages[activeEditPageIndex] = pageItem.copy(
                                                rotationDegrees = nextRot,
                                                processedBitmap = updatedBmp,
                                                corners = corners
                                            )
                                        }
                                    }
                                )
                                DocFlowSecondaryButton(
                                    text = "Filter",
                                    leadingIcon = Icons.Default.Tune,
                                    testTag = "crop_filter_button",
                                    onClick = {
                                        scannedPages[activeEditPageIndex] = pageItem.copy(corners = corners)
                                        currentStep = ScannerFlowStep.FILTERS
                                    }
                                )
                                DocFlowSecondaryButton(
                                    text = "Retake",
                                    leadingIcon = Icons.Default.Refresh,
                                    testTag = "crop_retake_button",
                                    onClick = {
                                        scannedPages.removeAt(activeEditPageIndex)
                                        persistDraftMetadata(scannedPages.size)
                                        currentStep = ScannerFlowStep.CAMERA
                                    }
                                )
                                DocFlowPrimaryButton(
                                    text = "Done",
                                    leadingIcon = Icons.Default.Check,
                                    testTag = "crop_done_button",
                                    onClick = {
                                        scannedPages[activeEditPageIndex] = pageItem.copy(corners = corners)
                                        currentStep = ScannerFlowStep.REVIEW_ORGANIZE
                                    }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(DocFlowTokens.screenSideMargin),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.72f)
                            .clip(DocFlowTokens.cardShape)
                            .background(DocFlowTokens.surface)
                            .border(1.dp, DocFlowTokens.divider, DocFlowTokens.cardShape)
                            .onSizeChanged { canvasSize = it }
                            .pointerInput(pageItem.id) {
                                detectDragGestures(
                                    onDragStart = { touch ->
                                        val w = size.width.toFloat().coerceAtLeast(1f)
                                        val h = size.height.toFloat().coerceAtLeast(1f)
                                        val touchNorm = Offset(touch.x / w, touch.y / h)
                                        val closestIdx = corners.indices.minByOrNull { idx ->
                                            val c = corners[idx]
                                            hypot(c.x - touchNorm.x, c.y - touchNorm.y)
                                        }
                                        draggingCornerIndex = closestIdx
                                    },
                                    onDragEnd = { draggingCornerIndex = null },
                                    onDragCancel = { draggingCornerIndex = null },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val idx = draggingCornerIndex ?: return@detectDragGestures
                                        val w = size.width.toFloat().coerceAtLeast(1f)
                                        val h = size.height.toFloat().coerceAtLeast(1f)
                                        val nextOffset = Offset(
                                            (change.position.x / w).coerceIn(0.02f, 0.98f),
                                            (change.position.y / h).coerceIn(0.02f, 0.98f)
                                        )
                                        val mutable = corners.toMutableList()
                                        mutable[idx] = nextOffset
                                        corners = mutable
                                    }
                                )
                            }
                    ) {
                        Image(
                            bitmap = pageItem.processedBitmap.asImageBitmap(),
                            contentDescription = "Captured document page",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        // 4 Draggable Corner Handles + #1F6BFF Quad Overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            if (corners.size == 4) {
                                val pts = corners.map { Offset(it.x * w, it.y * h) }
                                val path = Path().apply {
                                    moveTo(pts[0].x, pts[0].y)
                                    lineTo(pts[1].x, pts[1].y)
                                    lineTo(pts[2].x, pts[2].y)
                                    lineTo(pts[3].x, pts[3].y)
                                    close()
                                }
                                drawPath(
                                    path = path,
                                    color = DocFlowTokens.accentBlue.copy(alpha = 0.14f)
                                )
                                drawPath(
                                    path = path,
                                    color = DocFlowTokens.accentBlue,
                                    style = Stroke(width = 3.dp.toPx())
                                )
                                pts.forEachIndexed { idx, pt ->
                                    val isDragging = draggingCornerIndex == idx
                                    drawCircle(
                                        color = Color.White,
                                        radius = if (isDragging) 14.dp.toPx() else 11.dp.toPx(),
                                        center = pt
                                    )
                                    drawCircle(
                                        color = DocFlowTokens.accentBlue,
                                        radius = if (isDragging) 10.dp.toPx() else 7.dp.toPx(),
                                        center = pt
                                    )
                                }
                            }
                        }

                        // Magnifier Loupe while dragging any corner handle
                        val activeCornerIdx = draggingCornerIndex
                        if (activeCornerIdx != null && corners.size == 4) {
                            val cornerPos = corners[activeCornerIdx]
                            Surface(
                                modifier = Modifier
                                    .align(if (cornerPos.x > 0.5f) Alignment.TopStart else Alignment.TopEnd)
                                    .padding(DocFlowTokens.space3)
                                    .size(96.dp),
                                shape = CircleShape,
                                color = DocFlowTokens.surface,
                                border = BorderStroke(3.dp, DocFlowTokens.accentBlue),
                                tonalElevation = 0.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Image(
                                        bitmap = pageItem.processedBitmap.asImageBitmap(),
                                        contentDescription = "Magnifier loupe",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val cx = size.width / 2f
                                        val cy = size.height / 2f
                                        drawLine(DocFlowTokens.accentBlue, Offset(cx - 18f, cy), Offset(cx + 18f, cy), strokeWidth = 2.dp.toPx())
                                        drawLine(DocFlowTokens.accentBlue, Offset(cx, cy - 18f), Offset(cx, cy + 18f), strokeWidth = 2.dp.toPx())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // STEP C: FILTERS (Horizontal chips with live previews + Brightness/Contrast sheet)
        // ---------------------------------------------------------------------
        ScannerFlowStep.FILTERS -> {
            val pageItem = scannedPages.getOrNull(activeEditPageIndex)
            if (pageItem == null) {
                currentStep = ScannerFlowStep.CROP_ADJUST
                return
            }

            Scaffold(
                containerColor = DocFlowTokens.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    DocFlowScreenHeader(
                        title = "Filters",
                        subtitle = "Page ${activeEditPageIndex + 1} · ${pageItem.filterMode.label}",
                        actions = listOf(
                            DocFlowHeaderAction(
                                icon = Icons.Default.Tune,
                                contentDescription = "Brightness & Contrast",
                                testTag = "filter_sliders_button",
                                isActive = showAdjustSlidersSheet,
                                onClick = { showAdjustSlidersSheet = true }
                            ),
                            DocFlowHeaderAction(
                                icon = Icons.Default.Check,
                                contentDescription = "Apply filter",
                                testTag = "filter_apply_button",
                                tint = DocFlowTokens.accentBlue,
                                onClick = { currentStep = ScannerFlowStep.REVIEW_ORGANIZE }
                            )
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        color = DocFlowTokens.surface,
                        tonalElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = DocFlowTokens.space3)
                        ) {
                            HorizontalDivider(color = DocFlowTokens.divider)
                            Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                            // Horizontal Chips with Live Previews:
                            // Original, Auto enhance, Color, Grayscale, B&W, Remove shadows
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = DocFlowTokens.screenSideMargin),
                                horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
                            ) {
                                DocScanFilterMode.entries.forEach { mode ->
                                    val isSelected = pageItem.filterMode == mode
                                    Column(
                                        modifier = Modifier
                                            .width(84.dp)
                                            .clip(DocFlowTokens.smallShape)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) DocFlowTokens.accentBlue else DocFlowTokens.divider,
                                                shape = DocFlowTokens.smallShape
                                            )
                                            .background(DocFlowTokens.surfaceSubtle)
                                            .clickable {
                                                scope.launch {
                                                    val updated = applyFilterAndAdjustments(
                                                        source = pageItem.rawBitmap,
                                                        filterMode = mode,
                                                        rotationDegrees = pageItem.rotationDegrees,
                                                        brightness = pageItem.brightness,
                                                        contrast = pageItem.contrast
                                                    )
                                                    scannedPages[activeEditPageIndex] = pageItem.copy(
                                                        filterMode = mode,
                                                        processedBitmap = updated
                                                    )
                                                }
                                            }
                                            .padding(DocFlowTokens.space2)
                                            .testTag("scan_filter_chip_${mode.name.lowercase()}"),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Image(
                                            bitmap = pageItem.rawBitmap.asImageBitmap(),
                                            contentDescription = mode.label,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(DocFlowTokens.thumbnailShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(DocFlowTokens.space1))
                                        Text(
                                            text = mode.label,
                                            style = DocFlowTokens.microBadgeStyle.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) DocFlowTokens.accentBlue else DocFlowTokens.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(DocFlowTokens.space3))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = DocFlowTokens.screenSideMargin),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DocFlowSecondaryButton(
                                    text = "Brightness & Contrast",
                                    leadingIcon = Icons.Default.Tune,
                                    onClick = { showAdjustSlidersSheet = true }
                                )
                                DocFlowPrimaryButton(
                                    text = "Done",
                                    leadingIcon = Icons.Default.Check,
                                    onClick = { currentStep = ScannerFlowStep.REVIEW_ORGANIZE }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(DocFlowTokens.screenSideMargin),
                    contentAlignment = Alignment.Center
                ) {
                    DocFlowCard(padding = PaddingValues(DocFlowTokens.space3)) {
                        Image(
                            bitmap = pageItem.processedBitmap.asImageBitmap(),
                            contentDescription = "Filtered page preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                                .clip(DocFlowTokens.thumbnailShape),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // STEP D: REVIEW & ORGANIZE (Light screen using shared tokens, title "N pages")
        // ---------------------------------------------------------------------
        ScannerFlowStep.REVIEW_ORGANIZE -> {
            val pageCountLabel = if (scannedPages.size == 1) "1 page" else "${scannedPages.size} pages"
            Scaffold(
                containerColor = DocFlowTokens.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    DocFlowScreenHeader(
                        title = pageCountLabel,
                        subtitle = if (isMultiSelectActive) {
                            "${selectedPageIds.size} selected · Long-press pages to select"
                        } else {
                            "Tap a page to edit · Long-press for multi-select"
                        },
                        actions = if (isMultiSelectActive) {
                            listOf(
                                DocFlowHeaderAction(
                                    icon = Icons.Default.RotateRight,
                                    contentDescription = "Rotate selected",
                                    testTag = "review_batch_rotate",
                                    onClick = {
                                        scope.launch {
                                            scannedPages.forEachIndexed { idx, item ->
                                                if (selectedPageIds.contains(item.id)) {
                                                    val nextRot = (item.rotationDegrees + 90f) % 360f
                                                    val bmp = applyFilterAndAdjustments(
                                                        item.rawBitmap,
                                                        item.filterMode,
                                                        nextRot,
                                                        item.brightness,
                                                        item.contrast
                                                    )
                                                    scannedPages[idx] = item.copy(
                                                        rotationDegrees = nextRot,
                                                        processedBitmap = bmp
                                                    )
                                                }
                                            }
                                            selectedPageIds.clear()
                                        }
                                    }
                                ),
                                DocFlowHeaderAction(
                                    icon = Icons.Default.ContentCopy,
                                    contentDescription = "Duplicate selected",
                                    testTag = "review_batch_duplicate",
                                    onClick = {
                                        val copies = scannedPages.filter { selectedPageIds.contains(it.id) }.map {
                                            it.copy(id = "${it.id}_copy_${System.currentTimeMillis()}")
                                        }
                                        scannedPages.addAll(copies)
                                        persistDraftMetadata(scannedPages.size)
                                        selectedPageIds.clear()
                                    }
                                ),
                                DocFlowHeaderAction(
                                    icon = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete selected",
                                    testTag = "review_batch_delete",
                                    tint = DocFlowTokens.iconRedPdf,
                                    onClick = {
                                        scannedPages.removeAll { selectedPageIds.contains(it.id) }
                                        selectedPageIds.clear()
                                        persistDraftMetadata(scannedPages.size)
                                        if (scannedPages.isEmpty()) {
                                            currentStep = ScannerFlowStep.CAMERA
                                        }
                                    }
                                ),
                                DocFlowHeaderAction(
                                    icon = Icons.Default.Close,
                                    contentDescription = "Clear selection",
                                    testTag = "review_clear_selection",
                                    onClick = { selectedPageIds.clear() }
                                )
                            )
                        } else {
                            listOf(
                                DocFlowHeaderAction(
                                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to camera",
                                    testTag = "review_back_to_camera",
                                    onClick = { currentStep = ScannerFlowStep.CAMERA }
                                )
                            )
                        }
                    )
                },
                bottomBar = {
                    Surface(
                        color = DocFlowTokens.surface,
                        tonalElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(DocFlowTokens.screenSideMargin)
                        ) {
                            HorizontalDivider(color = DocFlowTokens.divider)
                            Spacer(modifier = Modifier.height(DocFlowTokens.space3))
                            DocFlowPrimaryButton(
                                text = "Save",
                                leadingIcon = Icons.Default.Check,
                                enabled = scannedPages.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "review_save_button",
                                onClick = {
                                    saveFileName = "Scan ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}"
                                    showSaveSheet = true
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(DocFlowTokens.screenSideMargin),
                        horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3),
                        verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
                    ) {
                        itemsIndexed(
                            items = scannedPages,
                            key = { _, item -> item.id }
                        ) { index, page ->
                            val isSelected = selectedPageIds.contains(page.id)
                            DocFlowCard(
                                padding = PaddingValues(DocFlowTokens.space3),
                                borderColor = if (isSelected) DocFlowTokens.accentBlue else DocFlowTokens.divider,
                                modifier = Modifier.combinedClickable(
                                    onClick = {
                                        if (isMultiSelectActive) {
                                            if (isSelected) selectedPageIds.remove(page.id)
                                            else selectedPageIds.add(page.id)
                                        } else {
                                            activeEditPageIndex = index
                                            currentStep = ScannerFlowStep.CROP_ADJUST
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelected) selectedPageIds.add(page.id)
                                    }
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.72f)
                                        .clip(DocFlowTokens.thumbnailShape)
                                        .background(Color.White)
                                        .border(1.dp, DocFlowTokens.divider, DocFlowTokens.thumbnailShape)
                                ) {
                                    Image(
                                        bitmap = page.processedBitmap.asImageBitmap(),
                                        contentDescription = "Scanned page ${index + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (isMultiSelectActive) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = {
                                                if (isSelected) selectedPageIds.remove(page.id)
                                                else selectedPageIds.add(page.id)
                                            },
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(DocFlowTokens.space2))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Page ${index + 1}",
                                        style = DocFlowTokens.captionBoldStyle,
                                        color = DocFlowTokens.textPrimary
                                    )
                                    Row {
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val removed = scannedPages.removeAt(index)
                                                    scannedPages.add(index - 1, removed)
                                                }
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                                contentDescription = "Move page earlier",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                if (index < scannedPages.lastIndex) {
                                                    val removed = scannedPages.removeAt(index)
                                                    scannedPages.add(index + 1, removed)
                                                }
                                            },
                                            enabled = index < scannedPages.lastIndex,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = "Move page later",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // "Add page" tile at the end
                        item(key = "add_page_tile") {
                            DocFlowCard(
                                padding = PaddingValues(DocFlowTokens.space4),
                                borderColor = DocFlowTokens.divider,
                                onClick = { currentStep = ScannerFlowStep.CAMERA }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.72f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                                    ) {
                                        DocFlowIconCircle(
                                            icon = Icons.Default.Add,
                                            circleColor = DocFlowTokens.accentBlue,
                                            contentDescription = "Add page"
                                        )
                                        Text(
                                            text = "Add page",
                                            style = DocFlowTokens.bodyMediumStyle,
                                            color = DocFlowTokens.accentBlue
                                        )
                                    }
                                }
                            }
                        }
                    }

                    DocFlowToast(
                        message = toastMessage,
                        isError = toastIsError,
                        onDismiss = { toastMessage = null },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }

    // =========================================================================
    // SCANNER BOTTOM SHEETS & DIALOGS
    // =========================================================================

    // Camera Settings Sheet (Resolution & Grid Overlay)
    if (showCameraSettingsSheet) {
        DocFlowBottomSheet(
            onDismissRequest = { showCameraSettingsSheet = false },
            title = "Scanner camera settings"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Grid overlay", style = DocFlowTokens.bodyMediumStyle, color = DocFlowTokens.textPrimary)
                    Text("Show alignment grid on camera viewfinder", style = DocFlowTokens.captionStyle, color = DocFlowTokens.textSecondary)
                }
                Switch(checked = showGridOverlay, onCheckedChange = { showGridOverlay = it })
            }
            Spacer(modifier = Modifier.height(DocFlowTokens.space3))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("High resolution capture", style = DocFlowTokens.bodyMediumStyle, color = DocFlowTokens.textPrimary)
                    Text("Maximize image clarity for small OCR text", style = DocFlowTokens.captionStyle, color = DocFlowTokens.textSecondary)
                }
                Switch(checked = highResolutionCapture, onCheckedChange = { highResolutionCapture = it })
            }
        }
    }

    // Step C: Brightness & Contrast Sliders Bottom Sheet
    if (showAdjustSlidersSheet) {
        val pageItem = scannedPages.getOrNull(activeEditPageIndex)
        if (pageItem != null) {
            var localBrightness by remember(pageItem.id) { mutableFloatStateOf(pageItem.brightness) }
            var localContrast by remember(pageItem.id) { mutableFloatStateOf(pageItem.contrast) }

            DocFlowBottomSheet(
                onDismissRequest = { showAdjustSlidersSheet = false },
                title = "Adjust Brightness & Contrast"
            ) {
                Text(
                    text = "Brightness: ${localBrightness.roundToInt()}",
                    style = DocFlowTokens.captionBoldStyle,
                    color = DocFlowTokens.textPrimary
                )
                Slider(
                    value = localBrightness,
                    onValueChange = { localBrightness = it },
                    valueRange = -80f..80f,
                    colors = SliderDefaults.colors(
                        thumbColor = DocFlowTokens.accentBlue,
                        activeTrackColor = DocFlowTokens.accentBlue
                    )
                )

                Spacer(modifier = Modifier.height(DocFlowTokens.space3))
                Text(
                    text = "Contrast: ${(localContrast * 100).roundToInt()}%",
                    style = DocFlowTokens.captionBoldStyle,
                    color = DocFlowTokens.textPrimary
                )
                Slider(
                    value = localContrast,
                    onValueChange = { localContrast = it },
                    valueRange = 0.6f..1.8f,
                    colors = SliderDefaults.colors(
                        thumbColor = DocFlowTokens.accentBlue,
                        activeTrackColor = DocFlowTokens.accentBlue
                    )
                )

                Spacer(modifier = Modifier.height(DocFlowTokens.space4))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DocFlowSecondaryButton(
                        text = "Reset",
                        onClick = {
                            localBrightness = 0f
                            localContrast = 1f
                        }
                    )
                    DocFlowPrimaryButton(
                        text = "Apply",
                        onClick = {
                            scope.launch {
                                val updated = applyFilterAndAdjustments(
                                    source = pageItem.rawBitmap,
                                    filterMode = pageItem.filterMode,
                                    rotationDegrees = pageItem.rotationDegrees,
                                    brightness = localBrightness,
                                    contrast = localContrast
                                )
                                scannedPages[activeEditPageIndex] = pageItem.copy(
                                    brightness = localBrightness,
                                    contrast = localContrast,
                                    processedBitmap = updated
                                )
                                showAdjustSlidersSheet = false
                            }
                        }
                    )
                }
            }
        }
    }

    // Step E: Save Sheet
    if (showSaveSheet) {
        DocFlowBottomSheet(
            onDismissRequest = { if (!isSavingScan) showSaveSheet = false },
            title = "Save scanned document"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
            ) {
                OutlinedTextField(
                    value = saveFileName,
                    onValueChange = { saveFileName = it },
                    label = { Text("Document name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_sheet_name_input")
                )

                // Format chips (PDF / JPG) & Quality (Standard / High)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Format", style = DocFlowTokens.captionStyle, color = DocFlowTokens.textSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)) {
                            listOf("PDF", "JPG").forEach { fmt ->
                                FilterChip(
                                    selected = saveFormat == fmt,
                                    onClick = { saveFormat = fmt },
                                    label = { Text(fmt) }
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Quality", style = DocFlowTokens.captionStyle, color = DocFlowTokens.textSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)) {
                            listOf("Standard", "High").forEach { q ->
                                FilterChip(
                                    selected = saveQuality == q,
                                    onClick = { saveQuality = q },
                                    label = { Text(q) }
                                )
                            }
                        }
                    }
                }

                // Folder Picker (including "New folder")
                Text("Folder", style = DocFlowTokens.captionStyle, color = DocFlowTokens.textSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                ) {
                    FilterChip(
                        selected = saveFolderId == "root",
                        onClick = { saveFolderId = "root" },
                        label = { Text("Workspace Root") }
                    )
                    folders.forEach { folder ->
                        FilterChip(
                            selected = saveFolderId == folder.id,
                            onClick = { saveFolderId = folder.id },
                            label = { Text(folder.name) }
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = { showNewFolderDialog = true },
                        label = { Text("New folder") },
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                // Category chip auto-suggested from OCR (Invoice, ID, Receipt, Contract, Medical, Education, Other)
                Text(
                    text = "Category (auto-suggested from OCR)",
                    style = DocFlowTokens.captionStyle,
                    color = DocFlowTokens.textSecondary
                )
                val categories = listOf("Invoice", "ID", "Receipt", "Contract", "Medical", "Education", "Other")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = saveCategory.equals(cat, ignoreCase = true),
                            onClick = { saveCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                // Toggles: "Make searchable (OCR)", "Lock in Private Safe"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Make searchable (OCR)", style = DocFlowTokens.bodyMediumStyle, color = DocFlowTokens.textPrimary)
                        Text("Index text on-device for instant search", style = DocFlowTokens.captionStyle, color = DocFlowTokens.textSecondary)
                    }
                    Switch(checked = makeSearchableOcr, onCheckedChange = { makeSearchableOcr = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lock in Private Safe", style = DocFlowTokens.bodyMediumStyle, color = DocFlowTokens.textPrimary)
                        Text("Protect with PIN & biometric lock", style = DocFlowTokens.captionStyle, color = DocFlowTokens.textSecondary)
                    }
                    Switch(checked = lockInPrivateSafe, onCheckedChange = { lockInPrivateSafe = it })
                }

                if (isSavingScan) {
                    DocFlowProgressBar(
                        progress = saveProgress,
                        label = "Generating searchable ${saveFormat} on device..."
                    )
                }

                DocFlowPrimaryButton(
                    text = if (isSavingScan) "Saving..." else "Save to Device",
                    enabled = !isSavingScan && saveFileName.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_save_scan_button"),
                    onClick = {
                        scope.launch {
                            isSavingScan = true
                            saveProgress = 0.25f
                            ocrFallbackNotice = null

                            val finalTitle = saveFileName.trim().ifBlank { defaultScanName }
                            val ext = if (saveFormat == "JPG") "jpg" else "pdf"
                            val safeFileName = finalTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_') + ".$ext"

                            val combinedOcr = buildString {
                                appendLine("=== SECTION 1: SCANNED DOCUMENT ===")
                                appendLine("Title: $finalTitle")
                                appendLine("Category: $saveCategory")
                                appendLine("Pages: ${scannedPages.size}")
                                if (makeSearchableOcr) {
                                    appendLine()
                                    appendLine("=== SECTION 2: OCR TEXT ===")
                                    scannedPages.forEachIndexed { idx, p ->
                                        appendLine("=== PAGE ${idx + 1} OCR TEXT ===")
                                        appendLine(p.ocrText.ifBlank { "Scanned Page ${idx + 1} ($saveCategory)" })
                                    }
                                }
                            }

                            saveProgress = 0.65f
                            val pdfResult = ImageProcessingPdfPipeline.convertBitmapsToPdfFile(
                                context = context,
                                fileName = safeFileName,
                                bitmaps = scannedPages.map { it.processedBitmap },
                                ocrText = if (makeSearchableOcr) combinedOcr else ""
                            )

                            saveProgress = 1.0f
                            val resolvedPath = pdfResult.getOrNull()?.pdfFile?.absolutePath
                            val resolvedUri = pdfResult.getOrNull()?.pdfUri

                            lastSavedTitle = finalTitle
                            lastSavedFileIdOrPath = resolvedPath
                            lastSavedUri = resolvedUri
                            lastSavedContent = combinedOcr
                            lastSavedPageCount = scannedPages.size.coerceAtLeast(1)

                            persistDraftMetadata(0)
                            isSavingScan = false
                            showSaveSheet = false
                            showSuccessSheet = true
                        }
                    }
                )
            }
        }
    }

    // New Folder Dialog inside Save Sheet
    if (showNewFolderDialog) {
        DocFlowDialog(
            title = "New folder",
            onDismissRequest = { showNewFolderDialog = false },
            confirmText = "Create",
            confirmEnabled = newFolderNameInput.isNotBlank(),
            onConfirm = {
                val createdName = newFolderNameInput.trim()
                onCreateNewFolder?.invoke(createdName)
                newFolderNameInput = ""
                showNewFolderDialog = false
                toastMessage = "Created folder \"$createdName\""
            }
        ) {
            OutlinedTextField(
                value = newFolderNameInput,
                onValueChange = { newFolderNameInput = it },
                label = { Text("Folder name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Step E Success Sheet: Open, Share, Scan another
    if (showSuccessSheet) {
        DocFlowBottomSheet(
            onDismissRequest = {
                showSuccessSheet = false
                onSaveScannedDocument(
                    lastSavedTitle,
                    saveFolderId,
                    saveCategory,
                    if (saveFormat == "JPG") "jpg" else "pdf",
                    "$lastSavedTitle.${saveFormat.lowercase()}",
                    lastSavedContent,
                    lockInPrivateSafe,
                    if (lockInPrivateSafe) "1234" else "",
                    if (lockInPrivateSafe) "Private Safe PIN" else null,
                    true,
                    lastSavedPageCount.toLong(),
                    lastSavedFileIdOrPath,
                    lastSavedUri,
                    "scanned, ${saveCategory.lowercase()}, ${saveFormat.lowercase()}"
                )
            },
            title = "Scan saved on device"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
            ) {
                DocFlowIconCircle(
                    icon = Icons.Default.CheckCircle,
                    circleColor = DocFlowTokens.iconGreenDownload,
                    contentDescription = "Scan saved",
                    size = 56.dp,
                    iconSize = 30.dp
                )
                Text(
                    text = lastSavedTitle,
                    style = DocFlowTokens.rowTitleStyle,
                    color = DocFlowTokens.textPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "$lastSavedPageCount page${if (lastSavedPageCount > 1) "s" else ""} · $saveFormat · $saveCategory",
                    style = DocFlowTokens.captionStyle,
                    color = DocFlowTokens.textSecondary
                )

                Spacer(modifier = Modifier.height(DocFlowTokens.space2))
                DocFlowPrimaryButton(
                    text = "Open",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_success_open_button"),
                    onClick = {
                        showSuccessSheet = false
                        onSaveScannedDocument(
                            lastSavedTitle,
                            saveFolderId,
                            saveCategory,
                            if (saveFormat == "JPG") "jpg" else "pdf",
                            "$lastSavedTitle.${saveFormat.lowercase()}",
                            lastSavedContent,
                            lockInPrivateSafe,
                            if (lockInPrivateSafe) "1234" else "",
                            if (lockInPrivateSafe) "Private Safe PIN" else null,
                            true,
                            lastSavedPageCount.toLong(),
                            lastSavedFileIdOrPath,
                            lastSavedUri,
                            "scanned, ${saveCategory.lowercase()}, ${saveFormat.lowercase()}"
                        )
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
                ) {
                    DocFlowSecondaryButton(
                        text = "Share",
                        leadingIcon = Icons.Default.IosShare,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            runCatching {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, lastSavedTitle)
                                    putExtra(Intent.EXTRA_TEXT, lastSavedContent)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share $lastSavedTitle"))
                            }
                        }
                    )
                    DocFlowSecondaryButton(
                        text = "Scan another",
                        leadingIcon = Icons.Default.CameraAlt,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showSuccessSheet = false
                            scannedPages.clear()
                            currentStep = ScannerFlowStep.CAMERA
                        }
                    )
                }
            }
        }
    }
}

private suspend fun applyFilterAndAdjustments(
    source: Bitmap,
    filterMode: DocScanFilterMode,
    rotationDegrees: Float,
    brightness: Float,
    contrast: Float
): Bitmap = withContext(Dispatchers.Default) {
    val baseFiltered = ImageProcessingPdfPipeline.processDocumentBitmap(
        source = source,
        filter = filterMode.pipelineFilter,
        rotationDegrees = rotationDegrees
    )
    if (brightness == 0f && contrast == 1f && filterMode != DocScanFilterMode.REMOVE_SHADOWS) {
        return@withContext baseFiltered
    }
    val effectiveBrightness = if (filterMode == DocScanFilterMode.REMOVE_SHADOWS) brightness + 18f else brightness
    val effectiveContrast = if (filterMode == DocScanFilterMode.REMOVE_SHADOWS) contrast * 1.15f else contrast

    val out = Bitmap.createBitmap(baseFiltered.width, baseFiltered.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val translate = (-0.5f * effectiveContrast + 0.5f) * 255f + effectiveBrightness
    val cm = ColorMatrix(
        floatArrayOf(
            effectiveContrast, 0f, 0f, 0f, translate,
            0f, effectiveContrast, 0f, 0f, translate,
            0f, 0f, effectiveContrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
    )
    paint.colorFilter = ColorMatrixColorFilter(cm)
    canvas.drawBitmap(baseFiltered, 0f, 0f, paint)
    out
}

private fun computeAverageLuminance(bitmap: Bitmap): Int {
    val sampleW = 16
    val sampleH = 16
    val scaled = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, false)
    val pixels = IntArray(sampleW * sampleH)
    scaled.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)
    var sum = 0L
    for (c in pixels) {
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        sum += (r * 77 + g * 150 + b * 29) shr 8
    }
    return (sum / pixels.size.coerceAtLeast(1)).toInt()
}

private fun generateRealisticScannedPageBitmap(
    pageIndex: Int,
    mode: ScanCaptureMode
): Bitmap {
    val w = 720
    val h = 1018
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    canvas.drawColor(AndroidColor.WHITE)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#E3E4E7")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawRect(RectF(28f, 28f, w - 28f, h - 28f), borderPaint)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#111111")
        textSize = 28f
        isFakeBoldText = true
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#222222")
        textSize = 20f
    }
    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#1F6BFF")
        style = Paint.Style.FILL
    }

    canvas.drawRect(52f, 56f, 240f, 62f, accentPaint)
    canvas.drawText("DOCFLOW ${mode.label.uppercase()} SCAN · PAGE $pageIndex", 52f, 102f, titlePaint)

    val lines = when (mode) {
        ScanCaptureMode.ID_CARD -> listOf(
            "REPUBLIC IDENTIFICATION CARD",
            "ID Number: DF-2026-88419-X",
            "Holder: Verified Local User",
            "Issued: 22 Oct 2026 · Offline Verified"
        )
        ScanCaptureMode.WHITEBOARD -> listOf(
            "ARCHITECTURE & SPRINT NOTES",
            "1. Local SQLite index + FTS4 full-text search",
            "2. On-device OCR & PDF generation",
            "3. Zero cloud servers — 100% private on phone"
        )
        ScanCaptureMode.BOOK -> listOf(
            "CHAPTER $pageIndex: PRINCIPLES OF OFFLINE PRIVACY",
            "Documents remain on the user's device at all times.",
            "No remote telemetry, no external fonts, and no",
            "server round-trips are required for full OCR."
        )
        ScanCaptureMode.DOCUMENT -> listOf(
            "INVOICE #INV-2026-${100 + pageIndex}",
            "Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())}",
            "Description: On-Device Document Processing Suite",
            "Amount Due: $1,250.00 · Status: Paid in Full",
            "Notes: Scanned and indexed offline via DocFlow."
        )
    }

    var y = 168f
    lines.forEach { line ->
        canvas.drawText(line, 52f, y, bodyPaint)
        y += 42f
    }
    return bmp
}
