package com.example.data.local

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import com.example.data.model.DocEntity
import com.example.data.model.FolderEntity
import com.google.firebase.Timestamp
import java.util.Date

enum class SyncState(val label: String) {
    SYNCED("Cloud Synced"),
    LOCAL_ONLY("Local Only"),
    PENDING_CREATE("Pending Upload"),
    PENDING_UPDATE("Pending Sync"),
    PENDING_DELETE("Pending Delete"),
    FAILED("Sync Error")
}

@Entity(tableName = "cached_documents")
data class CachedDocumentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val folderId: String,
    val category: String,
    val fileType: String,
    val fileName: String,
    val fileSize: Long,
    val content: String,
    val contentPreview: String = "",
    val tags: String = "",
    val localFilePath: String? = null,
    val isEncrypted: Boolean = false,
    val encryptionKeyHint: String? = null,
    val isOfflineCached: Boolean = true,
    val pageCount: Long = 1L,
    val syncState: String = SyncState.SYNCED.name,
    val lastSyncedAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val lastAccessedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDocEntity(): DocEntity {
        val ts = Timestamp(Date(updatedAtMillis))
        return DocEntity(
            id = id,
            userId = userId,
            title = title,
            folderId = folderId,
            category = category,
            fileType = fileType,
            fileName = fileName,
            fileSize = fileSize,
            content = content,
            isEncrypted = isEncrypted,
            encryptionKeyHint = encryptionKeyHint,
            isOfflineCached = isOfflineCached,
            pageCount = pageCount,
            createdAt = ts,
            updatedAt = ts
        )
    }

    fun parsedTags(): List<String> =
        tags.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /**
     * Extracts a human-readable matching line or snippet from the ML Kit OCR / PDF content
     * when full-text search matches inside the stored document body.
     */
    fun extractMatchingOcrSnippet(query: String): String? {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || isEncrypted) return null
        val matchingLine = content.lineSequence()
            .map { it.trim() }
            .firstOrNull { line ->
                line.isNotEmpty() && !line.startsWith("===") && line.contains(trimmed, ignoreCase = true)
            }
        return matchingLine?.take(140)
    }

    companion object {
        fun buildPreview(content: String, isEncrypted: Boolean): String {
            return if (isEncrypted) {
                "Encrypted AES-256 protected document"
            } else {
                content.replace(Regex("\\s+"), " ").trim().take(240)
            }
        }

        fun defaultTagsFor(title: String, category: String, fileType: String, customTags: String = ""): String {
            val combined = mutableSetOf<String>()
            if (customTags.isNotBlank()) {
                customTags.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.forEach {
                    combined.add(it)
                }
            }
            if (category.isNotBlank()) combined.add(category.trim().lowercase())
            if (fileType.isNotBlank()) combined.add(fileType.trim().lowercase())
            title.lowercase()
                .split(Regex("[^a-z0-9]+"))
                .filter { it.length >= 3 }
                .take(4)
                .forEach { combined.add(it) }
            return combined.joinToString(", ")
        }

        fun fromDocEntity(
            doc: DocEntity,
            syncState: SyncState = SyncState.SYNCED,
            localFilePath: String? = null,
            existingTags: String? = null,
            existingLastAccessedAtMillis: Long? = null
        ): CachedDocumentEntity {
            val updatedMs = doc.updatedAt?.toDate()?.time
                ?: doc.createdAt?.toDate()?.time
                ?: System.currentTimeMillis()
            val resolvedTags = if (!existingTags.isNullOrBlank()) {
                existingTags
            } else {
                defaultTagsFor(doc.title, doc.category, doc.fileType)
            }
            val resolvedAccessedMs = existingLastAccessedAtMillis ?: updatedMs
            return CachedDocumentEntity(
                id = doc.id,
                userId = doc.userId,
                title = doc.title,
                folderId = doc.folderId,
                category = doc.category,
                fileType = doc.fileType,
                fileName = doc.fileName,
                fileSize = doc.fileSize,
                content = doc.content,
                contentPreview = buildPreview(doc.content, doc.isEncrypted),
                tags = resolvedTags,
                localFilePath = localFilePath,
                isEncrypted = doc.isEncrypted,
                encryptionKeyHint = doc.encryptionKeyHint,
                isOfflineCached = doc.isOfflineCached,
                pageCount = doc.pageCount,
                syncState = syncState.name,
                lastSyncedAtMillis = System.currentTimeMillis(),
                updatedAtMillis = updatedMs,
                lastAccessedAtMillis = resolvedAccessedMs
            )
        }
    }
}

/**
 * Room FTS4 Virtual Table for high-speed Full-Text Search across
 * document title, full ML Kit OCR content, contentPreview, tags, category, and fileName.
 */
@Fts4(contentEntity = CachedDocumentEntity::class)
@Entity(tableName = "cached_documents_fts")
data class CachedDocumentFtsEntity(
    val title: String,
    val content: String,
    val contentPreview: String,
    val tags: String,
    val category: String,
    val fileName: String
)

@Entity(tableName = "cached_folders")
data class CachedFolderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val color: String,
    val icon: String,
    val syncState: String = SyncState.SYNCED.name,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun toFolderEntity(): FolderEntity {
        val ts = Timestamp(Date(updatedAtMillis))
        return FolderEntity(
            id = id,
            userId = userId,
            name = name,
            color = color,
            icon = icon,
            createdAt = ts,
            updatedAt = ts
        )
    }

    companion object {
        fun fromFolderEntity(
            folder: FolderEntity,
            syncState: SyncState = SyncState.SYNCED
        ): CachedFolderEntity {
            val updatedMs = folder.updatedAt?.toDate()?.time
                ?: folder.createdAt?.toDate()?.time
                ?: System.currentTimeMillis()
            return CachedFolderEntity(
                id = folder.id,
                userId = folder.userId,
                name = folder.name,
                color = folder.color,
                icon = folder.icon,
                syncState = syncState.name,
                updatedAtMillis = updatedMs
            )
        }
    }
}

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val documentId: String,
    val documentTitle: String,
    val action: String,
    val status: String,
    val details: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "document_versions")
data class DocumentVersionEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val userId: String,
    val versionNumber: Int,
    val versionLabel: String,
    val authorName: String,
    val stateVector: String,
    val snapshotContent: String,
    val byteSize: Long,
    val isEncrypted: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "document_comments")
data class DocumentCommentEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val userId: String,
    val authorName: String,
    val authorRole: String = "Editor",
    val anchorLineOrText: String,
    val body: String,
    val isSuggestion: Boolean = false,
    val suggestedReplacement: String = "",
    val status: String = "OPEN", // OPEN, RESOLVED, ACCEPTED, REJECTED
    val parentCommentId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

