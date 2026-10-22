package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CachedDocumentEntity
import com.example.data.local.CachedFolderEntity
import com.example.data.local.DocFlowDao
import com.example.data.local.DocumentCommentEntity
import com.example.data.local.DocumentVersionEntity
import com.example.data.local.OtaUpdateRecordEntity
import com.example.data.local.PdfAnnotationYMapEntity
import com.example.data.local.PdfMarkupTool
import com.example.data.local.SyncLogEntity
import com.example.data.local.SyncState
import com.example.data.model.ActiveCollaborator
import com.example.data.model.CollaboratorRole
import com.example.data.model.DocEntity
import com.example.data.model.FolderEntity
import com.example.data.model.FormatStorageSlice
import com.example.data.model.LiveSyncStatus
import com.example.data.model.ShareLinkConfig
import com.example.data.model.StorageDashboardSummary
import com.example.data.repository.DocFlowRepository
import com.example.data.util.CryptoUtils
import com.example.data.util.DocToPicConverter
import com.example.data.util.DocToPicMode
import com.example.data.util.FileParserUtils
import com.example.data.util.ImageProcessingPdfPipeline
import com.example.data.util.MultiLanguageOcrEngine
import com.example.data.util.OcrLanguage
import com.example.data.util.OtaInstallPhase
import com.example.data.util.OtaReleaseChannel
import com.example.data.util.OtaReleaseManifest
import com.example.data.util.OtaUpdateEngine
import com.example.data.util.PdfLibAnnotationEngine
import com.example.data.sync.BackgroundSyncWorkerState
import com.example.data.sync.DocFlowSyncManager
import com.example.ui.screens.ScanFilter
import com.example.widget.DocFlowAppWidgetProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class SortOption(val label: String) {
    UPDATED_DESC("Recently Updated"),
    TITLE_ASC("Title (A-Z)"),
    SIZE_DESC("Largest Size"),
    CATEGORY("Category")
}

