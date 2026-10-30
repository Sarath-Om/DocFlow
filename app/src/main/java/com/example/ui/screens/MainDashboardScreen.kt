package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ApryseDocumentModel
import com.example.data.model.DocumentFormat
import com.example.ui.theme.DocFlowTokens
import com.example.ui.viewmodel.DocumentUiState
import com.example.ui.viewmodel.DocumentViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main dashboard landing screen providing Storage Access Framework (SAF)
 * document pickers for PDF, Word, Excel, and PowerPoint, format filtering,
 * starter document generators, and recent file browsing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    viewModel: DocumentViewModel,
    onOpenDocumentViewer: (ApryseDocumentModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFormatFilter.collectAsStateWithLifecycle()
    val recentDocs by viewModel.recentDocuments.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // When document load succeeds, trigger navigation callback
    LaunchedEffect(uiState) {
        if (uiState is DocumentUiState.Success) {
            onOpenDocumentViewer((uiState as DocumentUiState.Success).document)
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissUserMessage()
        }
    }

    // Generic SAF file picker (All supported formats)
    val openAnyDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadDocument(it) }
    }

    // Format-specific SAF file pickers
    val openPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.loadDocument(it) } }

    val openWordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.loadDocument(it) } }

    val openExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.loadDocument(it) } }

    val openPptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.loadDocument(it) } }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_dashboard_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DocFlowTokens.AccentBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Apryse Document Suite",
                                    tint = DocFlowTokens.AccentBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "DocFlow Studio",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                            Text(
                                text = "Apryse Client-Side Engine",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner: 100% Client-Side Notice
                item {
                    ClientSideBannerCard()
                }

                // Primary Pick Document Action Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .testTag("primary_open_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Browse & Edit Files",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                            Text(
                                text = "Pick any PDF, Word (.docx), Excel (.xlsx), or PowerPoint (.pptx) file from your device storage securely via Android Storage Access Framework (SAF).",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            )
                            Button(
                                onClick = {
                                    openAnyDocumentLauncher.launch(DocumentFormat.allMimeTypes)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("open_any_file_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DocFlowTokens.AccentBlue
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Select Local Document",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Format Cards Grid: PDF, Word, Excel, PowerPoint
                item {
                    Text(
                        text = "Pick By Format",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FormatPickerTile(
                            title = "PDF",
                            subtitle = ".pdf files",
                            icon = Icons.Default.PictureAsPdf,
                            color = Color(0xFFC62828),
                            bgColor = Color(0xFFFFEBEE),
                            modifier = Modifier.weight(1f),
                            onClick = { openPdfLauncher.launch(DocumentFormat.PDF.mimeTypes) },
                            testTag = "pick_pdf_tile"
                        )
                        FormatPickerTile(
                            title = "Word",
                            subtitle = ".docx, .doc",
                            icon = Icons.Default.Description,
                            color = Color(0xFF1967D2),
                            bgColor = Color(0xFFE8F0FE),
                            modifier = Modifier.weight(1f),
                            onClick = { openWordLauncher.launch(DocumentFormat.WORD.mimeTypes) },
                            testTag = "pick_word_tile"
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FormatPickerTile(
                            title = "Excel",
                            subtitle = ".xlsx, .csv",
                            icon = Icons.Default.TableChart,
                            color = Color(0xFF137333),
                            bgColor = Color(0xFFE6F4EA),
                            modifier = Modifier.weight(1f),
                            onClick = { openExcelLauncher.launch(DocumentFormat.EXCEL.mimeTypes) },
                            testTag = "pick_excel_tile"
                        )
                        FormatPickerTile(
                            title = "PowerPoint",
                            subtitle = ".pptx, .ppt",
                            icon = Icons.Default.Slideshow,
                            color = Color(0xFFC5221F),
                            bgColor = Color(0xFFFCE8E6),
                            modifier = Modifier.weight(1f),
                            onClick = { openPptLauncher.launch(DocumentFormat.POWERPOINT.mimeTypes) },
                            testTag = "pick_powerpoint_tile"
                        )
                    }
                }

                // Instant Starter Documents
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Or Create Sample Document",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = "Test on-device viewing and editing immediately without external files.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(DocumentFormat.entries.toTypedArray()) { format ->
                                OutlinedButton(
                                    onClick = { viewModel.loadSampleDocument(format) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("sample_${format.extension}_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+ ${format.extension.uppercase()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Recent Documents Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recently Opened",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                        if (recentDocs.isNotEmpty()) {
                            Text(
                                text = "${recentDocs.size} files",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                if (recentDocs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "No documents opened yet",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Text(
                                        text = "Select a local file or tap a sample above to start",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(recentDocs) { doc ->
                        RecentDocumentCard(
                            document = doc,
                            onClick = { onOpenDocumentViewer(doc) }
                        )
                    }
                }
            }

            // Loading Overlay
            if (uiState is DocumentUiState.Loading) {
                Surface(
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(color = DocFlowTokens.AccentBlue)
                                Text(
                                    text = (uiState as DocumentUiState.Loading).message,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Error Overlay
            if (uiState is DocumentUiState.Error) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Error Opening Document",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = (uiState as DocumentUiState.Error).message,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Button(
                                    onClick = { viewModel.clearError() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatPickerTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = color
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = Color(0xFF49454F)
                )
            )
        }
    }
}

@Composable
private fun RecentDocumentCard(
    document: ApryseDocumentModel,
    onClick: () -> Unit
) {
    val dateStr = remember(document.lastModifiedMillis) {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(document.lastModifiedMillis))
    }

    val sizeStr = remember(document.sizeBytes) {
        when {
            document.sizeBytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", document.sizeBytes / (1024.0 * 1024.0))
            document.sizeBytes >= 1024 -> String.format(Locale.US, "%.1f KB", document.sizeBytes / 1024.0)
            else -> "${document.sizeBytes} B"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("recent_doc_${document.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FormatBadge(format = document.format)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = document.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = "$sizeStr • $dateStr",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ClientSideBannerCard() {
    Surface(
        color = Color(0xFFE8F0FE),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = DocFlowTokens.AccentBlue,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "100% Client-Side Processing",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = DocFlowTokens.AccentBlue,
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = "All PDF, Word, Excel, and PowerPoint rendering and editing happens offline on this device via the Apryse Mobile SDK. No external servers or cloud dependencies.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF3C4043),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}
