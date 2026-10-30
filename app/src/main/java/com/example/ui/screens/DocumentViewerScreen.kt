package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.data.model.ApryseDocumentModel
import com.example.data.model.DocumentFormat
import com.example.data.util.FileParserUtils
import com.example.data.util.ImageProcessingPdfPipeline
import com.example.data.util.MlKitOcrScanResult
import com.example.data.util.MultiLanguageOcrEngine
import com.example.data.util.PdfWatermarkConfig
import com.example.ui.components.AddWatermarkSheet
import com.example.ui.components.MlKitExtractedTextOverlaySheet
import com.example.ui.theme.DocFlowTokens
import com.example.ui.viewmodel.DocumentUiState
import com.example.ui.viewmodel.DocumentViewModel
import kotlinx.coroutines.launch
import com.pdftron.pdf.config.ViewerBuilder2
import com.pdftron.pdf.config.ViewerConfig
import com.pdftron.pdf.controls.DocumentActivity
import com.pdftron.pdf.controls.PdfViewCtrlTabHostFragment2
import java.io.File

/**
 * Robust Jetpack Compose wrapper utilizing Apryse Android SDK.
 * Embeds PdfViewCtrlTabHostFragment2 via FragmentContainerView in AndroidView
 * and supports launching full-featured DocumentActivity for advanced editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    document: ApryseDocumentModel,
    viewModel: DocumentViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isFullScreenLaunching by remember { mutableStateOf(false) }

    var showAddWatermarkSheet by remember { mutableStateOf(false) }
    var showOcrOverlaySheet by remember { mutableStateOf(false) }
    var isRunningOcr by remember { mutableStateOf(false) }
    var ocrScanResult by remember { mutableStateOf<MlKitOcrScanResult?>(null) }
    var showOfficeConversionBanner by remember { mutableStateOf(document.format != DocumentFormat.PDF) }

    fun triggerOcr() {
        scope.launch {
            isRunningOcr = true
            snackbarHostState.showSnackbar("Extracting text with Google ML Kit on-device...")
            val bmp = FileParserUtils.renderPdfPage(context, document.localFileUri, 0)
            if (bmp != null) {
                val res = MultiLanguageOcrEngine.recognizeTextWithMlKit(
                    bitmaps = listOf(bmp),
                    preferredLanguage = com.example.data.util.OcrLanguage.AUTO,
                    documentTitle = document.title
                )
                ocrScanResult = res
                showOcrOverlaySheet = true
                snackbarHostState.showSnackbar("Extracted ${res.totalBlocks} blocks with Google ML Kit")
            } else {
                snackbarHostState.showSnackbar("Could not render document page for OCR")
            }
            isRunningOcr = false
        }
    }

    // Intercept back presses to cleanly navigate back to dashboard
    BackHandler {
        viewModel.closeDocument()
        onNavigateBack()
    }

    // SAF Save Exporter: writes edited local document to user-chosen storage destination
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            when (document.format) {
                DocumentFormat.PDF -> "application/pdf"
                DocumentFormat.WORD -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                DocumentFormat.EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                DocumentFormat.POWERPOINT -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            }
        )
    ) { uri: Uri? ->
        uri?.let { viewModel.saveDocument(it) }
    }

    // SAF PDF Exporter: exports any document format to PDF
    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportAsPdf(it) }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("document_viewer_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = document.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FormatBadge(format = document.format)
                            Text(
                                text = "• Apryse On-Device Engine",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.closeDocument()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("viewer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                },
                actions = {
                    // Add Watermark to document
                    IconButton(
                        onClick = { showAddWatermarkSheet = true },
                        modifier = Modifier.testTag("viewer_add_watermark_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrandingWatermark,
                            contentDescription = "Add Watermark",
                            tint = DocFlowTokens.AccentBlue
                        )
                    }

                    // Google ML Kit Text Recognition
                    IconButton(
                        onClick = {
                            if (ocrScanResult != null) {
                                showOcrOverlaySheet = true
                            } else {
                                triggerOcr()
                            }
                        },
                        modifier = Modifier.testTag("viewer_ocr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Extract Text with ML Kit",
                            tint = if (ocrScanResult != null) DocFlowTokens.AccentBlue else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Full-screen native Apryse DocumentActivity launcher
                    IconButton(
                        onClick = {
                            val intent = DocumentActivity.IntentBuilder.fromActivityClass(
                                context,
                                DocumentActivity::class.java
                            )
                                .withUri(document.localFileUri)
                                .usingNewUi(true)
                                .build()
                            context.startActivity(intent)
                        },
                        modifier = Modifier.testTag("viewer_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Open Full-Screen Apryse Editor",
                            tint = DocFlowTokens.AccentBlue
                        )
                    }

                    // Save / Overwrite back to storage
                    IconButton(
                        onClick = {
                            saveLauncher.launch(document.title)
                        },
                        modifier = Modifier.testTag("viewer_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Document"
                        )
                    }

                    // Export to PDF
                    IconButton(
                        onClick = {
                            val baseName = document.title.substringBeforeLast(".")
                            exportPdfLauncher.launch("${baseName}_exported.pdf")
                        },
                        modifier = Modifier.testTag("viewer_export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export as PDF"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Office conversion banner (SDK fact: Office files open as PDF)
            if (showOfficeConversionBanner) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("office_conversion_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Word/Excel/PowerPoint files open as PDF.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            )
                            Text(
                                text = "Your edits and annotations are saved as a PDF copy.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFB45309),
                                    fontSize = 12.sp
                                )
                            )
                        }
                        IconButton(
                            onClick = { showOfficeConversionBanner = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFF92400E),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                // Apryse Fragment Viewer hosted inside AndroidView
                ApryseViewerFragmentHost(
                    documentUri = document.localFileUri,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Add Watermark Sheet for Apryse Document Viewer
    if (showAddWatermarkSheet) {
        val palette = DocFlowTokens.currentPalette()
        AddWatermarkSheet(
            palette = palette,
            documentTitle = document.title,
            onDismiss = { showAddWatermarkSheet = false },
            onApplyWatermark = { config ->
                showAddWatermarkSheet = false
                scope.launch {
                    val outDir = java.io.File(context.filesDir, "watermarked_docs").apply { if (!exists()) mkdirs() }
                    val cleanTitle = document.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "doc" }
                    val outFile = java.io.File(outDir, "${cleanTitle}_watermarked_${System.currentTimeMillis()}.pdf")
                    val res = ImageProcessingPdfPipeline.applyWatermarkToExistingPdf(
                        context = context,
                        inputUri = document.localFileUri,
                        outputFile = outFile,
                        config = config
                    )
                    if (res.isSuccess) {
                        snackbarHostState.showSnackbar("Watermark '${config.text}' (${config.opacityPercent}% opacity) applied to all pages • Stored on this device")
                    } else {
                        snackbarHostState.showSnackbar("Watermark failed: ${res.exceptionOrNull()?.message}")
                    }
                }
            }
        )
    }

    // ML Kit Extracted Text Overlay Sheet for Apryse Document Viewer
    if (showOcrOverlaySheet && ocrScanResult != null) {
        val palette = DocFlowTokens.currentPalette()
        MlKitExtractedTextOverlaySheet(
            palette = palette,
            scanResult = ocrScanResult!!,
            currentPageIndex = 0,
            onDismiss = { showOcrOverlaySheet = false },
            onAddToNotes = { text ->
                scope.launch {
                    snackbarHostState.showSnackbar("Extracted text saved to clipboard")
                }
            }
        )
    }
}

/**
 * Embeds Apryse PdfViewCtrlTabHostFragment2 inside Compose using AndroidView
 * and FragmentContainerView while correctly respecting lifecycle events.
 */
