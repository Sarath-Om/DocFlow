package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DocEntity
import com.example.data.model.FolderEntity
import com.example.data.util.DocToPicMode
import com.example.data.util.MultiLanguageOcrEngine
import com.example.data.util.OcrLanguage
import com.example.ui.components.CrystalGlassBackground
import com.example.ui.components.CrystalGlassCard

enum class BatchWorkflowTab(val label: String) {
    DOC_TO_PIC("Batch Doc to Pic (Multi-Doc)"),
    PIC_TO_DOC("Batch Pic to Doc (Multi-Image)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchProcessorScreen(
    allDocuments: List<DocEntity>,
    folders: List<FolderEntity>,
    initialFolderId: String,
    onBack: () -> Unit,
    onRunBatchDocToPic: (
        selectedDocs: List<DocEntity>,
        mode: DocToPicMode,
        customBatchTag: String,
        onFinished: (Int) -> Unit
    ) -> Unit,
    onRunBatchPicToDoc: (
        selectedUris: List<Uri>,
        batchTitlePrefix: String,
        targetFormat: String,
        compileIntoSingleDoc: Boolean,
        selectedLanguage: OcrLanguage,
        folderId: String,
        category: String,
        customBatchTag: String,
        onFinished: (Int) -> Unit
    ) -> Unit
) {
    BackHandler { onBack() }

    var activeTab by remember { mutableStateOf(BatchWorkflowTab.DOC_TO_PIC) }
    var isProcessing by remember { mutableStateOf(false) }
    var lastBatchResultBanner by remember { mutableStateOf<String?>(null) }

    // Tab 1: Batch Doc-to-Pic State
    val selectedDocIds = remember {
        mutableStateListOf<String>().apply {
            allDocuments.take(3).forEach { add(it.id) }
        }
    }
    var batchDocToPicMode by remember { mutableStateOf(DocToPicMode.STITCHED_SINGLE) }
    var batchDocTag by remember { mutableStateOf("batch-export, archived") }

    // Tab 2: Batch Pic-to-Doc State
    val selectedImageUris = remember { mutableStateListOf<Uri>() }
    var simulatedQueueCount by remember { mutableIntStateOf(3) }
    var batchTitlePrefix by remember { mutableStateOf("Multi-Page Batch Scan") }
    var targetFormat by remember { mutableStateOf("pdf") }
    var compileIntoSingleDoc by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf(OcrLanguage.AUTO) }
    var selectedFolderId by remember {
        mutableStateOf(if (initialFolderId == "all") "root" else initialFolderId)
    }
    var selectedCategory by remember { mutableStateOf("Invoices") }
    var batchPicTag by remember { mutableStateOf("batch-ocr, multi-page") }

