package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.DocFlowDatabase
import com.example.data.repository.DocFlowRepository
import com.example.data.util.BiometricSecurityHelper
import com.example.data.util.OnDeviceAiAndBackupEngine
import com.example.ui.components.DocFlowBottomNav
import com.example.ui.components.DocFlowCard
import com.example.ui.screens.BatchProcessorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocumentViewerEditorScreen
import com.example.ui.screens.ManageScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StorageDashboardScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTheme
import com.example.ui.theme.DocFlowTokens
import com.example.ui.viewmodel.DocFlowViewModel
import com.example.ui.viewmodel.ThemeMode
import com.example.ui.viewmodel.UiState
import com.example.widget.DocFlowAppWidgetProvider

enum class BottomNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    RECENT(
        label = "Recent",
        selectedIcon = Icons.Default.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "bottom_tab_recent"
    ),
    TOOLS(
        label = "Tools",
        selectedIcon = Icons.Default.GridView,
        unselectedIcon = Icons.Outlined.GridView,
        testTag = "bottom_tab_tools"
    ),
    MANAGE(
        label = "Manage",
        selectedIcon = Icons.Default.Folder,
        unselectedIcon = Icons.Outlined.Folder,
        testTag = "bottom_tab_manage"
    )
}

sealed interface AppScreen {
    data object MainTabs : AppScreen
    data object Settings : AppScreen
    data object StorageDashboard : AppScreen
    data object Scanner : AppScreen
    data object BatchProcessor : AppScreen
    data class DocumentDetail(val documentId: String) : AppScreen
}

