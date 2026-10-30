package com.example.data.model

import android.net.Uri

/**
 * Supported document formats for on-device viewing and editing via Apryse SDK.
 */
enum class DocumentFormat(
    val extension: String,
    val mimeTypes: Array<String>,
    val displayName: String
) {
    PDF(
        extension = "pdf",
        mimeTypes = arrayOf("application/pdf"),
        displayName = "PDF Document"
    ),
    WORD(
        extension = "docx",
        mimeTypes = arrayOf(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
        ),
        displayName = "Word Document (.docx)"
    ),
    EXCEL(
        extension = "xlsx",
        mimeTypes = arrayOf(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "text/csv"
        ),
        displayName = "Excel Spreadsheet (.xlsx)"
    ),
    POWERPOINT(
        extension = "pptx",
        mimeTypes = arrayOf(
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-powerpoint"
        ),
        displayName = "PowerPoint Presentation (.pptx)"
    );

    companion object {
        fun fromFileNameOrMime(name: String, mime: String? = null): DocumentFormat {
            val lower = name.lowercase()
            return when {
                lower.endsWith(".pdf") || mime == "application/pdf" -> PDF
                lower.endsWith(".docx") || lower.endsWith(".doc") ||
                    mime?.contains("wordprocessingml") == true || mime == "application/msword" -> WORD
                lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".csv") ||
                    mime?.contains("spreadsheetml") == true || mime == "application/vnd.ms-excel" -> EXCEL
                lower.endsWith(".pptx") || lower.endsWith(".ppt") ||
                    mime?.contains("presentationml") == true || mime == "application/vnd.ms-powerpoint" -> POWERPOINT
                else -> PDF
            }
        }

        val allMimeTypes: Array<String> = arrayOf(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-powerpoint"
        )
    }
}

/**
 * Data representation of a prepared local document ready for Apryse viewing and editing.
 */
data class ApryseDocumentModel(
    val id: String,
    val title: String,
    val originalUri: Uri,
    val localFileUri: Uri,
    val localFilePath: String,
    val format: DocumentFormat,
    val sizeBytes: Long,
    val pageCount: Int = 1,
    val lastModifiedMillis: Long = System.currentTimeMillis(),
    val isReadOnly: Boolean = false
)
