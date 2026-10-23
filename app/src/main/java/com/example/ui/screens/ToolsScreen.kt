package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DocumentCommentEntity
import com.example.data.local.DocumentVersionEntity
import com.example.data.model.DocEntity
import com.example.data.util.ExtractiveSummaryResult
import com.example.data.util.ImageProcessingPdfPipeline
import com.example.data.util.OcrLanguage
import com.example.data.util.OnDeviceAiAndBackupEngine
import com.example.data.util.PdfWatermarkConfig
import com.example.data.util.TranslationLanguage
import com.example.ui.components.AiMicDictationBottomSheet
import com.example.ui.components.DocFlowCard
import com.example.ui.components.DocFlowDivider
import com.example.ui.components.DocFlowEmptyState
import com.example.ui.components.DocFlowHeaderAction
import com.example.ui.components.DocFlowIconCircle
import com.example.ui.components.DocFlowListRow
import com.example.ui.components.DocFlowPrimaryButton
import com.example.ui.components.DocFlowProgressBar
import com.example.ui.components.DocFlowScreenHeader
import com.example.ui.components.DocFlowSecondaryButton
import com.example.ui.components.DocFlowSectionLabel
import com.example.ui.components.PdfWatermarkExportSheet
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens
import com.example.ui.viewmodel.DocFlowViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class PickedImagePage(
    val id: String,
    val label: String,
    val bitmap: Bitmap,
    val rotationDegrees: Float = 0f
)

