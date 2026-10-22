package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocFlowDao {

    @Query("SELECT * FROM cached_documents WHERE userId = :userId AND syncState != 'PENDING_DELETE' AND isDeleted = 0 ORDER BY updatedAtMillis DESC")
    fun observeCachedDocuments(userId: String): Flow<List<CachedDocumentEntity>>

    @Query("SELECT * FROM cached_documents WHERE userId = :userId AND syncState != 'PENDING_DELETE' AND isDeleted = 0 AND isPrivateSafe = 0 ORDER BY lastAccessedAtMillis DESC LIMIT 12")
    fun observeRecentCachedDocuments(userId: String): Flow<List<CachedDocumentEntity>>

    @Query("SELECT * FROM cached_documents WHERE userId = :userId AND isDeleted = 1 ORDER BY deletedAtMillis DESC")
    fun observeDeletedDocuments(userId: String): Flow<List<CachedDocumentEntity>>

    @Query("SELECT * FROM cached_documents WHERE userId = :userId AND isDeleted = 0 AND isPrivateSafe = 1 ORDER BY updatedAtMillis DESC")
    fun observePrivateSafeDocuments(userId: String): Flow<List<CachedDocumentEntity>>

    @Query("SELECT * FROM cached_documents WHERE userId = :userId AND isDeleted = 0 AND isPrivateSafe = 0 AND createdByMe = 1 ORDER BY updatedAtMillis DESC")
    fun observeCreatedByMeDocuments(userId: String): Flow<List<CachedDocumentEntity>>

    @Query("SELECT * FROM cached_documents WHERE syncState != 'PENDING_DELETE' AND isDeleted = 0 AND isPrivateSafe = 0 ORDER BY lastAccessedAtMillis DESC, updatedAtMillis DESC LIMIT :limit")
    suspend fun getRecentDocuments(limit: Int = 3): List<CachedDocumentEntity>

    @Query("UPDATE cached_documents SET lastAccessedAtMillis = :accessedAtMillis WHERE id = :docId")
    suspend fun updateDocumentLastAccessed(docId: String, accessedAtMillis: Long = System.currentTimeMillis())

    @Query("UPDATE cached_documents SET lastPageViewed = :pageIndex WHERE id = :docId")
    suspend fun updateDocumentLastPage(docId: String, pageIndex: Int)

    @Query("UPDATE cached_documents SET isDeleted = 1, deletedAtMillis = :deletedAtMillis WHERE id = :docId")
    suspend fun softDeleteDocument(docId: String, deletedAtMillis: Long = System.currentTimeMillis())

    @Query("UPDATE cached_documents SET isDeleted = 0, deletedAtMillis = NULL, updatedAtMillis = :restoredAtMillis WHERE id = :docId")
    suspend fun restoreSoftDeletedDocument(docId: String, restoredAtMillis: Long = System.currentTimeMillis())

    @Query("DELETE FROM cached_documents WHERE isDeleted = 1 AND deletedAtMillis IS NOT NULL AND deletedAtMillis < :cutoffMillis")
    suspend fun purgeExpiredDeletedDocuments(cutoffMillis: Long)

    @Query("DELETE FROM cached_documents WHERE userId = :userId AND isDeleted = 1")
    suspend fun emptyRecentlyDeleted(userId: String)

    @Query("UPDATE cached_documents SET isPrivateSafe = :isPrivateSafe, isEncrypted = :isEncrypted, content = :content, encryptionKeyHint = :keyHint, updatedAtMillis = :updatedAtMillis WHERE id = :docId")
    suspend fun updatePrivateSafeState(
        docId: String,
        isPrivateSafe: Boolean,
        isEncrypted: Boolean,
        content: String,
        keyHint: String?,
        updatedAtMillis: Long = System.currentTimeMillis()
    )

    @Query("UPDATE cached_documents SET folderId = :folderId, updatedAtMillis = :updatedAtMillis WHERE id = :docId")
    suspend fun moveDocumentToFolder(docId: String, folderId: String, updatedAtMillis: Long = System.currentTimeMillis())

    /**
     * Full-Text Search (FTS4) across title, contentPreview, tags, category, and fileName.
     * Excludes deleted and Private Safe items when Private Safe is locked.
     */
    @Query(
        """
        SELECT d.* FROM cached_documents d
        JOIN cached_documents_fts fts ON d.rowid = fts.rowid
        WHERE d.userId = :userId
          AND d.syncState != 'PENDING_DELETE'
          AND d.isDeleted = 0
          AND cached_documents_fts MATCH :ftsQuery
        ORDER BY d.updatedAtMillis DESC
        """
    )
    fun searchCachedDocumentsFts(userId: String, ftsQuery: String): Flow<List<CachedDocumentEntity>>

    /**
     * Fallback/substring search on title, full OCR content, contentPreview, tags, category, or fileName.
     */
    @Query(
        """
        SELECT * FROM cached_documents
        WHERE userId = :userId
          AND syncState != 'PENDING_DELETE'
          AND isDeleted = 0
          AND (
              title LIKE '%' || :rawQuery || '%'
              OR (isEncrypted = 0 AND content LIKE '%' || :rawQuery || '%')
              OR contentPreview LIKE '%' || :rawQuery || '%'
              OR tags LIKE '%' || :rawQuery || '%'
              OR category LIKE '%' || :rawQuery || '%'
              OR fileName LIKE '%' || :rawQuery || '%'
          )
        ORDER BY updatedAtMillis DESC
        """
    )
    fun searchCachedDocumentsSubstring(userId: String, rawQuery: String): Flow<List<CachedDocumentEntity>>

    /**
     * Full-text search specifically across stored PDF and scanned PDF documents (including ML Kit OCR text).
     */
    @Query(
        """
        SELECT * FROM cached_documents
        WHERE userId = :userId
          AND syncState != 'PENDING_DELETE'
          AND isDeleted = 0
          AND fileType IN ('pdf', 'scanned')
          AND (
              title LIKE '%' || :rawQuery || '%'
              OR (isEncrypted = 0 AND content LIKE '%' || :rawQuery || '%')
              OR contentPreview LIKE '%' || :rawQuery || '%'
              OR tags LIKE '%' || :rawQuery || '%'
          )
        ORDER BY updatedAtMillis DESC
        """
    )
    fun searchStoredPdfDocuments(userId: String, rawQuery: String): Flow<List<CachedDocumentEntity>>

    @Query("SELECT * FROM cached_documents WHERE userId = :userId AND id = :docId LIMIT 1")
    suspend fun getCachedDocumentById(userId: String, docId: String): CachedDocumentEntity?

    @Query("SELECT * FROM cached_documents WHERE userId = :userId")
    suspend fun getAllDocumentsForUser(userId: String): List<CachedDocumentEntity>

    @Query("SELECT * FROM cached_documents WHERE userId = :userId AND syncState != 'SYNCED'")
    suspend fun getPendingSyncDocuments(userId: String): List<CachedDocumentEntity>

    @Query("SELECT * FROM cached_documents WHERE syncState != 'SYNCED' ORDER BY updatedAtMillis ASC")
    suspend fun getAllPendingSyncDocuments(): List<CachedDocumentEntity>

    @Query(
        """
        SELECT (SELECT COUNT(*) FROM cached_documents WHERE userId = :userId AND syncState != 'SYNCED') +
               (SELECT COUNT(*) FROM pdf_annotations_ymap WHERE userId = :userId AND syncState != 'SYNCED')
        """
    )
    fun observePendingSyncCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocument(document: CachedDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocuments(documents: List<CachedDocumentEntity>)

    @Query("UPDATE cached_documents SET syncState = :syncState, lastSyncedAtMillis = :syncedAtMillis WHERE id = :docId")
    suspend fun updateDocumentSyncState(docId: String, syncState: String, syncedAtMillis: Long = System.currentTimeMillis())

    @Query("UPDATE cached_documents SET tags = :tags, updatedAtMillis = :updatedAtMillis WHERE id = :docId")
    suspend fun updateDocumentTags(docId: String, tags: String, updatedAtMillis: Long = System.currentTimeMillis())

    @Query("UPDATE cached_documents SET category = :category, updatedAtMillis = :updatedAtMillis WHERE id = :docId")
    suspend fun updateDocumentCategory(docId: String, category: String, updatedAtMillis: Long = System.currentTimeMillis())

    @Query("SELECT DISTINCT category FROM cached_documents WHERE userId = :userId AND syncState != 'PENDING_DELETE' AND isDeleted = 0 ORDER BY category ASC")
    fun observeCategories(userId: String): Flow<List<String>>

    @Query("DELETE FROM cached_documents WHERE id = :docId")
    suspend fun deleteDocumentById(docId: String)

    @Query("SELECT * FROM cached_folders WHERE userId = :userId AND syncState != 'PENDING_DELETE' ORDER BY name ASC")
    fun observeCachedFolders(userId: String): Flow<List<CachedFolderEntity>>

    @Query("SELECT * FROM cached_folders WHERE userId = :userId")
    suspend fun getAllFoldersForUser(userId: String): List<CachedFolderEntity>

    @Query("SELECT * FROM cached_folders WHERE syncState != 'SYNCED'")
    suspend fun getAllPendingSyncFolders(): List<CachedFolderEntity>

    @Query("UPDATE cached_folders SET syncState = :syncState, updatedAtMillis = :syncedAtMillis WHERE id = :folderId")
    suspend fun updateFolderSyncState(folderId: String, syncState: String, syncedAtMillis: Long = System.currentTimeMillis())

    @Query("UPDATE cached_folders SET name = :newName, color = :newColor, parentFolderId = :parentFolderId, updatedAtMillis = :updatedAtMillis WHERE id = :folderId")
    suspend fun updateFolderDetails(
        folderId: String,
        newName: String,
        newColor: String,
        parentFolderId: String? = null,
        updatedAtMillis: Long = System.currentTimeMillis()
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: CachedFolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolders(folders: List<CachedFolderEntity>)

    @Query("DELETE FROM cached_folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: String)

    @Query("SELECT * FROM sync_logs WHERE userId = :userId ORDER BY timestampMillis DESC LIMIT 40")
    fun observeRecentSyncLogs(userId: String): Flow<List<SyncLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLogEntity)

    @Query("DELETE FROM sync_logs WHERE userId = :userId")
    suspend fun clearSyncLogs(userId: String)

    @Query("SELECT * FROM document_versions WHERE documentId = :docId ORDER BY versionNumber DESC LIMIT 20")
    fun observeDocumentVersions(docId: String): Flow<List<DocumentVersionEntity>>

    @Query("SELECT * FROM document_versions WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeAllUserVersions(userId: String): Flow<List<DocumentVersionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentVersion(version: DocumentVersionEntity)

    @Query("DELETE FROM document_versions WHERE userId = :userId AND versionNumber < :keepMinVersion")
    suspend fun pruneOldVersions(userId: String, keepMinVersion: Int = 2)

    @Query("SELECT * FROM document_comments WHERE documentId = :docId ORDER BY createdAtMillis DESC")
    fun observeDocumentComments(docId: String): Flow<List<DocumentCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocumentComment(comment: DocumentCommentEntity)

    @Query("UPDATE document_comments SET status = :status WHERE id = :commentId")
    suspend fun updateCommentStatus(commentId: String, status: String)

    // Y.Map Live PDF Annotation Queries
    @Query("SELECT * FROM pdf_annotations_ymap WHERE documentId = :docId ORDER BY crdtClock ASC")
    fun observeDocumentAnnotations(docId: String): Flow<List<PdfAnnotationYMapEntity>>

    @Query("SELECT * FROM pdf_annotations_ymap WHERE userId = :userId AND syncState != 'SYNCED' ORDER BY crdtClock ASC")
    suspend fun getPendingSyncAnnotations(userId: String): List<PdfAnnotationYMapEntity>

    @Query("SELECT * FROM pdf_annotations_ymap WHERE syncState != 'SYNCED' ORDER BY crdtClock ASC")
    suspend fun getAllPendingSyncAnnotations(): List<PdfAnnotationYMapEntity>

    @Query("UPDATE pdf_annotations_ymap SET syncState = :syncState, updatedAtMillis = :syncedAtMillis WHERE ymapKey = :ymapKey")
    suspend fun updateAnnotationSyncState(ymapKey: String, syncState: String, syncedAtMillis: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPdfAnnotation(annotation: PdfAnnotationYMapEntity)

    @Query("DELETE FROM pdf_annotations_ymap WHERE ymapKey = :ymapKey")
    suspend fun deletePdfAnnotation(ymapKey: String)

    @Query("DELETE FROM pdf_annotations_ymap WHERE documentId = :docId AND pageIndex = :pageIndex")
    suspend fun clearPageAnnotations(docId: String, pageIndex: Int)

    // OTA App Update History Queries
    @Query("SELECT * FROM ota_update_records WHERE userId = :userId ORDER BY installedAtMillis DESC")
    fun observeOtaUpdateRecords(userId: String): Flow<List<OtaUpdateRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOtaUpdateRecord(record: OtaUpdateRecordEntity)
}
