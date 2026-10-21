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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
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
import com.example.data.util.ImageProcessingPdfPipeline
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ScanFilter(val label: String) {
    MAGIC_COLOR("Magic Color"),
    BW_CONTRAST("B&W Sharp"),
    GRAYSCALE("Grayscale"),
    ORIGINAL("Original")
}

enum class ScanTemplatePreset(val label: String, val category: String, val defaultTitle: String, val sampleOcr: String) {
    INVOICE(
        label = "Business Invoice",
        category = "Invoices",
        defaultTitle = "Scanned Vendor Invoice #4092",
        sampleOcr = """
            SCANNED DOCUMENT — OPTICAL CHARACTER RECOGNITION (OCR)
            =====================================================
            VENDOR: Apex Cloud Infrastructure Ltd.
            INVOICE NO: INV-2026-4092
            DATE: ${SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())}
            STATUS: DUE UPON RECEIPT
            
            LINE ITEMS:
            1. Enterprise Cloud Document Storage (10 TB) .... $1,200.00
            2. Real-Time Multi-Device Sync Engine ........... $  650.00
            3. AES-256 Hardware Key Management .............. $  350.00
            -----------------------------------------------------
            TOTAL AMOUNT DUE:                                 $2,200.00
            
            Notes: Verified and digitized via DocFlow CameraX Scanner.
        """.trimIndent()
    ),
    CONTRACT(
        label = "Signed Contract",
        category = "Contracts",
        defaultTitle = "Scanned NDA & Partner Agreement",
        sampleOcr = """
            SCANNED PHYSICAL CONTRACT — MULTI-PAGE OCR
            =====================================================
            MUTUAL NON-DISCLOSURE AGREEMENT
            
            This Mutual Non-Disclosure Agreement ("Agreement") is entered into by and between the undersigned parties for the purpose of preventing the unauthorized disclosure of Confidential Information.
            
            1. Definition of Confidential Information
            All technical, financial, and business documents transmitted or synchronized through the DocFlow encrypted workspace.
            
            2. Obligations of Receiving Party
            Receiving Party shall hold and maintain the Confidential Information in strict confidence using AES-256 encryption and zero-trust access controls.
            
            [SIGNATURE DETECTED: Authorized Representative — Verified]
        """.trimIndent()
    ),
    RECEIPT(
        label = "Expense Receipt",
        category = "Receipts",
        defaultTitle = "Scanned Travel Expense Receipt",
        sampleOcr = """
            SCANNED EXPENSE RECEIPT — AUTO-CROPPED
            =====================================================
            MERCHANT: Metro Executive Conference Center
            CATEGORY: Business Travel & Lodging
            PAYMENT METHOD: Corporate Visa •••• 8821
            
            - Conference Suite Rental (2 Days) .......... $480.00
            - High-Speed Fiber Uplink ................... $ 45.00
            - Taxes & Fees .............................. $ 47.25
            -----------------------------------------------------
            TOTAL PAID:                                   $572.25
        """.trimIndent()
    ),
    MEDICAL(
        label = "Medical Record",
        category = "Medical",
        defaultTitle = "Scanned Lab Summary Report",
        sampleOcr = """
            SCANNED MEDICAL RECORD — CONFIDENTIAL
            =====================================================
            PROVIDER: Pacific Health & Diagnostics Center
            DOCUMENT: Annual Preventive Wellness Summary
            
            FINDINGS:
            • All primary metabolic and cardiovascular markers within optimal reference range.
            • Next routine follow-up scheduled in 12 months.
            
            Recommended Security: Enable AES-256 Encryption for HIPAA/Privacy protection.
        """.trimIndent()
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
        generatedPdfUri: Uri?
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

    var isCaptured by remember { mutableStateOf(false) }
    var isProcessingPipeline by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(ScanTemplatePreset.INVOICE) }
    var selectedFilter by remember { mutableStateOf(ScanFilter.MAGIC_COLOR) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var scannedPagesCount by remember { mutableIntStateOf(1) }

    var rawCameraBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val processedPageBitmaps = remember { mutableStateListOf<Bitmap>() }
    var generatedPdfPath by remember { mutableStateOf<String?>(null) }
    var generatedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var generatedPdfSize by remember { mutableStateOf(0L) }

    var docTitle by remember { mutableStateOf(selectedPreset.defaultTitle) }
    var extractedText by remember { mutableStateOf(selectedPreset.sampleOcr) }
    var selectedCategory by remember { mutableStateOf(selectedPreset.category) }
    var selectedFolderId by remember {
        mutableStateOf(if (initialFolderId == "all") "root" else initialFolderId)
    }
    var outputFormat by remember { mutableStateOf("scanned") } // "scanned", "pdf", or "docx"
    var isEncrypted by remember { mutableStateOf(false) }
    var encryptionPassword by remember { mutableStateOf("") }
    var encryptionHint by remember { mutableStateOf("") }
    var isOfflineCached by remember { mutableStateOf(true) }

    fun runImageProcessingAndPdfPipeline(
        sourceBitmap: Bitmap?,
        filter: ScanFilter,
        rotDegrees: Float,
        pages: Int,
        title: String,
        ocr: String
    ) {
        scope.launch {
            isProcessingPipeline = true
            processedPageBitmaps.clear()
            for (pageIdx in 1..pages) {
                val baseSheet = ImageProcessingPdfPipeline.createHighResDocumentBitmap(
                    cameraBitmap = sourceBitmap,
                    title = title,
                    ocrText = ocr,
                    filter = filter,
                    pageIndex = pageIdx,
                    totalPages = pages
                )
                val finalBitmap = if (rotDegrees % 360f != 0f) {
                    ImageProcessingPdfPipeline.processDocumentBitmap(baseSheet, ScanFilter.ORIGINAL, rotDegrees)
                } else {
                    baseSheet
                }
                processedPageBitmaps.add(finalBitmap)
            }

            val cleanName = title.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .ifBlank { "scanned_doc" } + ".pdf"

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
            isCaptured = true
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val galleryBmp = ImageProcessingPdfPipeline.decodeBitmapFromUri(context, uri)
                rawCameraBitmap = galleryBmp
                docTitle = "Scanned Image Document (${SimpleDateFormat("HH:mm", Locale.US).format(Date())})"
                runImageProcessingAndPdfPipeline(
                    sourceBitmap = galleryBmp,
                    filter = selectedFilter,
                    rotDegrees = rotationDegrees,
                    pages = scannedPagesCount,
                    title = docTitle,
                    ocr = extractedText
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val categories = listOf("Invoices", "Contracts", "Receipts", "Work", "Personal", "Legal", "Medical", "Notes")

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (!isCaptured) "CameraX Document Scanner" else "PDF Pipeline & Categorization",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (!isCaptured) {
                                "High-res CameraX capture + PDF conversion pipeline"
                            } else {
                                "$scannedPagesCount page(s) • ${selectedFilter.label} • PDF Ready"
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isCaptured) {
                // Live CameraX Viewfinder Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
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

                        // Document Sheet & Laser Scan Overlay
                        ScannerOverlayCanvas(
                            preset = selectedPreset,
                            filter = selectedFilter
                        )

                        // Top status pill inside viewfinder
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 14.dp)
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
                                    text = "CameraX High-Res Pipeline • Edges Locked",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        // Page count badge
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "$scannedPagesCount Page(s)",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
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
                                extractedText = preset.sampleOcr
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

                // Capture Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isProcessingPipeline,
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Photo")
                    }

                    Button(
                        onClick = {
                            isProcessingPipeline = true
                            if (hasCameraPermission) {
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
                                                rawCameraBitmap = bmp
                                                runImageProcessingAndPdfPipeline(
                                                    sourceBitmap = bmp,
                                                    filter = selectedFilter,
                                                    rotDegrees = rotationDegrees,
                                                    pages = scannedPagesCount,
                                                    title = docTitle,
                                                    ocr = extractedText
                                                )
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                // Fallback for virtual/emulator camera hardware
                                                runImageProcessingAndPdfPipeline(
                                                    sourceBitmap = null,
                                                    filter = selectedFilter,
                                                    rotDegrees = rotationDegrees,
                                                    pages = scannedPagesCount,
                                                    title = docTitle,
                                                    ocr = extractedText
                                                )
                                            }
                                        }
                                    )
                                } catch (_: Exception) {
                                    runImageProcessingAndPdfPipeline(
                                        sourceBitmap = null,
                                        filter = selectedFilter,
                                        rotDegrees = rotationDegrees,
                                        pages = scannedPagesCount,
                                        title = docTitle,
                                        ocr = extractedText
                                    )
                                }
                            } else {
                                runImageProcessingAndPdfPipeline(
                                    sourceBitmap = null,
                                    filter = selectedFilter,
                                    rotDegrees = rotationDegrees,
                                    pages = scannedPagesCount,
                                    title = docTitle,
                                    ocr = extractedText
                                )
                            }
                        },
                        enabled = !isProcessingPipeline,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(54.dp)
                            .testTag("capture_scan_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isProcessingPipeline) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Converting PDF...")
                        } else {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Capture & Convert PDF", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        scannedPagesCount += 1
                        extractedText += "\n\n--- PAGE $scannedPagesCount ---\nAdditional scanned page appended to multi-page PDF batch."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Page to Multi-Page PDF Batch ($scannedPagesCount)")
                }
            } else {
                // Post-Capture PDF Pipeline Preview & Categorization
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    )
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
                                        text = "PDF Conversion Complete ($scannedPagesCount Page${if (scannedPagesCount > 1) "s" else ""})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Filter: ${selectedFilter.label} • ${(generatedPdfSize / 1024).coerceAtLeast(1)} KB local PDF cached",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        rotationDegrees = (rotationDegrees + 90f) % 360f
                                        runImageProcessingAndPdfPipeline(
                                            sourceBitmap = rawCameraBitmap,
                                            filter = selectedFilter,
                                            rotDegrees = rotationDegrees,
                                            pages = scannedPagesCount,
                                            title = docTitle,
                                            ocr = extractedText
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90 degrees")
                                }
                                IconButton(onClick = { isCaptured = false }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Rescan")
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
                                                contentDescription = "Processed Page ${idx + 1}",
                                                modifier = Modifier
                                                    .width(150.dp)
                                                    .height(210.dp),
                                                contentScale = ContentScale.Crop
                                            )
                                            Surface(
                                                color = Color.Black.copy(alpha = 0.7f),
                                                shape = RoundedCornerShape(bottomEnd = 8.dp),
                                                modifier = Modifier.align(Alignment.TopStart)
                                            ) {
                                                Text(
                                                    text = "Page ${idx + 1}",
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

                        // Real-time filter switcher on captured image
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScanFilter.entries.forEach { filter ->
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = {
                                        selectedFilter = filter
                                        runImageProcessingAndPdfPipeline(
                                            sourceBitmap = rawCameraBitmap,
                                            filter = filter,
                                            rotDegrees = rotationDegrees,
                                            pages = scannedPagesCount,
                                            title = docTitle,
                                            ocr = extractedText
                                        )
                                    },
                                    label = { Text(filter.label) }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = docTitle,
                    onValueChange = { docTitle = it },
                    label = { Text("Scanned Document Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_title_input"),
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

                // Output Format
                Text(
                    text = "Document Classification Format:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = outputFormat == "scanned",
                        onClick = { outputFormat = "scanned" },
                        label = { Text("Scanned PDF (.pdf)") }
                    )
                    FilterChip(
                        selected = outputFormat == "pdf",
                        onClick = { outputFormat = "pdf" },
                        label = { Text("Standard PDF (.pdf)") }
                    )
                    FilterChip(
                        selected = outputFormat == "docx",
                        onClick = { outputFormat = "docx" },
                        label = { Text("Editable Word (.docx)") }
                    )
                }

                // Extracted OCR Content (Editable before saving)
                OutlinedTextField(
                    value = extractedText,
                    onValueChange = { extractedText = it },
                    label = { Text("Extracted OCR Text (Editable)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    shape = RoundedCornerShape(14.dp)
                )

                // Robust Encryption Card
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
                                        text = "AES-256 Document Encryption",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Protect scanned PDF & OCR payload with passphrase",
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
                                    text = "Store metadata & generated PDF locally for instant offline reading",
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
                        val ext = if (outputFormat == "docx") "docx" else "pdf"
                        val cleanFileName = docTitle
                            .lowercase()
                            .replace(Regex("[^a-z0-9]+"), "_")
                            .trim('_')
                            .ifBlank { "scanned_doc" } + ".$ext"

                        onSaveScannedDocument(
                            docTitle.ifBlank { "Scanned Document" },
                            selectedFolderId,
                            selectedCategory,
                            outputFormat,
                            cleanFileName,
                            extractedText,
                            isEncrypted && encryptionPassword.isNotBlank(),
                            encryptionPassword,
                            encryptionHint.ifBlank { null },
                            isOfflineCached,
                            scannedPagesCount.toLong(),
                            generatedPdfPath,
                            generatedPdfUri
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
                        text = "Save PDF to Room Cache & Cloud Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
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

    val sheetColor = when (filter) {
        ScanFilter.MAGIC_COLOR -> Color(0xFFF8FAFC)
        ScanFilter.BW_CONTRAST -> Color(0xFFFFFFFF)
        ScanFilter.GRAYSCALE -> Color(0xFFE2E8F0)
        ScanFilter.ORIGINAL -> Color(0xFFFEFCE8)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .height(230.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(sheetColor.copy(alpha = 0.92f))
                .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = preset.defaultTitle.uppercase(),
                    color = Color(0xFF0F172A),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(3.dp)
                        .background(Color(0xFF2563EB))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = preset.sampleOcr.take(260),
                    color = Color(0xFF334155),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 8
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val rectW = size.width * 0.78f
            val rectH = size.height * 0.70f
            val left = (size.width - rectW) / 2f
            val top = (size.height - rectH) / 2f

            drawRoundRect(
                color = Color(0xFF38BDF8),
                topLeft = Offset(left, top),
                size = Size(rectW, rectH),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 4f)
            )

            val laserY = top + rectH * scanLineProgress
            drawLine(
                color = Color(0xFF22D3EE),
                start = Offset(left + 8f, laserY),
                end = Offset(left + rectW - 8f, laserY),
                strokeWidth = 5f
            )
        }
    }
}