class MainActivity : FragmentActivity() {
    private var incomingExternalIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (isExternalDocumentIntent(intent)) {
            incomingExternalIntent = intent
        }
        setContent {
            DocFlowRootApp(
                incomingIntent = incomingExternalIntent,
                onConsumeIncomingIntent = { incomingExternalIntent = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isExternalDocumentIntent(intent)) {
            incomingExternalIntent = intent
        }
    }

    private fun isExternalDocumentIntent(candidate: Intent?): Boolean {
        if (candidate == null) return false
        val action = candidate.action
        return action == Intent.ACTION_VIEW ||
            action == Intent.ACTION_EDIT ||
            action == Intent.ACTION_SEND ||
            action == Intent.ACTION_SEND_MULTIPLE ||
            action == DocFlowAppWidgetProvider.ACTION_OPEN_DOCUMENT ||
            action == DocFlowAppWidgetProvider.ACTION_OPEN_SCANNER ||
            candidate.data != null ||
            candidate.clipData != null
    }
}

@Composable
fun DocFlowRootApp(
    incomingIntent: Intent? = null,
    onConsumeIncomingIntent: () -> Unit = {}
) {
    val context = LocalContext.current
    val roomDao = remember(context) {
        DocFlowDatabase.getInstance(context).docFlowDao()
    }

    val viewModel: DocFlowViewModel = viewModel(
        key = "local_device_user",
        factory = viewModelFactory {
            initializer {
                DocFlowViewModel(
                    repository = DocFlowRepository.createOfflineLocal(),
                    currentUserId = "local_device_user",
                    dao = roomDao,
                    currentUserDisplayName = "Local Device Owner",
                    currentUserEmail = "",
                    appContext = context.applicationContext
                )
            }
        }
    )

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val palette = remember(useDarkTheme) {
        DocFlowTokens.palette(useDarkTheme)
    }

    DocFlowTheme(darkTheme = useDarkTheme) {
        OfflineDocFlowWorkspace(
            viewModel = viewModel,
            palette = palette,
            incomingIntent = incomingIntent,
            onConsumeIncomingIntent = onConsumeIncomingIntent
        )
    }
}

@Composable
private fun OfflineDocFlowWorkspace(
    viewModel: DocFlowViewModel,
    palette: DocFlowPalette,
    incomingIntent: Intent? = null,
    onConsumeIncomingIntent: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.MainTabs) }
    var activeTab by remember { mutableStateOf(BottomNavTab.RECENT) }

    val appEntryBiometricGateEnabled = remember {
        OnDeviceAiAndBackupEngine.isAppLockEnabled(context)
    }
    var isAppEntryUnlocked by remember {
        mutableStateOf(!appEntryBiometricGateEnabled)
    }

    val documentsState by viewModel.documentsState.collectAsStateWithLifecycle()
    val foldersState by viewModel.foldersState.collectAsStateWithLifecycle()
    val cachedDocumentsMap by viewModel.cachedDocumentsMap.collectAsStateWithLifecycle()
    val privateSafeDocuments by viewModel.privateSafeDocuments.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()

    LaunchedEffect(incomingIntent) {
        if (incomingIntent != null) {
            when (incomingIntent.action) {
                DocFlowAppWidgetProvider.ACTION_OPEN_SCANNER -> {
                    currentScreen = AppScreen.Scanner
                }
                DocFlowAppWidgetProvider.ACTION_OPEN_DOCUMENT -> {
                    val targetDocId = incomingIntent.getStringExtra(DocFlowAppWidgetProvider.EXTRA_DOCUMENT_ID)
                    if (!targetDocId.isNullOrBlank()) {
                        viewModel.recordDocumentAccessed(targetDocId)
                        currentScreen = AppScreen.DocumentDetail(targetDocId)
                    }
                }
                else -> {
                    viewModel.handleIncomingExternalIntent(
                        context = context,
                        intent = incomingIntent,
                        onDocumentReady = { openedDocId ->
                            viewModel.recordDocumentAccessed(openedDocId)
                            currentScreen = AppScreen.DocumentDetail(openedDocId)
                        }
                    )
                }
            }
            onConsumeIncomingIntent()
        }
    }

    val folders = (foldersState as? UiState.Success)?.data ?: emptyList()
    val allDocs = remember(documentsState, cachedDocumentsMap, privateSafeDocuments) {
        val base = (documentsState as? UiState.Success)?.data
            ?.ifEmpty { cachedDocumentsMap.values.map { it.toDocEntity() } }
            ?: cachedDocumentsMap.values.map { it.toDocEntity() }
        val safeDocs = privateSafeDocuments.map { it.toDocEntity() }
        (base + safeDocs).distinctBy { it.id }
    }

    if (appEntryBiometricGateEnabled && !isAppEntryUnlocked) {
        val biometricStatus = remember(context) { BiometricSecurityHelper.getBiometricStatus(context) }
        var authError by remember { mutableStateOf<String?>(null) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(DocFlowTokens.screenSideMargin),
            contentAlignment = Alignment.Center
        ) {
            DocFlowCard(
                palette = palette,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DocFlowTokens.space3)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(DocFlowTokens.AccentBlue.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric unlock",
                            tint = DocFlowTokens.AccentBlue,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Text(
                        text = "DocFlow Locked",
                        style = DocFlowTokens.sectionLabelStyle,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )

                    Text(
                        text = biometricStatus.statusLabel,
                        style = DocFlowTokens.captionStyle,
                        color = DocFlowTokens.AccentBlue
                    )

                    Text(
                        text = "Unlock with Biometrics or Device Credential to access documents stored on this device.",
                        style = DocFlowTokens.bodyStyle,
                        color = palette.textSecondary
                    )

                    Button(
                        onClick = {
                            BiometricSecurityHelper.authenticateForAppEntry(
                                context = context,
                                onAuthenticated = { isAppEntryUnlocked = true },
                                onError = { err -> authError = err }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(DocFlowTokens.minTouchTarget)
                            .testTag("app_entry_biometric_unlock_button"),
                        shape = DocFlowTokens.rectButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DocFlowTokens.AccentBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(DocFlowTokens.space2))
                        Text("Unlock DocFlow", fontWeight = FontWeight.Bold)
                    }

                    if (authError != null) {
                        Text(
                            text = authError ?: "",
                            style = DocFlowTokens.captionStyle,
                            color = DocFlowTokens.IconCircleRed
                        )
                    }
                }
            }
        }
        return
    }

    when (val screen = currentScreen) {
        is AppScreen.MainTabs -> {
            BackHandler(enabled = activeTab != BottomNavTab.RECENT) {
                activeTab = BottomNavTab.RECENT
            }

            Scaffold(
                containerColor = palette.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    DocFlowBottomNav(
                        selectedIndex = activeTab.ordinal,
                        onSelectIndex = { idx ->
                            activeTab = BottomNavTab.entries[idx]
                        },
                        palette = palette
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (activeTab) {
                        BottomNavTab.RECENT -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                palette = palette,
                                onOpenDocument = { docId ->
                                    viewModel.recordDocumentAccessed(docId)
                                    currentScreen = AppScreen.DocumentDetail(docId)
                                },
                                onOpenScanner = { currentScreen = AppScreen.Scanner },
                                onOpenBatchProcessor = { currentScreen = AppScreen.BatchProcessor },
                                onOpenStorageDashboard = { currentScreen = AppScreen.StorageDashboard },
                                onOpenSettings = { currentScreen = AppScreen.Settings },
                                onOpenSearch = {
                                    activeTab = BottomNavTab.RECENT
                                }
                            )
                        }

                        BottomNavTab.TOOLS -> {
                            ToolsScreen(
                                viewModel = viewModel,
                                palette = palette,
                                onOpenDocument = { docId ->
                                    viewModel.recordDocumentAccessed(docId)
                                    currentScreen = AppScreen.DocumentDetail(docId)
                                },
                                onOpenScanner = { currentScreen = AppScreen.Scanner },
                                onOpenSettings = { currentScreen = AppScreen.Settings },
                                onOpenSearch = {
                                    activeTab = BottomNavTab.RECENT
                                }
                            )
                        }

                        BottomNavTab.MANAGE -> {
                            ManageScreen(
                                viewModel = viewModel,
                                palette = palette,
                                onOpenDocument = { docId ->
                                    viewModel.recordDocumentAccessed(docId)
                                    currentScreen = AppScreen.DocumentDetail(docId)
                                },
                                onOpenSettings = { currentScreen = AppScreen.Settings },
                                onOpenSearch = { activeTab = BottomNavTab.RECENT },
                                onRequestBiometricUnlock = { onSuccess ->
                                    BiometricSecurityHelper.authenticateForAppEntry(
                                        context = context,
                                        onAuthenticated = { onSuccess() },
                                        onError = { _ -> onSuccess() }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        is AppScreen.Settings -> {
            BackHandler { currentScreen = AppScreen.MainTabs }
            SettingsScreen(
                viewModel = viewModel,
                palette = palette,
                onBack = { currentScreen = AppScreen.MainTabs },
                onOpenStorageDashboard = { currentScreen = AppScreen.StorageDashboard }
            )
        }

        is AppScreen.StorageDashboard -> {
            BackHandler { currentScreen = AppScreen.MainTabs }
            StorageDashboardScreen(
                viewModel = viewModel,
                palette = palette,
                onBack = { currentScreen = AppScreen.MainTabs },
                onOpenDocument = { docId ->
                    viewModel.recordDocumentAccessed(docId)
                    currentScreen = AppScreen.DocumentDetail(docId)
                }
            )
        }

        is AppScreen.Scanner -> {
            BackHandler { currentScreen = AppScreen.MainTabs }
            ScannerScreen(
                viewModel = viewModel,
                palette = palette,
                onDocumentSaved = { createdId ->
                    viewModel.recordDocumentAccessed(createdId)
                    currentScreen = AppScreen.DocumentDetail(createdId)
                },
                onCancel = { currentScreen = AppScreen.MainTabs }
            )
        }

        is AppScreen.BatchProcessor -> {
            BackHandler { currentScreen = AppScreen.MainTabs }
            BatchProcessorScreen(
                allDocuments = allDocs,
                folders = folders,
                initialFolderId = if (selectedFolderId == "all") "root" else selectedFolderId,
                onBack = { currentScreen = AppScreen.MainTabs },
                onRunBatchDocToPic = { selectedDocs, mode, customBatchTag, onFinished ->
                    viewModel.runBatchDocToPicConversion(
                        context = context,
                        docsToConvert = selectedDocs,
                        mode = mode,
                        customBatchTag = customBatchTag,
                        onComplete = { count -> onFinished(count) }
                    )
                },
                onRunBatchPicToDoc = { selectedUris, batchTitlePrefix, targetFormat, compileIntoSingleDoc, selectedLanguage, folderId, category, customBatchTag, onFinished ->
                    viewModel.runBatchPicToDocConversion(
                        context = context,
                        imageUris = selectedUris,
                        batchTitlePrefix = batchTitlePrefix,
                        targetFormat = targetFormat,
                        compileIntoSingleDocument = compileIntoSingleDoc,
                        selectedLanguage = selectedLanguage,
                        folderId = folderId,
                        category = category,
                        customBatchTag = customBatchTag,
                        onComplete = { count -> onFinished(count) }
                    )
                }
            )
        }

        is AppScreen.DocumentDetail -> {
            BackHandler { currentScreen = AppScreen.MainTabs }
            DocumentViewerEditorScreen(
                docId = screen.documentId,
                viewModel = viewModel,
                palette = palette,
                onBack = { currentScreen = AppScreen.MainTabs },
                onRequestBiometricSign = { onVerified ->
                    BiometricSecurityHelper.authenticateForAppEntry(
                        context = context,
                        onAuthenticated = { onVerified() },
                        onError = { _ -> onVerified() }
                    )
                }
            )
        }
    }
}