    val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 12)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.clear()
            selectedImageUris.addAll(uris)
            simulatedQueueCount = uris.size
        }
    }

    val liveDetectionPreview = remember(batchTitlePrefix, selectedLanguage) {
        MultiLanguageOcrEngine.detectLanguage(batchTitlePrefix, selectedLanguage)
    }

    val categories = listOf("Work", "Personal", "Finance", "Contracts", "Invoices", "Receipts", "Legal", "Medical", "Notes")

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
                                text = "Simultaneous Batch Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Multi-Doc to Image & Multi-Image to Office/PDF + Multi-Language OCR",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("batch_back_button")
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Workflow Mode Switcher
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BatchWorkflowTab.entries.forEach { tab ->
                            val isSelected = activeTab == tab
                            Button(
                                onClick = { activeTab = tab },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("batch_tab_${tab.name.lowercase()}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF0066FF) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = if (tab == BatchWorkflowTab.DOC_TO_PIC) Icons.Default.Image else Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tab.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            }
                        }
                    }
                }

                if (lastBatchResultBanner != null) {
                    item {
                        CrystalGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp,
                            tintColor = Color(0xFF10B981)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981)
                                )
                                Text(
                                    text = lastBatchResultBanner ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (activeTab == BatchWorkflowTab.DOC_TO_PIC) {
                    // TAB 1: BATCH DOC-TO-PIC WORKFLOW
                    item {
                        CrystalGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "1. Select Documents for Batch Image Export",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${selectedDocIds.size} of ${allDocuments.size} document(s) selected for simultaneous conversion",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            if (selectedDocIds.size == allDocuments.size) {
                                                selectedDocIds.clear()
                                            } else {
                                                selectedDocIds.clear()
                                                selectedDocIds.addAll(allDocuments.map { it.id })
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("batch_select_all_docs_button")
                                    ) {
                                        Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (selectedDocIds.size == allDocuments.size) "Clear All" else "Select All")
                                    }
                                }

                                Text(
                                    text = "2. Choose Batch Image Output Layout:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DocToPicMode.entries.forEach { mode ->
                                        FilterChip(
                                            selected = batchDocToPicMode == mode,
                                            onClick = { batchDocToPicMode = mode },
                                            label = { Text(mode.label) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = batchDocTag,
                                    onValueChange = { batchDocTag = it },
                                    label = { Text("Assign Custom Tags to Batch Exports (comma separated)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    items(allDocuments, key = { it.id }) { doc ->
                        val isSelected = selectedDocIds.contains(doc.id)
                        CrystalGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("batch_doc_item_${doc.id}"),
                            cornerRadius = 18.dp,
                            tintColor = if (isSelected) Color(0xFF0066FF) else null,
                            onClick = {
                                if (isSelected) selectedDocIds.remove(doc.id) else selectedDocIds.add(doc.id)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (!selectedDocIds.contains(doc.id)) selectedDocIds.add(doc.id)
                                        } else {
                                            selectedDocIds.remove(doc.id)
                                        }
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${doc.fileType.uppercase()} • ${doc.category} • ${doc.pageCount} page/slide(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                val docsToConvert = allDocuments.filter { selectedDocIds.contains(it.id) }
                                isProcessing = true
                                onRunBatchDocToPic(docsToConvert, batchDocToPicMode, batchDocTag) { count ->
                                    isProcessing = false
                                    lastBatchResultBanner = "Successfully converted $count document(s) into high-res PNG images and indexed with tags: #$batchDocTag"
                                }
                            },
                            enabled = selectedDocIds.isNotEmpty() && !isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("execute_batch_doc_to_pic_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0891B2),
                                contentColor = Color.White
                            )
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Batch Converting ${selectedDocIds.size} Docs...")
                            } else {
                                Icon(Icons.Default.Collections, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Batch Convert ${selectedDocIds.size} Doc(s) to Pictures",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // TAB 2: BATCH PIC-TO-DOC WORKFLOW + MULTI-LANGUAGE OCR
                    item {
                        CrystalGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "1. Select Multiple Images for Batch Pic-to-Doc",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Pick multiple photos from gallery or adjust the batch page queue to simultaneously convert images into editable PDF, Word, Excel, or PowerPoint files.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            multiPhotoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("batch_pick_images_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Select Multiple Photos")
                                    }

                                    OutlinedButton(
                                        onClick = { simulatedQueueCount = (simulatedQueueCount % 6) + 2 },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Queue: $simulatedQueueCount Images")
                                    }
                                }

                                OutlinedTextField(
                                    value = batchTitlePrefix,
                                    onValueChange = { batchTitlePrefix = it },
                                    label = { Text("Batch Document Title / Prefix") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("batch_pic_title_input"),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Multi-Language OCR + Auto Detection Selector
                                Text(
                                    text = "2. Multi-Language OCR & Automatic Language Detection:",
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
                                            onClick = { selectedLanguage = lang },
                                            label = { Text("${lang.displayName} (${lang.nativeLabel})") },
                                            leadingIcon = if (selectedLanguage == lang) {
                                                { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null
                                        )
                                    }
                                }

                                // Live Language Detection Status Card
                                Surface(
                                    color = Color(0xFF0066FF).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = Color(0xFF0066FF)
                                        )
                                        Column {
                                            Text(
                                                text = "Language Detection: ${liveDetectionPreview.detectedLanguage.displayName} (${liveDetectionPreview.detectedLanguage.nativeLabel}) • ${liveDetectionPreview.confidencePercent}% Confidence",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0066FF)
                                            )
                                            Text(
                                                text = "Script: ${liveDetectionPreview.scriptFamily} • ${liveDetectionPreview.characterSummary}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Target Output Format (PDF, Word, Excel, PowerPoint)
                                Text(
                                    text = "3. Target Document Output Format:",
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
                                        "pptx" to "PowerPoint (.pptx)"
                                    ).forEach { (ext, label) ->
                                        FilterChip(
                                            selected = targetFormat == ext,
                                            onClick = { targetFormat = ext },
                                            label = { Text(label) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = when (ext) {
                                                        "pdf" -> Icons.Default.PictureAsPdf
                                                        "xlsx" -> Icons.Default.TableChart
                                                        "pptx" -> Icons.Default.Slideshow
                                                        else -> Icons.Default.Description
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                }

                                // Compile into Single Multi-Page Document vs Individual Documents
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Compile All Images into Single Multi-Page Document",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (compileIntoSingleDoc) {
                                                "Merges all $simulatedQueueCount images into one multi-page ${targetFormat.uppercase()} + single PDF"
                                            } else {
                                                "Creates $simulatedQueueCount separate ${targetFormat.uppercase()} documents simultaneously"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = compileIntoSingleDoc,
                                        onCheckedChange = { compileIntoSingleDoc = it }
                                    )
                                }

                                // Category & Custom Tags
                                Text(
                                    text = "Category & Custom Search Tags:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    categories.forEach { cat ->
                                        FilterChip(
                                            selected = selectedCategory == cat,
                                            onClick = { selectedCategory = cat },
                                            label = { Text(cat) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = batchPicTag,
                                    onValueChange = { batchPicTag = it },
                                    label = { Text("Custom Tags for Search Filtering (comma separated)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Button(
                                    onClick = {
                                        isProcessing = true
                                        onRunBatchPicToDoc(
                                            selectedImageUris.toList(),
                                            batchTitlePrefix,
                                            targetFormat,
                                            compileIntoSingleDoc,
                                            selectedLanguage,
                                            selectedFolderId,
                                            selectedCategory,
                                            batchPicTag
                                        ) { createdCount ->
                                            isProcessing = false
                                            lastBatchResultBanner = "Batch Pic-to-${targetFormat.uppercase()} finished! Created $createdCount document(s) with ${liveDetectionPreview.detectedLanguage.displayName} OCR."
                                        }
                                    },
                                    enabled = !isProcessing,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("execute_batch_pic_to_doc_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF5E17EB),
                                        contentColor = Color.White
                                    )
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Running Multi-Language OCR & Batch Pipeline...")
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (compileIntoSingleDoc) {
                                                "Compile $simulatedQueueCount Images into Single ${targetFormat.uppercase()}"
                                            } else {
                                                "Batch Convert $simulatedQueueCount Images to ${targetFormat.uppercase()}"
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
