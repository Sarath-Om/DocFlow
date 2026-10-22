package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.FolderEntity
import com.example.data.util.DocToPicConverter
import com.example.data.util.ImageProcessingPdfPipeline
import com.example.data.util.MultiLanguageOcrEngine
import com.example.data.util.OcrLanguage
import com.example.ui.components.CrystalGlassBackground
import com.example.ui.components.CrystalGlassCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

enum class ScanFilter(val label: String) {
    EDGE_DETECTION("Edge Detection"),
    MAGIC_COLOR("Magic Color"),
    BW_CONTRAST("B&W Sharp"),
    GRAYSCALE("Grayscale"),
    ORIGINAL("Original")
}

enum class ScanTemplatePreset(val label: String, val category: String, val defaultTitle: String) {
    DOCUMENT(
        label = "General Document",
        category = "Work",
        defaultTitle = "Scanned Document"
    ),
    INVOICE(
        label = "Invoice",
        category = "Invoices",
        defaultTitle = "Scanned Invoice"
    ),
    CONTRACT(
        label = "Contract",
        category = "Contracts",
        defaultTitle = "Scanned Contract"
    ),
    RECEIPT(
        label = "Receipt",
        category = "Receipts",
        defaultTitle = "Scanned Receipt"
    ),
    PERSONAL(
        label = "Personal ID / Record",
        category = "Personal",
        defaultTitle = "Scanned Personal Record"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    folders: List<FolderEntity>,
    initialFolderId: String,
    onBack: () -> Unit,
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
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    var isCompiledReviewMode by remember { mutableStateOf(false) }
    var isProcessingPipeline by remember { mutableStateOf(false) }
    var captureStatusMessage by remember { mutableStateOf<String?>(null) }
    var selectedPreset by remember { mutableStateOf(ScanTemplatePreset.DOCUMENT) }
    var selectedFilter by remember { mutableStateOf(ScanFilter.EDGE_DETECTION) }
    var selectedLanguage by remember { mutableStateOf(OcrLanguage.AUTO) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }

    // Consecutive Multi-Page CameraX Queue (Real Captured / Imported Bitmaps)
    val capturedRawBitmaps = remember { mutableStateListOf<Bitmap>() }
    val processedPageBitmaps = remember { mutableStateListOf<Bitmap>() }

    var generatedPdfPath by remember { mutableStateOf<String?>(null) }
    var generatedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var generatedPdfSize by remember { mutableStateOf(0L) }

    var docTitle by remember { mutableStateOf("") }
    var extractedText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(selectedPreset.category) }
    var selectedFolderId by remember {
        mutableStateOf(if (initialFolderId == "all") "root" else initialFolderId)
    }
    var customTagsInput by remember { mutableStateOf("scanned, camerax, edge-detected, pdf") }
    var outputFormat by remember { mutableStateOf("pdf") }
    var isEncrypted by remember { mutableStateOf(false) }
    var encryptionPassword by remember { mutableStateOf("") }
    var encryptionHint by remember { mutableStateOf("") }
    var isOfflineCached by remember { mutableStateOf(true) }

    val detectedLanguageResult = remember(extractedText, selectedLanguage, docTitle) {
        MultiLanguageOcrEngine.detectLanguage(
            text = extractedText.ifBlank { docTitle },
            preferredLanguage = selectedLanguage
        )
    }

    /**
     * Appends one newly captured CameraX page to the consecutive multi-page queue,
     * processing it through the selected edge-detection / enhancement filter.
     */
    fun appendConsecutiveCapturedPage(cameraBmp: Bitmap) {
        scope.launch {
            isProcessingPipeline = true
            captureStatusMessage = null
            capturedRawBitmaps.add(cameraBmp)
            val filteredBitmap = ImageProcessingPdfPipeline.processDocumentBitmap(
                source = cameraBmp,
                filter = selectedFilter,
                rotationDegrees = rotationDegrees
            )
            processedPageBitmaps.add(filteredBitmap)
            if (docTitle.isBlank()) {
                val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
                docTitle = "${selectedPreset.defaultTitle} ($ts)"
            }
            isProcessingPipeline = false
        }
    }

