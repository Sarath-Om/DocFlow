package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CachedDocumentEntity
import com.example.data.local.CachedFolderEntity
import com.example.data.local.DocFlowDao
import com.example.data.local.SyncLogEntity
import com.example.data.local.SyncState
import com.example.data.model.DocEntity
import com.example.data.model.FolderEntity
import com.example.data.repository.DocFlowRepository
import com.example.data.util.CryptoUtils
import com.example.data.util.FileParserUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val dao: DocFlowDao? = null
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

    val cachedDocumentsMap: StateFlow<Map<String, CachedDocumentEntity>> =
        (dao?.observeCachedDocuments(currentUserId) ?: flowOf(emptyList()))
            .map { list -> list.associateBy { it.id } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyMap()
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

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

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

    val foldersState: StateFlow<UiState<List<FolderEntity>>> = repository.observeFolders()
        .onEach { remoteFolders ->
            dao?.let { localDao ->
                val entities = remoteFolders.map { CachedFolderEntity.fromFolderEntity(it, SyncState.SYNCED) }
                localDao.upsertFolders(entities)
            }
        }
        .map<List<FolderEntity>, UiState<List<FolderEntity>>> { folders ->
            UiState.Success(folders.sortedBy { it.name.lowercase() })
        }
        .catch { e ->
            Log.w(TAG, "Error observing folders", e)
            emit(UiState.Error(e.localizedMessage ?: "Failed to load folders"))
        }
        .stateIn(
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
                        existingTags = existingTags
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
        combine(_searchQuery, _selectedCategory, _selectedFileType, _offlineOnlyFilter, _sortOption) { q, cat, ft, off, sort ->
            FilterParams(q, cat, ft, off, sort)
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

            val matchesFolder = when (folderId) {
                "all" -> true
                else -> doc.folderId == folderId
            }

            val matchesCategory = params.category == "All" || doc.category.equals(params.category, ignoreCase = true)

            val matchesFileType = when (params.fileType.lowercase()) {
                "all" -> true
                "pdf" -> doc.fileType == "pdf"
                "docx" -> doc.fileType == "docx" || doc.fileType == "doc"
                "scanned" -> doc.fileType == "scanned"
                else -> true
            }

            val matchesOffline = !params.offlineOnly || doc.isOfflineCached

            matchesQuery && matchesFolder && matchesCategory && matchesFileType && matchesOffline
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
        val sortOpt: SortOption
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
        _themeMode.value = when (_themeMode.value) {
            ThemeMode.SYSTEM -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.SYSTEM
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

    fun updateDocumentTags(docId: String, rawTags: String) {
        viewModelScope.launch {
            val cleaned = rawTags.split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(", ")
            dao?.updateDocumentTags(docId, cleaned)
            _statusBannerMessage.value = "Tags updated in Room FTS index: $cleaned"
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
                        details = "Switched to Room SQLite + FTS4 local search cache"
                    )
                    _statusBannerMessage.value = "Offline Mode active: Reading & searching local Room FTS4 cache"
                } else {
                    repository.db.enableNetwork()
                    val pendingDocs = dao?.getPendingSyncDocuments(currentUserId) ?: emptyList()
                    pendingDocs.forEach { pending ->
                        dao?.updateDocumentSyncState(pending.id, SyncState.SYNCED.name)
                    }
                    recordSyncLog(
                        docId = "system",
                        docTitle = "Cloud Sync Engine",
                        action = "REALTIME_RESYNC",
                        status = "SYNCED",
                        details = "Reconnected & synchronized ${pendingDocs.size} queued local change(s)"
                    )
                    _statusBannerMessage.value = "Cloud Sync restored: All Room queued changes synchronized in real-time"
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error toggling network mode", e)
            }
        }
    }

    fun createFolder(name: String, color: String, icon: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = repository.createFolder(name = name, color = color, icon = icon)
            if (result.isSuccess) {
                val folderId = result.getOrThrow()
                dao?.upsertFolder(
                    CachedFolderEntity(
                        id = folderId,
                        userId = currentUserId,
                        name = name.trim(),
                        color = color,
                        icon = icon,
                        syncState = if (_isOfflineMode.value) SyncState.LOCAL_ONLY.name else SyncState.SYNCED.name
                    )
                )
                recordSyncLog(
                    docId = folderId,
                    docTitle = name,
                    action = "FOLDER_CREATE",
                    status = if (_isOfflineMode.value) "LOCAL_ONLY" else "SYNCED",
                    details = "Personalized folder '$name' saved to Room & Firestore"
                )
                _statusBannerMessage.value = "Folder '$name' created and synced"
            } else {
                _statusBannerMessage.value = "Failed to create folder: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updateFolder(folder: FolderEntity, newName: String, newColor: String, newIcon: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val updated = folder.copy(name = newName, color = newColor, icon = newIcon)
            val result = repository.updateFolder(updated)
            if (result.isSuccess) {
                dao?.upsertFolder(
                    CachedFolderEntity.fromFolderEntity(
                        updated,
                        if (_isOfflineMode.value) SyncState.PENDING_UPDATE else SyncState.SYNCED
                    )
                )
                _statusBannerMessage.value = "Folder '$newName' updated"
            }
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            val result = repository.deleteFolder(folderId)
            if (result.isSuccess) {
                dao?.deleteFolderById(folderId)
                if (_selectedFolderId.value == folderId) {
                    _selectedFolderId.value = "all"
                }
                _statusBannerMessage.value = "Folder deleted"
            }
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
        onCreated: (String) -> Unit = {}
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
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
                category = category.ifBlank { "General" },
                fileType = fileType,
                fileName = fileName,
                fileSize = sizeBytes,
                content = finalContent,
                isEncrypted = isEncrypted && password.isNotBlank(),
                encryptionKeyHint = if (isEncrypted) encryptionKeyHint else null,
                isOfflineCached = isOfflineCached,
                pageCount = pageCount
            )
            if (result.isSuccess) {
                val id = result.getOrThrow()
                if (generatedPdfUri != null) {
                    _localDocumentUris.value = _localDocumentUris.value + (id to generatedPdfUri)
                }
                val syncState = if (_isOfflineMode.value) SyncState.LOCAL_ONLY else SyncState.SYNCED
                val resolvedTags = CachedDocumentEntity.defaultTagsFor(title, category, fileType, customTags)
                val preview = CachedDocumentEntity.buildPreview(plainContent, isEncrypted && password.isNotBlank())
                dao?.upsertDocument(
                    CachedDocumentEntity(
                        id = id,
                        userId = currentUserId,
                        title = title.trim(),
                        folderId = folderId.ifBlank { "root" },
                        category = category.ifBlank { "General" },
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
                        syncState = syncState.name
                    )
                )
                recordSyncLog(
                    docId = id,
                    docTitle = title,
                    action = if (localPdfPath != null) "CAMERAX_PDF_SCAN" else "DOC_CREATE",
                    status = syncState.label,
                    details = "Indexed in Room FTS4 ($pageCount pg, ${sizeBytes / 1024} KB)"
                )
                _statusBannerMessage.value = "Document '$title' indexed in Room FTS & synced"
                onCreated(id)
            } else {
                _statusBannerMessage.value = "Sync error: ${result.exceptionOrNull()?.localizedMessage}"
            }
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
                    _localDocumentUris.value = _localDocumentUris.value + (docId to uri)
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
                    _statusBannerMessage.value = "Opened & indexed '${parsed.fileName}'"
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
                    action = "LIVE_EDIT_SYNC",
                    status = syncState.label,
                    details = "Instant edit indexed in Room FTS4 & synced (${newSize / 1024} KB)"
                )
                _statusBannerMessage.value = "Changes saved to Room FTS & synced in real-time"
            } else {
                dao?.updateDocumentSyncState(doc.id, SyncState.FAILED.name)
                _statusBannerMessage.value = "Failed to update document: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
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
        viewModelScope.launch {
            val title = cachedDocumentsMap.value[docId]?.title ?: "Document"
            val result = repository.deleteDocument(docId)
            if (result.isSuccess) {
                dao?.deleteDocumentById(docId)
                recordSyncLog(
                    docId = docId,
                    docTitle = title,
                    action = "DOC_DELETE",
                    status = "SYNCED",
                    details = "Removed from Room FTS4 cache & Cloud storage"
                )
                _statusBannerMessage.value = "Document deleted from Room cache and cloud"
            }
        }
    }

    fun seedSampleWorkspace() {
        viewModelScope.launch {
            val folderWorkRes = repository.createFolder("Work & Contracts", "#2563EB", "work")
            val folderFinanceRes = repository.createFolder("Invoices & Tax", "#059669", "receipt")
            val folderScansRes = repository.createFolder("Scanned Physical Docs", "#7C3AED", "scanner")

            val workFolderId = folderWorkRes.getOrDefault("root")
            val financeFolderId = folderFinanceRes.getOrDefault("root")
            val scansFolderId = folderScansRes.getOrDefault("root")

            createOrScanDocument(
                title = "Q4 Executive Financial Summary",
                folderId = financeFolderId,
                category = "Invoices",
                fileType = "pdf",
                fileName = "Q4_Financial_Summary_2026.pdf",
                plainContent = """
                    EXECUTIVE FINANCIAL SUMMARY — Q4
                    Document Type: Official PDF Report
                    Classification: Internal Operations
                    
                    1. OVERVIEW & HIGHLIGHTS
                    Total recurring revenue increased by 28.4% quarter-over-quarter, driven by enterprise cloud adoption and cross-platform document workflow automation.
                    
                    2. KEY METRICS
                    • Gross Margin: 76.2% (+2.1% YoY)
                    • Operating Cash Flow: $4.82M
                    • Cloud Storage & Sync Uptime: 99.99%
                    
                    3. ACTION ITEMS & APPROVALS
                    - Finalize vendor invoices by Friday 5:00 PM
                    - Archive physical tax receipts using the built-in CameraX Scanner
                    - Enable AES-256 encryption on all board legal attachments
                """.trimIndent(),
                isEncrypted = false,
                password = "",
                encryptionKeyHint = null,
                isOfflineCached = true,
                pageCount = 4L,
                customTags = "finance, q4, executive, revenue, tax, pdf"
            )

            createOrScanDocument(
                title = "Master Cloud Services Agreement",
                folderId = workFolderId,
                category = "Contracts",
                fileType = "docx",
                fileName = "Master_Services_Agreement_v3.docx",
                plainContent = """
                    MASTER CLOUD SERVICES AGREEMENT (WORD DOCX)
                    Between: Global Enterprise Corp & Client Partner
                    Effective Date: October 21, 2026
                    
                    SECTION 1. SCOPE OF SERVICES
                    Provider shall deliver real-time document synchronization, end-to-end AES-256 encryption, and cross-platform PDF/Word editing capabilities across all authorized endpoints.
                    
                    SECTION 2. DATA SECURITY & ENCRYPTION
                    All scanned physical documents and imported Word/PDF files are protected under zero-trust security rules with strict per-user isolation and offline Room SQLite caching.
                    
                    SECTION 3. REAL-TIME COLLABORATION
                    Edits made to this Word document synchronize across all connected devices within seconds. You can edit any clause directly in the DocFlow Live Editor.
                """.trimIndent(),
                isEncrypted = false,
                password = "",
                encryptionKeyHint = null,
                isOfflineCached = true,
                pageCount = 6L,
                customTags = "contract, legal, enterprise, word, sla, agreement"
            )

            createOrScanDocument(
                title = "Confidential IP Filing (Encrypted Scan)",
                folderId = scansFolderId,
                category = "Legal",
                fileType = "scanned",
                fileName = "Scan_IP_Filing_Encrypted.pdf",
                plainContent = """
                    CONFIDENTIAL PATENT & IP FILING — SCANNED ARCHIVE
                    Scan ID: SCN-2026-8841
                    Encryption: AES-256-CBC Verified
                    
                    This physical document was scanned directly into DocFlow and encrypted with passphrase '1234'.
                    
                    Summary of Claims:
                    1. High-speed real-time delta synchronization for multi-page PDF and DOCX buffers.
                    2. Zero-knowledge local Room encryption vault with instant offline availability.
                """.trimIndent(),
                isEncrypted = true,
                password = "1234",
                encryptionKeyHint = "Default Demo PIN: 1234",
                isOfflineCached = true,
                pageCount = 2L,
                customTags = "patent, confidential, scan, encrypted, vault, legal"
            )

            recordSyncLog(
                docId = "seed_workspace",
                docTitle = "Starter Workspace",
                action = "WORKSPACE_INIT",
                status = "SYNCED",
                details = "Seeded PDF, Word DOCX & Encrypted Scan into Room FTS4 & Cloud"
            )
            _statusBannerMessage.value = "Sample PDF, Word & Encrypted Scanned documents loaded!"
        }
    }

    private companion object {
        const val TAG = "DocFlowVM"
    }
}
