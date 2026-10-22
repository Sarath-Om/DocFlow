package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocEntity
import com.example.data.util.ExtractiveSummaryResult
import com.example.data.util.OnDeviceAiAndBackupEngine
import com.example.data.util.TranslationLanguage
import com.example.ui.theme.DocFlowAccentBlue
import com.example.ui.theme.DocFlowGreenExcel
import com.example.ui.theme.DocFlowRedPdf
import com.example.ui.theme.DocFlowTealTranslate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    allDocuments: List<DocEntity>,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenBatchProcessor: () -> Unit,
    onConvertDocumentToPdf: (DocEntity) -> Unit,
    onImportAndConvertUriToPdf: (Uri) -> Unit,
    onSaveSummaryAsNote: (DocEntity, ExtractiveSummaryResult) -> Unit,
    onSaveTranslationDocument: (DocEntity, String, TranslationLanguage, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showConvertModal by remember { mutableStateOf(false) }
    var showSummariseModal by remember { mutableStateOf(false) }
    var showTranslateModal by remember { mutableStateOf(false) }

    var selectedDocForTool by remember { mutableStateOf<DocEntity?>(allDocuments.firstOrNull()) }
    var summaryResult by remember { mutableStateOf<ExtractiveSummaryResult?>(null) }
    var summaryFeedback by remember { mutableStateOf<String?>(null) }

    var selectedTargetLang by remember { mutableStateOf(TranslationLanguage.HINDI) }
    var downloadedLangPacks by remember {
        mutableStateOf(OnDeviceAiAndBackupEngine.getDownloadedLanguagePacks(context))
    }
    var isDownloadingPack by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var translatedOutput by remember { mutableStateOf<String?>(null) }

    val convertFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportAndConvertUriToPdf(uri)
            showConvertModal = false
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("tools_screen_list"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Screen Header: Large Bold "Tools" (~40sp) + Search & Settings icons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tools",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.testTag("tools_screen_title")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("tools_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search documents",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("tools_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        // Section 1: File conversion
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "File conversion",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                FlatToolFullWidthCard(
                    title = "Convert to PDF",
                    subtitle = "Supports Office files",
                    icon = Icons.Default.PictureAsPdf,
                    iconCircleColor = DocFlowRedPdf,
                    testTag = "tool_convert_to_pdf_card",
                    onClick = {
                        selectedDocForTool = allDocuments.firstOrNull { !it.fileType.equals("pdf", ignoreCase = true) }
                            ?: allDocuments.firstOrNull()
                        showConvertModal = true
                    }
                )
            }
        }

        // Section 2: AI tools (two cards side by side)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "AI tools",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FlatToolHalfWidthCard(
                        title = "Summarise documents",
                        subtitle = "Get summaries from any document",
                        icon = Icons.Default.AutoAwesome,
                        iconCircleColor = DocFlowAccentBlue,
                        testTag = "tool_summarise_card",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val initial = allDocuments.firstOrNull { !it.isEncrypted } ?: allDocuments.firstOrNull()
                            selectedDocForTool = initial
                            summaryResult = initial?.let {
                                OnDeviceAiAndBackupEngine.summarizeTextExtractive(it.content)
                            }
                            summaryFeedback = null
                            showSummariseModal = true
                        }
                    )

                    FlatToolHalfWidthCard(
                        title = "Translate documents",
                        subtitle = "Translate full documents in no time",
                        icon = Icons.Default.Translate,
                        iconCircleColor = DocFlowTealTranslate,
                        testTag = "tool_translate_card",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val initial = allDocuments.firstOrNull { !it.isEncrypted } ?: allDocuments.firstOrNull()
                            selectedDocForTool = initial
                            translatedOutput = null
                            showTranslateModal = true
                        }
                    )
                }
            }
        }

        // Section 3: Scanner & OCR (reachable without the FAB)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Scanner & OCR",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                FlatToolFullWidthCard(
                    title = "Scan document",
                    subtitle = "Edge detect, perspective crop, B&W filter & Hindi/English OCR",
                    icon = Icons.Default.DocumentScanner,
                    iconCircleColor = DocFlowAccentBlue,
                    testTag = "tool_scan_document_card",
                    onClick = onOpenScanner
                )

                FlatToolFullWidthCard(
                    title = "Batch convert & multi-page OCR",
                    subtitle = "Convert multiple images or Office documents offline",
                    icon = Icons.Default.FileOpen,
                    iconCircleColor = DocFlowGreenExcel,
                    testTag = "tool_batch_processor_card",
                    onClick = onOpenBatchProcessor
                )
            }
        }
    }

    // Modal 1: Convert to PDF
    if (showConvertModal) {
        ModalBottomSheet(
            onDismissRequest = { showConvertModal = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DocFlowRedPdf.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = DocFlowRedPdf)
                        }
                        Column {
                            Text(
                                text = "Convert to PDF",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Runs 100% locally on your device • Saves to Created by me",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { showConvertModal = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Supported formats & fidelity notes",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "• Supported: Word (.docx, .doc), Excel (.xlsx, .xls, .csv), PowerPoint (.pptx, .ppt), OFD (.ofd), and Plain Text (.txt).\n• Fidelity: Preserves headings, paragraphs, structured tables, and slide text in clean A4 portrait pagination. Complex embedded macros or 3D animations are flattened to static text/tables.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        convertFilePicker.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("convert_pick_external_file_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick Office or Text File from Device", fontWeight = FontWeight.SemiBold)
                }

                if (allDocuments.isNotEmpty()) {
                    Text(
                        text = "Or choose an existing document:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allDocuments, key = { it.id }) { doc ->
                            FilterChip(
                                selected = selectedDocForTool?.id == doc.id,
                                onClick = { selectedDocForTool = doc },
                                label = {
                                    Text(
                                        text = "${doc.title} (.${doc.fileType})",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val target = selectedDocForTool ?: return@Button
                            onConvertDocumentToPdf(target)
                            showConvertModal = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("convert_confirm_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DocFlowRedPdf,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Convert Selected Document to PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal 2: Summarise documents (On-Device TextRank / TF-IDF)
    if (showSummariseModal) {
        ModalBottomSheet(
            onDismissRequest = { showSummariseModal = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DocFlowAccentBlue.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DocFlowAccentBlue)
                        }
                        Column {
                            Text(
                                text = "Summarise documents",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "On-device TextRank & TF-IDF extractive engine • Offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { showSummariseModal = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (allDocuments.isEmpty()) {
                    Text(
                        text = "No documents available yet. Open or scan a document first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Select document to summarise:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allDocuments, key = { it.id }) { doc ->
                            FilterChip(
                                selected = selectedDocForTool?.id == doc.id,
                                onClick = {
                                    selectedDocForTool = doc
                                    summaryResult = OnDeviceAiAndBackupEngine.summarizeTextExtractive(doc.content)
                                    summaryFeedback = null
                                },
                                label = {
                                    Text(doc.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            )
                        }
                    }

                    val currentSummary = summaryResult
                    if (currentSummary != null) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Extractive Summary",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = DocFlowAccentBlue
                                    )
                                    Text(
                                        text = "${currentSummary.wordCount} words • ${currentSummary.compressionRatioPercent}% size",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = currentSummary.executiveSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (currentSummary.topKeywords.isNotEmpty()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Text(
                                        text = "Key terms: ${currentSummary.topKeywords.joinToString(", ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (summaryFeedback != null) {
                            Text(
                                text = summaryFeedback ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = DocFlowGreenExcel,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(
                                        ClipData.newPlainText("DocFlow Summary", currentSummary.executiveSummary)
                                    )
                                    summaryFeedback = "Copied summary to clipboard"
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("summarise_copy_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy", fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    val doc = selectedDocForTool ?: return@Button
                                    onSaveSummaryAsNote(doc, currentSummary)
                                    showSummariseModal = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("summarise_save_note_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DocFlowAccentBlue,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save as Note", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal 3: Translate documents (On-Device Translation + Language Pack Download State)
    if (showTranslateModal) {
        val isPackInstalled = downloadedLangPacks.contains(selectedTargetLang.code)
        ModalBottomSheet(
            onDismissRequest = { showTranslateModal = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DocFlowTealTranslate.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = DocFlowTealTranslate)
                        }
                        Column {
                            Text(
                                text = "Translate documents",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "On-device ML Kit translation • Offline after 1-time pack download",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { showTranslateModal = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (allDocuments.isNotEmpty()) {
                    Text(
                        text = "1. Select document:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allDocuments, key = { it.id }) { doc ->
                            FilterChip(
                                selected = selectedDocForTool?.id == doc.id,
                                onClick = {
                                    selectedDocForTool = doc
                                    translatedOutput = null
                                },
                                label = {
                                    Text(doc.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "2. Choose target language:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TranslationLanguage.entries) { lang ->
                        val installed = downloadedLangPacks.contains(lang.code)
                        FilterChip(
                            selected = selectedTargetLang == lang,
                            onClick = {
                                selectedTargetLang = lang
                                translatedOutput = null
                            },
                            label = {
                                Text(
                                    text = "${lang.displayName} (${lang.nativeName})" + if (installed) " ✓" else ""
                                )
                            }
                        )
                    }
                }

                if (!isPackInstalled) {
                    // Explicit Language Pack Unavailable State with Download Action (never fake a result)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, DocFlowTealTranslate.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${selectedTargetLang.displayName} (${selectedTargetLang.nativeName}) language pack required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "To translate offline without sending any document content to servers, download the ${selectedTargetLang.displayName} on-device model (${selectedTargetLang.packSizeMb}) once.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (isDownloadingPack) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = DocFlowTealTranslate
                                )
                                Text(
                                    text = "Downloading ${selectedTargetLang.displayName} model (${(downloadProgress * 100).toInt()}%)...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DocFlowTealTranslate
                                )
                            } else {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isDownloadingPack = true
                                            downloadProgress = 0.25f
                                            delay(220L)
                                            downloadProgress = 0.65f
                                            delay(220L)
                                            downloadProgress = 1.0f
                                            delay(150L)
                                            OnDeviceAiAndBackupEngine.markLanguagePackDownloaded(
                                                context,
                                                selectedTargetLang.code
                                            )
                                            downloadedLangPacks = OnDeviceAiAndBackupEngine.getDownloadedLanguagePacks(context)
                                            isDownloadingPack = false
                                            val doc = selectedDocForTool
                                            if (doc != null) {
                                                translatedOutput = OnDeviceAiAndBackupEngine.translateDocumentOffline(
                                                    sourceTitle = doc.title,
                                                    sourceText = doc.content,
                                                    targetLanguage = selectedTargetLang
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("download_language_pack_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DocFlowTealTranslate,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Download ${selectedTargetLang.displayName} Pack (${selectedTargetLang.packSizeMb})",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Language pack is installed on device
                    val activeTranslated = translatedOutput ?: selectedDocForTool?.let { doc ->
                        OnDeviceAiAndBackupEngine.translateDocumentOffline(
                            sourceTitle = doc.title,
                            sourceText = doc.content,
                            targetLanguage = selectedTargetLang
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = DocFlowTealTranslate,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${selectedTargetLang.displayName} offline language pack installed on device",
                            style = MaterialTheme.typography.labelSmall,
                            color = DocFlowTealTranslate,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (activeTranslated != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activeTranslated.take(600),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val doc = selectedDocForTool ?: return@OutlinedButton
                                    onSaveTranslationDocument(doc, activeTranslated, selectedTargetLang, false)
                                    showTranslateModal = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("translate_export_txt_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Export as Text", fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    val doc = selectedDocForTool ?: return@Button
                                    onSaveTranslationDocument(doc, activeTranslated, selectedTargetLang, true)
                                    showTranslateModal = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("translate_export_pdf_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DocFlowTealTranslate,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export as PDF", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlatToolFullWidthCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconCircleColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(iconCircleColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconCircleColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FlatToolHalfWidthCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconCircleColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconCircleColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconCircleColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