    /**
     * Compiles all consecutively captured pages through the edge-detection filter
     * into a single multipage PDF file in local storage for Room persistence.
     */
    fun compileConsecutivePagesToSinglePdf(
        filter: ScanFilter = selectedFilter,
        rotDegrees: Float = rotationDegrees
    ) {
        if (capturedRawBitmaps.isEmpty()) {
            captureStatusMessage = "Capture at least one page with the camera or select photos from your device first."
            return
        }
        scope.launch {
            isProcessingPipeline = true
            captureStatusMessage = null
            val totalPages = capturedRawBitmaps.size
            processedPageBitmaps.clear()

            val effectiveTitle = docTitle.ifBlank {
                "${selectedPreset.defaultTitle} ($totalPages Page${if (totalPages > 1) "s" else ""})"
            }
            docTitle = effectiveTitle

            for (idx in 0 until totalPages) {
                val rawBmp = capturedRawBitmaps[idx]
                val baseSheet = ImageProcessingPdfPipeline.createHighResDocumentBitmap(
                    cameraBitmap = rawBmp,
                    title = "$effectiveTitle — Page ${idx + 1}",
                    ocrText = extractedText,
                    filter = filter,
                    pageIndex = idx + 1,
                    totalPages = totalPages
                )
                val finalBitmap = if (rotDegrees % 360f != 0f) {
                    ImageProcessingPdfPipeline.processDocumentBitmap(baseSheet, ScanFilter.ORIGINAL, rotDegrees)
                } else {
                    baseSheet
                }
                processedPageBitmaps.add(finalBitmap)
            }

            val cleanName = effectiveTitle.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .ifBlank { "compiled_multi_page_scan" } + ".pdf"

            val pdfRes = ImageProcessingPdfPipeline.convertBitmapsToPdfFile(
                context = context,
                fileName = cleanName,
                bitmaps = processedPageBitmaps.toList()
            )
            if (pdfRes.isSuccess) {
                val res = pdfRes.getOrThrow()
                generatedPdfPath = res.pdfFile.absolutePath
                generatedPdfUri = res.pdfUri
                generatedPdfSize = res.fileSize
            }
            isProcessingPipeline = false
            isCompiledReviewMode = true
        }
    }

