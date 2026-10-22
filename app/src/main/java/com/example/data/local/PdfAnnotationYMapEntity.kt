package com.example.data.local

import androidx.compose.ui.geometry.Offset
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PdfMarkupTool(val label: String) {
    PEN("Draw"),
    HIGHLIGHT("Highlight"),
    TEXT_BOX("Text Box"),
    MARKER("Marker"),
    STICKY_NOTE("Sticky Note"),
    ERASER("Eraser"),
    RULER("Ruler")
}

/**
 * Represents a live-synchronized Y.Map CRDT entry for PDF & Document Annotations
 * (freehand Draw strokes, Highlight paths, Text Boxes, Marker strokes, and Sticky Notes)
 * stored locally in Room and synced to Firebase via WorkManager.
 */
@Entity(
    tableName = "pdf_annotations_ymap",
    indices = [
        Index(value = ["documentId", "pageIndex"]),
        Index(value = ["userId"])
    ]
)
data class PdfAnnotationYMapEntity(
    @PrimaryKey
    val ymapKey: String,
    val documentId: String,
    val userId: String,
    val pageIndex: Int = 0,
    val toolType: String = PdfMarkupTool.HIGHLIGHT.name,
    val colorHex: String = "#FBBF24",
    val strokeWidth: Float = 14f,
    /**
     * Normalized coordinates (0f..1f) encoded as `x1,y1;x2,y2;...` so annotations scale
     * cleanly across any screen density and PDF export page size.
     */
    val normalizedPointsEncoded: String = "",
    val stickyNoteText: String = "",
    val authorName: String = "",
    val crdtClock: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val syncState: String = SyncState.PENDING_CREATE.name
) {
    fun decodeNormalizedPoints(): List<Offset> {
        if (normalizedPointsEncoded.isBlank()) return emptyList()
        return normalizedPointsEncoded.split(";")
            .mapNotNull { pair ->
                val parts = pair.split(",")
                if (parts.size == 2) {
                    val x = parts[0].toFloatOrNull()
                    val y = parts[1].toFloatOrNull()
                    if (x != null && y != null) Offset(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f)) else null
                } else {
                    null
                }
            }
    }

    companion object {
        fun encodeNormalizedPoints(points: List<Offset>): String {
            return points.joinToString(";") { pt ->
                "${String.format(java.util.Locale.US, "%.4f", pt.x.coerceIn(0f, 1f))},${String.format(java.util.Locale.US, "%.4f", pt.y.coerceIn(0f, 1f))}"
            }
        }
    }
}
