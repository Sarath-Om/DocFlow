package com.example.data.model

enum class CollaboratorRole(val label: String, val canEdit: Boolean, val canComment: Boolean) {
    OWNER("Owner", canEdit = true, canComment = true),
    EDITOR("Editor", canEdit = true, canComment = true),
    COMMENTER("Commenter", canEdit = false, canComment = true),
    VIEWER("Viewer", canEdit = false, canComment = false)
}

enum class LiveSyncStatus(val badgeText: String) {
    SAVED("Saved • Live CRDT Synced"),
    SAVING("Saving... • Encrypting Delta"),
    OFFLINE_QUEUED("Offline — changes will sync")
}

enum class EditorCollabMode(val label: String, val description: String) {
    EDITING("Editing", "Direct sub-second live block & keystroke edits"),
    SUGGESTING("Suggesting", "Edits appear as tracked suggestions for owner approval"),
    VIEWING("Viewing", "Read-only inspection mode (enforced for Viewers)")
}

data class ActiveCollaborator(
    val id: String,
    val name: String,
    val email: String,
    val role: CollaboratorRole,
    val colorHex: String,
    val activeCursorLabel: String,
    val isOnline: Boolean = true,
    val wrappedKeyFingerprint: String = "X25519-AES256-GCM"
)

data class ShareLinkConfig(
    val documentId: String,
    val role: CollaboratorRole = CollaboratorRole.VIEWER,
    val expiresInHours: Int = 72,
    val tokenHash: String = "df_live_9f8a7c2e",
    val fragmentKeyPreview: String = "#key=aes256gcm_e2ee_91b4c8d2",
    val revoked: Boolean = false
)

data class FormatStorageSlice(
    val formatKey: String,
    val displayLabel: String,
    val subtitle: String,
    val bytesUsed: Long,
    val documentCount: Int,
    val percentage: Float,
    val colorHex: Long
)

data class StorageDashboardSummary(
    val totalBytesUsed: Long = 0L,
    val quotaBytes: Long = 50L * 1024L * 1024L, // 50 MB workspace tier
    val offlinePinnedBytes: Long = 0L,
    val offlinePinnedCount: Int = 0,
    val encryptedVaultCount: Int = 0,
    val versionSnapshotsCount: Int = 0,
    val slices: List<FormatStorageSlice> = emptyList()
)