    fun triggerCameraXPageCapture(compileImmediatelyAfter: Boolean) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            captureStatusMessage = "Camera permission is required to capture a live scan, or use Batch Photos to import real images."
            return
        }
        isProcessingPipeline = true
        captureStatusMessage = null
        try {
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bmp = try {
                            image.toBitmap()
                        } catch (_: Exception) {
                            null
                        } finally {
                            image.close()
                        }
                        if (bmp != null) {
                            if (compileImmediatelyAfter) {
                                capturedRawBitmaps.add(bmp)
                                compileConsecutivePagesToSinglePdf()
                            } else {
                                appendConsecutiveCapturedPage(bmp)
                            }
                        } else {
                            isProcessingPipeline = false
                            captureStatusMessage = "Could not decode camera frame. Please try again or import from Batch Photos."
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        isProcessingPipeline = false
                        captureStatusMessage = "Camera capture error: ${exception.localizedMessage ?: "Camera unavailable"}"
                    }
                }
            )
        } catch (e: Exception) {
            isProcessingPipeline = false
            captureStatusMessage = "Camera not ready: ${e.localizedMessage ?: "Use Batch Photos if no hardware camera is attached"}"
        }
    }

    val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isProcessingPipeline = true
                captureStatusMessage = null
                uris.forEach { uri ->
                    val galleryBmp = ImageProcessingPdfPipeline.decodeBitmapFromUri(context, uri)
                    if (galleryBmp != null) {
                        capturedRawBitmaps.add(galleryBmp)
                    }
                }
                if (capturedRawBitmaps.isNotEmpty()) {
                    if (docTitle.isBlank()) {
                        docTitle = "${selectedPreset.defaultTitle} (${capturedRawBitmaps.size} Pages)"
                    }
                    compileConsecutivePagesToSinglePdf()
                } else {
                    isProcessingPipeline = false
                    captureStatusMessage = "Could not decode selected image(s)."
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val categories = listOf("Work", "Personal", "Finance", "Contracts", "Invoices", "Receipts", "Legal", "Medical", "Notes")
    val activePageCount = processedPageBitmaps.size.coerceAtLeast(1)

    CrystalGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = {
                        Column {
                            Text(
                                text = if (!isCompiledReviewMode) {
                                    "CameraX Multi-Page Scanner & OCR"
                                } else {
                                    "Compiled Multi-Page PDF & Multi-Language OCR"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (!isCompiledReviewMode) {
                                    "${processedPageBitmaps.size} page(s) in queue • Consecutive CameraX capture"
                                } else {
                                    "$activePageCount page(s) compiled into single PDF • ${detectedLanguageResult.detectedLanguage.displayName} OCR"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("scanner_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!isCompiledReviewMode) {
                    // Live CameraX Viewfinder Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(310.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF090D16))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (hasCameraPermission) {
                                CameraXCapturePreview(
                                    imageCapture = imageCapture,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            ScannerOverlayCanvas(
                                preset = selectedPreset,
                                filter = selectedFilter
                            )

                            Surface(
                                color = Color.Black.copy(alpha = 0.68f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DocumentScanner,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "CameraX Consecutive Multi-Page Mode • ${selectedLanguage.displayName}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            Surface(
                                color = Color(0xFF0066FF),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "${processedPageBitmaps.size} Captured Page(s)",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (!captureStatusMessage.isNullOrBlank()) {
                        CrystalGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 14.dp,
                            tintColor = Color(0xFFF59E0B)
                        ) {
                            Text(
                                text = captureStatusMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Consecutive Multi-Page Thumbnail Queue Strip
                    if (processedPageBitmaps.isNotEmpty()) {
                        CrystalGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 18.dp,
                            tintColor = Color(0xFF0066FF)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Consecutive Multi-Page Queue (${processedPageBitmaps.size} Pages Ready for Single PDF)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    OutlinedButton(
                                        onClick = { compileConsecutivePagesToSinglePdf() },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("compile_multi_page_pdf_button")
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Compile Single PDF (${processedPageBitmaps.size}p)")
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    processedPageBitmaps.forEachIndexed { idx, bmp ->
                                        Box(
                                            modifier = Modifier
                                                .width(96.dp)
                                                .height(130.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, Color(0xFF0066FF), RoundedCornerShape(10.dp))
                                        ) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "Queued Page ${idx + 1}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            Surface(
                                                color = Color.Black.copy(alpha = 0.72f),
                                                shape = RoundedCornerShape(bottomEnd = 8.dp),
                                                modifier = Modifier.align(Alignment.TopStart)
                                            ) {
                                                Text(
                                                    text = "P${idx + 1}",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (idx < capturedRawBitmaps.size) capturedRawBitmaps.removeAt(idx)
                                                    processedPageBitmaps.removeAt(idx)
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove Page",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Multi-Language OCR Selector with Auto Language Detection
                    Text(
                        text = "Multi-Language OCR & Automatic Language Detection:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OcrLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = selectedLanguage == lang,
                                onClick = {
                                    selectedLanguage = lang
                                },
                                label = { Text("${lang.displayName} (${lang.nativeLabel})") },
                                leadingIcon = if (selectedLanguage == lang) {
                                    { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.testTag("ocr_lang_${lang.code}")
                            )
                        }
                    }

                    // Preset physical document selector
                    Text(
                        text = "Select Document Preset / OCR Target:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScanTemplatePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = selectedPreset == preset,
                                onClick = {
                                    selectedPreset = preset
                                    docTitle = preset.defaultTitle
                                    selectedCategory = preset.category
                                },
                                label = { Text(preset.label) }
                            )
                        }
                    }

                    // Filter Enhancement Selector
                    Text(
                        text = "Image Processing Pipeline Filter:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScanFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label) },
                                leadingIcon = if (selectedFilter == filter) {
                                    { Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }

                    // Consecutive Multi-Page CameraX Capture Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                multiPhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isProcessingPipeline,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Batch Photos", maxLines = 1)
                        }

                        Button(
                            onClick = {
                                triggerCameraXPageCapture(compileImmediatelyAfter = false)
                            },
                            enabled = !isProcessingPipeline,
                            modifier = Modifier
                                .weight(1.25f)
                                .height(54.dp)
                                .testTag("consecutive_capture_page_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5E17EB),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Capture Page #${processedPageBitmaps.size + 1}", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    Button(
                        onClick = {
                            if (processedPageBitmaps.isNotEmpty()) {
                                compileConsecutivePagesToSinglePdf()
                            } else {
                                triggerCameraXPageCapture(compileImmediatelyAfter = true)
                            }
                        },
                        enabled = !isProcessingPipeline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("capture_scan_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0066FF),
                            contentColor = Color.White
                        )
                    ) {
                        if (isProcessingPipeline) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compiling Multi-Page PDF & Running OCR...")
                        } else {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (processedPageBitmaps.isNotEmpty()) {
                                    "Compile ${processedPageBitmaps.size} Captured Page(s) into Single PDF"
                                } else {
                                    "Capture & Compile Single PDF Document"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Post-Capture Multi-Page PDF Pipeline Preview, Language Detection Badge & Categorization
                    CrystalGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        tintColor = Color(0xFF0066FF)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Compiled Single PDF ($activePageCount Page${if (activePageCount > 1) "s" else ""})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Filter: ${selectedFilter.label} • ${(generatedPdfSize / 1024).coerceAtLeast(1)} KB multi-page PDF cached",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            rotationDegrees = (rotationDegrees + 90f) % 360f
                                            compileConsecutivePagesToSinglePdf(
                                                filter = selectedFilter,
                                                rotDegrees = rotationDegrees
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90 degrees")
                                    }
                                    IconButton(
                                        onClick = { isCompiledReviewMode = false }
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Capture More Pages")
                                    }
                                }
                            }

                            // Live Processed Bitmap Page Previews
                            if (processedPageBitmaps.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    processedPageBitmaps.forEachIndexed { idx, bmp ->
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                                        ) {
                                            Box {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = "Compiled Page ${idx + 1}",
                                                    modifier = Modifier
                                                        .width(145.dp)
                                                        .height(200.dp),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.72f),
                                                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                                                    modifier = Modifier.align(Alignment.TopStart)
                                                ) {
                                                    Text(
                                                        text = "Page ${idx + 1} of $activePageCount",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Add Another Consecutive Page Button right in Review Mode
                            OutlinedButton(
                                onClick = { isCompiledReviewMode = false },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Capture Additional Page with CameraX (Currently $activePageCount Pages)")
                            }
                        }
                    }

                    // Multi-Language OCR & Automatic Language Detection Banner
                    CrystalGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        tintColor = Color(0xFF059669)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "OCR Language Detected: ${detectedLanguageResult.detectedLanguage.displayName} (${detectedLanguageResult.detectedLanguage.nativeLabel}) • ${detectedLanguageResult.confidencePercent}% Accuracy",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }
                            Text(
                                text = "Script Family: ${detectedLanguageResult.scriptFamily} • ${detectedLanguageResult.characterSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OcrLanguage.entries.forEach { lang ->
                                    FilterChip(
                                        selected = selectedLanguage == lang,
                                        onClick = {
                                            selectedLanguage = lang
                                        },
                                        label = { Text(lang.nativeLabel) }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { docTitle = it },
                        label = { Text("Compiled Document Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scan_title_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = customTagsInput,
                        onValueChange = { customTagsInput = it },
                        label = { Text("Custom Search Tags (comma separated)") },
                        leadingIcon = {
                            Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scan_tags_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Category Selection
                    Text(
                        text = "Assign Category:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory.equals(cat, ignoreCase = true),
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    // Personalized Folder Assignment
                    Text(
                        text = "Save to Personalized Folder:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFolderId == "root",
                            onClick = { selectedFolderId = "root" },
                            label = { Text("Main Workspace") }
                        )
                        folders.forEach { folder ->
                            FilterChip(
                                selected = selectedFolderId == folder.id,
                                onClick = { selectedFolderId = folder.id },
                                label = { Text(folder.name) }
                            )
                        }
                    }

                    // Pic-to-Doc Target Format (PDF, Word, Excel, PowerPoint, Scanned)
                    Text(
                        text = "Pic-to-Doc Target Output Format (Multi-Language OCR + Embedded Image):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "pdf" to "PDF (.pdf)",
                            "docx" to "Word (.docx)",
                            "xlsx" to "Excel (.xlsx)",
                            "pptx" to "PowerPoint (.pptx)",
                            "scanned" to "Scanned Vault (.pdf)"
                        ).forEach { (fmtKey, fmtLabel) ->
                            FilterChip(
                                selected = outputFormat == fmtKey,
                                onClick = {
                                    outputFormat = fmtKey
                                },
                                label = { Text(fmtLabel) },
                                modifier = Modifier.testTag("pic_to_doc_format_$fmtKey")
                            )
                        }
                    }

                    // Extracted Multi-Language OCR & Structured Block Content
                    OutlinedTextField(
                        value = extractedText,
                        onValueChange = { extractedText = it },
                        label = {
                            Text("Extracted Multi-Language OCR & Table Blocks (${detectedLanguageResult.detectedLanguage.displayName})")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // AES-256 & Biometric Security Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EnhancedEncryption,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = "AES-256 + Biometric Security Gate",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Require Fingerprint / Face Unlock or Passphrase before viewing",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = isEncrypted,
                                    onCheckedChange = { isEncrypted = it },
                                    modifier = Modifier.testTag("scan_encryption_switch")
                                )
                            }

                            if (isEncrypted) {
                                OutlinedTextField(
                                    value = encryptionPassword,
                                    onValueChange = { encryptionPassword = it },
                                    label = { Text("Encryption Passphrase / PIN (Required)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = encryptionHint,
                                    onValueChange = { encryptionHint = it },
                                    label = { Text("Passphrase Hint (Optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Cache in Local Room DB for Offline Access",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Store metadata & compiled single PDF locally for instant offline access",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isOfflineCached,
                                    onCheckedChange = { isOfflineCached = it }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val ext = when (outputFormat) {
                                "docx" -> "docx"
                                "xlsx" -> "xlsx"
                                "pptx" -> "pptx"
                                else -> "pdf"
                            }
                            val cleanFileName = docTitle
                                .lowercase()
                                .replace(Regex("[^a-z0-9]+"), "_")
                                .trim('_')
                                .ifBlank { "compiled_multi_page" } + ".$ext"

                            val mergedTags = buildString {
                                append(customTagsInput)
                                append(", ")
                                append(detectedLanguageResult.detectedLanguage.code)
                                if (isEncrypted) append(", biometric-locked")
                            }

                            onSaveScannedDocument(
                                docTitle.ifBlank { "Compiled Multi-Page Document" },
                                selectedFolderId,
                                selectedCategory,
                                outputFormat,
                                cleanFileName,
                                extractedText,
                                isEncrypted && encryptionPassword.isNotBlank(),
                                encryptionPassword,
                                encryptionHint.ifBlank { null },
                                isOfflineCached,
                                activePageCount.toLong(),
                                generatedPdfPath,
                                generatedPdfUri,
                                mergedTags
                            )
                        },
                        enabled = docTitle.isNotBlank() && (!isEncrypted || encryptionPassword.isNotBlank()) && !isProcessingPipeline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("save_scanned_doc_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Save $activePageCount-Page ${outputFormat.uppercase()} & Sync",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraXCapturePreview(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
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
                } catch (_: Exception) {
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@Composable
private fun ScannerOverlayCanvas(
    preset: ScanTemplatePreset,
    filter: ScanFilter
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line"
    )

    val frameAccent = when (filter) {
        ScanFilter.EDGE_DETECTION -> Color(0xFF00F4FE)
        ScanFilter.MAGIC_COLOR -> Color(0xFF38BDF8)
        ScanFilter.BW_CONTRAST -> Color(0xFFFFFFFF)
        ScanFilter.GRAYSCALE -> Color(0xFF94A3B8)
        ScanFilter.ORIGINAL -> Color(0xFFFBBF24)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rectW = size.width * 0.78f
            val rectH = size.height * 0.70f
            val left = (size.width - rectW) / 2f
            val top = (size.height - rectH) / 2f

            drawRoundRect(
                color = frameAccent,
                topLeft = Offset(left, top),
                size = Size(rectW, rectH),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 4f)
            )

            val laserY = top + rectH * scanLineProgress
            drawLine(
                color = frameAccent,
                start = Offset(left + 8f, laserY),
                end = Offset(left + rectW - 8f, laserY),
                strokeWidth = 5f
            )
        }

        Surface(
            color = Color.Black.copy(alpha = 0.58f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Text(
                text = "${preset.label} • ${filter.label}",
                color = frameAccent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}
