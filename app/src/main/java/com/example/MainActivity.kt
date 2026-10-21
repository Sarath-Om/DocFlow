package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.DocFlowDatabase
import com.example.data.local.SyncState
import com.example.data.repository.DocFlowRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocumentViewerEditorScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.theme.DocFlowTheme
import com.example.ui.viewmodel.DocFlowViewModel
import com.example.ui.viewmodel.ThemeMode
import com.example.ui.viewmodel.UiState
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

sealed interface AppScreen {
    data object Dashboard : AppScreen
    data object Scanner : AppScreen
    data class DocumentDetail(val documentId: String) : AppScreen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DocFlowRootApp()
        }
    }
}

@Composable
fun DocFlowRootApp() {
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
                        dao = roomDao
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
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Dashboard) }

    val documentsState by viewModel.documentsState.collectAsStateWithLifecycle()
    val filteredDocuments by viewModel.filteredDocuments.collectAsStateWithLifecycle()
    val foldersState by viewModel.foldersState.collectAsStateWithLifecycle()
    val cachedDocumentsMap by viewModel.cachedDocumentsMap.collectAsStateWithLifecycle()
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val localDocumentUris by viewModel.localDocumentUris.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedFileType by viewModel.selectedFileType.collectAsStateWithLifecycle()
    val offlineOnlyFilter by viewModel.offlineOnlyFilter.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    val folders = (foldersState as? UiState.Success)?.data ?: emptyList()
    val allDocs = (documentsState as? UiState.Success)?.data
        ?.ifEmpty { cachedDocumentsMap.values.map { it.toDocEntity() } }
        ?: cachedDocumentsMap.values.map { it.toDocEntity() }

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
                onOpenDocument = { doc -> currentScreen = AppScreen.DocumentDetail(doc.id) },
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
                            currentScreen = AppScreen.DocumentDetail(importedId)
                        }
                    )
                },
                onSeedSampleWorkspace = viewModel::seedSampleWorkspace,
                onClearSyncHistory = viewModel::clearSyncHistory,
                onSignOut = onSignOut
            )
        }

        is AppScreen.Scanner -> {
            ScannerScreen(
                folders = folders,
                initialFolderId = if (selectedFolderId == "all") "root" else selectedFolderId,
                onBack = { currentScreen = AppScreen.Dashboard },
                onSaveScannedDocument = { title, folderId, category, fileType, fileName, content, isEncrypted, password, keyHint, isOfflineCached, pageCount, localPdfPath, generatedPdfUri ->
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
                        customTags = "scanned, camerax, pdf, ${category.lowercase()}",
                        onCreated = { createdId ->
                            currentScreen = AppScreen.DocumentDetail(createdId)
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

                DocumentViewerEditorScreen(
                    document = activeDoc,
                    folders = folders,
                    localUri = localDocumentUris[activeDoc.id],
                    syncStateLabel = syncLabel,
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
                    onDelete = {
                        viewModel.deleteDocument(activeDoc.id)
                        currentScreen = AppScreen.Dashboard
                    }
                )
            }
        }
    }
}
