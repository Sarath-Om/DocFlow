package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class DocEntity(
    @DocumentId var id: String = "",
    var userId: String = "",
    var title: String = "",
    var folderId: String = "root",
    var category: String = "General",
    var fileType: String = "pdf",
    var fileName: String = "",
    var fileSize: Long = 0L,
    var content: String = "",
    @get:PropertyName("isEncrypted") @set:PropertyName("isEncrypted") var isEncrypted: Boolean = false,
    var encryptionKeyHint: String? = null,
    @get:PropertyName("isOfflineCached") @set:PropertyName("isOfflineCached") var isOfflineCached: Boolean = true,
    var pageCount: Long = 1L,
    @ServerTimestamp var createdAt: Timestamp? = null,
    @ServerTimestamp var updatedAt: Timestamp? = null
)
