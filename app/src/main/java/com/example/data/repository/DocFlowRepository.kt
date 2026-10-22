package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.DocEntity
import com.example.data.model.FolderEntity
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class DocFlowRepository(
    private val firestoreInstance: FirebaseFirestore? = null,
    val offlineLocalOnly: Boolean = false
) {
    val db: FirebaseFirestore
        get() = firestoreInstance ?: FirebaseFirestore.getInstance()

    constructor(context: Context) : this(
        firestoreInstance = runCatching {
            FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
        }.getOrNull(),
        offlineLocalOnly = true
    )

    companion object {
        fun createOfflineLocal(): DocFlowRepository = DocFlowRepository(
            firestoreInstance = null,
            offlineLocalOnly = true
        )
    }

    private val auth by lazy { runCatching { Firebase.auth }.getOrNull() }

    fun requireUserId(): String {
        if (offlineLocalOnly) {
            return "local_device_user"
        }
        return auth?.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeFolders(): Flow<List<FolderEntity>> {
        if (offlineLocalOnly || firestoreInstance == null) {
            return flowOf(emptyList())
        }
        return flow {
            val uid = requireUserId()
            val path = "users/$uid/folders"
            emitAll(
                db.collection("users").document(uid).collection("folders")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            FolderEntity::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                    .catch { error ->
                        if (error is Exception) {
                            handleFirestoreError(error, OperationType.LIST, path)
                        }
                        throw error
                    }
            )
        }
    }

    fun observeDocuments(folderIdFilter: String? = null): Flow<List<DocEntity>> {
        if (offlineLocalOnly || firestoreInstance == null) {
            return flowOf(emptyList())
        }
        return flow {
            val uid = requireUserId()
            val path = "users/$uid/documents"
            val query = db.collection("users").document(uid).collection("documents")
            emitAll(
                query.snapshots()
                    .map { snapshot ->
                        val docs = snapshot.toObjects(
                            DocEntity::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        if (folderIdFilter != null && folderIdFilter != "all") {
                            docs.filter { it.folderId == folderIdFilter }
                        } else {
                            docs
                        }
                    }
                    .catch { error ->
                        if (error is Exception) {
                            handleFirestoreError(error, OperationType.LIST, path)
                        }
                        throw error
                    }
            )
        }
    }

    suspend fun createFolder(
        name: String,
        color: String = "#1F6BFF",
        icon: String = "folder",
        parentFolderId: String? = null
    ): Result<String> {
        val folderId = "folder_" + UUID.randomUUID().toString().replace("-", "").take(16)
        if (offlineLocalOnly || firestoreInstance == null) {
            return Result.success(folderId)
        }
        val uid = requireUserId()
        val path = "users/$uid/folders/$folderId"
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "name" to name.trim(),
            "color" to color,
            "icon" to icon,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!parentFolderId.isNullOrBlank()) {
            payload["parentFolderId"] = parentFolderId
        }
        return try {
            db.collection("users").document(uid).collection("folders").document(folderId).set(payload).await()
            Result.success(folderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateFolder(folder: FolderEntity): Result<Unit> {
        if (offlineLocalOnly || firestoreInstance == null) {
            return Result.success(Unit)
        }
        val uid = requireUserId()
        val path = "users/$uid/folders/${folder.id}"
        val updates = mapOf<String, Any>(
            "name" to folder.name.trim(),
            "color" to folder.color,
            "icon" to folder.icon,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            db.collection("users").document(uid).collection("folders").document(folder.id).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteFolder(folderId: String): Result<Unit> {
        if (offlineLocalOnly || firestoreInstance == null) {
            return Result.success(Unit)
        }
        val uid = requireUserId()
        val path = "users/$uid/folders/$folderId"
        return try {
            db.collection("users").document(uid).collection("folders").document(folderId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun createDocument(
        title: String,
        folderId: String = "root",
        category: String = "General",
        fileType: String = "pdf",
        fileName: String,
        fileSize: Long,
        content: String,
        isEncrypted: Boolean = false,
        encryptionKeyHint: String? = null,
        isOfflineCached: Boolean = true,
        pageCount: Long = 1L
    ): Result<String> {
        val docId = "doc_" + UUID.randomUUID().toString().replace("-", "").take(16)
        if (offlineLocalOnly || firestoreInstance == null) {
            return Result.success(docId)
        }
        val uid = requireUserId()
        val path = "users/$uid/documents/$docId"
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "title" to title.trim(),
            "folderId" to (if (folderId.isBlank()) "root" else folderId),
            "category" to category,
            "fileType" to fileType.lowercase(),
            "fileName" to fileName,
            "fileSize" to fileSize,
            "content" to content,
            "isEncrypted" to isEncrypted,
            "isOfflineCached" to isOfflineCached,
            "pageCount" to pageCount,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!encryptionKeyHint.isNullOrBlank()) {
            payload["encryptionKeyHint"] = encryptionKeyHint
        }

        return try {
            db.collection("users").document(uid).collection("documents").document(docId).set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDocument(
        docId: String,
        title: String,
        folderId: String,
        category: String,
        content: String,
        isEncrypted: Boolean,
        encryptionKeyHint: String? = null,
        isOfflineCached: Boolean = true,
        pageCount: Long = 1L,
        fileSize: Long = 0L
    ): Result<Unit> {
        if (offlineLocalOnly || firestoreInstance == null) {
            return Result.success(Unit)
        }
        val uid = requireUserId()
        val path = "users/$uid/documents/$docId"
        val updates = mutableMapOf<String, Any>(
            "title" to title.trim(),
            "folderId" to folderId,
            "category" to category,
            "content" to content,
            "isEncrypted" to isEncrypted,
            "isOfflineCached" to isOfflineCached,
            "pageCount" to pageCount,
            "fileSize" to fileSize,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (encryptionKeyHint != null) {
            updates["encryptionKeyHint"] = encryptionKeyHint
        }

        return try {
            db.collection("users").document(uid).collection("documents").document(docId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(docId: String): Result<Unit> {
        if (offlineLocalOnly || firestoreInstance == null) {
            return Result.success(Unit)
        }
        val uid = requireUserId()
        val path = "users/$uid/documents/$docId"
        return try {
            db.collection("users").document(uid).collection("documents").document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun getDocumentById(docId: String): Result<DocEntity?> {
        if (offlineLocalOnly || firestoreInstance == null) {
            return Result.success(null)
        }
        val uid = requireUserId()
        val path = "users/$uid/documents/$docId"
        return try {
            val snapshot = db.collection("users").document(uid).collection("documents").document(docId).get().await()
            Result.success(
                snapshot.toObject(
                    DocEntity::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun upsertPdfAnnotation(annotation: com.example.data.local.PdfAnnotationYMapEntity): Result<Unit> {
        if (offlineLocalOnly || firestoreInstance == null) {
            return Result.success(Unit)
        }
        val uid = auth?.currentUser?.uid ?: annotation.userId.ifBlank { return Result.failure(IllegalStateException("Missing userId")) }
        val safeDocId = annotation.documentId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "doc_default" }
        val safeKey = annotation.ymapKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "ann_default" }
        val path = "users/$uid/documents/$safeDocId/annotations/$safeKey"
        val payload = mapOf(
            "ymapKey" to safeKey,
            "documentId" to safeDocId,
            "userId" to uid,
            "pageIndex" to annotation.pageIndex,
            "toolType" to annotation.toolType,
            "colorHex" to annotation.colorHex,
            "strokeWidth" to annotation.strokeWidth.toDouble(),
            "normalizedPointsEncoded" to annotation.normalizedPointsEncoded,
            "stickyNoteText" to annotation.stickyNoteText,
            "authorName" to annotation.authorName,
            "crdtClock" to annotation.crdtClock,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            db.collection("users")
                .document(uid)
                .collection("documents")
                .document(safeDocId)
                .collection("annotations")
                .document(safeKey)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePdfAnnotation(documentId: String, ymapKey: String): Result<Unit> {
        if (offlineLocalOnly || firestoreInstance == null) {
            return Result.success(Unit)
        }
        val uid = auth?.currentUser?.uid ?: return Result.failure(IllegalStateException("User not signed in"))
        val safeDocId = documentId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "doc_default" }
        val safeKey = ymapKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "ann_default" }
        val path = "users/$uid/documents/$safeDocId/annotations/$safeKey"
        return try {
            db.collection("users")
                .document(uid)
                .collection("documents")
                .document(safeDocId)
                .collection("annotations")
                .document(safeKey)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