private data class ConversionSuccessState(
    val createdDocId: String,
    val outputTitle: String,
    val outputFormat: String,
    val summaryDetail: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ToolsScreen(
    viewModel: DocFlowViewModel,
    palette: DocFlowPalette,
    onOpenDocument: (String) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val documents by viewModel.filteredDocuments.collectAsState()
    val allVersions by viewModel.allUserVersions.collectAsState()

    // Conversion Sheets State
    var showImageToPdfSheet by remember { mutableStateOf(false) }
    var showPdfToWordSheet by remember { mutableStateOf(false) }
    var showWordToPdfSheet by remember { mutableStateOf(false) }
    var showOfficeToPdfSheet by remember { mutableStateOf(false) }
    var showExportVersionsNotesSheet by remember { mutableStateOf(false) }
    var showWatermarkSheetForPendingAction by remember { mutableStateOf<((PdfWatermarkConfig) -> Unit)?>(null) }
    var watermarkSheetTitle by remember { mutableStateOf("Document") }

    // Progress & Cancelable Conversion State
    var isConverting by remember { mutableStateOf(false) }
    var conversionProgress by remember { mutableFloatStateOf(0f) }
    var conversionStatusLabel by remember { mutableStateOf("") }
    var conversionCancelled by remember { mutableStateOf(false) }
    var conversionSuccess by remember { mutableStateOf<ConversionSuccessState?>(null) }

    // Image to PDF State
    val pickedImages = remember { mutableStateListOf<PickedImagePage>() }
    var imgPageSize by remember { mutableStateOf("A4") } // A4, LETTER, FIT
    var imgOrientation by remember { mutableStateOf("PORTRAIT") } // PORTRAIT, LANDSCAPE
    var imgMargins by remember { mutableStateOf("SMALL") } // NONE, SMALL, BIG
    var imgQuality by remember { mutableStateOf("HIGH") } // STANDARD, HIGH
    var imgOutputBaseTitle by remember { mutableStateOf("Scanned Images") }

    // PDF to Word State
    var selectedPdfForWord by remember { mutableStateOf<DocEntity?>(null) }
    var runOcrForPdfToWord by remember { mutableStateOf(false) }
    var pdfToWordOcrLang by remember { mutableStateOf(OcrLanguage.ENGLISH) }

    // Word / Office to PDF State
    var selectedDocForPdf by remember { mutableStateOf<DocEntity?>(null) }

    // Export Versions / Notes State
    var selectedDocForVersionExport by remember { mutableStateOf<DocEntity?>(null) }
    var exportTabMode by remember { mutableIntStateOf(0) } // 0 = Document Versions, 1 = Document Notes

    // AI Tools & Create Doc / PPT / XLS / AI Mic State
    var showSummariseSheet by remember { mutableStateOf(false) }
    var showTranslateSheet by remember { mutableStateOf(false) }
    var showAiMicSheet by remember { mutableStateOf(false) }
    var showCreateOfficeDialog by remember { mutableStateOf(false) }
    var createOfficeFormat by remember { mutableStateOf("docx") }
    var createOfficeTitle by remember { mutableStateOf("") }
    var createOfficeBody by remember { mutableStateOf("") }

    var selectedDocForSummary by remember { mutableStateOf<DocEntity?>(null) }
    var summaryResult by remember { mutableStateOf<ExtractiveSummaryResult?>(null) }

    var selectedDocForTranslate by remember { mutableStateOf<DocEntity?>(null) }
    var selectedTargetLang by remember { mutableStateOf(TranslationLanguage.HINDI) }
    var downloadedPacks by remember {
        mutableStateOf(OnDeviceAiAndBackupEngine.getDownloadedLanguagePacks(context))
    }
    var translatedOutput by remember { mutableStateOf<String?>(null) }

    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                uris.forEachIndexed { idx, uri ->
                    val decoded = ImageProcessingPdfPipeline.decodeBitmapFromUri(context, uri)
                    if (decoded != null) {
                        val maxDim = if (imgQuality == "STANDARD") 1080 else 1600
                        val safeBmp = if (decoded.width > maxDim || decoded.height > maxDim) {
                            val scale = maxDim.toFloat() / maxOf(decoded.width, decoded.height).toFloat()
                            Bitmap.createScaledBitmap(
                                decoded,
                                (decoded.width * scale).toInt().coerceAtLeast(1),
                                (decoded.height * scale).toInt().coerceAtLeast(1),
                                true
                            )
                        } else {
                            decoded
                        }
                        pickedImages.add(
                            PickedImagePage(
                                id = "${System.currentTimeMillis()}_$idx",
                                label = "Image ${pickedImages.size + 1}",
                                bitmap = safeBmp
                            )
                        )
                    }
                }
            }
        }
    }

    val externalFileConvertPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importDocumentFromUri(
                context = context,
                uri = uri,
                folderId = "root",
                category = "Work",
                isEncrypted = false,
                password = "",
                encryptionKeyHint = null,
                onImported = { importedId ->
                    val doc = viewModel.cachedDocumentsMap.value[importedId]?.toDocEntity()
                    if (doc != null) {
                        selectedDocForPdf = doc
                        watermarkSheetTitle = doc.title
                        showWatermarkSheetForPendingAction = { wmConfig ->
                            viewModel.convertFileToPdfAndSave(context, doc, wmConfig) { pdfDocId ->
                                conversionSuccess = ConversionSuccessState(
                                    createdDocId = pdfDocId,
                                    outputTitle = viewModel.buildConvertedDocumentTitle(doc.title),
                                    outputFormat = "PDF",
                                    summaryDetail = "Saved to Manage > Created by me & top of Recent • Stored on this device"
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("tools_screen_list"),
        contentPadding = PaddingValues(bottom = 104.dp)
    ) {
        item {
            DocFlowScreenHeader(
                title = "Tools",
                subtitle = "Stored on this device • Offline conversions & AI",
                palette = palette,
                actions = listOf(
                    DocFlowHeaderAction(
                        icon = Icons.Filled.Search,
                        contentDescription = "Search",
                        testTag = "tools_search_button",
                        onClick = onOpenSearch
                    ),
                    DocFlowHeaderAction(
                        icon = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        testTag = "tools_settings_button",
                        onClick = onOpenSettings
                    )
                )
            )
        }

        // Conversion Progress Bar Card (when a local conversion is running)
        if (isConverting) {
            item {
                DocFlowCard(
                    palette = palette,
                    modifier = Modifier.padding(vertical = DocFlowTokens.space2)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = conversionStatusLabel,
                            style = DocFlowTokens.rowTitleStyle,
                            color = palette.textPrimary
                        )
                        TextButton(
                            onClick = {
                                conversionCancelled = true
                                isConverting = false
                            },
                            modifier = Modifier.testTag("conversion_cancel_button")
                        ) {
                            Text("Cancel", color = DocFlowTokens.IconCircleRed, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(DocFlowTokens.space2))
                    DocFlowProgressBar(progress = conversionProgress, palette = palette)
                }
            }
        }

        // =====================================================================
        // SECTION 1: FILE CONVERSION (Image to PDF, PDF to Word, Word to PDF, Office to PDF, Export Versions/Notes to PDF)
        // =====================================================================
        item {
            DocFlowSectionLabel(text = "File conversion", palette = palette)
            DocFlowCard(
                palette = palette,
                contentPadding = PaddingValues(vertical = DocFlowTokens.space1)
            ) {
                // 1A. Image to PDF
                DocFlowListRow(
                    title = "Image to PDF",
                    subtitle = "Combine JPG, PNG, WEBP, HEIC or camera scans into PDF • Stored on this device",
                    palette = palette,
                    onClick = { showImageToPdfSheet = true },
                    showChevron = true,
                    modifier = Modifier.testTag("tool_card_image_to_pdf"),
                    leading = {
                        DocFlowIconCircle(
                            icon = Icons.Filled.Image,
                            circleColor = DocFlowTokens.IconCircleRed
                        )
                    }
                )

                DocFlowDivider(
                    palette = palette,
                    modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin)
                )

                // 1B. PDF to Word (.docx)
                DocFlowListRow(
                    title = "PDF to Word",
                    subtitle = "Extract text, headings, tables & OCR scanned PDFs into editable .docx",
                    palette = palette,
                    onClick = { showPdfToWordSheet = true },
                    showChevron = true,
                    modifier = Modifier.testTag("tool_card_pdf_to_word"),
                    leading = {
                        DocFlowIconCircle(
                            icon = Icons.Filled.Description,
                            circleColor = DocFlowTokens.AccentBlue
                        )
                    }
                )

                DocFlowDivider(
                    palette = palette,
                    modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin)
                )

                // 1C. Word to PDF (.doc, .docx)
                DocFlowListRow(
                    title = "Word to PDF",
                    subtitle = "Render .docx / .doc into paginated PDF with optional text watermark",
                    palette = palette,
                    onClick = { showWordToPdfSheet = true },
                    showChevron = true,
                    modifier = Modifier.testTag("tool_card_word_to_pdf"),
                    leading = {
                        DocFlowIconCircle(
                            icon = Icons.Filled.PictureAsPdf,
                            circleColor = DocFlowTokens.IconCircleRed
                        )
                    }
                )

                DocFlowDivider(
                    palette = palette,
                    modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin)
                )

                // 1D. Office to PDF (.xlsx, .pptx, .txt)
                DocFlowListRow(
                    title = "Convert to PDF",
                    subtitle = "Convert DOCX, XLSX, PPTX & TXT to PDF locally with watermark support",
                    palette = palette,
                    onClick = { showOfficeToPdfSheet = true },
                    showChevron = true,
                    modifier = Modifier.testTag("tool_card_convert_pdf"),
                    leading = {
                        DocFlowIconCircle(
                            icon = Icons.Filled.PictureAsPdf,
                            circleColor = DocFlowTokens.IconCircleOrange
                        )
                    }
                )

                DocFlowDivider(
                    palette = palette,
                    modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin)
                )

                // 1E. Export Versions & Notes to PDF
                DocFlowListRow(
                    title = "Export Versions & Notes to PDF",
                    subtitle = "Select individual document versions or notes and generate a local PDF",
                    palette = palette,
                    onClick = { showExportVersionsNotesSheet = true },
                    showChevron = true,
                    modifier = Modifier.testTag("tool_card_export_versions_notes_pdf"),
                    leading = {
                        DocFlowIconCircle(
                            icon = Icons.Filled.History,
                            circleColor = DocFlowTokens.IconCircleGreen
                        )
                    }
                )
            }
        }

        // =====================================================================
        // SECTION 2: CREATE & DICTATE (Create Doc / PPT / XLS + AI Mic Noise Clean)
        // =====================================================================
        item {
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))
            DocFlowSectionLabel(text = "Create & Voice Dictation", palette = palette)
            DocFlowCard(
                palette = palette,
                contentPadding = PaddingValues(vertical = DocFlowTokens.space1)
            ) {
                DocFlowListRow(
                    title = "Create Doc, PPT & XLS (Open in Word UI)",
                    subtitle = "Author Word (.docx), PowerPoint (.pptx), or Excel (.xlsx) on this device",
                    palette = palette,
                    onClick = {
                        createOfficeFormat = "docx"
                        createOfficeTitle = ""
                        createOfficeBody = ""
                        showCreateOfficeDialog = true
                    },
                    showChevron = true,
                    modifier = Modifier.testTag("tool_card_create_office"),
                    leading = {
                        DocFlowIconCircle(
                            icon = Icons.Filled.NoteAdd,
                            circleColor = DocFlowTokens.AccentBlue
                        )
                    }
                )

                DocFlowDivider(
                    palette = palette,
                    modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin)
                )

                DocFlowListRow(
                    title = "AI Mic • Sound to Text (Noise Clean)",
                    subtitle = "Convert speech to clean text with hardware DSP noise suppression",
                    palette = palette,
                    onClick = { showAiMicSheet = true },
                    showChevron = true,
                    modifier = Modifier.testTag("tool_card_ai_mic"),
                    leading = {
                        DocFlowIconCircle(
                            icon = Icons.Filled.GraphicEq,
                            circleColor = DocFlowTokens.IconCircleTeal
                        )
                    }
                )
            }
        }

        // =====================================================================
        // SECTION 3: AI TOOLS (AI Summarise, Translate PDF — On-Device Only)
        // =====================================================================
        item {
            Spacer(modifier = Modifier.height(DocFlowTokens.space2))
            DocFlowSectionLabel(text = "AI tools", palette = palette)
            DocFlowCard(
                palette = palette,
                contentPadding = PaddingValues(vertical = DocFlowTokens.space1)
            ) {
                DocFlowListRow(
                    title = "AI Summarise",
                    subtitle = "On-device TF-IDF & TextRank key bullet extraction • Stored on this device",
                    palette = palette,
                    onClick = {
                        selectedDocForSummary = documents.firstOrNull()
                        summaryResult = selectedDocForSummary?.let {
                            OnDeviceAiAndBackupEngine.summarizeTextExtractive(it.content)
                        }
                        showSummariseSheet = true
                    },
                    showChevron = true,
                    modifier = Modifier.testTag("tool_card_ai_summarise"),
                    leading = {
                        DocFlowIconCircle(
                            icon = Icons.Filled.AutoAwesome,
                            circleColor = DocFlowTokens.IconCircleBlue
                        )
                    }
                )

                DocFlowDivider(
                    palette = palette,
                    modifier = Modifier.padding(horizontal = DocFlowTokens.cardPaddingMin)
                )

                DocFlowListRow(
                    title = "Translate PDF",
                    subtitle = "Offline translation with downloadable language packs (Hindi, English, Spanish, French, German, Arabic)",
                    palette = palette,
                    onClick = {
                        selectedDocForTranslate = documents.firstOrNull()
                        translatedOutput = null
                        showTranslateSheet = true
                    },
                    showChevron = true,
                    modifier = Modifier.testTag("tool_card_translate_pdf"),
                    leading = {
                        DocFlowIconCircle(
                            icon = Icons.Filled.Translate,
                            circleColor = DocFlowTokens.IconCircleTeal
                        )
                    }
                )
            }
        }
    }

    // =========================================================================
    // SHEET A: IMAGE TO PDF (Multiple images, scan, reorder, rotate, remove, page size, orientation, margins, quality, watermark)
    // =========================================================================
    if (showImageToPdfSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImageToPdfSheet = false },
            containerColor = palette.surface,
            shape = DocFlowTokens.sheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.cardPaddingMax, vertical = DocFlowTokens.space2)
            ) {
                Text(
                    text = "Image to PDF",
                    style = DocFlowTokens.sectionLabelStyle,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = "Pick JPG, PNG, WEBP, HEIC or Scan • Reorder, rotate & add optional watermark • Stored on this device",
                    style = DocFlowTokens.captionStyle,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                OutlinedTextField(
                    value = imgOutputBaseTitle,
                    onValueChange = { imgOutputBaseTitle = it },
                    label = { Text("Output base name (<name> (converted).pdf)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(DocFlowTokens.space2))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                ) {
                    DocFlowSecondaryButton(
                        text = "Pick Images",
                        onClick = { multipleImagePickerLauncher.launch("image/*") },
                        palette = palette,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("img_to_pdf_pick_button")
                    )
                    DocFlowSecondaryButton(
                        text = "Use Camera Scan",
                        onClick = {
                            showImageToPdfSheet = false
                            onOpenScanner()
                        },
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                // Page Reorder / Rotate / Remove Strip
                if (pickedImages.isNotEmpty()) {
                    Text(
                        text = "${pickedImages.size} image(s) queued (1 image per page)",
                        style = DocFlowTokens.captionStyle,
                        fontWeight = FontWeight.SemiBold,
                        color = DocFlowTokens.AccentBlue
                    )
                    Spacer(modifier = Modifier.height(DocFlowTokens.space1))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                    ) {
                        pickedImages.forEachIndexed { index, item ->
                            Column(
                                modifier = Modifier
                                    .width(108.dp)
                                    .clip(DocFlowTokens.buttonShape)
                                    .background(palette.background)
                                    .border(1.dp, palette.divider, DocFlowTokens.buttonShape)
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    bitmap = item.bitmap.asImageBitmap(),
                                    contentDescription = item.label,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(DocFlowTokens.thumbnailShape),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    text = "Page ${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.textPrimary
                                )
                                Row(
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val moved = pickedImages.removeAt(index)
                                                pickedImages.add(index - 1, moved)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move Left", modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val rotated = ImageProcessingPdfPipeline.processDocumentBitmap(
                                                    source = item.bitmap,
                                                    filter = ScanFilter.ORIGINAL,
                                                    rotationDegrees = 90f
                                                )
                                                pickedImages[index] = item.copy(
                                                    bitmap = rotated,
                                                    rotationDegrees = (item.rotationDegrees + 90f) % 360f
                                                )
                                            }
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Filled.RotateRight, contentDescription = "Rotate", modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = { pickedImages.removeAt(index) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = DocFlowTokens.IconCircleRed, modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < pickedImages.lastIndex) {
                                                val moved = pickedImages.removeAt(index)
                                                pickedImages.add(index + 1, moved)
                                            }
                                        },
                                        enabled = index < pickedImages.lastIndex,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move Right", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(DocFlowTokens.space2))
                }

                // Page Size, Orientation, Margins & Quality Options
                Text("Page Size, Orientation & Margins", style = DocFlowTokens.captionStyle, color = palette.textSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("A4" to "A4", "LETTER" to "Letter", "FIT" to "Fit to image").forEach { (k, label) ->
                        FilterChip(
                            selected = imgPageSize == k,
                            onClick = { imgPageSize = k },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                    listOf("PORTRAIT" to "Portrait", "LANDSCAPE" to "Landscape").forEach { (k, label) ->
                        FilterChip(
                            selected = imgOrientation == k,
                            onClick = { imgOrientation = k },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                    listOf("NONE" to "No Margin", "SMALL" to "Small Margin", "BIG" to "Big Margin").forEach { (k, label) ->
                        FilterChip(
                            selected = imgMargins == k,
                            onClick = { imgMargins = k },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                    listOf("STANDARD" to "Standard Quality", "HIGH" to "High Quality").forEach { (k, label) ->
                        FilterChip(
                            selected = imgQuality == k,
                            onClick = { imgQuality = k },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                ) {
                    DocFlowSecondaryButton(
                        text = "Add Watermark...",
                        onClick = {
                            watermarkSheetTitle = imgOutputBaseTitle
                            showWatermarkSheetForPendingAction = { wmConfig ->
                                showImageToPdfSheet = false
                                val bitmaps = pickedImages.map { it.bitmap }
                                viewModel.convertImagesToPdfAndSave(
                                    context = context,
                                    baseTitle = imgOutputBaseTitle,
                                    bitmaps = bitmaps,
                                    pageSizeOption = imgPageSize,
                                    orientationOption = imgOrientation,
                                    marginOption = imgMargins,
                                    watermarkConfig = wmConfig,
                                    onCreated = { createdId ->
                                        conversionSuccess = ConversionSuccessState(
                                            createdDocId = createdId,
                                            outputTitle = viewModel.buildConvertedDocumentTitle(imgOutputBaseTitle),
                                            outputFormat = "PDF",
                                            summaryDetail = "${bitmaps.size.coerceAtLeast(1)} page(s) • $imgPageSize ($imgOrientation) • Watermark applied • Stored on this device"
                                        )
                                    }
                                )
                            }
                        },
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                    DocFlowPrimaryButton(
                        text = "Convert to PDF",
                        onClick = {
                            showImageToPdfSheet = false
                            val bitmaps = pickedImages.map { it.bitmap }
                            viewModel.convertImagesToPdfAndSave(
                                context = context,
                                baseTitle = imgOutputBaseTitle,
                                bitmaps = bitmaps,
                                pageSizeOption = imgPageSize,
                                orientationOption = imgOrientation,
                                marginOption = imgMargins,
                                watermarkConfig = PdfWatermarkConfig(enabled = false),
                                onCreated = { createdId ->
                                    conversionSuccess = ConversionSuccessState(
                                        createdDocId = createdId,
                                        outputTitle = viewModel.buildConvertedDocumentTitle(imgOutputBaseTitle),
                                        outputFormat = "PDF",
                                        summaryDetail = "${bitmaps.size.coerceAtLeast(1)} page(s) • $imgPageSize ($imgOrientation) • Stored on this device"
                                    )
                                }
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("img_to_pdf_confirm_button")
                    )
                }
                Spacer(modifier = Modifier.height(DocFlowTokens.space4))
            }
        }
    }

    // =========================================================================
    // SHEET B: PDF TO WORD (.docx with OCR option & honest layout notice)
    // =========================================================================
    if (showPdfToWordSheet) {
        val pdfDocs = documents.filter {
            it.fileType.equals("pdf", true) || it.fileType.equals("scanned", true)
        }
        ModalBottomSheet(
            onDismissRequest = { showPdfToWordSheet = false },
            containerColor = palette.surface,
            shape = DocFlowTokens.sheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.cardPaddingMax, vertical = DocFlowTokens.space2)
            ) {
                Text(
                    text = "PDF to Word (.docx)",
                    style = DocFlowTokens.sectionLabelStyle,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space1))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(DocFlowTokens.buttonShape)
                        .background(DocFlowTokens.AccentBlue.copy(alpha = 0.10f))
                        .padding(DocFlowTokens.space3)
                ) {
                    Text(
                        text = "Notice: Complex layouts may differ from the original. Extracts reading order, headings, paragraphs, lists & tables locally on this device.",
                        style = DocFlowTokens.captionStyle,
                        color = palette.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(DocFlowTokens.space2))

                // Scanned PDF OCR Toggle (Hindi + English)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Run OCR first (for scanned PDFs)",
                            style = DocFlowTokens.bodyStyle,
                            fontWeight = FontWeight.Medium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "On-device ML Kit Text Recognition (English + हिन्दी)",
                            style = DocFlowTokens.captionStyle,
                            color = palette.textSecondary
                        )
                    }
                    Switch(
                        checked = runOcrForPdfToWord,
                        onCheckedChange = { runOcrForPdfToWord = it }
                    )
                }

                if (runOcrForPdfToWord) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = pdfToWordOcrLang == OcrLanguage.ENGLISH,
                            onClick = { pdfToWordOcrLang = OcrLanguage.ENGLISH },
                            label = { Text("English OCR") }
                        )
                        FilterChip(
                            selected = pdfToWordOcrLang == OcrLanguage.HINDI,
                            onClick = { pdfToWordOcrLang = OcrLanguage.HINDI },
                            label = { Text("हिन्दी + English OCR") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(DocFlowTokens.space2))

                if (pdfDocs.isEmpty()) {
                    DocFlowEmptyState(
                        icon = Icons.Filled.PictureAsPdf,
                        title = "No PDF files on this device yet",
                        subtitle = "Scan a document or convert a file to PDF first, then convert it to Word (.docx).",
                        palette = palette
                    )
                } else {
                    pdfDocs.take(6).forEach { doc ->
                        DocFlowListRow(
                            title = doc.title,
                            subtitle = "${doc.fileName} • ${doc.pageCount} page(s) • Tap to convert to .docx",
                            palette = palette,
                            onClick = {
                                showPdfToWordSheet = false
                                viewModel.convertPdfToWordAndSave(
                                    context = context,
                                    sourceDoc = doc,
                                    runOcrFirst = runOcrForPdfToWord,
                                    ocrLanguage = pdfToWordOcrLang
                                ) { newDocId, pages, tables, images ->
                                    conversionSuccess = ConversionSuccessState(
                                        createdDocId = newDocId,
                                        outputTitle = viewModel.buildConvertedDocumentTitle(doc.title),
                                        outputFormat = "DOCX",
                                        summaryDetail = "Pages converted: $pages • Tables detected: $tables • Images kept: $images • Complex layouts may differ from the original"
                                    )
                                }
                            },
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Filled.PictureAsPdf,
                                    circleColor = DocFlowTokens.IconCircleRed
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(DocFlowTokens.space4))
            }
        }
    }

    // =========================================================================
    // SHEET C: WORD TO PDF (.doc, .docx) WITH WATERMARK OPTION
    // =========================================================================
    if (showWordToPdfSheet) {
        val wordDocs = documents.filter {
            it.fileType.equals("docx", true) || it.fileType.equals("doc", true)
        }
        ModalBottomSheet(
            onDismissRequest = { showWordToPdfSheet = false },
            containerColor = palette.surface,
            shape = DocFlowTokens.sheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.cardPaddingMax, vertical = DocFlowTokens.space2)
            ) {
                Text(
                    text = "Word to PDF (.docx / .doc)",
                    style = DocFlowTokens.sectionLabelStyle,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = "Preserves headings, tables, headers/footers & page size • Supports custom Text Watermark",
                    style = DocFlowTokens.captionStyle,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                DocFlowPrimaryButton(
                    text = "Pick .docx / .doc File from Device",
                    onClick = {
                        showWordToPdfSheet = false
                        externalFileConvertPicker.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                if (wordDocs.isEmpty()) {
                    Text(
                        text = "No Word documents in Recent yet. Create a Word document or pick one from device storage above.",
                        style = DocFlowTokens.captionStyle,
                        color = palette.textSecondary
                    )
                } else {
                    wordDocs.take(6).forEach { doc ->
                        DocFlowListRow(
                            title = doc.title,
                            subtitle = "${doc.fileName} • Tap to configure watermark & convert to PDF",
                            palette = palette,
                            onClick = {
                                showWordToPdfSheet = false
                                watermarkSheetTitle = doc.title
                                showWatermarkSheetForPendingAction = { wmConfig ->
                                    viewModel.convertFileToPdfAndSave(context, doc, wmConfig) { pdfDocId ->
                                        conversionSuccess = ConversionSuccessState(
                                            createdDocId = pdfDocId,
                                            outputTitle = viewModel.buildConvertedDocumentTitle(doc.title),
                                            outputFormat = "PDF",
                                            summaryDetail = "Converted Word to PDF${if (wmConfig.enabled) " with '${wmConfig.text}' watermark" else ""} • Stored on this device"
                                        )
                                    }
                                }
                            },
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Filled.Description,
                                    circleColor = DocFlowTokens.AccentBlue
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(DocFlowTokens.space4))
            }
        }
    }

    // =========================================================================
    // SHEET D: OFFICE TO PDF (DOCX, XLSX, PPTX, TXT)
    // =========================================================================
    if (showOfficeToPdfSheet) {
        val convertibleDocs = documents.filter {
            it.fileType.lowercase() in setOf("docx", "doc", "xlsx", "xls", "csv", "pptx", "ppt", "txt", "md", "note")
        }
        ModalBottomSheet(
            onDismissRequest = { showOfficeToPdfSheet = false },
            containerColor = palette.surface,
            shape = DocFlowTokens.sheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.cardPaddingMax, vertical = DocFlowTokens.space2)
            ) {
                Text(
                    text = "Convert to PDF",
                    style = DocFlowTokens.sectionLabelStyle,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space1))
                Text(
                    text = "Supported formats: DOCX, XLSX/CSV, PPTX, TXT.\nFidelity note: Text, headings, tables, and slide blocks are preserved locally on this device.",
                    style = DocFlowTokens.captionStyle,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                DocFlowPrimaryButton(
                    text = "Pick File from Device Storage",
                    onClick = {
                        showOfficeToPdfSheet = false
                        externalFileConvertPicker.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                convertibleDocs.take(6).forEach { doc ->
                    DocFlowListRow(
                        title = doc.title,
                        subtitle = "${doc.fileType.uppercase()} • ${doc.fileName} • Tap to convert",
                        palette = palette,
                        onClick = {
                            showOfficeToPdfSheet = false
                            watermarkSheetTitle = doc.title
                            showWatermarkSheetForPendingAction = { wmConfig ->
                                viewModel.convertFileToPdfAndSave(context, doc, wmConfig) { newId ->
                                    conversionSuccess = ConversionSuccessState(
                                        createdDocId = newId,
                                        outputTitle = viewModel.buildConvertedDocumentTitle(doc.title),
                                        outputFormat = "PDF",
                                        summaryDetail = "Saved in Manage > Created by me & top of Recent • Stored on this device"
                                    )
                                }
                            }
                        },
                        leading = {
                            DocFlowIconCircle(
                                icon = Icons.Filled.PictureAsPdf,
                                circleColor = DocFlowTokens.IconCircleRed
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(DocFlowTokens.space4))
            }
        }
    }

    // =========================================================================
    // SHEET E: EXPORT DOCUMENT VERSIONS OR NOTES TO PDF
    // =========================================================================
    if (showExportVersionsNotesSheet) {
        val activeDoc = selectedDocForVersionExport ?: documents.firstOrNull()
        val commentsForActiveDoc by remember(activeDoc?.id) {
            if (activeDoc != null) viewModel.observeDocumentComments(activeDoc.id)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }.collectAsState(initial = emptyList())

        val versionsForActiveDoc = remember(allVersions, activeDoc?.id) {
            if (activeDoc == null) allVersions else allVersions.filter { it.documentId == activeDoc.id }
        }

        ModalBottomSheet(
            onDismissRequest = { showExportVersionsNotesSheet = false },
            containerColor = palette.surface,
            shape = DocFlowTokens.sheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.cardPaddingMax, vertical = DocFlowTokens.space2)
            ) {
                Text(
                    text = "Export Versions or Notes to PDF",
                    style = DocFlowTokens.sectionLabelStyle,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = "Select individual document versions or notes and generate a standalone PDF stored on this device.",
                    style = DocFlowTokens.captionStyle,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space2))

                // Document selector chips
                if (documents.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        documents.take(8).forEach { doc ->
                            FilterChip(
                                selected = activeDoc?.id == doc.id,
                                onClick = { selectedDocForVersionExport = doc },
                                label = { Text(doc.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(DocFlowTokens.space2))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = exportTabMode == 0,
                        onClick = { exportTabMode = 0 },
                        label = { Text("Document Versions (${versionsForActiveDoc.size})") },
                        modifier = Modifier.testTag("export_pdf_tab_versions")
                    )
                    FilterChip(
                        selected = exportTabMode == 1,
                        onClick = { exportTabMode = 1 },
                        label = { Text("Document Notes (${commentsForActiveDoc.size})") },
                        modifier = Modifier.testTag("export_pdf_tab_notes")
                    )
                }

                Spacer(modifier = Modifier.height(DocFlowTokens.space2))

                if (activeDoc == null) {
                    DocFlowEmptyState(
                        icon = Icons.Filled.History,
                        title = "No documents available",
                        subtitle = "Create or edit a document first to save versions and notes on this device.",
                        palette = palette
                    )
                } else if (exportTabMode == 0) {
                    if (versionsForActiveDoc.isEmpty()) {
                        // Offer exporting current baseline version snapshot
                        DocFlowListRow(
                            title = "${activeDoc.title} — Current Version (v1)",
                            subtitle = "Export current document version snapshot to PDF",
                            palette = palette,
                            onClick = {
                                val snapshotVer = DocumentVersionEntity(
                                    id = "ver_${System.currentTimeMillis()}",
                                    documentId = activeDoc.id,
                                    userId = viewModel.currentUserId,
                                    versionNumber = 1,
                                    versionLabel = "Current Local Version",
                                    authorName = "Local Owner",
                                    stateVector = "sv-1",
                                    snapshotContent = activeDoc.content,
                                    byteSize = activeDoc.fileSize
                                )
                                viewModel.exportVersionSnapshotAsPdf(context, activeDoc, snapshotVer)
                                showExportVersionsNotesSheet = false
                            },
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Filled.PictureAsPdf,
                                    circleColor = DocFlowTokens.IconCircleRed
                                )
                            }
                        )
                    } else {
                        versionsForActiveDoc.forEach { ver ->
                            DocFlowListRow(
                                title = "v${ver.versionNumber}: ${ver.versionLabel}",
                                subtitle = "Author: ${ver.authorName} • Tap to generate PDF",
                                palette = palette,
                                onClick = {
                                    viewModel.exportVersionSnapshotAsPdf(context, activeDoc, ver)
                                    showExportVersionsNotesSheet = false
                                },
                                leading = {
                                    DocFlowIconCircle(
                                        icon = Icons.Filled.History,
                                        circleColor = DocFlowTokens.AccentBlue
                                    )
                                }
                            )
                        }
                    }
                } else {
                    if (commentsForActiveDoc.isEmpty()) {
                        DocFlowListRow(
                            title = "Export Summary Note for '${activeDoc.title}'",
                            subtitle = "Generate a standalone PDF note from this document's local notes",
                            palette = palette,
                            onClick = {
                                val adhocNote = DocumentCommentEntity(
                                    id = "note_${System.currentTimeMillis()}",
                                    documentId = activeDoc.id,
                                    userId = viewModel.currentUserId,
                                    authorName = "Local Owner",
                                    authorRole = "OWNER",
                                    anchorLineOrText = activeDoc.title,
                                    body = activeDoc.content.take(400),
                                    status = "OPEN"
                                )
                                viewModel.exportCommentOrNoteAsPdf(context, activeDoc, adhocNote)
                                showExportVersionsNotesSheet = false
                            },
                            leading = {
                                DocFlowIconCircle(
                                    icon = Icons.Filled.PictureAsPdf,
                                    circleColor = DocFlowTokens.IconCircleGreen
                                )
                            }
                        )
                    } else {
                        commentsForActiveDoc.forEach { comment ->
                            DocFlowListRow(
                                title = comment.body.take(48),
                                subtitle = "By ${comment.authorName} • Anchor: ${comment.anchorLineOrText}",
                                palette = palette,
                                onClick = {
                                    viewModel.exportCommentOrNoteAsPdf(context, activeDoc, comment)
                                    showExportVersionsNotesSheet = false
                                },
                                leading = {
                                    DocFlowIconCircle(
                                        icon = Icons.Filled.Description,
                                        circleColor = DocFlowTokens.IconCircleGreen
                                    )
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(DocFlowTokens.space4))
            }
        }
    }

    // Watermark Configuration Sheet
    showWatermarkSheetForPendingAction?.let { pendingCallback ->
        PdfWatermarkExportSheet(
            palette = palette,
            documentTitle = watermarkSheetTitle,
            previewTextSnippet = watermarkSheetTitle,
            initialEnabled = true,
            confirmLabel = "Convert & Export PDF",
            onDismiss = { showWatermarkSheetForPendingAction = null },
            onConfirmExport = { config ->
                val action = pendingCallback
                showWatermarkSheetForPendingAction = null
                action(config)
            }
        )
    }

    // Conversion Success Sheet (Open / Share / Convert another)
    conversionSuccess?.let { success ->
        ModalBottomSheet(
            onDismissRequest = { conversionSuccess = null },
            containerColor = palette.surface,
            shape = DocFlowTokens.sheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.cardPaddingMax, vertical = DocFlowTokens.space3),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DocFlowIconCircle(
                    icon = Icons.Filled.CheckCircle,
                    circleColor = DocFlowTokens.IconCircleGreen,
                    size = 56.dp
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space3))
                Text(
                    text = "Conversion Complete",
                    style = DocFlowTokens.sectionLabelStyle,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = success.outputTitle,
                    style = DocFlowTokens.rowTitleStyle,
                    color = DocFlowTokens.AccentBlue
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = success.summaryDetail,
                    style = DocFlowTokens.captionStyle,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space4))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                ) {
                    DocFlowSecondaryButton(
                        text = "Share",
                        onClick = {
                            val doc = viewModel.cachedDocumentsMap.value[success.createdDocId]?.toDocEntity()
                            if (doc != null) {
                                viewModel.shareAnyDocument(context, doc)
                            }
                        },
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                    DocFlowSecondaryButton(
                        text = "Convert another",
                        onClick = { conversionSuccess = null },
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                    DocFlowPrimaryButton(
                        text = "Open",
                        onClick = {
                            val targetId = success.createdDocId
                            conversionSuccess = null
                            onOpenDocument(targetId)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("conversion_success_open_button")
                    )
                }
                Spacer(modifier = Modifier.height(DocFlowTokens.space4))
            }
        }
    }

    // AI Mic Voice-to-Text Sheet
    if (showAiMicSheet) {
        AiMicDictationBottomSheet(
            palette = palette,
            initialText = "",
            confirmButtonLabel = "Save as Word Doc (.docx)",
            onDismiss = { showAiMicSheet = false },
            onConfirmCleanText = { cleaned ->
                val docTitle = cleaned.lines().firstOrNull()?.take(36)?.trim()?.ifBlank { "AI Mic Transcript" }
                    ?: "AI Mic Transcript"
                val cleanName = docTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                    .ifBlank { "ai_mic" } + ".docx"
                viewModel.createOrScanDocument(
                    title = docTitle,
                    folderId = "root",
                    category = "Work",
                    fileType = "docx",
                    fileName = cleanName,
                    plainContent = cleaned,
                    isEncrypted = false,
                    password = "",
                    encryptionKeyHint = null,
                    isOfflineCached = true,
                    pageCount = 1L,
                    customTags = "ai-mic, noise-clean, voice-to-text, created-by-me, docx",
                    createdByMe = true,
                    onCreated = { createdId ->
                        showAiMicSheet = false
                        onOpenDocument(createdId)
                    }
                )
            }
        )
    }

    // Create Word / PPT / XLS Dialog
    if (showCreateOfficeDialog) {
        AlertDialog(
            onDismissRequest = { showCreateOfficeDialog = false },
            title = { Text("Create Doc / PPT / XLS", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("docx" to "Word (.docx)", "pptx" to "PPT (.pptx)", "xlsx" to "Excel (.xlsx)").forEach { (ext, label) ->
                            FilterChip(
                                selected = createOfficeFormat == ext,
                                onClick = { createOfficeFormat = ext },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = createOfficeTitle,
                        onValueChange = { createOfficeTitle = it },
                        label = { Text("Document Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = createOfficeBody,
                        onValueChange = { createOfficeBody = it },
                        label = { Text("Initial Content") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ext = createOfficeFormat.lowercase()
                        val title = createOfficeTitle.trim().ifBlank {
                            when (ext) {
                                "pptx" -> "New Presentation"
                                "xlsx" -> "New Spreadsheet"
                                else -> "New Word Document"
                            }
                        }
                        val cleanName = title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                            .ifBlank { "office_doc" } + ".$ext"
                        val body = createOfficeBody.ifBlank { title }
                        viewModel.createOrScanDocument(
                            title = title,
                            folderId = "root",
                            category = "Work",
                            fileType = ext,
                            fileName = cleanName,
                            plainContent = body,
                            isEncrypted = false,
                            password = "",
                            encryptionKeyHint = null,
                            isOfflineCached = true,
                            pageCount = 1L,
                            customTags = "created-by-me, $ext",
                            createdByMe = true,
                            onCreated = { createdId ->
                                showCreateOfficeDialog = false
                                onOpenDocument(createdId)
                            }
                        )
                    }
                ) {
                    Text("Create & Open", fontWeight = FontWeight.Bold, color = DocFlowTokens.AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateOfficeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // AI Summarise Sheet
    if (showSummariseSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSummariseSheet = false },
            containerColor = palette.surface,
            shape = DocFlowTokens.sheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.cardPaddingMax, vertical = DocFlowTokens.space2)
            ) {
                Text(
                    text = "AI Summarise (On-Device TextRank)",
                    style = DocFlowTokens.sectionLabelStyle,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = "100% local extractive ranking • Stored on this device",
                    style = DocFlowTokens.captionStyle,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    documents.take(8).forEach { doc ->
                        FilterChip(
                            selected = selectedDocForSummary?.id == doc.id,
                            onClick = {
                                selectedDocForSummary = doc
                                summaryResult = OnDeviceAiAndBackupEngine.summarizeTextExtractive(doc.content)
                            },
                            label = { Text(doc.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                val currentSummary = summaryResult
                val targetDoc = selectedDocForSummary
                if (currentSummary != null && targetDoc != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DocFlowTokens.buttonShape)
                            .background(palette.background)
                            .padding(DocFlowTokens.cardPaddingMin)
                    ) {
                        Text(
                            text = "${currentSummary.wordCount} words • ${currentSummary.sentenceCount} sentences • ${currentSummary.compressionRatioPercent}% ratio",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DocFlowTokens.AccentBlue
                        )
                        Spacer(modifier = Modifier.height(DocFlowTokens.space2))
                        currentSummary.bulletPoints.forEach { point ->
                            Text(
                                text = "• $point",
                                style = DocFlowTokens.bodyStyle,
                                color = palette.textPrimary,
                                modifier = Modifier.padding(vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                    ) {
                        DocFlowSecondaryButton(
                            text = "Copy",
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("DocFlow Summary", currentSummary.executiveSummary)
                                )
                                Toast.makeText(context, "Summary copied", Toast.LENGTH_SHORT).show()
                            },
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )
                        DocFlowPrimaryButton(
                            text = "Save as Note",
                            onClick = {
                                viewModel.saveSummaryAsNote(targetDoc, currentSummary) { noteId ->
                                    showSummariseSheet = false
                                    onOpenDocument(noteId)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Text(
                        text = "Select or create a document first to generate an offline summary.",
                        style = DocFlowTokens.captionStyle,
                        color = palette.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(DocFlowTokens.space5))
            }
        }
    }

    // Translate PDF & Docs Sheet
    if (showTranslateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTranslateSheet = false },
            containerColor = palette.surface,
            shape = DocFlowTokens.sheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocFlowTokens.cardPaddingMax, vertical = DocFlowTokens.space2)
            ) {
                Text(
                    text = "Translate PDF & Documents (Offline)",
                    style = DocFlowTokens.sectionLabelStyle,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = "Downloadable on-device language packs • Stored on this device",
                    style = DocFlowTokens.captionStyle,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    documents.take(8).forEach { doc ->
                        FilterChip(
                            selected = selectedDocForTranslate?.id == doc.id,
                            onClick = {
                                selectedDocForTranslate = doc
                                translatedOutput = null
                            },
                            label = { Text(doc.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(DocFlowTokens.space2))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TranslationLanguage.entries.forEach { lang ->
                        val isInstalled = downloadedPacks.contains(lang.code)
                        FilterChip(
                            selected = selectedTargetLang == lang,
                            onClick = {
                                selectedTargetLang = lang
                                translatedOutput = null
                            },
                            label = {
                                Text("${lang.displayName} (${lang.nativeName})${if (isInstalled) " ✓" else " ⬇"}")
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                val isPackReady = downloadedPacks.contains(selectedTargetLang.code)
                val docToTranslate = selectedDocForTranslate

                if (!isPackReady) {
                    DocFlowPrimaryButton(
                        text = "Download ${selectedTargetLang.displayName} Pack (${selectedTargetLang.packSizeMb})",
                        onClick = {
                            OnDeviceAiAndBackupEngine.markLanguagePackDownloaded(context, selectedTargetLang.code)
                            downloadedPacks = OnDeviceAiAndBackupEngine.getDownloadedLanguagePacks(context)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (docToTranslate != null) {
                    if (translatedOutput == null) {
                        DocFlowPrimaryButton(
                            text = "Translate '${docToTranslate.title}' to ${selectedTargetLang.displayName}",
                            onClick = {
                                translatedOutput = OnDeviceAiAndBackupEngine.translateDocumentOffline(
                                    sourceTitle = docToTranslate.title,
                                    sourceText = docToTranslate.content,
                                    targetLanguage = selectedTargetLang
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(DocFlowTokens.buttonShape)
                                .background(palette.background)
                                .padding(DocFlowTokens.cardPaddingMin)
                        ) {
                            Text(
                                text = translatedOutput!!.take(480),
                                style = DocFlowTokens.bodyStyle,
                                color = palette.textPrimary,
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(DocFlowTokens.space3))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DocFlowTokens.space2)
                        ) {
                            DocFlowSecondaryButton(
                                text = "Save as TXT",
                                onClick = {
                                    viewModel.saveTranslationDocument(
                                        context = context,
                                        sourceDoc = docToTranslate,
                                        translatedText = translatedOutput!!,
                                        targetLang = selectedTargetLang,
                                        exportAsPdf = false
                                    ) { newId ->
                                        showTranslateSheet = false
                                        onOpenDocument(newId)
                                    }
                                },
                                palette = palette,
                                modifier = Modifier.weight(1f)
                            )
                            DocFlowPrimaryButton(
                                text = "Export PDF",
                                onClick = {
                                    viewModel.saveTranslationDocument(
                                        context = context,
                                        sourceDoc = docToTranslate,
                                        translatedText = translatedOutput!!,
                                        targetLang = selectedTargetLang,
                                        exportAsPdf = true
                                    ) { newId ->
                                        showTranslateSheet = false
                                        onOpenDocument(newId)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(DocFlowTokens.space5))
            }
        }
    }
}
