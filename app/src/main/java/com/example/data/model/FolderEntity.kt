package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FolderEntity(
    @DocumentId var id: String = "",
    var userId: String = "",
    var name: String = "",
    var color: String = "#2563EB",
    var icon: String = "folder",
    @ServerTimestamp var createdAt: Timestamp? = null,
    @ServerTimestamp var updatedAt: Timestamp? = null
)
