package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.DocFlowDatabase
import com.example.data.local.SyncState
import com.example.data.repository.DocFlowRepository
import com.example.data.util.BiometricSecurityHelper
import com.example.data.util.OnDeviceAiAndBackupEngine
import com.example.data.util.TranslationLanguage
import com.example.ui.screens.BatchProcessorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocumentViewerEditorScreen
import com.example.ui.screens.ManageScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StorageDashboardScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.BadgePdfRed
import com.example.ui.theme.BadgeXlsGreen
import com.example.ui.theme.DocFlowAccentBlue
import com.example.ui.theme.DocFlowTheme
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

    DocFlowTheme(darkTheme = useDarkTheme) {
        OfflineDocFlowWorkspace(
            viewModel = viewModel,
            incomingIntent = incomingIntent,
            onConsumeIncomingIntent = onConsumeIncomingIntent
        )
    }
}

@Composable
private fun OfflineDocFlowWorkspace(
    viewModel: DocFlowViewModel,
    incomingIntent: Intent? = null,
    onConsumeIncomingIntent: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.MainTabs) }
    var activeTab by remember { mutableStateOf(BottomNavTab.RECENT) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Optional App-Entry Biometric Lock Gate
    var appEntryBiometricGateEnabled by remember {
        mutableStateOf(OnDeviceAiAndBackupEngine.isAppLockEnabled(context))
    }
    var isAppEntryUnlocked by remember {
        mutableStateOf(!OnDeviceAiAndBackupEngine.isAppLockEnabled(context))
    }

    val documentsState by viewModel.documentsState.collectAsStateWithLifecycle()
    val filteredDocuments by viewModel.filteredDocuments.collectAsStateWithLifecycle()
    val foldersState by viewModel.foldersState.collectAsStateWithLifecycle()
    val cachedDocumentsMap by viewModel.cachedDocumentsMap.collectAsStateWithLifecycle()
    val recentCachedDocuments by viewModel.recentCachedDocuments.collectAsStateWithLifecycle()
    val deletedDocuments by viewModel.deletedDocuments.collectAsStateWithLifecycle()
    val privateSafeDocuments by viewModel.privateSafeDocuments.collectAsStateWithLifecycle()
    val createdByMeDocuments by viewModel.createdByMeDocuments.collectAsStateWithLifecycle()
    val storageDashboardSummary by viewModel.storageDashboardSummary.collectAsStateWithLifecycle()
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val backgroundSyncWorkerState by viewModel.backgroundSyncWorkerState.collectAsStateWithLifecycle()
    val localDocumentUris by viewModel.localDocumentUris.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedFileType by viewModel.selectedFileType.collectAsStateWithLifecycle()
    val selectedTagFilters by viewModel.selectedTagFilters.collectAsStateWithLifecycle()
    val offlineOnlyFilter by viewModel.offlineOnlyFilter.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val biometricVaultEnabled by viewModel.biometricVaultEnabled.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    val liveSyncStatus by viewModel.liveSyncStatus.collectAsStateWithLifecycle()
    val currentUserRole by viewModel.currentUserRole.collectAsStateWithLifecycle()
    val activeCollaborators by viewModel.activeCollaborators.collectAsStateWithLifecycle()

    // Handle external "Open In App" / "Share In App" and Widget Quick Actions
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

    // App-Entry Biometric Lock Gate when enabled
    if (appEntryBiometricGateEnabled && !isAppEntryUnlocked) {
        val biometricStatus = remember(context) { BiometricSecurityHelper.getBiometricStatus(context) }
        var authError by remember { mutableStateOf<String?>(null) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(DocFlowAccentBlue.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = DocFlowAccentBlue,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Text(
                        text = "DocFlow Locked",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = biometricStatus.statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = DocFlowAccentBlue
                    )

                    Text(
                        text = "Unlock with Biometrics or Device Credential to access your offline documents.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            .height(50.dp)
                            .testTag("app_entry_biometric_unlock_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DocFlowAccentBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock DocFlow", fontWeight = FontWeight.Bold)
                    }

                    if (authError != null) {
                        Text(
                            text = authError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
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
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    FlatBottomNavigationBar(
                        activeTab = activeTab,
                        onSelectTab = { selected -> activeTab = selected }
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
                                documentsState = documentsState,
                                filteredDocuments = filteredDocuments,
                                foldersState = foldersState,
                                cachedDocumentsMap = cachedDocumentsMap,
                                syncLogs = syncLogs,
                                pendingSyncCount = pendingSyncCount,
                                searchQuery = searchQuery,
                                selectedFolderId = selectedFolderId,
                                selectedCategory = selectedCategory,
                                selectedFileType = selectedFileType,
                                offlineOnlyFilter = offlineOnlyFilter,
                                sortOption = sortOption,
                                isOfflineMode = isOfflineMode,
                                themeMode = themeMode,
                                statusBannerMessage = statusBannerMessage,
                                onSearchQueryChange = viewModel::setSearchQuery,
                                onSelectFolder = viewModel::selectFolder,
                                onSelectCategory = viewModel::selectCategory,
                                onSelectFileType = viewModel::selectFileType,
                                onToggleOfflineOnlyFilter = viewModel::toggleOfflineOnlyFilter,
                                onSortOptionChange = viewModel::setSortOption,
                                onToggleOfflineNetworkMode = viewModel::toggleOfflineNetworkMode,
                                onToggleThemeMode = viewModel::toggleThemeMode,
                                onDismissStatusBanner = viewModel::clearStatusBanner,
                                onCreateFolder = viewModel::createFolder,
                                onDeleteFolder = viewModel::deleteFolder,
                                onOpenScanner = { currentScreen = AppScreen.Scanner },
                                onOpenDocument = { doc ->
                                    viewModel.recordDocumentAccessed(doc.id)
                                    currentScreen = AppScreen.DocumentDetail(doc.id)
                                },
                                onToggleDocumentOffline = viewModel::toggleOfflineAvailable,
                                onCreateDocument = { title, folderId, category, fileType, fileName, content, isEncrypted, password, keyHint, customTags ->
                                    viewModel.createOrScanDocument(
                                        title = title,
                                        folderId = folderId,
                                        category = category,
                                        fileType = fileType,
                                        fileName = fileName,
                                        plainContent = content,
                                        isEncrypted = isEncrypted,
                                        password = password,
                                        encryptionKeyHint = keyHint,
                                        isOfflineCached = true,
                                        pageCount = ((content.length / 1800) + 1).toLong(),
                                        customTags = customTags,
                                        createdByMe = true,
                                        onCreated = { createdId ->
                                            viewModel.recordDocumentAccessed(createdId)
                                            currentScreen = AppScreen.DocumentDetail(createdId)
                                        }
                                    )
                                },
                                onImportFileUri = { uri ->
                                    viewModel.importDocumentFromUri(
                                        context = context,
                                        uri = uri,
                                        folderId = if (selectedFolderId == "all") "root" else selectedFolderId,
                                        category = if (selectedCategory == "All") "Work" else selectedCategory,
                                        isEncrypted = false,
                                        password = "",
                                        encryptionKeyHint = null,
                                        onImported = { importedId ->
                                            viewModel.recordDocumentAccessed(importedId)
                                            currentScreen = AppScreen.DocumentDetail(importedId)
                                        }
                                    )
                                },
                                onShareDocument = { doc ->
                                    viewModel.shareAnyDocument(context, doc)
                                },
                                onSeedSampleWorkspace = viewModel::seedSampleWorkspace,
                                onClearSyncHistory = viewModel::clearSyncHistory,
                                selectedTagFilters = selectedTagFilters,
                                biometricVaultEnabled = biometricVaultEnabled,
                                onToggleTagFilter = viewModel::toggleTagFilter,
                                onClearTagFilters = viewModel::clearTagFilters,
                                onToggleBiometricVault = viewModel::toggleBiometricVault,
                                onOpenBatchProcessor = { currentScreen = AppScreen.BatchProcessor },
                                onAddCustomTagToDoc = viewModel::addCustomTagToDocument,
                                onUpdateCategoryAndTags = viewModel::updateDocumentCategoryAndTags,
                                recentCachedDocuments = recentCachedDocuments,
                                onOpenStorageDashboard = { currentScreen = AppScreen.StorageDashboard },
                                activeCollaborators = activeCollaborators,
                                onOpenOtaAndSettings = { currentScreen = AppScreen.Settings },
                                backgroundSyncWorkerState = backgroundSyncWorkerState,
                                onTriggerWorkManagerSync = viewModel::triggerWorkManagerSupabaseSync,
                                onBatchSoftDelete = viewModel::softDeleteDocuments,
                                onBatchMoveToFolder = viewModel::moveDocumentsToFolder,
                                onBatchTagDocuments = viewModel::batchTagDocuments,
                                onMoveToPrivateSafe = viewModel::moveToPrivateSafe
                            )
                        }

                        BottomNavTab.TOOLS -> {
                            ToolsScreen(
                                allDocuments = filteredDocuments,
                                onOpenSearch = {
                                    activeTab = BottomNavTab.RECENT
                                },
                                onOpenSettings = { currentScreen = AppScreen.Settings },
                                onOpenScanner = { currentScreen = AppScreen.Scanner },
                                onOpenBatchProcessor = { currentScreen = AppScreen.BatchProcessor },
                                onConvertDocumentToPdf = { sourceDoc ->
                                    viewModel.convertFileToPdfAndSave(
                                        context = context,
                                        sourceDoc = sourceDoc,
                                        onCreated = { newId ->
                                            viewModel.recordDocumentAccessed(newId)
                                            currentScreen = AppScreen.DocumentDetail(newId)
                                        }
                                    )
                                },
                                onImportAndConvertUriToPdf = { uri ->
                                    viewModel.importDocumentFromUri(
                                        context = context,
                                        uri = uri,
                                        folderId = "root",
                                        category = "Work",
                                        isEncrypted = false,
                                        password = "",
                                        encryptionKeyHint = null,
                                        onImported = { importedId ->
                                            val importedDoc = cachedDocumentsMap[importedId]?.toDocEntity()
                                            if (importedDoc != null) {
                                                viewModel.convertFileToPdfAndSave(
                                                    context = context,
                                                    sourceDoc = importedDoc,
                                                    onCreated = { pdfId ->
                                                        viewModel.recordDocumentAccessed(pdfId)
                                                        currentScreen = AppScreen.DocumentDetail(pdfId)
                                                    }
                                                )
                                            }
                                        }
                                    )
                                },
                                onSaveSummaryAsNote = { sourceDoc, summaryResult ->
                                    viewModel.saveSummaryAsNote(
                                        sourceDoc = sourceDoc,
                                        summaryResult = summaryResult,
                                        onCreated = { noteId ->
                                            viewModel.recordDocumentAccessed(noteId)
                                            currentScreen = AppScreen.DocumentDetail(noteId)
                                        }
                                    )
                                },
                                onSaveTranslationDocument = { sourceDoc, translatedText, targetLang, exportAsPdf ->
                                    viewModel.saveTranslationDocument(
                                        context = context,
                                        sourceDoc = sourceDoc,
                                        translatedText = translatedText,
                                        targetLang = targetLang,
                                        exportAsPdf = exportAsPdf,
                                        onCreated = { createdId ->
                                            viewModel.recordDocumentAccessed(createdId)
                                            currentScreen = AppScreen.DocumentDetail(createdId)
                                        }
                                    )
                                }
                            )
                        }

                        BottomNavTab.MANAGE -> {
                            ManageScreen(
                                allDocuments = filteredDocuments,
                                cachedDocumentsMap = cachedDocumentsMap,
                                createdByMeDocs = createdByMeDocuments,
                                privateSafeDocs = privateSafeDocuments,
                                deletedDocs = deletedDocuments,
                                folders = folders,
                                onOpenSearch = { activeTab = BottomNavTab.RECENT },
                                onOpenSettings = { currentScreen = AppScreen.Settings },
                                onOpenDocument = { doc ->
                                    viewModel.recordDocumentAccessed(doc.id)
                                    currentScreen = AppScreen.DocumentDetail(doc.id)
                                },
                                onImportFileUri = { uri ->
                                    viewModel.importDocumentFromUri(
                                        context = context,
                                        uri = uri,
                                        folderId = "root",
                                        category = "Work",
                                        isEncrypted = false,
                                        password = "",
                                        encryptionKeyHint = null,
                                        onImported = { importedId ->
                                            viewModel.recordDocumentAccessed(importedId)
                                            currentScreen = AppScreen.DocumentDetail(importedId)
                                        }
                                    )
                                },
                                onCreateFolder = { name, color, icon, parentId ->
                                    viewModel.createFolder(name, color, icon, parentId)
                                },
                                onRenameOrUpdateFolder = { folderId, newName, newColor, parentId ->
                                    viewModel.renameOrUpdateFolder(folderId, newName, newColor, parentId)
                                },
                                onDeleteFolder = viewModel::deleteFolder,
                                onMoveToPrivateSafe = viewModel::moveToPrivateSafe,
                                onRemoveFromPrivateSafe = viewModel::removeFromPrivateSafe,
                                onRestoreDeletedDocument = viewModel::restoreDeletedDocument,
                                onPermanentlyDeleteDocument = viewModel::permanentlyDeleteDocument,
                                onEmptyRecentlyDeleted = viewModel::emptyRecentlyDeleted
                            )
                        }
                    }
                }
            }
        }

        is AppScreen.Settings -> {
            BackHandler { currentScreen = AppScreen.MainTabs }
            SettingsScreen(
                themeMode = themeMode,
                biometricVaultEnabled = biometricVaultEnabled,
                appEntryBiometricGateEnabled = appEntryBiometricGateEnabled,
                storageSummary = storageDashboardSummary,
                totalDocumentsCount = allDocs.size,
                deletedDocumentsCount = deletedDocuments.size,
                privateSafeCount = privateSafeDocuments.size,
                onBack = { currentScreen = AppScreen.MainTabs },
                onSelectThemeMode = viewModel::setThemeMode,
                onToggleBiometricVault = viewModel::toggleBiometricVault,
                onToggleAppEntryBiometricGate = {
                    val next = !appEntryBiometricGateEnabled
                    appEntryBiometricGateEnabled = next
                    OnDeviceAiAndBackupEngine.setAppLockEnabled(context, next)
                    if (next) {
                        isAppEntryUnlocked = false
                    }
                },
                onOpenStorageDashboard = { currentScreen = AppScreen.StorageDashboard },
                onClearCacheAndCompact = {
                    viewModel.clearThumbnailAndTempCache(context)
                    viewModel.compactSnapshotsAndCache()
                },
                onExportBackup = { passphrase ->
                    viewModel.exportBackupArchive(context, passphrase)
                },
                onImportBackupUri = { uri, passphrase ->
                    viewModel.importBackupArchive(context, uri, passphrase)
                }
            )
        }

        is AppScreen.StorageDashboard -> {
            BackHandler { currentScreen = AppScreen.MainTabs }
            StorageDashboardScreen(
                storageSummary = storageDashboardSummary,
                allDocuments = allDocs,
                activeFormatFilter = selectedFileType,
                onBackToWorkspace = { currentScreen = AppScreen.MainTabs },
                onOpenBatchStudio = { currentScreen = AppScreen.BatchProcessor },
                onFilterWorkspaceByFormat = { formatKey ->
                    viewModel.selectFileType(formatKey)
                    activeTab = BottomNavTab.RECENT
                    currentScreen = AppScreen.MainTabs
                },
                onOpenDocument = { doc ->
                    viewModel.recordDocumentAccessed(doc.id)
                    currentScreen = AppScreen.DocumentDetail(doc.id)
                },
                onCompactSnapshotsAndCache = viewModel::compactSnapshotsAndCache,
                onSeedSampleWorkspace = viewModel::seedSampleWorkspace
            )
        }

        is AppScreen.Scanner -> {
            BackHandler { currentScreen = AppScreen.MainTabs }
            ScannerScreen(
                folders = folders,
                initialFolderId = if (selectedFolderId == "all") "root" else selectedFolderId,
                onBack = { currentScreen = AppScreen.MainTabs },
                onCreateNewFolder = { folderName ->
                    viewModel.createFolder(folderName, "#1F6BFF", "folder", "root")
                },
                onSaveScannedDocument = { title, folderId, category, fileType, fileName, content, isEncrypted, password, keyHint, isOfflineCached, pageCount, localPdfPath, generatedPdfUri, customTags ->
                    viewModel.createOrScanDocument(
                        title = title,
                        folderId = folderId,
                        category = category,
                        fileType = fileType,
                        fileName = fileName,
                        plainContent = content,
                        isEncrypted = isEncrypted,
                        password = password,
                        encryptionKeyHint = keyHint,
                        isOfflineCached = isOfflineCached,
                        pageCount = pageCount,
                        localPdfPath = localPdfPath,
                        generatedPdfUri = generatedPdfUri,
                        customTags = customTags,
                        createdByMe = true,
                        onCreated = { createdId ->
                            viewModel.recordDocumentAccessed(createdId)
                            currentScreen = AppScreen.DocumentDetail(createdId)
                        }
                    )
                }
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
            val activeDoc = allDocs.find { it.id == screen.documentId }
            if (activeDoc == null) {
                currentScreen = AppScreen.MainTabs
            } else {
                val cachedEntity = cachedDocumentsMap[activeDoc.id]
                    ?: privateSafeDocuments.find { it.id == activeDoc.id }
                val syncLabel = cachedEntity?.syncState?.let {
                    runCatching { SyncState.valueOf(it).label }.getOrDefault("Stored on Device")
                } ?: "Stored on Device"
                val docTags = cachedEntity?.parsedTags() ?: emptyList()

                val versionsFlow = remember(activeDoc.id) {
                    viewModel.observeVersionsForDocument(activeDoc.id)
                }
                val versions by versionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                val commentsFlow = remember(activeDoc.id) {
                    viewModel.observeCommentsForDocument(activeDoc.id)
                }
                val comments by commentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                val annotationsFlow = remember(activeDoc.id) {
                    viewModel.observeAnnotationsForDocument(activeDoc.id)
                }
                val annotations by annotationsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                DocumentViewerEditorScreen(
                    document = activeDoc,
                    folders = folders,
                    localUri = localDocumentUris[activeDoc.id],
                    localFilePath = cachedEntity?.localFilePath,
                    syncStateLabel = syncLabel,
                    tags = docTags,
                    biometricVaultEnabled = biometricVaultEnabled,
                    onBack = { currentScreen = AppScreen.MainTabs },
                    onSaveEdits = { newTitle, newFolderId, newCategory, plainContent, isEncrypted, password, keyHint, isOfflineCached ->
                        viewModel.updateDocumentContent(
                            doc = activeDoc,
                            newTitle = newTitle,
                            newFolderId = newFolderId,
                            newCategory = newCategory,
                            plainContent = plainContent,
                            isEncrypted = isEncrypted,
                            password = password,
                            encryptionKeyHint = keyHint,
                            isOfflineCached = isOfflineCached
                        )
                    },
                    onSaveDocToPicToWorkspace = { exportModeLabel, pageCount, imageFile, imageUri ->
                        viewModel.saveDocToPicExportToWorkspace(
                            sourceDoc = activeDoc,
                            exportModeLabel = exportModeLabel,
                            pageCount = pageCount,
                            imageFile = imageFile,
                            imageUri = imageUri
                        )
                    },
                    onConvertFormat = { targetFormat, convertedPlainContent ->
                        viewModel.convertDocumentToFormat(
                            sourceDoc = activeDoc,
                            targetFormat = targetFormat,
                            plainContent = convertedPlainContent,
                            onConverted = { newDocId ->
                                viewModel.recordDocumentAccessed(newDocId)
                                currentScreen = AppScreen.DocumentDetail(newDocId)
                            }
                        )
                    },
                    onAddCustomTag = { newTag ->
                        viewModel.addCustomTagToDocument(activeDoc, newTag)
                    },
                    onRemoveCustomTag = { tagToRemove ->
                        viewModel.removeTagFromDocument(activeDoc, tagToRemove)
                    },
                    onToggleBiometricLock = {
                        viewModel.toggleDocumentBiometricLock(activeDoc)
                    },
                    onDelete = {
                        viewModel.softDeleteDocument(activeDoc.id)
                        currentScreen = AppScreen.MainTabs
                    },
                    liveSyncStatus = liveSyncStatus,
                    collaborators = activeCollaborators,
                    currentUserRole = currentUserRole,
                    versions = versions,
                    comments = comments,
                    annotations = annotations,
                    onUpsertAnnotation = { pageIndex, tool, colorHex, strokeWidth, normalizedPoints, stickyNoteText ->
                        viewModel.upsertYMapAnnotation(
                            doc = activeDoc,
                            pageIndex = pageIndex,
                            tool = tool,
                            colorHex = colorHex,
                            strokeWidth = strokeWidth,
                            normalizedPoints = normalizedPoints,
                            stickyNoteText = stickyNoteText
                        )
                    },
                    onUndoAnnotation = { pageAnns ->
                        viewModel.undoLastYMapAnnotation(pageAnns)
                    },
                    onClearPageAnnotations = { pageIndex ->
                        viewModel.clearPageYMapAnnotations(activeDoc.id, pageIndex)
                    },
                    onSignDocumentWithBiometrics = { currentDecryptedContent, password ->
                        viewModel.signDocumentWithBiometrics(activeDoc, currentDecryptedContent, password)
                    },
                    onShareAndExportAnnotatedPdf = { currentDecryptedContent, isSigned ->
                        viewModel.exportAndShareAnnotatedPdf(
                            context = context,
                            doc = activeDoc,
                            plainContent = currentDecryptedContent,
                            annotations = annotations,
                            isSigned = isSigned
                        )
                    },
                    onLiveKeystrokeEdit = { newTitle, newFolderId, newCategory, plainContent, isEncrypted, password, keyHint, isOfflineCached ->
                        viewModel.onLiveDocumentKeystrokeEdit(
                            doc = activeDoc,
                            newTitle = newTitle,
                            newFolderId = newFolderId,
                            newCategory = newCategory,
                            plainContent = plainContent,
                            isEncrypted = isEncrypted,
                            password = password,
                            encryptionKeyHint = keyHint,
                            isOfflineCached = isOfflineCached
                        )
                    },
                    onCreateNamedVersion = { versionLabel, plainContent ->
                        viewModel.createNamedVersionSnapshot(activeDoc, versionLabel, plainContent)
                    },
                    onRestoreVersion = { version, password, onRestoredContent ->
                        viewModel.restoreVersionSnapshot(activeDoc, version, password, onRestoredContent)
                    },
                    onExportVersionAsPdf = { version ->
                        viewModel.exportVersionSnapshotAsPdf(context, activeDoc, version)
                    },
                    onAddComment = { anchorText, body, isSuggestion, suggestedReplacement ->
                        viewModel.addCommentOrSuggestion(activeDoc, anchorText, body, isSuggestion, suggestedReplacement)
                    },
                    onResolveComment = { commentId ->
                        viewModel.resolveComment(commentId)
                    },
                    onAcceptSuggestion = { comment, currentPlainContent, password, onMergedContent ->
                        viewModel.acceptSuggestion(activeDoc, comment, currentPlainContent, password, onMergedContent)
                    },
                    onRejectSuggestion = { commentId ->
                        viewModel.rejectSuggestion(commentId)
                    },
                    onExportCommentAsPdf = { comment ->
                        viewModel.exportCommentOrNoteAsPdf(context, activeDoc, comment)
                    },
                    onExportAllNotesAsPdf = { allComments ->
                        viewModel.exportAllNotesAsPdf(context, activeDoc, allComments, annotations)
                    },
                    onInviteCollaborator = { email, role ->
                        viewModel.inviteCollaborator(email, role)
                    },
                    onRevokeCollaboratorAndRotateKey = { collabId ->
                        viewModel.revokeCollaboratorAndRotateKey(collabId)
                    },
                    onSwitchCurrentUserRole = { role ->
                        viewModel.switchCurrentUserRole(role)
                    },
                    onMoveToPrivateSafe = { docId ->
                        viewModel.moveToPrivateSafe(docId, "1234")
                    },
                    initialPageIndex = cachedEntity?.lastPageViewed ?: 0,
                    onPageChanged = { pageIdx ->
                        viewModel.updateDocumentLastPage(activeDoc.id, pageIdx)
                    }
                )
            }
        }
    }

    if (showSettingsSheet) {
        OfflineDocFlowSettingsSheet(
            themeMode = themeMode,
            biometricVaultEnabled = biometricVaultEnabled,
            appEntryBiometricGateEnabled = appEntryBiometricGateEnabled,
            totalDocumentsCount = allDocs.size,
            deletedDocumentsCount = deletedDocuments.size,
            onSelectThemeMode = viewModel::setThemeMode,
            onToggleBiometricVault = viewModel::toggleBiometricVault,
            onToggleAppEntryBiometricGate = {
                val next = !appEntryBiometricGateEnabled
                appEntryBiometricGateEnabled = next
                if (next) {
                    showSettingsSheet = false
                    isAppEntryUnlocked = false
                }
            },
            onOpenStorageAnalytics = {
                showSettingsSheet = false
                currentScreen = AppScreen.StorageDashboard
            },
            onCompactLocalStorage = viewModel::compactSnapshotsAndCache,
            onExportBackup = { passphrase ->
                viewModel.exportBackupArchive(context, passphrase)
            },
            onImportBackupUri = { uri, passphrase ->
                viewModel.importBackupArchive(context, uri, passphrase)
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}

@Composable
private fun FlatBottomNavigationBar(
    activeTab: BottomNavTab,
    onSelectTab: (BottomNavTab) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 0.7.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavTab.entries.forEach { tab ->
                    val isSelected = activeTab == tab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clickable { onSelectTab(tab) }
                            .testTag(tab.testTag)
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tab.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineDocFlowSettingsSheet(
    themeMode: ThemeMode,
    biometricVaultEnabled: Boolean,
    appEntryBiometricGateEnabled: Boolean,
    totalDocumentsCount: Int,
    deletedDocumentsCount: Int,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleBiometricVault: () -> Unit,
    onToggleAppEntryBiometricGate: () -> Unit,
    onOpenStorageAnalytics: () -> Unit,
    onCompactLocalStorage: () -> Unit,
    onExportBackup: (String) -> Unit,
    onImportBackupUri: ( android.net.Uri, String ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var backupPassphrase by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var pinSavedConfirmation by remember { mutableStateOf(false) }
    var lockTimeoutMinutes by remember {
        mutableIntStateOf(OnDeviceAiAndBackupEngine.getLockTimeoutMinutes(context))
    }
    var downloadedLangs by remember {
        mutableStateOf(OnDeviceAiAndBackupEngine.getDownloadedLanguagePacks(context))
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportBackupUri(uri, backupPassphrase.ifBlank { "docflow-backup" })
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Settings",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 1. Privacy & Offline Architecture Summary Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(BadgeXlsGreen.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = BadgeXlsGreen
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your files stay on your phone",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "100% offline • No servers • No account • Zero analytics or tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Appearance / Theme
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = DocFlowAccentBlue)
                        Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ThemeMode.SYSTEM to "System",
                            ThemeMode.LIGHT to "Light",
                            ThemeMode.DARK to "Dark"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onSelectThemeMode(mode) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // 3. App Lock & Private Safe Security
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = DocFlowAccentBlue)
                        Text("App Lock & Private Safe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric App Lock on Launch", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Require fingerprint or face unlock to open DocFlow",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = appEntryBiometricGateEnabled,
                            onCheckedChange = { onToggleAppEntryBiometricGate() }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric Unlock for Encrypted Files", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Use Android Keystore AES-256-GCM hardware key",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = biometricVaultEnabled,
                            onCheckedChange = { onToggleBiometricVault() }
                        )
                    }

                    Text("Auto-lock Timeout", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 5, 15, 30).forEach { mins ->
                            FilterChip(
                                selected = lockTimeoutMinutes == mins,
                                onClick = {
                                    lockTimeoutMinutes = mins
                                    OnDeviceAiAndBackupEngine.setLockTimeoutMinutes(context, mins)
                                },
                                label = { Text("${mins}m") }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = {
                                newPinInput = it
                                pinSavedConfirmation = false
                            },
                            label = { Text("Update Private Safe PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            enabled = newPinInput.length >= 4,
                            onClick = {
                                OnDeviceAiAndBackupEngine.setPrivateSafePin(context, newPinInput)
                                newPinInput = ""
                                pinSavedConfirmation = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DocFlowAccentBlue)
                        ) {
                            Text("Save PIN", color = Color.White)
                        }
                    }
                    if (pinSavedConfirmation) {
                        Text(
                            text = "Private Safe PIN updated.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BadgeXlsGreen
                        )
                    }
                }
            }

            // 4. On-Device Translation Language Packs
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = DocFlowAccentBlue)
                        Text("Offline Language Packs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Downloaded packs run 100% on-device without internet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TranslationLanguage.entries.forEach { lang ->
                        val isInstalled = lang.code in downloadedLangs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${lang.displayName} (${lang.nativeName})", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${lang.packSizeMb} • ${if (isInstalled) "Installed offline" else "Not installed"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isInstalled) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = BadgeXlsGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    if (lang.code != "en" && lang.code != "hi") {
                                        TextButton(
                                            onClick = {
                                                OnDeviceAiAndBackupEngine.removeLanguagePack(context, lang.code)
                                                downloadedLangs = OnDeviceAiAndBackupEngine.getDownloadedLanguagePacks(context)
                                            }
                                        ) {
                                            Text("Remove", color = BadgePdfRed, fontSize = 12.sp)
                                        }
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        OnDeviceAiAndBackupEngine.markLanguagePackDownloaded(context, lang.code)
                                        downloadedLangs = OnDeviceAiAndBackupEngine.getDownloadedLanguagePacks(context)
                                    }
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Encrypted .docflow Backup & Restore
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = DocFlowAccentBlue)
                        Text("Encrypted .docflow Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Export or restore all documents, folders, tags, and Private Safe items in an AES-256-GCM encrypted archive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = backupPassphrase,
                        onValueChange = { backupPassphrase = it },
                        label = { Text("Backup Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                onExportBackup(backupPassphrase.ifBlank { "docflow-backup" })
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DocFlowAccentBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export .docflow", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = {
                                restoreBackupLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore")
                        }
                    }
                }
            }

            // 6. Storage & Cache Management
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = DocFlowAccentBlue)
                        Text("Storage & Cache", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "$totalDocumentsCount active documents • $deletedDocumentsCount in Recently deleted (auto-purged after 30 days)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onOpenStorageAnalytics,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Storage Breakdown")
                        }
                        OutlinedButton(
                            onClick = {
                                onCompactLocalStorage()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Cache")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