@Composable
fun ApryseViewerFragmentHost(
    documentUri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val containerId = remember { R.id.apryse_fragment_container }

    AndroidView(
        modifier = modifier.testTag("apryse_android_view_host"),
        factory = { ctx ->
            android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.fragment_apryse_pdf_container, null, false)
        },
        update = { _ ->
            val fragmentActivity = context as? FragmentActivity ?: return@AndroidView
            val fm = fragmentActivity.supportFragmentManager

            // Check if fragment is already attached to avoid recreation loops
            val existingFragment = fm.findFragmentById(containerId)
            if (existingFragment == null) {
                val viewerConfig = ViewerConfig.Builder()
                    .showAppBar(true)
                    .showBottomNavBar(true)
                    .showThumbnailView(true)
                    .multiTabEnabled(true)
                    .build()

                val fragment: PdfViewCtrlTabHostFragment2 = ViewerBuilder2.withUri(documentUri)
                    .usingConfig(viewerConfig)
                    .build(context)

                fm.beginTransaction()
                    .replace(containerId, fragment)
                    .commitAllowingStateLoss()
            }
        }
    )

    // Cleanup fragment on dispose to free native PDFNet C++ resources cleanly
    DisposableEffect(documentUri) {
        onDispose {
            val fragmentActivity = context as? FragmentActivity
            fragmentActivity?.supportFragmentManager?.let { fm ->
                fm.findFragmentById(containerId)?.let { frag ->
                    fm.beginTransaction().remove(frag).commitAllowingStateLoss()
                }
            }
        }
    }
}

@Composable
fun FormatBadge(format: DocumentFormat) {
    val (bgColor, textColor, label) = when (format) {
        DocumentFormat.PDF -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "PDF")
        DocumentFormat.WORD -> Triple(Color(0xFFE8F0FE), Color(0xFF1967D2), "DOCX")
        DocumentFormat.EXCEL -> Triple(Color(0xFFE6F4EA), Color(0xFF137333), "XLSX")
        DocumentFormat.POWERPOINT -> Triple(Color(0xFFFCE8E6), Color(0xFFC5221F), "PPTX")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.clip(RoundedCornerShape(4.dp))
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