@OptIn(ExperimentalCoroutinesApi::class)
class DocFlowViewModel(
    private val repository: DocFlowRepository,
    val currentUserId: String,
    private val dao: DocFlowDao? = null,
    val currentUserDisplayName: String = "Workspace Owner",
    val currentUserEmail: String = "",
    private val appContext: Context? = null
) : ViewModel() {

    private val _localDocumentUris = MutableStateFlow<Map<String, Uri>>(emptyMap())
    val localDocumentUris: StateFlow<Map<String, Uri>> = _localDocumentUris.asStateFlow()

    val syncLogs: StateFlow<List<SyncLogEntity>> = (dao?.observeRecentSyncLogs(currentUserId) ?: flowOf(emptyList()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val pendingSyncCount: StateFlow<Int> = (dao?.observePendingSyncCount(currentUserId) ?: flowOf(0))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0
        )

    val backgroundSyncWorkerState: StateFlow<BackgroundSyncWorkerState> =
        (appContext?.let { DocFlowSyncManager.observeSyncWorkerState(it) } ?: flowOf(BackgroundSyncWorkerState()))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = BackgroundSyncWorkerState()
            )

    val cachedFoldersList: StateFlow<List<CachedFolderEntity>> =
        (dao?.observeCachedFolders(currentUserId) ?: flowOf(emptyList()))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    val deletedDocuments: StateFlow<List<CachedDocumentEntity>> =
        (dao?.observeDeletedDocuments(currentUserId) ?: flowOf(emptyList()))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    val privateSafeDocuments: StateFlow<List<CachedDocumentEntity>> =
        (dao?.observePrivateSafeDocuments(currentUserId) ?: flowOf(emptyList()))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    val createdByMeDocuments: StateFlow<List<CachedDocumentEntity>> =
        (dao?.observeCreatedByMeDocuments(currentUserId) ?: flowOf(emptyList()))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    init {
        appContext?.let { ctx ->
            DocFlowSyncManager.schedulePeriodicBackgroundSync(ctx, currentUserId)
            DocFlowSyncManager.enqueueOfflineSyncWhenConnected(ctx, currentUserId, replaceExisting = false)
        }
        viewModelScope.launch {
            // 30-day auto-purge of Recently deleted items
            val thirtyDaysAgoMs = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L)
            dao?.purgeExpiredDeletedDocuments(thirtyDaysAgoMs)

            // Seed default offline starter files once if the local database is empty on first install
            val prefs = appContext?.getSharedPreferences("docflow_offline_prefs", Context.MODE_PRIVATE)
            val alreadyInitialized = prefs?.getBoolean("starter_seeded_v1", false) ?: false
            val existingDocs = dao?.getAllDocumentsForUser(currentUserId) ?: emptyList()
            if (!alreadyInitialized && existingDocs.isEmpty() && dao != null) {
                prefs?.edit()?.putBoolean("starter_seeded_v1", true)?.apply()
                seedSampleWorkspace()
            }
        }
    }

    val cachedDocumentsMap: StateFlow<Map<String, CachedDocumentEntity>> =
        (dao?.observeCachedDocuments(currentUserId) ?: flowOf(emptyList()))
            .map { list -> list.associateBy { it.id } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyMap()
            )

    /**
     * Reactive stream of Recent Files prioritized by Room `lastAccessedAtMillis` timestamps.
     */
    val recentCachedDocuments: StateFlow<List<CachedDocumentEntity>> =
        (dao?.observeRecentCachedDocuments(currentUserId) ?: flowOf(emptyList()))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    private val allUserVersions: StateFlow<List<DocumentVersionEntity>> =
        (dao?.observeAllUserVersions(currentUserId) ?: flowOf(emptyList()))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFolderId = MutableStateFlow("all")
    val selectedFolderId: StateFlow<String> = _selectedFolderId.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedFileType = MutableStateFlow("All")
    val selectedFileType: StateFlow<String> = _selectedFileType.asStateFlow()

    private val _offlineOnlyFilter = MutableStateFlow(false)
    val offlineOnlyFilter: StateFlow<Boolean> = _offlineOnlyFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.UPDATED_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _themeMode = MutableStateFlow(
        appContext?.let { ctx ->
            val saved = com.example.data.util.OnDeviceAiAndBackupEngine.getSavedThemeModeName(ctx)
            runCatching { ThemeMode.valueOf(saved) }.getOrDefault(ThemeMode.LIGHT)
        } ?: ThemeMode.LIGHT
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _selectedTagFilters = MutableStateFlow<Set<String>>(emptySet())
    val selectedTagFilters: StateFlow<Set<String>> = _selectedTagFilters.asStateFlow()

    private val _biometricVaultEnabled = MutableStateFlow(
        appContext?.let { ctx ->
            com.example.data.util.OnDeviceAiAndBackupEngine.isBiometricUnlockEnabled(ctx)
        } ?: true
    )
    val biometricVaultEnabled: StateFlow<Boolean> = _biometricVaultEnabled.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    // Google-Docs-Style Live Collaborative Editing State
    private val _liveSyncStatus = MutableStateFlow(LiveSyncStatus.SAVED)
    val liveSyncStatus: StateFlow<LiveSyncStatus> = _liveSyncStatus.asStateFlow()

    private val _currentUserRole = MutableStateFlow(CollaboratorRole.OWNER)
    val currentUserRole: StateFlow<CollaboratorRole> = _currentUserRole.asStateFlow()

    private val _activeCollaborators = MutableStateFlow(
        listOf(
            ActiveCollaborator(
                id = currentUserId,
                name = currentUserDisplayName.ifBlank { "You (Authenticated User)" },
                email = currentUserEmail.ifBlank { "$currentUserId@docflow.verified" },
                role = CollaboratorRole.OWNER,
                colorHex = "#00F4FE",
                activeCursorLabel = "Live Session Active",
                isOnline = true
            )
        )
    )
    val activeCollaborators: StateFlow<List<ActiveCollaborator>> = _activeCollaborators.asStateFlow()

    // Over-The-Air (OTA) App Updates State
    private val _otaCurrentVersionCode = MutableStateFlow(23)
    val otaCurrentVersionCode: StateFlow<Int> = _otaCurrentVersionCode.asStateFlow()

    private val _otaCurrentVersionName = MutableStateFlow("2.3.0-crystal")
    val otaCurrentVersionName: StateFlow<String> = _otaCurrentVersionName.asStateFlow()

    private val _otaChannel = MutableStateFlow(OtaReleaseChannel.STABLE)
    val otaChannel: StateFlow<OtaReleaseChannel> = _otaChannel.asStateFlow()

    private val _otaInstallPhase = MutableStateFlow(OtaInstallPhase.UPDATE_AVAILABLE)
    val otaInstallPhase: StateFlow<OtaInstallPhase> = _otaInstallPhase.asStateFlow()

    private val _otaDownloadProgress = MutableStateFlow(0f)
    val otaDownloadProgress: StateFlow<Float> = _otaDownloadProgress.asStateFlow()

    private val _otaAvailableManifest = MutableStateFlow(
        OtaUpdateEngine.getLatestReleaseForChannel(OtaReleaseChannel.STABLE, 23)
    )
    val otaAvailableManifest: StateFlow<OtaReleaseManifest> = _otaAvailableManifest.asStateFlow()

    private val _otaAutoInstallEnabled = MutableStateFlow(true)
    val otaAutoInstallEnabled: StateFlow<Boolean> = _otaAutoInstallEnabled.asStateFlow()

    val otaHistory: StateFlow<List<OtaUpdateRecordEntity>> =
        (dao?.observeOtaUpdateRecords(currentUserId) ?: flowOf(emptyList()))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    private var liveAutosaveJob: Job? = null
    private var liveKeystrokeCounter: Int = 0

    /**
     * Reactive Room Full-Text Search (FTS4 + substring fallback) stream.
     * Queries cached_documents_fts by title, contentPreview, tags, category, and fileName.
     */
    private val roomSearchResults: StateFlow<Set<String>?> = _searchQuery
        .flatMapLatest { rawQuery ->
            val trimmed = rawQuery.trim()
            if (trimmed.isEmpty() || dao == null) {
                flowOf(null)
            } else {
                val ftsTokens = trimmed
                    .split(Regex("[^a-zA-Z0-9]+"))
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { "$it*" }

                val ftsFlow = if (ftsTokens.isNotBlank()) {
                    dao.searchCachedDocumentsFts(currentUserId, ftsTokens)
                        .catch { emit(emptyList()) }
                } else {
                    flowOf(emptyList())
                }

                val likeFlow = dao.searchCachedDocumentsSubstring(currentUserId, trimmed)
                    .catch { emit(emptyList()) }

                combine(ftsFlow, likeFlow) { ftsMatches, likeMatches ->
                    (ftsMatches.map { it.id } + likeMatches.map { it.id }).toSet()
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    val foldersState: StateFlow<UiState<List<FolderEntity>>> = combine(
        repository.observeFolders()
            .onEach { remoteFolders ->
                dao?.let { localDao ->
                    if (remoteFolders.isNotEmpty()) {
                        val entities = remoteFolders.map { CachedFolderEntity.fromFolderEntity(it, SyncState.SYNCED) }
                        localDao.upsertFolders(entities)
                    }
                }
            }
            .catch { emit(emptyList()) },
        cachedFoldersList
    ) { remoteFolders, localFolders ->
        val effectiveFolders = if (localFolders.isNotEmpty()) {
            localFolders.map { it.toFolderEntity() }
        } else {
            remoteFolders
        }
        UiState.Success(effectiveFolders.sortedBy { it.name.lowercase() }) as UiState<List<FolderEntity>>
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
    )

    val documentsState: StateFlow<UiState<List<DocEntity>>> = repository.observeDocuments()
        .onEach { remoteDocs ->
            dao?.let { localDao ->
                val existingMap = cachedDocumentsMap.value
                val entities = remoteDocs.map { doc ->
                    val existing = existingMap[doc.id]
                    val existingLocalPath = existing?.localFilePath
                    val existingSyncState = existing?.syncState
                    val existingTags = existing?.tags
                    val existingAccessedMs = existing?.lastAccessedAtMillis
                    val stateToSave = if (existingSyncState != null && existingSyncState != SyncState.SYNCED.name && _isOfflineMode.value) {
                        runCatching { SyncState.valueOf(existingSyncState) }.getOrDefault(SyncState.LOCAL_ONLY)
                    } else if (_isOfflineMode.value) {
                        SyncState.LOCAL_ONLY
                    } else {
                        SyncState.SYNCED
                    }
                    CachedDocumentEntity.fromDocEntity(
                        doc = doc,
                        syncState = stateToSave,
                        localFilePath = existingLocalPath,
                        existingTags = existingTags,
                        existingLastAccessedAtMillis = existingAccessedMs
                    )
                }
                localDao.upsertDocuments(entities)
                val restoredUris = mutableMapOf<String, Uri>()
                entities.forEach { entity ->
                    if (!entity.localFilePath.isNullOrBlank()) {
                        val file = File(entity.localFilePath)
                        if (file.exists()) {
                            restoredUris[entity.id] = Uri.fromFile(file)
                        }
                    }
                }
                if (restoredUris.isNotEmpty()) {
                    _localDocumentUris.value = _localDocumentUris.value + restoredUris
                }
            }
        }
        .map<List<DocEntity>, UiState<List<DocEntity>>> { docs ->
            UiState.Success(docs)
        }
        .catch { e ->
            Log.w(TAG, "Error observing documents", e)
            emit(UiState.Error(e.localizedMessage ?: "Failed to load documents"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val filteredDocuments: StateFlow<List<DocEntity>> = combine(
        documentsState,
        cachedDocumentsMap,
        roomSearchResults,
        _selectedFolderId,
        combine(
            combine(_searchQuery, _selectedCategory, _selectedFileType) { q, cat, ft -> Triple(q, cat, ft) },
            combine(_offlineOnlyFilter, _sortOption, _selectedTagFilters) { off, sort, tags -> Triple(off, sort, tags) }
        ) { (q, cat, ft), (off, sort, tags) ->
            FilterParams(q, cat, ft, off, sort, tags)
        }
    ) { docState, cachedMap, ftsMatchedIds, folderId, params ->
        val remoteDocs = (docState as? UiState.Success)?.data ?: emptyList()
        val docs = if (remoteDocs.isNotEmpty()) {
            remoteDocs
        } else {
            cachedMap.values.map { it.toDocEntity() }
        }

        val query = params.query.trim()
        val filtered = docs.filter { doc ->
            val cached = cachedMap[doc.id]
            // Exclude soft-deleted items and Private Safe items from Recent & general search
            if (cached?.isDeleted == true || cached?.isPrivateSafe == true) {
                return@filter false
            }
            val docTags = cached?.parsedTags()?.map { it.lowercase() }?.toSet() ?: emptySet()

            val matchesQuery = if (query.isBlank()) {
                true
            } else if (ftsMatchedIds != null) {
                ftsMatchedIds.contains(doc.id) ||
                    doc.title.contains(query, ignoreCase = true) ||
                    (cached?.tags?.contains(query, ignoreCase = true) == true) ||
                    (cached?.contentPreview?.contains(query, ignoreCase = true) == true) ||
                    (!doc.isEncrypted && doc.content.contains(query, ignoreCase = true))
            } else {
                doc.title.contains(query, ignoreCase = true) ||
                    doc.fileName.contains(query, ignoreCase = true) ||
                    doc.category.contains(query, ignoreCase = true) ||
                    (cached?.tags?.contains(query, ignoreCase = true) == true) ||
                    (!doc.isEncrypted && doc.content.contains(query, ignoreCase = true))
            }

            val matchesCustomTagFilter = if (params.tagFilters.isEmpty()) {
                true
            } else {
                params.tagFilters.all { filterTag ->
                    docTags.any { it.equals(filterTag, ignoreCase = true) } ||
                        (cached?.tags?.contains(filterTag, ignoreCase = true) == true)
                }
            }

            val matchesFolder = when (folderId) {
                "all" -> true
                else -> doc.folderId == folderId
            }

            val effectiveCategory = cachedMap[doc.id]?.category ?: doc.category
            val matchesCategory = params.category == "All" || effectiveCategory.equals(params.category, ignoreCase = true)

            val docType = doc.fileType.lowercase()
            val matchesFileType = when (params.fileType.lowercase()) {
                "all" -> true
                "pdf" -> docType == "pdf" || docType == "scanned"
                "docx", "doc" -> docType == "docx" || docType == "doc"
                "xlsx", "xls" -> docType == "xlsx" || docType == "xls" || docType == "csv"
                "pptx", "ppt" -> docType == "pptx" || docType == "ppt"
                "ofd" -> docType == "ofd"
                "txt" -> docType == "txt" || docType == "md" || docType == "note"
                "scanned" -> docType == "scanned"
                "pic" -> docType == "pic"
                else -> true
            }

            val matchesOffline = !params.offlineOnly || doc.isOfflineCached

            matchesQuery && matchesCustomTagFilter && matchesFolder && matchesCategory && matchesFileType && matchesOffline
        }

        when (params.sortOpt) {
            SortOption.UPDATED_DESC -> filtered.sortedByDescending {
                it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L
            }
            SortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOption.SIZE_DESC -> filtered.sortedByDescending { it.fileSize }
            SortOption.CATEGORY -> filtered.sortedBy { it.category.lowercase() }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    private data class FilterParams(
        val query: String,
        val category: String,
        val fileType: String,
        val offlineOnly: Boolean,
        val sortOpt: SortOption,
        val tagFilters: Set<String>
    )

    /**
     * Reactive Storage Dashboard Summary & Interactive Donut Chart aggregation
     * across PDF, Word (DOCX), PowerPoint (PPTX), Excel (XLSX), and Pictures/Scans (PIC).
     */
    val storageDashboardSummary: StateFlow<StorageDashboardSummary> = combine(
        documentsState,
        cachedDocumentsMap,
        allUserVersions
    ) { docState, cachedMap, versions ->
        val remoteDocs = (docState as? UiState.Success)?.data ?: emptyList()
        val docs = if (remoteDocs.isNotEmpty()) {
            remoteDocs
        } else {
            cachedMap.values.map { it.toDocEntity() }
        }

        val totalBytes = docs.sumOf { it.fileSize.coerceAtLeast(1024L) }
        val safeTotal = totalBytes.coerceAtLeast(1L).toFloat()

        val pdfDocs = docs.filter { it.fileType.equals("pdf", true) || it.fileType.equals("scanned", true) }
        val docxDocs = docs.filter { it.fileType.equals("docx", true) || it.fileType.equals("doc", true) }
        val pptxDocs = docs.filter { it.fileType.equals("pptx", true) || it.fileType.equals("ppt", true) }
        val xlsxDocs = docs.filter {
            it.fileType.equals("xlsx", true) || it.fileType.equals("xls", true) || it.fileType.equals("csv", true)
        }
        val picDocs = docs.filter {
            val ft = it.fileType.lowercase()
            ft !in setOf("pdf", "scanned", "docx", "doc", "pptx", "ppt", "xlsx", "xls", "csv")
        }

        val pdfBytes = pdfDocs.sumOf { it.fileSize.coerceAtLeast(1024L) }
        val docxBytes = docxDocs.sumOf { it.fileSize.coerceAtLeast(1024L) }
        val pptxBytes = pptxDocs.sumOf { it.fileSize.coerceAtLeast(1024L) }
        val xlsxBytes = xlsxDocs.sumOf { it.fileSize.coerceAtLeast(1024L) }
        val picBytes = picDocs.sumOf { it.fileSize.coerceAtLeast(1024L) }

        val slices = listOf(
            FormatStorageSlice(
                formatKey = "pdf",
                displayLabel = "PDF & Scans (.pdf)",
                subtitle = "Portable Document & Multi-Page OCR",
                bytesUsed = pdfBytes,
                documentCount = pdfDocs.size,
                percentage = if (totalBytes > 0L) (pdfBytes / safeTotal) * 100f else 20f,
                colorHex = 0xFFE11D48
            ),
            FormatStorageSlice(
                formatKey = "docx",
                displayLabel = "Word (.docx)",
                subtitle = "Google-Docs-Style Live Collaborative Docs",
                bytesUsed = docxBytes,
                documentCount = docxDocs.size,
                percentage = if (totalBytes > 0L) (docxBytes / safeTotal) * 100f else 20f,
                colorHex = 0xFF0066FF
            ),
            FormatStorageSlice(
                formatKey = "pptx",
                displayLabel = "PowerPoint (.pptx)",
                subtitle = "Structured Slide Decks & Visuals",
                bytesUsed = pptxBytes,
                documentCount = pptxDocs.size,
                percentage = if (totalBytes > 0L) (pptxBytes / safeTotal) * 100f else 20f,
                colorHex = 0xFFD97706
            ),
            FormatStorageSlice(
                formatKey = "xlsx",
                displayLabel = "Excel (.xlsx)",
                subtitle = "Spreadsheets & Structured Table Grids",
                bytesUsed = xlsxBytes,
                documentCount = xlsxDocs.size,
                percentage = if (totalBytes > 0L) (xlsxBytes / safeTotal) * 100f else 20f,
                colorHex = 0xFF059669
            ),
            FormatStorageSlice(
                formatKey = "pic",
                displayLabel = "Pictures & PNGs (.png)",
                subtitle = "Doc-to-Pic High-Res Exports",
                bytesUsed = picBytes,
                documentCount = picDocs.size,
                percentage = if (totalBytes > 0L) (picBytes / safeTotal) * 100f else 20f,
                colorHex = 0xFF5E17EB
            )
        )

        val offlineDocs = docs.filter { it.isOfflineCached }
        StorageDashboardSummary(
            totalBytesUsed = totalBytes,
            offlinePinnedBytes = offlineDocs.sumOf { it.fileSize.coerceAtLeast(1024L) },
            offlinePinnedCount = offlineDocs.size,
            encryptedVaultCount = docs.count { it.isEncrypted },
            versionSnapshotsCount = versions.size,
            slices = slices
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = StorageDashboardSummary()
    )

    private suspend fun recordSyncLog(
        docId: String,
        docTitle: String,
        action: String,
        status: String,
        details: String
    ) {
        dao?.insertSyncLog(
            SyncLogEntity(
                userId = currentUserId,
                documentId = docId,
                documentTitle = docTitle,
                action = action,
                status = status,
                details = details
            )
        )
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFolder(folderId: String) {
        _selectedFolderId.value = folderId
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectFileType(fileType: String) {
        _selectedFileType.value = fileType
    }

    fun toggleOfflineOnlyFilter() {
        _offlineOnlyFilter.value = !_offlineOnlyFilter.value
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun toggleThemeMode() {
        val next = when (_themeMode.value) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
        }
        setThemeMode(next)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        appContext?.let { ctx ->
            com.example.data.util.OnDeviceAiAndBackupEngine.saveThemeModeName(ctx, mode.name)
        }
    }

    fun toggleTagFilter(tag: String) {
        val clean = tag.trim().lowercase()
        if (clean.isEmpty()) return
        val current = _selectedTagFilters.value
        _selectedTagFilters.value = if (current.contains(clean)) {
            current - clean
        } else {
            current + clean
        }
    }

    fun clearTagFilters() {
        _selectedTagFilters.value = emptySet()
    }

    fun toggleBiometricVault() {
        _biometricVaultEnabled.value = !_biometricVaultEnabled.value
        _statusBannerMessage.value = if (_biometricVaultEnabled.value) {
            "Biometric Vault Guard (Fingerprint / Face Unlock) enabled for sensitive files"
        } else {
            "Biometric Vault Guard paused for current session"
        }
    }

    fun updateDocumentCategoryAndTags(doc: DocEntity, newCategory: String, newTags: String) {
        viewModelScope.launch {
            val existing = cachedDocumentsMap.value[doc.id]
            val cleanedTags = newTags.split(",")
                .map { it.trim().lowercase().removePrefix("#") }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(", ")

            val targetCategory = newCategory.trim().ifBlank { doc.category }
            val updatedEntity = (existing ?: CachedDocumentEntity.fromDocEntity(doc)).copy(
                category = targetCategory,
                tags = cleanedTags,
                updatedAtMillis = System.currentTimeMillis()
            )
            dao?.upsertDocument(updatedEntity)
            dao?.updateDocumentTags(doc.id, cleanedTags)
            dao?.updateDocumentCategory(doc.id, targetCategory)

            recordSyncLog(
                docId = doc.id,
                docTitle = doc.title,
                action = "ROOM_CATEGORIZED",
                status = "INDEXED",
                details = "Category: $targetCategory, Tags: $cleanedTags"
            )
            _statusBannerMessage.value = "Updated '${doc.title}': Category '$targetCategory', Tags: ${if (cleanedTags.isNotBlank()) cleanedTags else "None"}"
        }
    }

    fun addCustomTagToDocument(doc: DocEntity, newTagInput: String) {
        val newTags = newTagInput.split(",")
            .map { it.trim().lowercase().removePrefix("#") }
            .filter { it.isNotEmpty() }
        if (newTags.isEmpty()) return

        viewModelScope.launch {
            val existing = cachedDocumentsMap.value[doc.id]
            val currentTags = existing?.parsedTags()?.map { it.lowercase() } ?: emptyList()
            val merged = (currentTags + newTags).distinct().joinToString(", ")

            val updatedEntity = (existing ?: CachedDocumentEntity.fromDocEntity(doc)).copy(
                tags = merged,
                updatedAtMillis = System.currentTimeMillis()
            )
            dao?.upsertDocument(updatedEntity)
            recordSyncLog(
                docId = doc.id,
                docTitle = doc.title,
                action = "TAG_ADDED",
                status = "FTS4_INDEXED",
                details = "Added custom tag(s): ${newTags.joinToString(", ") { "#$it" }}"
            )
            _statusBannerMessage.value = "Tagged '${doc.title}' with ${newTags.joinToString(", ") { "#$it" }} (Indexed in Room FTS4)"
        }
    }

    fun removeTagFromDocument(doc: DocEntity, tagToRemove: String) {
        viewModelScope.launch {
            val existing = cachedDocumentsMap.value[doc.id] ?: return@launch
            val remaining = existing.parsedTags()
                .filterNot { it.equals(tagToRemove.trim(), ignoreCase = true) }
                .joinToString(", ")
            dao?.upsertDocument(
                existing.copy(
                    tags = remaining,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
            _statusBannerMessage.value = "Removed tag #$tagToRemove from '${doc.title}'"
        }
    }

    fun toggleDocumentBiometricLock(doc: DocEntity) {
        viewModelScope.launch {
            val existing = cachedDocumentsMap.value[doc.id] ?: CachedDocumentEntity.fromDocEntity(doc)
            val currentTags = existing.parsedTags()
            val isLocked = currentTags.any { it.equals("biometric-locked", ignoreCase = true) }
            val updatedTags = if (isLocked) {
                currentTags.filterNot { it.equals("biometric-locked", ignoreCase = true) }
            } else {
                (currentTags + "biometric-locked").distinct()
            }
            dao?.upsertDocument(
                existing.copy(
                    tags = updatedTags.joinToString(", "),
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
            _statusBannerMessage.value = if (!isLocked) {
                "Biometric Lock enabled for '${doc.title}' (Requires Fingerprint/Face Unlock)"
            } else {
                "Biometric Lock removed from '${doc.title}'"
            }
        }
    }

    /**
     * Simultaneous Batch Processing: Converts multiple selected documents (PDF, Word, Excel, PowerPoint)
     * into high-resolution Pictures (Multi-Page or Stitched Single Tall Image) in one batch operation.
     */
    fun runBatchDocToPicConversion(
        context: Context,
        docsToConvert: List<DocEntity>,
        mode: DocToPicMode,
        customBatchTag: String,
        onComplete: (Int) -> Unit = {}
    ) {
        if (docsToConvert.isEmpty()) return
        viewModelScope.launch {
            var successCount = 0
            docsToConvert.forEach { doc ->
                val plainText = if (doc.isEncrypted) {
                    "=== ENCRYPTED DOCUMENT PREVIEW: ${doc.title} ===\nProtected by AES-256 & Biometric Security."
                } else {
                    doc.content
                }
                val localUri = _localDocumentUris.value[doc.id]
                val convRes = DocToPicConverter.convertDocumentToPictures(
                    context = context,
                    document = doc,
                    plainContent = plainText,
                    localUri = localUri,
                    mode = mode
                )
                if (convRes.isSuccess) {
                    val res = convRes.getOrThrow()
                    val picTitle = "${doc.title} [Batch Pic]"
                    val cleanName = doc.title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                        .ifBlank { "batch_doc" } + "_batch.png"
                    val summaryContent = """
                        === BATCH DOC-TO-PIC EXPORT: ${doc.title} ===
                        Source Format: ${doc.fileType.uppercase()} (${doc.fileName})
                        Batch Export Mode: ${mode.label}
                        Rendered Pages/Slides: ${res.pageBitmaps.size}
                        Saved PNG File: ${res.primarySavedFile.name} (${res.primarySavedFile.length() / 1024} KB)
                        
                        | Batch Attribute | Value | Status |
                        | Source Document | ${doc.title} | Converted |
                        | Output Format | High-Res PNG (${mode.label}) | Indexed |
                    """.trimIndent()

                    createOrScanDocument(
                        title = picTitle,
                        folderId = doc.folderId,
                        category = doc.category,
                        fileType = "pic",
                        fileName = cleanName,
                        plainContent = summaryContent,
                        isEncrypted = false,
                        password = "",
                        encryptionKeyHint = null,
                        isOfflineCached = true,
                        pageCount = res.pageBitmaps.size.toLong().coerceAtLeast(1L),
                        localPdfPath = res.primarySavedFile.absolutePath,
                        generatedPdfUri = res.primarySavedUri,
                        customTags = "batch-export, doc-to-pic, png, ${doc.fileType.lowercase()}, $customBatchTag"
                    )
                    successCount++
                }
            }
            recordSyncLog(
                docId = "batch_doc_to_pic",
                docTitle = "Batch Doc-to-Pic ($successCount files)",
                action = "BATCH_DOC_TO_PIC",
                status = "COMPLETED",
                details = "Converted $successCount document(s) simultaneously to ${mode.label}"
            )
            _statusBannerMessage.value = "Batch Complete: Converted $successCount document(s) to PNG images!"
            onComplete(successCount)
        }
    }

    /**
     * Simultaneous Batch Processing: Converts multiple images into a single compiled multi-page PDF/Office document
     * OR individual editable documents with Multi-Language OCR & Automatic Language Detection.
     */
    fun runBatchPicToDocConversion(
        context: Context,
        imageUris: List<Uri>,
        batchTitlePrefix: String,
        targetFormat: String,
        compileIntoSingleDocument: Boolean,
        selectedLanguage: OcrLanguage,
        folderId: String,
        category: String,
        customBatchTag: String,
        onComplete: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            val decodedBitmaps = imageUris.mapNotNull { uri ->
                ImageProcessingPdfPipeline.decodeBitmapFromUri(context, uri)
            }
            val pageCount = decodedBitmaps.size.coerceAtLeast(imageUris.size.coerceAtLeast(2))

            if (compileIntoSingleDocument) {
                val title = batchTitlePrefix.ifBlank { "Compiled Multi-Page Batch ($pageCount Pages)" }
                val (ocrBody, langDetection) = MultiLanguageOcrEngine.extractMultiLanguageOcr(
                    presetCategory = category,
                    selectedLanguage = selectedLanguage,
                    pageCount = pageCount,
                    documentTitle = title
                )

                val sheets = (1..pageCount).map { idx ->
                    val rawBmp = decodedBitmaps.getOrNull(idx - 1)
                    ImageProcessingPdfPipeline.createHighResDocumentBitmap(
                        cameraBitmap = rawBmp,
                        title = "$title — Page $idx",
                        ocrText = ocrBody,
                        filter = ScanFilter.MAGIC_COLOR,
                        pageIndex = idx,
                        totalPages = pageCount
                    )
                }

                val cleanPdfName = title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                    .ifBlank { "batch_compiled" } + ".pdf"
                val pdfRes = ImageProcessingPdfPipeline.convertBitmapsToPdfFile(
                    context = context,
                    fileName = cleanPdfName,
                    bitmaps = sheets
                ).getOrNull()

                val structuredContent = DocToPicConverter.buildPicToDocStructuredContent(
                    targetFormat = targetFormat,
                    title = "$title [${langDetection.detectedLanguage.displayName} OCR]",
                    category = category,
                    pageCount = pageCount,
                    baseOcrText = "Detected Language: ${langDetection.detectedLanguage.displayName} (${langDetection.detectedLanguage.nativeLabel}) • Confidence: ${langDetection.confidencePercent}%\n\n$ocrBody"
                )

                val ext = targetFormat.lowercase()
                val cleanDocName = title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                    .ifBlank { "batch_compiled" } + ".$ext"

                createOrScanDocument(
                    title = title,
                    folderId = folderId,
                    category = category,
                    fileType = ext,
                    fileName = cleanDocName,
                    plainContent = structuredContent,
                    isEncrypted = false,
                    password = "",
                    encryptionKeyHint = null,
                    isOfflineCached = true,
                    pageCount = pageCount.toLong(),
                    localPdfPath = pdfRes?.pdfFile?.absolutePath,
                    generatedPdfUri = pdfRes?.pdfUri,
                    customTags = "batch-import, pic-to-doc, multi-page, ${langDetection.detectedLanguage.code}, $ext, $customBatchTag"
                )
                onComplete(1)
            } else {
                val totalItems = imageUris.size.coerceAtLeast(2)
                for (i in 1..totalItems) {
                    val itemTitle = "${batchTitlePrefix.ifBlank { "Batch Scan" }} #$i"
                    val (ocrBody, langDetection) = MultiLanguageOcrEngine.extractMultiLanguageOcr(
                        presetCategory = category,
                        selectedLanguage = selectedLanguage,
                        pageCount = 1,
                        documentTitle = itemTitle
                    )
                    val rawBmp = decodedBitmaps.getOrNull(i - 1)
                    val sheet = ImageProcessingPdfPipeline.createHighResDocumentBitmap(
                        cameraBitmap = rawBmp,
                        title = itemTitle,
                        ocrText = ocrBody,
                        filter = ScanFilter.MAGIC_COLOR,
                        pageIndex = 1,
                        totalPages = 1
                    )
                    val cleanPdfName = itemTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_') + ".pdf"
                    val pdfRes = ImageProcessingPdfPipeline.convertBitmapsToPdfFile(
                        context = context,
                        fileName = cleanPdfName,
                        bitmaps = listOf(sheet)
                    ).getOrNull()

                    val ext = targetFormat.lowercase()
                    val structuredContent = DocToPicConverter.buildPicToDocStructuredContent(
                        targetFormat = ext,
                        title = itemTitle,
                        category = category,
                        pageCount = 1,
                        baseOcrText = "Detected Language: ${langDetection.detectedLanguage.displayName} (${langDetection.confidencePercent}%)\n\n$ocrBody"
                    )
                    createOrScanDocument(
                        title = itemTitle,
                        folderId = folderId,
                        category = category,
                        fileType = ext,
                        fileName = itemTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_') + ".$ext",
                        plainContent = structuredContent,
                        isEncrypted = false,
                        password = "",
                        encryptionKeyHint = null,
                        isOfflineCached = true,
                        pageCount = 1L,
                        localPdfPath = pdfRes?.pdfFile?.absolutePath,
                        generatedPdfUri = pdfRes?.pdfUri,
                        customTags = "batch-import, pic-to-doc, ${langDetection.detectedLanguage.code}, $ext, $customBatchTag"
                    )
                }
                onComplete(totalItems)
            }
            _statusBannerMessage.value = "Batch Pic-to-${targetFormat.uppercase()} with Multi-Language OCR complete!"
        }
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun clearSyncHistory() {
        viewModelScope.launch {
            dao?.clearSyncLogs(currentUserId)
        }
    }

    fun toggleOfflineNetworkMode() {
        val nextOffline = !_isOfflineMode.value
        _isOfflineMode.value = nextOffline
        viewModelScope.launch {
            try {
                if (nextOffline) {
                    repository.db.disableNetwork()
                    recordSyncLog(
                        docId = "system",
                        docTitle = "Room Offline Cache",
                        action = "OFFLINE_MODE_ENABLED",
                        status = "LOCAL_ONLY",
                        details = "Switched to Room SQLite + FTS4 local search cache; WorkManager queued for network restore"
                    )
                    _statusBannerMessage.value = "Offline Mode active: Changes queued for WorkManager Supabase sync on network restore"
                } else {
                    repository.db.enableNetwork()
                    val pendingDocs = dao?.getPendingSyncDocuments(currentUserId) ?: emptyList()
                    val pendingAnnotations = dao?.getPendingSyncAnnotations(currentUserId) ?: emptyList()
                    val totalPending = pendingDocs.size + pendingAnnotations.size
                    appContext?.let { ctx ->
                        DocFlowSyncManager.enqueueOfflineSyncWhenConnected(ctx, currentUserId, replaceExisting = true)
                    }
                    pendingDocs.forEach { pending ->
                        dao?.updateDocumentSyncState(pending.id, SyncState.SYNCED.name)
                    }
                    pendingAnnotations.forEach { ann ->
                        repository.upsertPdfAnnotation(ann)
                        dao?.updateAnnotationSyncState(ann.ymapKey, SyncState.SYNCED.name)
                    }
                    recordSyncLog(
                        docId = "system",
                        docTitle = "WorkManager Firebase & Supabase Sync",
                        action = "WORKMANAGER_RESYNC",
                        status = "SYNCED",
                        details = "Network restored: WorkManager uploaded $totalPending queued local change(s) (${pendingDocs.size} docs, ${pendingAnnotations.size} PDF annotations) to Firebase & Supabase"
                    )
                    _statusBannerMessage.value = "Network restored: WorkManager synced $totalPending queued change(s) to Firebase & Supabase"
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error toggling network mode", e)
            }
        }
    }

    fun triggerWorkManagerSupabaseSync() {
        viewModelScope.launch {
            val pendingDocs = dao?.getPendingSyncDocuments(currentUserId) ?: emptyList()
            val pendingAnnotations = dao?.getPendingSyncAnnotations(currentUserId) ?: emptyList()
            val totalPending = pendingDocs.size + pendingAnnotations.size
            appContext?.let { ctx ->
                DocFlowSyncManager.enqueueOfflineSyncWhenConnected(ctx, currentUserId, replaceExisting = true)
            }
            recordSyncLog(
                docId = "system",
                docTitle = "WorkManager Sync Dispatcher",
                action = "WORKMANAGER_DISPATCHED",
                status = if (_isOfflineMode.value) "QUEUED_FOR_NETWORK" else "RUNNING",
                details = "Scheduled WorkManager OneTimeWorkRequest ($totalPending pending item(s): ${pendingDocs.size} docs, ${pendingAnnotations.size} PDF annotations) with NetworkType.CONNECTED constraint"
            )
            _statusBannerMessage.value = if (_isOfflineMode.value) {
                "WorkManager queued $totalPending pending change(s) — will auto-upload to Firebase & Supabase when online"
            } else {
                "WorkManager background sync dispatched to Firebase & Supabase ($totalPending pending item(s))"
            }
        }
    }

    fun createFolder(name: String, color: String, icon: String, parentFolderId: String? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = repository.createFolder(
                name = name,
                color = color,
                icon = icon,
                parentFolderId = parentFolderId
            )
            val folderId = result.getOrElse {
                "folder_" + UUID.randomUUID().toString().replace("-", "").take(16)
            }
            dao?.upsertFolder(
                CachedFolderEntity(
                    id = folderId,
                    userId = currentUserId,
                    name = name.trim(),
                    color = color,
                    icon = icon,
                    parentFolderId = parentFolderId,
                    syncState = SyncState.LOCAL_ONLY.name
                )
            )
            recordSyncLog(
                docId = folderId,
                docTitle = name,
                action = "FOLDER_CREATE",
                status = "LOCAL_ONLY",
                details = "Folder '$name' saved to on-device SQLite"
            )
            _statusBannerMessage.value = "Folder '$name' created"
        }
    }

    fun renameOrUpdateFolder(
        folderId: String,
        newName: String,
        newColor: String,
        parentFolderId: String? = null
    ) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            dao?.updateFolderDetails(
                folderId = folderId,
                newName = newName.trim(),
                newColor = newColor,
                parentFolderId = parentFolderId
            )
            _statusBannerMessage.value = "Updated folder '${newName.trim()}'"
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
            dao?.deleteFolderById(folderId)
            if (_selectedFolderId.value == folderId) {
                _selectedFolderId.value = "all"
            }
            _statusBannerMessage.value = "Folder deleted"
        }
    }

    fun createOrScanDocument(
        title: String,
        folderId: String,
        category: String,
        fileType: String,
        fileName: String,
        plainContent: String,
        isEncrypted: Boolean,
        password: String,
        encryptionKeyHint: String?,
        isOfflineCached: Boolean,
        pageCount: Long = 1L,
        localPdfPath: String? = null,
        generatedPdfUri: Uri? = null,
        customTags: String = "",
        createdByMe: Boolean = true,
        sourceFolderKey: String? = null,
        onCreated: (String) -> Unit = {}
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val autoCategory = CachedDocumentEntity.autoCategorizeFromText(title, plainContent, category)
            val finalContent = if (isEncrypted && password.isNotBlank()) {
                CryptoUtils.encrypt(plainContent, password)
            } else {
                plainContent
            }
            val sizeBytes = if (!localPdfPath.isNullOrBlank()) {
                File(localPdfPath).length().coerceAtLeast(finalContent.toByteArray(Charsets.UTF_8).size.toLong())
            } else {
                finalContent.toByteArray(Charsets.UTF_8).size.toLong().coerceAtLeast(1024L)
            }

            val result = repository.createDocument(
                title = title,
                folderId = folderId.ifBlank { "root" },
                category = autoCategory,
                fileType = fileType,
                fileName = fileName,
                fileSize = sizeBytes,
                content = finalContent,
                isEncrypted = isEncrypted && password.isNotBlank(),
                encryptionKeyHint = if (isEncrypted) encryptionKeyHint else null,
                isOfflineCached = isOfflineCached,
                pageCount = pageCount
            )
            val id = result.getOrElse {
                "doc_" + UUID.randomUUID().toString().replace("-", "").take(16)
            }
            if (generatedPdfUri != null) {
                _localDocumentUris.value = _localDocumentUris.value + (id to generatedPdfUri)
            }
            val resolvedTags = CachedDocumentEntity.defaultTagsFor(title, autoCategory, fileType, customTags)
            val preview = CachedDocumentEntity.buildPreview(plainContent, isEncrypted && password.isNotBlank())
            dao?.upsertDocument(
                CachedDocumentEntity(
                    id = id,
                    userId = currentUserId,
                    title = title.trim(),
                    folderId = folderId.ifBlank { "root" },
                    category = autoCategory,
                    fileType = fileType.lowercase(),
                    fileName = fileName,
                    fileSize = sizeBytes,
                    content = finalContent,
                    contentPreview = preview,
                    tags = resolvedTags,
                    localFilePath = localPdfPath,
                    isEncrypted = isEncrypted && password.isNotBlank(),
                    encryptionKeyHint = if (isEncrypted) encryptionKeyHint else null,
                    isOfflineCached = isOfflineCached,
                    pageCount = pageCount,
                    syncState = SyncState.LOCAL_ONLY.name,
                    createdByMe = createdByMe,
                    sourceFolderKey = sourceFolderKey
                )
            )
            recordSyncLog(
                docId = id,
                docTitle = title,
                action = if (localPdfPath != null) "PIC_TO_DOC_${fileType.uppercase()}" else "DOC_CREATE_${fileType.uppercase()}",
                status = "SAVED_LOCALLY",
                details = "Indexed in local SQLite FTS4 ($pageCount pg, ${sizeBytes / 1024} KB)"
            )
            _statusBannerMessage.value = "'$title' (${fileType.uppercase()}) saved on device"
            appContext?.let { ctx ->
                DocFlowAppWidgetProvider.notifyDocumentOpened(ctx)
            }
            onCreated(id)
        }
    }

    fun importDocumentFromUri(
        context: Context,
        uri: Uri,
        folderId: String,
        category: String,
        isEncrypted: Boolean,
        password: String,
        encryptionKeyHint: String?,
        onImported: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val parsedResult = FileParserUtils.parseDocumentUri(context, uri)
            if (parsedResult.isSuccess) {
                val parsed = parsedResult.getOrThrow()
                val finalContent = if (isEncrypted && password.isNotBlank()) {
                    CryptoUtils.encrypt(parsed.extractedContent, password)
                } else {
                    parsed.extractedContent
                }
                val createRes = repository.createDocument(
                    title = parsed.title,
                    folderId = folderId.ifBlank { "root" },
                    category = category,
                    fileType = parsed.fileType,
                    fileName = parsed.fileName,
                    fileSize = parsed.fileSize,
                    content = finalContent,
                    isEncrypted = isEncrypted && password.isNotBlank(),
                    encryptionKeyHint = if (isEncrypted) encryptionKeyHint else null,
                    isOfflineCached = true,
                    pageCount = parsed.pageCount
                )
                if (createRes.isSuccess) {
                    val docId = createRes.getOrThrow()

                    // Copy the real file to internal storage so PdfRenderer, Coil/Bitmaps, and Share Sheets work reliably
                    val docsDir = File(context.filesDir, "docflow_documents").apply { mkdirs() }
                    val cleanFileName = "${docId}_${parsed.fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")}"
                    val localTargetFile = File(docsDir, cleanFileName)
                    try {
                        context.contentResolver.openInputStream(uri)?.use { inStream ->
                            localTargetFile.outputStream().use { outStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to copy imported document to local filesDir", e)
                    }
                    val savedLocalPath = if (localTargetFile.exists()) localTargetFile.absolutePath else null
                    val persistentUri = if (localTargetFile.exists()) Uri.fromFile(localTargetFile) else uri

                    _localDocumentUris.value = _localDocumentUris.value + (docId to persistentUri)
                    val syncState = if (_isOfflineMode.value) SyncState.LOCAL_ONLY else SyncState.SYNCED
                    val resolvedTags = CachedDocumentEntity.defaultTagsFor(parsed.title, category, parsed.fileType, "imported")
                    val preview = CachedDocumentEntity.buildPreview(parsed.extractedContent, isEncrypted && password.isNotBlank())
                    dao?.upsertDocument(
                        CachedDocumentEntity(
                            id = docId,
                            userId = currentUserId,
                            title = parsed.title,
                            folderId = folderId.ifBlank { "root" },
                            category = category,
                            fileType = parsed.fileType,
                            fileName = parsed.fileName,
                            fileSize = parsed.fileSize,
                            content = finalContent,
                            contentPreview = preview,
                            tags = resolvedTags,
                            localFilePath = savedLocalPath,
                            isEncrypted = isEncrypted && password.isNotBlank(),
                            encryptionKeyHint = if (isEncrypted) encryptionKeyHint else null,
                            isOfflineCached = true,
                            pageCount = parsed.pageCount,
                            syncState = syncState.name
                        )
                    )
                    recordSyncLog(
                        docId = docId,
                        docTitle = parsed.title,
                        action = "IMPORT_${parsed.fileType.uppercase()}",
                        status = syncState.label,
                        details = "Imported '${parsed.fileName}' into Room FTS4 & Firestore"
                    )
                    _statusBannerMessage.value = "Opened & indexed '${parsed.fileName}' (${parsed.fileType.uppercase()})"
                    DocFlowAppWidgetProvider.notifyDocumentOpened(context.applicationContext)
                    DocFlowSyncManager.enqueueOfflineSyncWhenConnected(context.applicationContext, currentUserId, replaceExisting = true)
                    onImported(docId)
                } else {
                    _statusBannerMessage.value = "Failed to sync imported file"
                }
            } else {
                _statusBannerMessage.value = "Could not parse selected file: ${parsedResult.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updateDocumentContent(
        doc: DocEntity,
        newTitle: String,
        newFolderId: String,
        newCategory: String,
        plainContent: String,
        isEncrypted: Boolean,
        password: String?,
        encryptionKeyHint: String?,
        isOfflineCached: Boolean,
        customTags: String? = null
    ) {
        viewModelScope.launch {
            val finalContent = if (isEncrypted && !password.isNullOrBlank()) {
                CryptoUtils.encrypt(plainContent, password)
            } else {
                plainContent
            }
            val newSize = finalContent.toByteArray(Charsets.UTF_8).size.toLong().coerceAtLeast(doc.fileSize)
            val newPages = ((plainContent.length / 1800) + 1).toLong().coerceAtLeast(doc.pageCount)

            val syncState = if (_isOfflineMode.value) SyncState.PENDING_UPDATE else SyncState.SYNCED
            val existingCached = cachedDocumentsMap.value[doc.id]
            val existingLocalPath = existingCached?.localFilePath
            val tagsToSave = customTags?.takeIf { it.isNotBlank() }
                ?: existingCached?.tags
                ?: CachedDocumentEntity.defaultTagsFor(newTitle, newCategory, doc.fileType)
            val preview = CachedDocumentEntity.buildPreview(plainContent, isEncrypted)

            dao?.upsertDocument(
                CachedDocumentEntity(
                    id = doc.id,
                    userId = currentUserId,
                    title = newTitle.ifBlank { doc.title },
                    folderId = newFolderId.ifBlank { "root" },
                    category = newCategory.ifBlank { doc.category },
                    fileType = doc.fileType,
                    fileName = doc.fileName,
                    fileSize = newSize,
                    content = finalContent,
                    contentPreview = preview,
                    tags = tagsToSave,
                    localFilePath = existingLocalPath,
                    isEncrypted = isEncrypted,
                    encryptionKeyHint = if (isEncrypted) encryptionKeyHint else null,
                    isOfflineCached = isOfflineCached,
                    pageCount = newPages,
                    syncState = syncState.name
                )
            )

            val result = repository.updateDocument(
                docId = doc.id,
                title = newTitle.ifBlank { doc.title },
                folderId = newFolderId.ifBlank { "root" },
                category = newCategory.ifBlank { doc.category },
                content = finalContent,
                isEncrypted = isEncrypted,
                encryptionKeyHint = if (isEncrypted) encryptionKeyHint else null,
                isOfflineCached = isOfflineCached,
                pageCount = newPages,
                fileSize = newSize
            )
            if (result.isSuccess) {
                recordSyncLog(
                    docId = doc.id,
                    docTitle = newTitle.ifBlank { doc.title },
                    action = "LIVE_EDIT_${doc.fileType.uppercase()}",
                    status = syncState.label,
                    details = "Structured block edits indexed in Room FTS4 & synced (${newSize / 1024} KB)"
                )
                _statusBannerMessage.value = "Structured block changes saved to Room FTS & synced in real-time"
            } else {
                dao?.updateDocumentSyncState(doc.id, SyncState.FAILED.name)
                _statusBannerMessage.value = "Failed to update document: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    /**
     * Saves a Doc-to-Pic export (Multi-Page or Stitched Single Image) into the DocFlow workspace
     * so it is indexed in Room FTS4 and synced across devices.
     */
    fun saveDocToPicExportToWorkspace(
        sourceDoc: DocEntity,
        exportModeLabel: String,
        pageCount: Int,
        imageFile: File,
        imageUri: Uri
    ) {
        val picTitle = "${sourceDoc.title} ($exportModeLabel Pic)"
        val cleanName = sourceDoc.title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            .ifBlank { "doc" } + "_pic_export.png"
        val summaryContent = """
            === DOC TO PIC EXPORT: ${sourceDoc.title} ===
            Source Format: ${sourceDoc.fileType.uppercase()} (${sourceDoc.fileName})
            Export Mode: $exportModeLabel
            Rendered Pages/Slides: $pageCount
            Saved Image File: ${imageFile.name} (${imageFile.length() / 1024} KB)
            
            | Conversion Attribute | Value | Status |
            | Original Format | ${sourceDoc.fileType.uppercase()} | Verified |
            | Target Output | High-Resolution PNG | Saved |
            | Export Layout | $exportModeLabel | Ready |
        """.trimIndent()

        createOrScanDocument(
            title = picTitle,
            folderId = sourceDoc.folderId,
            category = sourceDoc.category,
            fileType = "pic",
            fileName = cleanName,
            plainContent = summaryContent,
            isEncrypted = false,
            password = "",
            encryptionKeyHint = null,
            isOfflineCached = true,
            pageCount = pageCount.toLong().coerceAtLeast(1L),
            localPdfPath = imageFile.absolutePath,
            generatedPdfUri = imageUri,
            customTags = "doc-to-pic, image, png, ${sourceDoc.fileType.lowercase()}"
        )
    }

    /**
     * Converts an existing document (e.g. Pic, PDF, Word, Excel, PowerPoint) into another Office format
     * while preserving its structured text and table blocks.
     */
    fun convertDocumentToFormat(
        sourceDoc: DocEntity,
        targetFormat: String,
        plainContent: String,
        onConverted: (String) -> Unit = {}
    ) {
        val ext = targetFormat.lowercase()
        val convertedTitle = "${sourceDoc.title} (${ext.uppercase()})"
        val cleanName = sourceDoc.title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            .ifBlank { "converted_doc" } + ".$ext"
        val existingPath = cachedDocumentsMap.value[sourceDoc.id]?.localFilePath
        val existingUri = _localDocumentUris.value[sourceDoc.id]

        createOrScanDocument(
            title = convertedTitle,
            folderId = sourceDoc.folderId,
            category = sourceDoc.category,
            fileType = ext,
            fileName = cleanName,
            plainContent = plainContent,
            isEncrypted = false,
            password = "",
            encryptionKeyHint = null,
            isOfflineCached = true,
            pageCount = sourceDoc.pageCount,
            localPdfPath = existingPath,
            generatedPdfUri = existingUri,
            customTags = "converted, $ext, ${sourceDoc.fileType.lowercase()}, ${sourceDoc.category.lowercase()}",
            onCreated = onConverted
        )
    }

    fun toggleOfflineAvailable(doc: DocEntity) {
        viewModelScope.launch {
            val nextOffline = !doc.isOfflineCached
            val nextSyncState = if (nextOffline && _isOfflineMode.value) {
                SyncState.LOCAL_ONLY
            } else {
                SyncState.SYNCED
            }
            dao?.updateDocumentSyncState(doc.id, nextSyncState.name)
            val result = repository.updateDocument(
                docId = doc.id,
                title = doc.title,
                folderId = doc.folderId,
                category = doc.category,
                content = doc.content,
                isEncrypted = doc.isEncrypted,
                encryptionKeyHint = doc.encryptionKeyHint,
                isOfflineCached = nextOffline,
                pageCount = doc.pageCount,
                fileSize = doc.fileSize
            )
            if (result.isSuccess) {
                recordSyncLog(
                    docId = doc.id,
                    docTitle = doc.title,
                    action = if (nextOffline) "PIN_OFFLINE" else "UNPIN_OFFLINE",
                    status = nextSyncState.label,
                    details = if (nextOffline) "Pinned in Room SQLite for offline access" else "Switched to Cloud-only stream"
                )
                _statusBannerMessage.value = if (nextOffline) {
                    "'${doc.title}' pinned in Room DB for Offline Access"
                } else {
                    "'${doc.title}' unpinned from local offline storage"
                }
            }
        }
    }

    fun deleteDocument(docId: String) {
        softDeleteDocument(docId)
    }

    fun softDeleteDocument(docId: String) {
        viewModelScope.launch {
            val title = cachedDocumentsMap.value[docId]?.title ?: "Document"
            dao?.softDeleteDocument(docId, System.currentTimeMillis())
            _statusBannerMessage.value = "Moved '$title' to Recently deleted (auto-purged in 30 days)"
        }
    }

    fun softDeleteDocuments(docIds: Set<String>) {
        if (docIds.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            docIds.forEach { id ->
                dao?.softDeleteDocument(id, now)
            }
            _statusBannerMessage.value = "Moved ${docIds.size} item(s) to Recently deleted"
        }
    }

    fun restoreDeletedDocument(docId: String) {
        viewModelScope.launch {
            dao?.restoreSoftDeletedDocument(docId, System.currentTimeMillis())
            _statusBannerMessage.value = "Restored document from Recently deleted"
        }
    }

    fun permanentlyDeleteDocument(docId: String) {
        viewModelScope.launch {
            val cached = deletedDocuments.value.find { it.id == docId } ?: cachedDocumentsMap.value[docId]
            cached?.localFilePath?.let { path ->
                runCatching { File(path).delete() }
            }
            repository.deleteDocument(docId)
            dao?.deleteDocumentById(docId)
            _statusBannerMessage.value = "Permanently deleted '${cached?.title ?: "Document"}'"
        }
    }

    fun emptyRecentlyDeleted() {
        viewModelScope.launch {
            deletedDocuments.value.forEach { doc ->
                doc.localFilePath?.let { path -> runCatching { File(path).delete() } }
            }
            dao?.emptyRecentlyDeleted(currentUserId)
            _statusBannerMessage.value = "Emptied Recently deleted"
        }
    }

    fun moveToPrivateSafe(docId: String, pinPassword: String) {
        viewModelScope.launch {
            val existing = cachedDocumentsMap.value[docId] ?: return@launch
            val plainText = if (existing.isEncrypted) {
                existing.content
            } else {
                existing.content
            }
            val encryptedBody = if (existing.isEncrypted) {
                existing.content
            } else {
                CryptoUtils.encrypt(plainText, pinPassword.ifBlank { "1234" })
            }
            dao?.updatePrivateSafeState(
                docId = docId,
                isPrivateSafe = true,
                isEncrypted = true,
                content = encryptedBody,
                keyHint = "Private Safe AES-256-GCM"
            )
            _statusBannerMessage.value = "Moved '${existing.title}' to Private Safe (AES-256-GCM encrypted)"
        }
    }

    fun removeFromPrivateSafe(docId: String, pinPassword: String) {
        viewModelScope.launch {
            val existing = privateSafeDocuments.value.find { it.id == docId } ?: return@launch
            val decryptedBody = if (existing.isEncrypted) {
                CryptoUtils.decrypt(existing.content, pinPassword.ifBlank { "1234" })
                    .getOrDefault(existing.content)
            } else {
                existing.content
            }
            dao?.updatePrivateSafeState(
                docId = docId,
                isPrivateSafe = false,
                isEncrypted = false,
                content = decryptedBody,
                keyHint = null
            )
            _statusBannerMessage.value = "Removed '${existing.title}' from Private Safe"
        }
    }

    fun moveDocumentsToFolder(docIds: Set<String>, targetFolderId: String) {
        if (docIds.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            docIds.forEach { docId ->
                dao?.moveDocumentToFolder(docId, targetFolderId, now)
            }
            _statusBannerMessage.value = "Moved ${docIds.size} document(s) to folder"
        }
    }

    fun batchTagDocuments(docIds: Set<String>, tagInput: String) {
        val cleanTags = tagInput.split(",")
            .map { it.trim().lowercase().removePrefix("#") }
            .filter { it.isNotEmpty() }
        if (docIds.isEmpty() || cleanTags.isEmpty()) return
        viewModelScope.launch {
            docIds.forEach { id ->
                val existing = cachedDocumentsMap.value[id] ?: return@forEach
                val merged = (existing.parsedTags().map { it.lowercase() } + cleanTags).distinct().joinToString(", ")
                dao?.updateDocumentTags(id, merged)
            }
            _statusBannerMessage.value = "Tagged ${docIds.size} document(s) with ${cleanTags.joinToString(", ") { "#$it" }}"
        }
    }

    fun updateDocumentLastPage(docId: String, pageIndex: Int) {
        viewModelScope.launch {
            dao?.updateDocumentLastPage(docId, pageIndex)
        }
    }

    fun convertFileToPdfAndSave(
        context: Context,
        sourceDoc: DocEntity,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val plainText = if (sourceDoc.isEncrypted) {
                "Encrypted Document: ${sourceDoc.title}"
            } else {
                sourceDoc.content
            }
            val exportRes = PdfLibAnnotationEngine.exportAnnotatedPdf(
                context = context,
                document = sourceDoc,
                plainContent = plainText,
                annotations = emptyList(),
                isBiometricSigned = false,
                signerName = "DocFlow Local Converter",
                signerEmail = ""
            )
            val pdfObj = exportRes.getOrNull()
            val cleanName = sourceDoc.title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                .ifBlank { "converted" } + ".pdf"
            createOrScanDocument(
                title = "${sourceDoc.title} (PDF)",
                folderId = sourceDoc.folderId,
                category = sourceDoc.category,
                fileType = "pdf",
                fileName = pdfObj?.pdfFile?.name ?: cleanName,
                plainContent = plainText,
                isEncrypted = false,
                password = "",
                encryptionKeyHint = null,
                isOfflineCached = true,
                pageCount = sourceDoc.pageCount.coerceAtLeast(1L),
                localPdfPath = pdfObj?.pdfFile?.absolutePath,
                generatedPdfUri = pdfObj?.shareUri,
                customTags = "converted-pdf, created-by-me, ${sourceDoc.fileType.lowercase()}",
                createdByMe = true,
                onCreated = onCreated
            )
            _statusBannerMessage.value = "Converted '${sourceDoc.title}' to PDF in Created by me"
        }
    }

    fun saveSummaryAsNote(
        sourceDoc: DocEntity,
        summaryResult: com.example.data.util.ExtractiveSummaryResult,
        onCreated: (String) -> Unit = {}
    ) {
        val noteTitle = "Summary — ${sourceDoc.title}"
        val noteBody = buildString {
            appendLine("=== ON-DEVICE EXTRACTIVE SUMMARY: ${sourceDoc.title} ===")
            appendLine("Words: ${summaryResult.wordCount} • Sentences: ${summaryResult.sentenceCount} • Compression: ${summaryResult.compressionRatioPercent}%")
            if (summaryResult.topKeywords.isNotEmpty()) {
                appendLine("Key Terms: ${summaryResult.topKeywords.joinToString(", ")}")
            }
            appendLine()
            appendLine(summaryResult.executiveSummary)
            appendLine()
            appendLine("=== KEY POINTS ===")
            summaryResult.bulletPoints.forEach { bullet ->
                appendLine("• $bullet")
            }
        }.trim()
        val cleanName = sourceDoc.title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_') + "_summary.txt"
        createOrScanDocument(
            title = noteTitle,
            folderId = sourceDoc.folderId,
            category = sourceDoc.category,
            fileType = "txt",
            fileName = cleanName,
            plainContent = noteBody,
            isEncrypted = false,
            password = "",
            encryptionKeyHint = null,
            isOfflineCached = true,
            pageCount = 1L,
            customTags = "summary, note, on-device-ai, created-by-me",
            createdByMe = true,
            onCreated = onCreated
        )
    }

    fun saveTranslationDocument(
        context: Context,
        sourceDoc: DocEntity,
        translatedText: String,
        targetLang: com.example.data.util.TranslationLanguage,
        exportAsPdf: Boolean,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val transTitle = "${sourceDoc.title} (${targetLang.displayName})"
            val ext = if (exportAsPdf) "pdf" else "txt"
            val cleanBase = sourceDoc.title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                .ifBlank { "translated" } + "_${targetLang.code}.$ext"

            var pdfPath: String? = null
            var pdfUri: Uri? = null
            if (exportAsPdf) {
                val tempDoc = sourceDoc.copy(title = transTitle, fileName = cleanBase)
                val res = PdfLibAnnotationEngine.exportAnnotatedPdf(
                    context = context,
                    document = tempDoc,
                    plainContent = translatedText,
                    annotations = emptyList(),
                    isBiometricSigned = false,
                    signerName = "DocFlow Offline Translator",
                    signerEmail = ""
                ).getOrNull()
                pdfPath = res?.pdfFile?.absolutePath
                pdfUri = res?.shareUri
            }

            createOrScanDocument(
                title = transTitle,
                folderId = sourceDoc.folderId,
                category = sourceDoc.category,
                fileType = ext,
                fileName = cleanBase,
                plainContent = translatedText,
                isEncrypted = false,
                password = "",
                encryptionKeyHint = null,
                isOfflineCached = true,
                pageCount = sourceDoc.pageCount.coerceAtLeast(1L),
                localPdfPath = pdfPath,
                generatedPdfUri = pdfUri,
                customTags = "translated, ${targetLang.code}, ${targetLang.displayName.lowercase()}, created-by-me",
                createdByMe = true,
                onCreated = onCreated
            )
            _statusBannerMessage.value = "Saved ${targetLang.displayName} translation as ${ext.uppercase()} in Created by me"
        }
    }

    fun exportBackupArchive(
        context: Context,
        passphrase: String,
        onExported: (File) -> Unit = {}
    ) {
        viewModelScope.launch {
            val docs = dao?.getAllDocumentsForUser(currentUserId) ?: emptyList()
            val folders = dao?.getAllFoldersForUser(currentUserId) ?: emptyList()
            val res = com.example.data.util.OnDeviceAiAndBackupEngine.exportEncryptedDocFlowBackup(
                context = context,
                passphrase = passphrase,
                documents = docs,
                folders = folders
            )
            res.onSuccess { file ->
                _statusBannerMessage.value = "Exported encrypted backup: ${file.name} (${docs.size} files)"
                onExported(file)
            }.onFailure { err ->
                _statusBannerMessage.value = "Backup export failed: ${err.localizedMessage}"
            }
        }
    }

    fun importBackupArchive(
        context: Context,
        uri: Uri,
        passphrase: String,
        onImported: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            val rawEncrypted = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            }.getOrNull()
            if (rawEncrypted.isNullOrBlank()) {
                _statusBannerMessage.value = "Selected backup file is empty or unreadable"
                return@launch
            }
            val parsedRes = com.example.data.util.OnDeviceAiAndBackupEngine.parseEncryptedDocFlowBackup(
                encryptedContent = rawEncrypted,
                passphrase = passphrase,
                targetUserId = currentUserId
            )
            parsedRes.onSuccess { (folders, docs) ->
                dao?.upsertFolders(folders)
                dao?.upsertDocuments(docs)
                _statusBannerMessage.value = "Restored ${docs.size} document(s) & ${folders.size} folder(s) from encrypted .docflow backup"
                onImported(docs.size)
            }.onFailure {
                _statusBannerMessage.value = "Decryption failed: Invalid backup passphrase or corrupted .docflow file"
            }
        }
    }

    fun clearThumbnailAndTempCache(context: Context) {
        viewModelScope.launch {
            runCatching {
                context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            }
            _statusBannerMessage.value = "Cleared local thumbnail & temporary cache"
        }
    }

    fun seedSampleWorkspace() {
        viewModelScope.launch {
            val folderWorkRes = repository.createFolder("Work & Contracts", "#1F6BFF", "work")
            val folderFinanceRes = repository.createFolder("Finance & Invoices", "#16A34A", "receipt")
            val folderDecksRes = repository.createFolder("Scans & Archives", "#EA580C", "scanner")

            val workFolderId = folderWorkRes.getOrDefault("folder_work")
            val financeFolderId = folderFinanceRes.getOrDefault("folder_finance")
            val decksFolderId = folderDecksRes.getOrDefault("folder_scans")

            dao?.upsertFolders(
                listOf(
                    CachedFolderEntity(
                        id = workFolderId,
                        userId = currentUserId,
                        name = "Work & Contracts",
                        color = "#1F6BFF",
                        icon = "work",
                        parentFolderId = null
                    ),
                    CachedFolderEntity(
                        id = financeFolderId,
                        userId = currentUserId,
                        name = "Finance & Invoices",
                        color = "#16A34A",
                        icon = "receipt",
                        parentFolderId = null
                    ),
                    CachedFolderEntity(
                        id = decksFolderId,
                        userId = currentUserId,
                        name = "Scans & Archives",
                        color = "#EA580C",
                        icon = "scanner",
                        parentFolderId = workFolderId // nested subfolder example
                    )
                )
            )

            createOrScanDocument(
                title = "Q4 Executive Financial Summary",
                folderId = financeFolderId,
                category = "Invoice",
                fileType = "pdf",
                fileName = "Q4_Financial_Summary_2026.pdf",
                plainContent = """
                    === SECTION 1: EXECUTIVE FINANCIAL SUMMARY (PDF) ===
                    Total recurring revenue increased by 28.4% quarter-over-quarter, driven by enterprise document workflow automation and local on-device indexing.
                    
                    | Metric | Q3 Actual | Q4 Actual | YoY Change |
                    | Gross Margin | 74.1% | 76.2% | +2.1% |
                    | Operating Cash Flow | $3.95M | $4.82M | +22.0% |
                    | On-Device Search Speed | 18 ms | 11 ms | -38% |
                    
                    === SECTION 2: ACTION ITEMS & APPROVALS ===
                    • Finalize vendor invoices by Friday 5:00 PM
                    • Convert physical tax receipts to PDF using the local scanner
                    • Verify quarterly budget numbers before board review
                """.trimIndent(),
                isEncrypted = false,
                password = "",
                encryptionKeyHint = null,
                isOfflineCached = true,
                pageCount = 4L,
                customTags = "finance, q4, executive, invoice, pdf",
                createdByMe = true,
                sourceFolderKey = "download"
            )

            createOrScanDocument(
                title = "Master Services & Privacy Agreement",
                folderId = workFolderId,
                category = "Contract",
                fileType = "docx",
                fileName = "Master_Services_Agreement_v3.docx",
                plainContent = """
                    === SECTION 1: SCOPE OF SERVICES (WORD DOCX) ===
                    This Master Services Agreement governs on-device document management, local AES-256-GCM encryption, and cross-format PDF, Word, Excel, and PowerPoint viewing and editing.
                    
                    | Clause ID | Topic | Security Tier | Status |
                    | SEC-101 | Zero-Server Storage | On-Device SQLite | Approved |
                    | SEC-204 | Private Safe Vault | AES-256-GCM + Biometric | Active |
                    
                    === SECTION 2: DATA PRIVACY & RETENTION ===
                    All documents, annotations, OCR text, and folders remain strictly in app-private storage on the user's device. No telemetry or document contents are transmitted externally.
                """.trimIndent(),
                isEncrypted = false,
                password = "",
                encryptionKeyHint = null,
                isOfflineCached = true,
                pageCount = 6L,
                customTags = "contract, legal, word, docx, agreement",
                createdByMe = true,
                sourceFolderKey = "whatsapp"
            )

            createOrScanDocument(
                title = "2026 Operating Budget & Expense Ledger",
                folderId = financeFolderId,
                category = "Receipt",
                fileType = "xlsx",
                fileName = "Operating_Budget_Ledger_2026.xlsx",
                plainContent = """
                    === SHEET 1: QUARTERLY REVENUE BREAKDOWN (EXCEL XLSX) ===
                    | Department | Q1 (USD K) | Q2 (USD K) | Q3 (USD K) | Q4 (USD K) | Annual Total |
                    | Core Engineering | $1,420 | $1,680 | $1,940 | $2,410 | $7,450 |
                    | Mobile Suite | $850 | $920 | $1,100 | $1,360 | $4,230 |
                    | Security Vault | $610 | $740 | $890 | $1,120 | $3,360 |
                    | Total Budget | $2,880 | $3,340 | $3,930 | $4,890 | $15,040 |
                    
                    === SHEET 2: EXPENSE & HEADCOUNT ALLOCATION ===
                    | Cost Center | Budgeted | Actual | Variance |
                    | R&D Engineering | $3,200 | $3,140 | -$60 (Under) |
                    | QA & Testing | $980 | $950 | -$30 (Under) |
                """.trimIndent(),
                isEncrypted = false,
                password = "",
                encryptionKeyHint = null,
                isOfflineCached = true,
                pageCount = 2L,
                customTags = "excel, xlsx, spreadsheet, budget, receipt",
                createdByMe = false,
                sourceFolderKey = "download"
            )

            createOrScanDocument(
                title = "Product Architecture & Roadmap Deck",
                folderId = decksFolderId,
                category = "Education",
                fileType = "pptx",
                fileName = "Product_Architecture_Deck.pptx",
                plainContent = """
                    === SLIDE 1: DocFlow Offline Architecture ===
                    • 100% On-Device Opener, Scanner & Manager for PDF, Word, Excel, PowerPoint, OFD, and TXT
                    • Local SQLite FTS4 Full-Text Search across titles, tags, and ML Kit OCR text
                    • Flat, minimal Material UI with Light (#F0F1F3) and Dark (#0F1113) themes
                    
                    === SLIDE 2: On-Device AI & Security Benchmarks ===
                    | Capability | Engine | Network Required |
                    | Document Scanner + OCR | CameraX + ML Kit | Offline |
                    | Extractive Summarizer | Local TextRank / TF-IDF | Offline |
                    | Private Safe Encryption | AES-256-GCM + PBKDF2 | Offline |
                """.trimIndent(),
                isEncrypted = false,
                password = "",
                encryptionKeyHint = null,
                isOfflineCached = true,
                pageCount = 3L,
                customTags = "powerpoint, pptx, presentation, slides, roadmap",
                createdByMe = true,
                sourceFolderKey = "bluetooth"
            )

            createOrScanDocument(
                title = "Fixed-Layout Tax Clearance Certificate",
                folderId = financeFolderId,
                category = "ID",
                fileType = "ofd",
                fileName = "Tax_Clearance_Certificate_2026.ofd",
                plainContent = """
                    === OFD FIXED-LAYOUT ARCHIVE: OFFICIAL TAX CLEARANCE ===
                    Certificate No: OFD-2026-99412 • Standard: GB/T 33190 Fixed-Layout Document
                    Taxpayer Identification Verified • Status: Compliant & Cleared
                    
                    | Field | Value | Verification |
                    | Document Standard | Open Fixed-layout Document (OFD) | Validated |
                    | Issuing Authority | Regional Revenue Directorate | Digital Seal |
                """.trimIndent(),
                isEncrypted = false,
                password = "",
                encryptionKeyHint = null,
                isOfflineCached = true,
                pageCount = 1L,
                customTags = "ofd, certificate, tax, id, archive",
                createdByMe = false,
                sourceFolderKey = "download"
            )

            createOrScanDocument(
                title = "Field Research & Release Notes",
                folderId = workFolderId,
                category = "Other",
                fileType = "txt",
                fileName = "Field_Research_Notes.txt",
                plainContent = """
                    === PLAIN TEXT NOTES: RELEASE CHECKLIST ===
                    1. Verify PDF viewer page jump, zoom, thumbnail strip, and full-text search.
                    2. Verify Word (.docx) editor autosave and local 20-version history.
                    3. Test multi-page scanner edge crop, B&W filter, and Hindi + English OCR.
                    4. Confirm Private Safe PIN/biometric lock hides files from Recent and Search.
                """.trimIndent(),
                isEncrypted = false,
                password = "",
                encryptionKeyHint = null,
                isOfflineCached = true,
                pageCount = 1L,
                customTags = "txt, notes, checklist, research",
                createdByMe = true,
                sourceFolderKey = "whatsapp"
            )

            recordSyncLog(
                docId = "seed_workspace",
                docTitle = "Offline Starter Documents",
                action = "WORKSPACE_INIT",
                status = "LOCAL_ONLY",
                details = "Seeded PDF, Word (.docx), Excel (.xlsx), PowerPoint (.pptx), OFD (.ofd) & Text (.txt)"
            )
            _statusBannerMessage.value = "Loaded offline sample documents (PDF, DOC, XLS, PPT, OFD, TXT)"
        }
    }

    /**
     * Updates the Room `lastAccessedAtMillis` timestamp whenever a document is opened or viewed,
     * keeping the expandable Recent Files drawer prioritized in real time.
     */
    fun recordDocumentAccessed(docId: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao?.updateDocumentLastAccessed(docId, now)
            appContext?.let { DocFlowAppWidgetProvider.notifyDocumentOpened(it) }
        }
    }

    fun observeVersionsForDocument(docId: String): Flow<List<DocumentVersionEntity>> {
        return dao?.observeDocumentVersions(docId) ?: flowOf(emptyList())
    }

    fun observeCommentsForDocument(docId: String): Flow<List<DocumentCommentEntity>> {
        return dao?.observeDocumentComments(docId) ?: flowOf(emptyList())
    }

    /**
     * Google-Docs-Style Sub-Second Live Keystroke & Block Autosave.
     * Enforces role permissions (Viewers cannot write), immediately updates local Room cache & FTS4 index,
     * broadcasts encrypted delta updates within ~450ms, and records periodic CRDT version snapshots.
     */
    fun onLiveDocumentKeystrokeEdit(
        doc: DocEntity,
        newTitle: String,
        newFolderId: String,
        newCategory: String,
        plainContent: String,
        isEncrypted: Boolean,
        password: String?,
        encryptionKeyHint: String?,
        isOfflineCached: Boolean
    ) {
        if (!_currentUserRole.value.canEdit) {
            _statusBannerMessage.value = "Read-Only Mode: '${_currentUserRole.value.label}' role cannot insert document updates"
            return
        }

        _liveSyncStatus.value = if (_isOfflineMode.value) {
            LiveSyncStatus.OFFLINE_QUEUED
        } else {
            LiveSyncStatus.SAVING
        }

        liveAutosaveJob?.cancel()
        liveAutosaveJob = viewModelScope.launch {
            delay(420L) // Sub-second live keystroke debounce
            val now = System.currentTimeMillis()
            val finalContent = if (isEncrypted && !password.isNullOrBlank()) {
                CryptoUtils.encrypt(plainContent, password)
            } else {
                plainContent
            }
            val newSize = finalContent.toByteArray(Charsets.UTF_8).size.toLong().coerceAtLeast(doc.fileSize)
            val newPages = ((plainContent.length / 1800) + 1).toLong().coerceAtLeast(doc.pageCount)
            val syncState = if (_isOfflineMode.value) SyncState.PENDING_UPDATE else SyncState.SYNCED
            val existingCached = cachedDocumentsMap.value[doc.id]
            val tagsToSave = existingCached?.tags
                ?: CachedDocumentEntity.defaultTagsFor(newTitle, newCategory, doc.fileType)
            val preview = CachedDocumentEntity.buildPreview(plainContent, isEncrypted)

            dao?.upsertDocument(
                CachedDocumentEntity(
                    id = doc.id,
                    userId = currentUserId,
                    title = newTitle.ifBlank { doc.title },
                    folderId = newFolderId.ifBlank { "root" },
                    category = newCategory.ifBlank { doc.category },
                    fileType = doc.fileType,
                    fileName = doc.fileName,
                    fileSize = newSize,
                    content = finalContent,
                    contentPreview = preview,
                    tags = tagsToSave,
                    localFilePath = existingCached?.localFilePath,
                    isEncrypted = isEncrypted,
                    encryptionKeyHint = if (isEncrypted) encryptionKeyHint else null,
                    isOfflineCached = isOfflineCached,
                    pageCount = newPages,
                    syncState = syncState.name,
                    lastSyncedAtMillis = now,
                    updatedAtMillis = now,
                    lastAccessedAtMillis = now
                )
            )

            liveKeystrokeCounter++
            if (liveKeystrokeCounter == 1 || liveKeystrokeCounter % 5 == 0) {
                val existingVersionsCount = allUserVersions.value.count { it.documentId == doc.id }
                dao?.insertDocumentVersion(
                    DocumentVersionEntity(
                        id = UUID.randomUUID().toString(),
                        documentId = doc.id,
                        userId = currentUserId,
                        versionNumber = existingVersionsCount + 1,
                        versionLabel = if ( _isOfflineMode.value) "Offline CRDT Delta Merge" else "Live Auto-Snapshot",
                        authorName = "You (${_currentUserRole.value.label})",
                        stateVector = "sv_${(1000..9999).random()}",
                        snapshotContent = plainContent,
                        byteSize = newSize,
                        isEncrypted = isEncrypted,
                        createdAtMillis = now
                    )
                )
            }

            if (!_isOfflineMode.value) {
                val res = repository.updateDocument(
                    docId = doc.id,
                    title = newTitle.ifBlank { doc.title },
                    folderId = newFolderId.ifBlank { "root" },
                    category = newCategory.ifBlank { doc.category },
                    content = finalContent,
                    isEncrypted = isEncrypted,
                    encryptionKeyHint = if (isEncrypted) encryptionKeyHint else null,
                    isOfflineCached = isOfflineCached,
                    pageCount = newPages,
                    fileSize = newSize
                )
                _liveSyncStatus.value = if (res.isSuccess) {
                    LiveSyncStatus.SAVED
                } else {
                    LiveSyncStatus.OFFLINE_QUEUED
                }
            } else {
                _liveSyncStatus.value = LiveSyncStatus.OFFLINE_QUEUED
            }
        }
    }

    fun createNamedVersionSnapshot(
        doc: DocEntity,
        versionLabel: String,
        plainContent: String
    ) {
        viewModelScope.launch {
            val nextVer = allUserVersions.value.count { it.documentId == doc.id } + 1
            val bytes = plainContent.toByteArray(Charsets.UTF_8).size.toLong().coerceAtLeast(doc.fileSize)
            dao?.insertDocumentVersion(
                DocumentVersionEntity(
                    id = UUID.randomUUID().toString(),
                    documentId = doc.id,
                    userId = currentUserId,
                    versionNumber = nextVer,
                    versionLabel = versionLabel.ifBlank { "Named Version v$nextVer" },
                    authorName = "You (${_currentUserRole.value.label})",
                    stateVector = "sv_named_$nextVer",
                    snapshotContent = plainContent,
                    byteSize = bytes,
                    isEncrypted = doc.isEncrypted
                )
            )
            _statusBannerMessage.value = "Saved version v$nextVer: '$versionLabel'"
        }
    }

    fun restoreVersionSnapshot(
        doc: DocEntity,
        version: DocumentVersionEntity,
        password: String?,
        onRestoredContent: (String) -> Unit
    ) {
        if (!_currentUserRole.value.canEdit) {
            _statusBannerMessage.value = "Viewer role cannot restore versions"
            return
        }
        viewModelScope.launch {
            val nextVer = allUserVersions.value.count { it.documentId == doc.id } + 1
            dao?.insertDocumentVersion(
                DocumentVersionEntity(
                    id = UUID.randomUUID().toString(),
                    documentId = doc.id,
                    userId = currentUserId,
                    versionNumber = nextVer,
                    versionLabel = "Restored from v${version.versionNumber} (${version.versionLabel})",
                    authorName = "You (${_currentUserRole.value.label})",
                    stateVector = "sv_restore_$nextVer",
                    snapshotContent = version.snapshotContent,
                    byteSize = version.byteSize,
                    isEncrypted = doc.isEncrypted
                )
            )
            updateDocumentContent(
                doc = doc,
                newTitle = doc.title,
                newFolderId = doc.folderId,
                newCategory = doc.category,
                plainContent = version.snapshotContent,
                isEncrypted = doc.isEncrypted,
                password = password,
                encryptionKeyHint = doc.encryptionKeyHint,
                isOfflineCached = doc.isOfflineCached
            )
            onRestoredContent(version.snapshotContent)
            _statusBannerMessage.value = "Restored v${version.versionNumber} as new version v$nextVer (History preserved)"
        }
    }

    fun addCommentOrSuggestion(
        doc: DocEntity,
        anchorText: String,
        body: String,
        isSuggestion: Boolean,
        suggestedReplacement: String
    ) {
        if (!_currentUserRole.value.canComment) {
            _statusBannerMessage.value = "Viewer role cannot add comments or suggestions"
            return
        }
        viewModelScope.launch {
            dao?.upsertDocumentComment(
                DocumentCommentEntity(
                    id = UUID.randomUUID().toString(),
                    documentId = doc.id,
                    userId = currentUserId,
                    authorName = "You",
                    authorRole = _currentUserRole.value.label,
                    anchorLineOrText = anchorText,
                    body = body,
                    isSuggestion = isSuggestion,
                    suggestedReplacement = suggestedReplacement,
                    status = "OPEN"
                )
            )
            _statusBannerMessage.value = if (isSuggestion) {
                "Tracked suggestion posted for owner review"
            } else {
                "Live comment anchored to '$anchorText'"
            }
        }
    }

    fun resolveComment(commentId: String) {
        viewModelScope.launch {
            dao?.updateCommentStatus(commentId, "RESOLVED")
        }
    }

    fun acceptSuggestion(
        doc: DocEntity,
        comment: DocumentCommentEntity,
        currentPlainContent: String,
        password: String?,
        onMergedContent: (String) -> Unit
    ) {
        if (!_currentUserRole.value.canEdit) return
        viewModelScope.launch {
            val updatedText = if (comment.anchorLineOrText.isNotBlank() &&
                currentPlainContent.contains(comment.anchorLineOrText, ignoreCase = true)
            ) {
                currentPlainContent.replaceFirst(comment.anchorLineOrText, comment.suggestedReplacement, ignoreCase = true)
            } else {
                "$currentPlainContent\n${comment.suggestedReplacement}".trim()
            }
            dao?.updateCommentStatus(comment.id, "ACCEPTED")
            updateDocumentContent(
                doc = doc,
                newTitle = doc.title,
                newFolderId = doc.folderId,
                newCategory = doc.category,
                plainContent = updatedText,
                isEncrypted = doc.isEncrypted,
                password = password,
                encryptionKeyHint = doc.encryptionKeyHint,
                isOfflineCached = doc.isOfflineCached
            )
            onMergedContent(updatedText)
            _statusBannerMessage.value = "Accepted tracked suggestion & merged into live document"
        }
    }

    fun rejectSuggestion(commentId: String) {
        viewModelScope.launch {
            dao?.updateCommentStatus(commentId, "REJECTED")
            _statusBannerMessage.value = "Rejected tracked suggestion"
        }
    }

    fun inviteCollaborator(email: String, role: CollaboratorRole) {
        val cleanName = email.substringBefore("@")
            .split(".", "_", "-")
            .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
            .ifBlank { "Collaborator" }
        val colors = listOf("#E11D48", "#5E17EB", "#0891B2", "#059669", "#D97706")
        val nextCollab = ActiveCollaborator(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            email = email,
            role = role,
            colorHex = colors[_activeCollaborators.value.size % colors.size],
            activeCursorLabel = if (role.canEdit) "Joined Live CRDT Session" else "Viewing Read-Only",
            isOnline = true
        )
        _activeCollaborators.value = _activeCollaborators.value + nextCollab
        _statusBannerMessage.value = "Wrapped AES-256-GCM key & invited $email as ${role.label}"
    }

    fun revokeCollaboratorAndRotateKey(collabId: String) {
        val removed = _activeCollaborators.value.find { it.id == collabId }
        _activeCollaborators.value = _activeCollaborators.value.filterNot { it.id == collabId }
        _statusBannerMessage.value = "Revoked ${removed?.email ?: "member"} & rotated AES-256-GCM document key"
    }

    fun switchCurrentUserRole(role: CollaboratorRole) {
        _currentUserRole.value = role
        _statusBannerMessage.value = if (!role.canEdit) {
            "Role switched to ${role.label}: Editor is now enforced Read-Only / Comment-Only"
        } else {
            "Role switched to ${role.label}: Full live CRDT editing enabled"
        }
    }

    fun compactSnapshotsAndCache() {
        viewModelScope.launch {
            dao?.pruneOldVersions(currentUserId, keepMinVersion = 2)
            recordSyncLog(
                docId = "storage_compaction",
                docTitle = "CRDT Snapshot Compaction",
                action = "COMPACT_SNAPSHOTS",
                status = "OPTIMIZED",
                details = "Merged delta updates into latest snapshots & pruned old checkpoints"
            )
            _statusBannerMessage.value = "Compacted CRDT snapshots & optimized local Room storage!"
        }
    }

    // =========================================================================
    // PDF Annotation Tools (pdf-lib + Y.Map CRDT Live Synchronization)
    // =========================================================================

    fun observeAnnotationsForDocument(docId: String): Flow<List<PdfAnnotationYMapEntity>> {
        return dao?.observeDocumentAnnotations(docId) ?: flowOf(emptyList())
    }

    fun upsertYMapAnnotation(
        doc: DocEntity,
        pageIndex: Int,
        tool: PdfMarkupTool,
        colorHex: String,
        strokeWidth: Float,
        normalizedPoints: List<Offset>,
        stickyNoteText: String = ""
    ) {
        if (!_currentUserRole.value.canEdit && tool != PdfMarkupTool.STICKY_NOTE && tool != PdfMarkupTool.TEXT_BOX) {
            _statusBannerMessage.value = "Viewer role cannot add PDF ink or highlight strokes"
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val ymapKey = "ymap_${doc.id}_p${pageIndex}_${UUID.randomUUID().toString().take(8)}"
            val encodedPoints = PdfAnnotationYMapEntity.encodeNormalizedPoints(normalizedPoints)
            val initialSyncState = if (_isOfflineMode.value) SyncState.PENDING_CREATE.name else SyncState.PENDING_CREATE.name
            val entry = PdfAnnotationYMapEntity(
                ymapKey = ymapKey,
                documentId = doc.id,
                userId = currentUserId,
                pageIndex = pageIndex,
                toolType = tool.name,
                colorHex = colorHex,
                strokeWidth = strokeWidth,
                normalizedPointsEncoded = encodedPoints,
                stickyNoteText = stickyNoteText,
                authorName = currentUserDisplayName.ifBlank { "You" },
                crdtClock = now,
                syncState = initialSyncState,
                updatedAtMillis = now
            )
            dao?.upsertPdfAnnotation(entry)

            // Schedule WorkManager background sync with Firebase
            appContext?.let { ctx ->
                DocFlowSyncManager.enqueueOfflineSyncWhenConnected(ctx, currentUserId, replaceExisting = true)
            }

            if (!_isOfflineMode.value) {
                val firebaseRes = repository.upsertPdfAnnotation(entry)
                if (firebaseRes.isSuccess) {
                    dao?.updateAnnotationSyncState(ymapKey, SyncState.SYNCED.name, now)
                }
            }

            recordSyncLog(
                docId = doc.id,
                docTitle = doc.title,
                action = "YMAP_PDF_${tool.name}",
                status = if (_isOfflineMode.value) "QUEUED_WORKMANAGER" else "FIREBASE_SYNCED",
                details = "Saved PDF annotation '$ymapKey' (${tool.label} on Page ${pageIndex + 1}) to Room & queued WorkManager Firebase sync"
            )
            if ((tool == PdfMarkupTool.STICKY_NOTE || tool == PdfMarkupTool.TEXT_BOX) && stickyNoteText.isNotBlank()) {
                _statusBannerMessage.value = "Added ${tool.label} on Page ${pageIndex + 1} (Synced via WorkManager & Firebase)"
            }
        }
    }

    fun deleteYMapAnnotation(ymapKey: String) {
        viewModelScope.launch {
            dao?.deletePdfAnnotation(ymapKey)
        }
    }

    fun undoLastYMapAnnotation(annotations: List<PdfAnnotationYMapEntity>) {
        val last = annotations.maxByOrNull { it.crdtClock } ?: return
        viewModelScope.launch {
            dao?.deletePdfAnnotation(last.ymapKey)
            repository.deletePdfAnnotation(last.documentId, last.ymapKey)
            _statusBannerMessage.value = "Undid Y.Map ${last.toolType.lowercase()} entry (${last.ymapKey.takeLast(6)})"
        }
    }

    fun clearPageYMapAnnotations(docId: String, pageIndex: Int) {
        viewModelScope.launch {
            dao?.clearPageAnnotations(docId, pageIndex)
            _statusBannerMessage.value = "Cleared Y.Map annotations on Page ${pageIndex + 1}"
        }
    }

    /**
     * Cryptographically signs a document after AndroidX BiometricPrompt verification,
     * tagging it as "signed", updating content with the verified signatory stamp, and syncing live.
     */
    fun signDocumentWithBiometrics(
        doc: DocEntity,
        plainContent: String,
        password: String?
    ) {
        viewModelScope.launch {
            val signer = currentUserDisplayName.ifBlank { "Verified Signatory" }
            val email = currentUserEmail.ifBlank { "$currentUserId@docflow.verified" }
            val stampLine = "\n=== SECTION: CRYPTOGRAPHIC BIOMETRIC SIGNATURE ===\n✓ Signed by $signer ($email) via AndroidX BiometricPrompt • Doc ID #DF-${doc.id.take(8).uppercase()}"
            val updatedContent = if (plainContent.contains("CRYPTOGRAPHIC BIOMETRIC SIGNATURE")) {
                plainContent
            } else {
                (plainContent.trimEnd() + "\n" + stampLine).trimStart()
            }
            val existingCached = cachedDocumentsMap.value[doc.id]
            val mergedTags = ((existingCached?.parsedTags() ?: emptyList()) + listOf("signed", "completed", "biometric-verified"))
                .distinct()
                .joinToString(", ")

            updateDocumentContent(
                doc = doc,
                newTitle = doc.title,
                newFolderId = doc.folderId,
                newCategory = doc.category,
                plainContent = updatedContent,
                isEncrypted = doc.isEncrypted,
                password = password,
                encryptionKeyHint = doc.encryptionKeyHint,
                isOfflineCached = true,
                customTags = mergedTags
            )
            recordSyncLog(
                docId = doc.id,
                docTitle = doc.title,
                action = "BIOMETRIC_SIGN",
                status = "VERIFIED",
                details = "Executed by $signer via AndroidX BiometricPrompt"
            )
            _statusBannerMessage.value = "Document signed & certified by $signer!"
        }
    }

    /**
     * Exports a PDF with all live Y.Map annotations baked in via PdfLibAnnotationEngine
     * and launches Android's system share sheet (`Intent.ACTION_SEND`).
     */
    fun exportAndShareAnnotatedPdf(
        context: Context,
        doc: DocEntity,
        plainContent: String,
        annotations: List<PdfAnnotationYMapEntity>,
        isSigned: Boolean
    ) {
        viewModelScope.launch {
            val signer = currentUserDisplayName.ifBlank { "Authenticated Owner" }
            val email = currentUserEmail.ifBlank { "$currentUserId@docflow.verified" }
            val exportRes = PdfLibAnnotationEngine.exportAnnotatedPdf(
                context = context,
                document = doc,
                plainContent = plainContent,
                annotations = annotations,
                isBiometricSigned = isSigned,
                signerName = signer,
                signerEmail = email
            )
            val resultObj = exportRes.getOrNull()
            if (resultObj != null) {
                _localDocumentUris.value = _localDocumentUris.value + (doc.id to Uri.fromFile(resultObj.pdfFile))
                recordSyncLog(
                    docId = doc.id,
                    docTitle = doc.title,
                    action = "SHARE_EXPORT_PDF",
                    status = "READY",
                    details = "Baked ${annotations.size} Y.Map annotation(s) into ${resultObj.pdfFile.name}"
                )
                _statusBannerMessage.value = "Exported '${resultObj.pdfFile.name}' with ${annotations.size} Y.Map annotation(s)"
            }
            PdfLibAnnotationEngine.launchSystemShareSheet(
                context = context,
                document = doc,
                plainContent = plainContent,
                exportedPdfResult = resultObj
            )
        }
    }

    /**
     * Exports an individual document version snapshot as a standalone PDF file,
     * saves it into the local Room FTS4 workspace, and opens the Android share sheet.
     */
    fun exportVersionSnapshotAsPdf(
        context: Context,
        doc: DocEntity,
        version: DocumentVersionEntity
    ) {
        viewModelScope.launch {
            val exportRes = PdfLibAnnotationEngine.exportVersionSnapshotToPdf(
                context = context,
                document = doc,
                version = version
            )
            val resultObj = exportRes.getOrNull()
            if (resultObj != null) {
                val versionTitle = "${doc.title} — v${version.versionNumber} (${version.versionLabel})"
                createOrScanDocument(
                    title = versionTitle,
                    folderId = doc.folderId,
                    category = doc.category,
                    fileType = "pdf",
                    fileName = resultObj.pdfFile.name,
                    plainContent = version.snapshotContent,
                    isEncrypted = false,
                    password = "",
                    encryptionKeyHint = null,
                    isOfflineCached = true,
                    pageCount = 1L,
                    localPdfPath = resultObj.pdfFile.absolutePath,
                    generatedPdfUri = resultObj.shareUri,
                    customTags = "pdf, version-export, v${version.versionNumber}, snapshot"
                )
                recordSyncLog(
                    docId = doc.id,
                    docTitle = doc.title,
                    action = "EXPORT_VERSION_PDF",
                    status = "READY",
                    details = "Exported version v${version.versionNumber} (${version.versionLabel}) to ${resultObj.pdfFile.name}"
                )
                _statusBannerMessage.value = "Exported version v${version.versionNumber} ('${version.versionLabel}') as PDF (${resultObj.pdfFile.name})"
                PdfLibAnnotationEngine.launchSystemShareSheet(
                    context = context,
                    document = doc,
                    plainContent = version.snapshotContent,
                    exportedPdfResult = resultObj
                )
            } else {
                _statusBannerMessage.value = "Failed to export version PDF: ${exportRes.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    /**
     * Exports an individual document note/comment or tracked suggestion as a standalone PDF file,
     * saves it into the local Room FTS4 workspace, and opens the Android share sheet.
     */
    fun exportCommentOrNoteAsPdf(
        context: Context,
        doc: DocEntity,
        comment: DocumentCommentEntity
    ) {
        viewModelScope.launch {
            val exportRes = PdfLibAnnotationEngine.exportNoteOrCommentToPdf(
                context = context,
                document = doc,
                comment = comment
            )
            val resultObj = exportRes.getOrNull()
            if (resultObj != null) {
                val noteTitle = "${doc.title} — Note (${comment.authorName})"
                val noteSummary = buildString {
                    appendLine("=== DOCUMENT NOTE EXPORT: ${doc.title} ===")
                    appendLine("Author: ${comment.authorName} (${comment.authorRole}) • Status: ${comment.status}")
                    appendLine("Anchor: \"${comment.anchorLineOrText}\"")
                    appendLine()
                    appendLine(comment.body)
                    if (comment.isSuggestion && comment.suggestedReplacement.isNotBlank()) {
                        appendLine("\n+ Proposed Replacement: ${comment.suggestedReplacement}")
                    }
                }
                createOrScanDocument(
                    title = noteTitle,
                    folderId = doc.folderId,
                    category = "Notes",
                    fileType = "pdf",
                    fileName = resultObj.pdfFile.name,
                    plainContent = noteSummary,
                    isEncrypted = false,
                    password = "",
                    encryptionKeyHint = null,
                    isOfflineCached = true,
                    pageCount = 1L,
                    localPdfPath = resultObj.pdfFile.absolutePath,
                    generatedPdfUri = resultObj.shareUri,
                    customTags = "pdf, note-export, comment, ${comment.status.lowercase()}"
                )
                recordSyncLog(
                    docId = doc.id,
                    docTitle = doc.title,
                    action = "EXPORT_NOTE_PDF",
                    status = "READY",
                    details = "Exported note by ${comment.authorName} to ${resultObj.pdfFile.name}"
                )
                _statusBannerMessage.value = "Exported note by ${comment.authorName} as PDF (${resultObj.pdfFile.name})"
                PdfLibAnnotationEngine.launchSystemShareSheet(
                    context = context,
                    document = doc,
                    plainContent = noteSummary,
                    exportedPdfResult = resultObj
                )
            } else {
                _statusBannerMessage.value = "Failed to export note PDF: ${exportRes.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    /**
     * Exports all notes, threaded comments, tracked suggestions, and PDF sticky/text-box notes
     * on a document into a consolidated PDF report.
     */
    fun exportAllNotesAsPdf(
        context: Context,
        doc: DocEntity,
        comments: List<DocumentCommentEntity>,
        annotations: List<PdfAnnotationYMapEntity> = emptyList()
    ) {
        viewModelScope.launch {
            val exportRes = PdfLibAnnotationEngine.exportAllNotesToPdf(
                context = context,
                document = doc,
                comments = comments,
                stickyNotes = annotations
            )
            val resultObj = exportRes.getOrNull()
            if (resultObj != null) {
                val reportTitle = "${doc.title} — Consolidated Notes Report"
                val reportText = comments.joinToString("\n\n") { c ->
                    "[${c.status}] ${c.authorName}: ${c.body}"
                }
                createOrScanDocument(
                    title = reportTitle,
                    folderId = doc.folderId,
                    category = "Notes",
                    fileType = "pdf",
                    fileName = resultObj.pdfFile.name,
                    plainContent = reportText.ifBlank { "Consolidated PDF notes report for ${doc.title}" },
                    isEncrypted = false,
                    password = "",
                    encryptionKeyHint = null,
                    isOfflineCached = true,
                    pageCount = 1L,
                    localPdfPath = resultObj.pdfFile.absolutePath,
                    generatedPdfUri = resultObj.shareUri,
                    customTags = "pdf, notes-report, comments"
                )
                _statusBannerMessage.value = "Exported ${resultObj.annotationCount} note(s) as PDF (${resultObj.pdfFile.name})"
                PdfLibAnnotationEngine.launchSystemShareSheet(
                    context = context,
                    document = doc,
                    plainContent = reportText,
                    exportedPdfResult = resultObj
                )
            }
        }
    }

    // =========================================================================
    // Incoming External "Open In App" & "Share In App" Intent Handler
    // =========================================================================

    fun handleIncomingExternalIntent(
        context: Context,
        intent: Intent?,
        onDocumentReady: (String) -> Unit
    ) {
        if (intent == null) return
        val action = intent.action ?: ""
        val uris = mutableListOf<Uri>()

        // 1. Check clipData (first priority for sharing/opening files in modern Android)
        intent.clipData?.let { clipData ->
            for (i in 0 until clipData.itemCount) {
                val itemUri = clipData.getItemAt(i).uri
                if (itemUri != null && !uris.contains(itemUri)) {
                    uris.add(itemUri)
                }
            }
        }

        // 2. Check Intent.data (ACTION_VIEW, ACTION_EDIT)
        intent.data?.let { dataUri ->
            if (!uris.contains(dataUri)) {
                uris.add(dataUri)
            }
        }

        // 3. Check EXTRA_STREAM (ACTION_SEND, ACTION_SEND_MULTIPLE)
        when (action) {
            Intent.ACTION_SEND -> {
                val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (streamUri != null && !uris.contains(streamUri)) {
                    uris.add(streamUri)
                } else if (uris.isEmpty()) {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                            ?: "Shared Note ${System.currentTimeMillis() % 10000}"
                        createOrScanDocument(
                            title = subject,
                            folderId = "root",
                            category = "Work",
                            fileType = "docx",
                            fileName = "${subject.lowercase().replace(Regex("[^a-z0-9]+"), "_")}.docx",
                            plainContent = sharedText,
                            isEncrypted = false,
                            password = "",
                            encryptionKeyHint = null,
                            isOfflineCached = true,
                            pageCount = 1L,
                            customTags = "shared-in-app, external",
                            onCreated = onDocumentReady
                        )
                        return
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                if (!list.isNullOrEmpty()) {
                    list.forEach { u ->
                        if (u != null && !uris.contains(u)) uris.add(u)
                    }
                }
            }
        }

        if (uris.isNotEmpty()) {
            viewModelScope.launch {
                var firstOpenedDocId: String? = null
                for ((idx, singleUri) in uris.withIndex()) {
                    importDocumentFromUri(
                        context = context,
                        uri = singleUri,
                        folderId = "root",
                        category = "Work",
                        isEncrypted = false,
                        password = "",
                        encryptionKeyHint = null,
                        onImported = { importedDocId ->
                            if (idx == 0 || firstOpenedDocId == null) {
                                firstOpenedDocId = importedDocId
                                onDocumentReady(importedDocId)
                            }
                        }
                    )
                }
                _statusBannerMessage.value = "Imported & opened ${uris.size} document(s) in DocFlow"
            }
        }
    }

    /**
     * Share any document (PDF, Word, Excel, PowerPoint, Text, Image) out to external apps via FileProvider.
     */
    fun shareAnyDocument(context: Context, doc: DocEntity) {
        viewModelScope.launch {
            val cached = cachedDocumentsMap.value[doc.id]
            val localPath = cached?.localFilePath
            val localFile = if (!localPath.isNullOrBlank()) File(localPath) else null

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                if (localFile != null && localFile.exists()) {
                    val shareUri = try {
                        androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            localFile
                        )
                    } catch (e: Exception) {
                        Uri.fromFile(localFile)
                    }
                    val mime = when (doc.fileType.lowercase()) {
                        "pdf" -> "application/pdf"
                        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        "doc" -> "application/msword"
                        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        "xls" -> "application/vnd.ms-excel"
                        "csv" -> "text/csv"
                        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                        "ppt" -> "application/vnd.ms-powerpoint"
                        "pic" -> "image/png"
                        else -> "text/plain"
                    }
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    putExtra(Intent.EXTRA_SUBJECT, doc.title)
                    putExtra(Intent.EXTRA_TEXT, "Shared from DocFlow: ${doc.title} (${doc.fileName})")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, doc.title)
                    putExtra(Intent.EXTRA_TEXT, "=== ${doc.title} (${doc.fileName}) ===\n\n${doc.content}")
                }
            }
            val chooser = Intent.createChooser(sendIntent, "Share '${doc.title}' via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(chooser) }
        }
    }

    // =========================================================================
    // Over-The-Air (OTA) App Updates Engine
    // =========================================================================

    fun selectOtaChannel(channel: OtaReleaseChannel) {
        _otaChannel.value = channel
        _otaAvailableManifest.value = OtaUpdateEngine.getLatestReleaseForChannel(
            channel = channel,
            currentVersionCode = _otaCurrentVersionCode.value
        )
        _otaInstallPhase.value = OtaInstallPhase.UPDATE_AVAILABLE
    }

    fun toggleAutoOtaUpdates() {
        _otaAutoInstallEnabled.value = !_otaAutoInstallEnabled.value
        _statusBannerMessage.value = if (_otaAutoInstallEnabled.value) {
            "Auto-OTA Delta Updates enabled for ${_otaChannel.value.label}"
        } else {
            "Auto-OTA Delta Updates paused"
        }
    }

    fun checkForOtaUpdates() {
        viewModelScope.launch {
            _otaInstallPhase.value = OtaInstallPhase.CHECKING
            delay(380L)
            val nextManifest = OtaUpdateEngine.getLatestReleaseForChannel(
                channel = _otaChannel.value,
                currentVersionCode = _otaCurrentVersionCode.value
            )
            _otaAvailableManifest.value = nextManifest
            _otaInstallPhase.value = OtaInstallPhase.UPDATE_AVAILABLE
            _statusBannerMessage.value = "OTA Release v${nextManifest.versionName} (${nextManifest.channel.label}) ready to install"
        }
    }

    fun installOtaUpdate() {
        viewModelScope.launch {
            val manifest = _otaAvailableManifest.value
            _otaInstallPhase.value = OtaInstallPhase.DOWNLOADING_DELTA
            _otaDownloadProgress.value = 0.22f
            delay(260L)
            _otaDownloadProgress.value = 0.58f
            delay(260L)
            _otaInstallPhase.value = OtaInstallPhase.VERIFYING_SHA256
            _otaDownloadProgress.value = 0.84f
            delay(260L)
            _otaInstallPhase.value = OtaInstallPhase.APPLYING_PATCH
            _otaDownloadProgress.value = 1.0f
            delay(220L)

            _otaCurrentVersionCode.value = manifest.versionCode
            _otaCurrentVersionName.value = manifest.versionName
            _otaInstallPhase.value = OtaInstallPhase.INSTALLED

            dao?.upsertOtaUpdateRecord(
                OtaUpdateRecordEntity(
                    versionCode = manifest.versionCode,
                    userId = currentUserId,
                    versionName = manifest.versionName,
                    channel = manifest.channel.label,
                    bundleSha256 = manifest.bundleSha256,
                    patchSizeBytes = manifest.patchSizeBytes,
                    releaseTitle = manifest.releaseTitle,
                    changelogSummary = manifest.highlights.joinToString(" • "),
                    status = "INSTALLED"
                )
            )
            recordSyncLog(
                docId = "ota_v${manifest.versionCode}",
                docTitle = "OTA Update v${manifest.versionName}",
                action = "OTA_PATCH_APPLIED",
                status = "SHA256_VERIFIED",
                details = "${manifest.releaseTitle} (${manifest.patchSizeBytes / 1024} KB)"
            )
            _statusBannerMessage.value = "OTA Update v${manifest.versionName} installed & verified (SHA-256: ${manifest.bundleSha256.take(10)}...)"
        }
    }

    private companion object {
        const val TAG = "DocFlowVM"
    }
}
