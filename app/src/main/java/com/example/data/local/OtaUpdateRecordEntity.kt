package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores installed Over-The-Air (OTA) App Update records, bundle hashes, and release notes in Room.
 */
@Entity(
    tableName = "ota_update_records",
    indices = [Index(value = ["userId", "installedAtMillis"])]
)
data class OtaUpdateRecordEntity(
    @PrimaryKey
    val versionCode: Int,
    val userId: String,
    val versionName: String,
    val channel: String,
    val bundleSha256: String,
    val patchSizeBytes: Long,
    val releaseTitle: String,
    val changelogSummary: String,
    val status: String = "INSTALLED",
    val installedAtMillis: Long = System.currentTimeMillis()
)
