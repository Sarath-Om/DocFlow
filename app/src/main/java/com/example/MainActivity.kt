package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.data.local.SyncState
import com.example.data.repository.DocFlowRepository
import com.example.data.util.BiometricSecurityHelper
import com.example.ui.auth.AuthScreen
import com.example.ui.components.CrystalGlassBackground
import com.example.ui.components.CrystalGlassCard
import com.example.ui.components.HotlinkedDocFlowIconBadge
import com.example.ui.components.OtaUpdateAndSettingsModal
import com.example.ui.screens.BatchProcessorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocumentViewerEditorScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.StorageDashboardScreen
import com.example.widget.DocFlowAppWidgetProvider
import com.example.ui.theme.CrystalCyan
import com.example.ui.theme.CrystalSapphire
import com.example.ui.theme.CrystalViolet
import com.example.ui.theme.DarkOnSurfaceVariant
import com.example.ui.theme.DocFlowTheme
import com.example.ui.viewmodel.DocFlowViewModel
import com.example.ui.viewmodel.ThemeMode
import com.example.ui.viewmodel.UiState
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

sealed interface AppScreen {
    data object Dashboard : AppScreen
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
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        DocFlowTheme {
            AuthScreen(
                onSignInSuccess = {
                    currentUser = auth.currentUser
                }
            )
        }
    } else {
        val databaseId = context.getString(R.string.firestore_database_id)
        val firestore = remember(databaseId) {
            FirebaseFirestore.getInstance(FirebaseApp.getInstance(), databaseId)
        }
        val roomDao = remember(context) {
            DocFlowDatabase.getInstance(context).docFlowDao()
        }

        val viewModel: DocFlowViewModel = viewModel(
            key = user.uid,
            factory = viewModelFactory {
                initializer {
                    DocFlowViewModel(
                        repository = DocFlowRepository(firestore),
                        currentUserId = user.uid,
                        dao = roomDao,
                        currentUserDisplayName = user.displayName ?: "Workspace Owner",
                        currentUserEmail = user.email ?: "",
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
            AuthenticatedWorkspace(
                viewModel = viewModel,
                incomingIntent = incomingIntent,
                onConsumeIncomingIntent = onConsumeIncomingIntent,
                onSignOut = {
                    auth.signOut()
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedWorkspace(
    viewModel: DocFlowViewModel,
    incomingIntent: Intent? = null,
    onConsumeIncomingIntent: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Dashboard) }
    var showOtaSettingsModal by remember { mutableStateOf(false) }

    // App-Entry Biometric Gate state (enabled/disabled by user in OTA & Security Settings)
    var appEntryBiometricGateEnabled by remember { mutableStateOf(false) }
    var isAppEntryUnlocked by remember { mutableStateOf(true) }

    val documentsState by viewModel.documentsState.collectAsStateWithLifecycle()
    val filteredDocuments by viewModel.filteredDocuments.collectAsStateWithLifecycle()
    val foldersState by viewModel.foldersState.collectAsStateWithLifecycle()
    val cachedDocumentsMap by viewModel.cachedDocumentsMap.collectAsStateWithLifecycle()
    val recentCachedDocuments by viewModel.recentCachedDocuments.collectAsStateWithLifecycle()
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

    // OTA App Update states
    val otaChannel by viewModel.otaChannel.collectAsStateWithLifecycle()
    val otaAutoInstallEnabled by viewModel.otaAutoInstallEnabled.collectAsStateWithLifecycle()
    val otaCurrentVersionName by viewModel.otaCurrentVersionName.collectAsStateWithLifecycle()
    val otaCurrentVersionCode by viewModel.otaCurrentVersionCode.collectAsStateWithLifecycle()
    val otaInstallPhase by viewModel.otaInstallPhase.collectAsStateWithLifecycle()
    val otaDownloadProgress by viewModel.otaDownloadProgress.collectAsStateWithLifecycle()
    val otaAvailableManifest by viewModel.otaAvailableManifest.collectAsStateWithLifecycle()
    val otaHistory by viewModel.otaHistory.collectAsStateWithLifecycle()

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
    val allDocs = (documentsState as? UiState.Success)?.data
        ?.ifEmpty { cachedDocumentsMap.values.map { it.toDocEntity() } }
        ?: cachedDocumentsMap.values.map { it.toDocEntity() }

    // App-Entry Biometric Lock Gate (androidx.biometric) when locked
    if (appEntryBiometricGateEnabled && !isAppEntryUnlocked) {
        val biometricStatus = remember(context) { BiometricSecurityHelper.getBiometricStatus(context) }
        var authError by remember { mutableStateOf<String?>(null) }

        CrystalGlassBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CrystalGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    tintColor = CrystalViolet
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        HotlinkedDocFlowIconBadge(size = 56.dp)

                        Text(
                            text = "DocFlow Biometric App Entry",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Surface(
                            color = CrystalSapphire.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = biometricStatus.statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = CrystalCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Text(
                            text = "Authenticate with Fingerprint or Face Unlock (androidx.biometric) to decrypt and access your workspace documents.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkOnSurfaceVariant
                        )

                        Button(
                            onClick = {
                                BiometricSecurityHelper.authenticateForAppEntry(
                                    context = context,
                                    onAuthenticated = {
                                        isAppEntryUnlocked = true
                                    },
                                    onError = { err ->
                                        authError = err
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("app_entry_biometric_unlock_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrystalSapphire,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Authenticate with Biometrics", fontWeight = FontWeight.Bold)
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
        }
        return
    }

    when (val screen = currentScreen) {
        is AppScreen.Dashboard -> {
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
                onSignOut = onSignOut,
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
                otaVersionName = otaCurrentVersionName,
                otaInstallPhase = otaInstallPhase,
                onOpenOtaAndSettings = { showOtaSettingsModal = true },
                backgroundSyncWorkerState = backgroundSyncWorkerState,
                onTriggerWorkManagerSync = viewModel::triggerWorkManagerSupabaseSync
            )
        }

        is AppScreen.StorageDashboard -> {
            StorageDashboardScreen(
                storageSummary = storageDashboardSummary,
                allDocuments = allDocs,
                activeFormatFilter = selectedFileType,
                onBackToWorkspace = { currentScreen = AppScreen.Dashboard },
                onOpenBatchStudio = { currentScreen = AppScreen.BatchProcessor },
                onFilterWorkspaceByFormat = { formatKey ->
                    viewModel.selectFileType(formatKey)
                    currentScreen = AppScreen.Dashboard
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
            ScannerScreen(
                folders = folders,
                initialFolderId = if (selectedFolderId == "all") "root" else selectedFolderId,
                onBack = { currentScreen = AppScreen.Dashboard },
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
                        onCreated = { createdId ->
                            viewModel.recordDocumentAccessed(createdId)
                            currentScreen = AppScreen.DocumentDetail(createdId)
                        }
                    )
                }
            )
        }

        is AppScreen.BatchProcessor -> {
            BatchProcessorScreen(
                allDocuments = allDocs,
                folders = folders,
                initialFolderId = if (selectedFolderId == "all") "root" else selectedFolderId,
                onBack = { currentScreen = AppScreen.Dashboard },
                onRunBatchDocToPic = { selectedDocs, mode, customBatchTag, onFinished ->
                    viewModel.runBatchDocToPicConversion(
                        context = context,
                        docsToConvert = selectedDocs,
                        mode = mode,
                        customBatchTag = customBatchTag,
                        onComplete = { count ->
                            onFinished(count)
                        }
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
                        onComplete = { count ->
                            onFinished(count)
                        }
                    )
                }
            )
        }

        is AppScreen.DocumentDetail -> {
            val activeDoc = allDocs.find { it.id == screen.documentId }
            if (activeDoc == null) {
                currentScreen = AppScreen.Dashboard
            } else {
                val cachedEntity = cachedDocumentsMap[activeDoc.id]
                val syncLabel = cachedEntity?.syncState?.let {
                    runCatching { SyncState.valueOf(it).label }.getOrDefault("Cloud Synced")
                } ?: "Cloud Synced"
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
                    onBack = { currentScreen = AppScreen.Dashboard },
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
                        viewModel.deleteDocument(activeDoc.id)
                        currentScreen = AppScreen.Dashboard
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
                    onInviteCollaborator = { email, role ->
                        viewModel.inviteCollaborator(email, role)
                    },
                    onRevokeCollaboratorAndRotateKey = { collabId ->
                        viewModel.revokeCollaboratorAndRotateKey(collabId)
                    },
                    onSwitchCurrentUserRole = { role ->
                        viewModel.switchCurrentUserRole(role)
                    }
                )
            }
        }
    }

    if (showOtaSettingsModal) {
        OtaUpdateAndSettingsModal(
            currentVersionName = otaCurrentVersionName,
            currentVersionCode = otaCurrentVersionCode,
            selectedChannel = otaChannel,
            installPhase = otaInstallPhase,
            downloadProgress = otaDownloadProgress,
            availableManifest = otaAvailableManifest,
            autoOtaEnabled = otaAutoInstallEnabled,
            otaHistory = otaHistory,
            biometricVaultEnabled = biometricVaultEnabled,
            appEntryBiometricGateEnabled = appEntryBiometricGateEnabled,
            isOfflineMode = isOfflineMode,
            userEmail = viewModel.currentUserEmail.ifBlank { "${viewModel.currentUserId}@docflow.verified" },
            onSelectChannel = viewModel::selectOtaChannel,
            onCheckForUpdates = viewModel::checkForOtaUpdates,
            onInstallOtaUpdate = viewModel::installOtaUpdate,
            onToggleAutoOta = viewModel::toggleAutoOtaUpdates,
            onToggleBiometricVault = viewModel::toggleBiometricVault,
            onToggleAppEntryBiometricGate = {
                val nextEnabled = !appEntryBiometricGateEnabled
                appEntryBiometricGateEnabled = nextEnabled
                if (nextEnabled) {
                    showOtaSettingsModal = false
                    isAppEntryUnlocked = false
                }
            },
            onToggleOfflineMode = viewModel::toggleOfflineNetworkMode,
            onSignOut = {
                showOtaSettingsModal = false
                onSignOut()
            },
            onDismiss = { showOtaSettingsModal = false }
        )
    }
}
