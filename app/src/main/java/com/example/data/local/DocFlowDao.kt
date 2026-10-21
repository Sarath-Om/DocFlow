package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocFlowDao {

    @Query("SELECT * FROM cached_documents WHERE userId = :userId AND syncState != 'PENDING_DELETE' ORDER BY updatedAtMillis DESC")
    fun observeCachedDocuments(userId: String): Flow<List<CachedDocumentEntity>>

    /**
     * Full-Text Search (FTS4) across title, contentPreview, tags, category, and fileName.
     */
    @Query(
        """
        SELECT d.* FROM cached_documents d
        JOIN cached_documents_fts fts ON d.rowid = fts.rowid
        WHERE d.userId = :userId
          AND d.syncState != 'PENDING_DELETE'
          AND cached_documents_fts MATCH :ftsQuery
        ORDER BY d.updatedAtMillis DESC
        """
    )
    fun searchCachedDocumentsFts(userId: String, ftsQuery: String): Flow<List<CachedDocumentEntity>>

    /**
     * Fallback/substring search on title, contentPreview, tags, category, or fileName.
     */
    @Query(
        """
        SELECT * FROM cached_documents
        WHERE userId = :userId
          AND syncState != 'PENDING_DELETE'
          AND (
              title LIKE '%' || :rawQuery || '%'
              OR contentPreview LIKE '%' || :rawQuery || '%'
              OR tags LIKE '%' || :rawQuery || '%'
              OR category LIKE '%' || :rawQuery || '%'
              OR fileName LIKE '%' || :rawQuery || '%'
          )
        ORDER BY updatedAtMillis DESC
        """
    )
    fun searchCachedDocumentsSubstring(userId: String, rawQuery: String): Flow<List<CachedDocumentEntity>>

    @Query("SELECT * FROM cached_documents WHERE userId = :userId AND id = :docId LIMIT 1")
    suspend fun getCachedDocumentById(userId: String, docId: String): CachedDocumentEntity?

    @Query("SELECT * FROM cached_documents WHERE userId = :userId AND syncState != 'SYNCED'")
    suspend fun getPendingSyncDocuments(userId: String): List<CachedDocumentEntity>

    @Query("SELECT COUNT(*) FROM cached_documents WHERE userId = :userId AND syncState != 'SYNCED'")
    fun observePendingSyncCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocument(document: CachedDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocuments(documents: List<CachedDocumentEntity>)

    @Query("UPDATE cached_documents SET syncState = :syncState, lastSyncedAtMillis = :syncedAtMillis WHERE id = :docId")
    suspend fun updateDocumentSyncState(docId: String, syncState: String, syncedAtMillis: Long = System.currentTimeMillis())

    @Query("UPDATE cached_documents SET tags = :tags, updatedAtMillis = :updatedAtMillis WHERE id = :docId")
    suspend fun updateDocumentTags(docId: String, tags: String, updatedAtMillis: Long = System.currentTimeMillis())

    @Query("DELETE FROM cached_documents WHERE id = :docId")
    suspend fun deleteDocumentById(docId: String)

    @Query("SELECT * FROM cached_folders WHERE userId = :userId AND syncState != 'PENDING_DELETE' ORDER BY name ASC")
    fun observeCachedFolders(userId: String): Flow<List<CachedFolderEntity>>

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
}
