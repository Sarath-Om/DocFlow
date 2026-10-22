package com.example.data.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.DocumentCommentEntity
import com.example.data.local.DocumentVersionEntity
import com.example.data.local.PdfAnnotationYMapEntity
import com.example.data.local.PdfMarkupTool
import com.example.data.model.DocEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AnnotatedPdfExportResult(
    val pdfFile: File,
    val shareUri: Uri,
    val annotationCount: Int,
    val isBiometricSigned: Boolean
)

/**
 * pdf-lib style PDF Annotation, Version Snapshot & Note Export Engine for Android.
 * Renders document pages and bakes Y.Map CRDT annotations, individual document version snapshots,
 * or individual/consolidated document notes directly into a real PDF file.
 */
object PdfLibAnnotationEngine {

    /**
     * Exports an individual DocumentVersionEntity snapshot as a standalone PDF file
     * using the existing FileProvider and PDF generation infrastructure.
     */
    suspend fun exportVersionSnapshotToPdf(
        context: Context,
        document: DocEntity,
        version: DocumentVersionEntity
    ): Result<AnnotatedPdfExportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US).format(Date(version.createdAtMillis))
            val structuredText = buildString {
                appendLine("=== VERSION SNAPSHOT v${version.versionNumber}: ${version.versionLabel} ===")
                appendLine("Document: ${document.title} (${document.fileName})")
                appendLine("Version Author: ${version.authorName}")
                appendLine("CRDT State Vector: ${version.stateVector}")
                appendLine("Snapshot Timestamp: $dateStr")
                appendLine("Snapshot Size: ${version.byteSize} bytes")
                appendLine()
                appendLine("=== SNAPSHOT CONTENT ===")
                appendLine(version.snapshotContent.ifBlank { "(Empty Version Snapshot)" })
            }

            val cleanBase = document.title.lowercase(Locale.US)
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .ifBlank { "docflow_doc" }
            val fileName = "${cleanBase}_v${version.versionNumber}_snapshot.pdf"

            renderStructuredExportToPdf(
                context = context,
                outputFileName = fileName,
                headerBrand = "DOCFLOW VERSION SNAPSHOT EXPORT • CRDT VECTOR ${version.stateVector.uppercase(Locale.US)}",
                headerTitle = "${document.title} — v${version.versionNumber} (${version.versionLabel})",
                headerMeta = "SAVED: $dateStr   |   AUTHOR: ${version.authorName}   |   DOC ID: #DF-${document.id.take(8).uppercase(Locale.US)}",
                bodyContent = structuredText,
                annotationCount = 0
            )
        }
    }

    /**
     * Exports an individual DocumentCommentEntity (note, comment, or tracked suggestion)
     * as a standalone PDF file.
     */
    suspend fun exportNoteOrCommentToPdf(
        context: Context,
        document: DocEntity,
        comment: DocumentCommentEntity
    ): Result<AnnotatedPdfExportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US).format(Date(comment.createdAtMillis))
            val noteType = if (comment.isSuggestion) "TRACKED SUGGESTION" else "DOCUMENT NOTE / COMMENT"
            val structuredText = buildString {
                appendLine("=== $noteType (${comment.status}) ===")
                appendLine("Document: ${document.title} (${document.fileName})")
                appendLine("Author: ${comment.authorName} (${comment.authorRole})")
                appendLine("Created At: $dateStr")
                appendLine("Anchored Text / Section: \"${comment.anchorLineOrText}\"")
                appendLine()
                appendLine("=== NOTE / COMMENT BODY ===")
                appendLine(comment.body)
                if (comment.isSuggestion && comment.suggestedReplacement.isNotBlank()) {
                    appendLine()
                    appendLine("=== PROPOSED REPLACEMENT TEXT ===")
                    appendLine("+ ${comment.suggestedReplacement}")
                }
            }

            val cleanBase = document.title.lowercase(Locale.US)
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .ifBlank { "docflow_doc" }
            val shortId = comment.id.replace(Regex("[^a-zA-Z0-9]"), "").take(6).ifBlank { "note" }
            val fileName = "${cleanBase}_note_${shortId}.pdf"

            renderStructuredExportToPdf(
                context = context,
                outputFileName = fileName,
                headerBrand = "DOCFLOW NOTE & COMMENT PDF EXPORT • ${comment.status}",
                headerTitle = "${document.title} — Note by ${comment.authorName}",
                headerMeta = "CREATED: $dateStr   |   ROLE: ${comment.authorRole}   |   NOTE ID: #$shortId",
                bodyContent = structuredText,
                annotationCount = 1
            )
        }
    }

    /**
     * Exports all notes, comments, tracked suggestions, and sticky-note/text-box PDF annotations
     * on a document into a consolidated Notes Report PDF file.
     */
    suspend fun exportAllNotesToPdf(
        context: Context,
        document: DocEntity,
        comments: List<DocumentCommentEntity>,
        stickyNotes: List<PdfAnnotationYMapEntity> = emptyList()
    ): Result<AnnotatedPdfExportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val nowStr = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US).format(Date())
            val textNotes = stickyNotes.filter { it.stickyNoteText.isNotBlank() }
            val totalNotesCount = comments.size + textNotes.size

            val structuredText = buildString {
                appendLine("=== CONSOLIDATED DOCUMENT NOTES & ANNOTATIONS REPORT ===")
                appendLine("Document: ${document.title} (${document.fileName})")
                appendLine("Exported At: $nowStr")
                appendLine("Total Notes & Comments: $totalNotesCount (${comments.size} threaded comments, ${textNotes.size} PDF page notes)")
                appendLine()

                if (comments.isNotEmpty()) {
                    appendLine("=== SECTION 1: THREADED NOTES & TRACKED SUGGESTIONS ===")
                    comments.forEachIndexed { index, item ->
                        val itemDate = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(item.createdAtMillis))
                        appendLine("${index + 1}. [${item.status}] ${item.authorName} (${item.authorRole}) • $itemDate")
                        appendLine("   Anchor: \"${item.anchorLineOrText}\"")
                        appendLine("   Note: ${item.body}")
                        if (item.isSuggestion && item.suggestedReplacement.isNotBlank()) {
                            appendLine("   + Proposed Replacement: \"${item.suggestedReplacement}\"")
                        }
                        appendLine()
                    }
                }

                if (textNotes.isNotEmpty()) {
                    appendLine("=== SECTION 2: ON-PAGE PDF TEXT BOXES & STICKY NOTES ===")
                    textNotes.forEachIndexed { index, ann ->
                        appendLine("${index + 1}. [Page ${ann.pageIndex + 1} • ${ann.toolType}] ${ann.authorName.ifBlank { "Owner" }}: ${ann.stickyNoteText}")
                    }
                }
            }

            val cleanBase = document.title.lowercase(Locale.US)
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .ifBlank { "docflow_doc" }
            val fileName = "${cleanBase}_notes_report.pdf"

            renderStructuredExportToPdf(
                context = context,
                outputFileName = fileName,
                headerBrand = "DOCFLOW NOTES & REVIEW REPORT • $totalNotesCount ENTRIES",
                headerTitle = "${document.title} — Notes Report",
                headerMeta = "EXPORTED: $nowStr   |   DOC ID: #DF-${document.id.take(8).uppercase(Locale.US)}",
                bodyContent = structuredText,
                annotationCount = totalNotesCount
            )
        }
    }

    private fun renderStructuredExportToPdf(
        context: Context,
        outputFileName: String,
        headerBrand: String,
        headerTitle: String,
        headerMeta: String,
        bodyContent: String,
        annotationCount: Int
    ): AnnotatedPdfExportResult {
        val exportDir = File(context.filesDir, "annotated_pdfs").apply { mkdirs() }
        val outFile = File(exportDir, outputFileName)

        val lines = bodyContent.lines()
        val linesPerPage = 26
        val totalPages = ((lines.size + linesPerPage - 1) / linesPerPage).coerceIn(1, 12)
        val pageWidth = 1080
        val pageHeight = 1520

        var wroteViaPdfDocument = false
        try {
            val pdfDocument = PdfDocument()
            for (pageIdx in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIdx + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FFFFFF")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1F6BFF")
                    alpha = 180
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                canvas.drawRoundRect(
                    RectF(36f, 36f, pageWidth - 36f, pageHeight - 36f),
                    24f,
                    24f,
                    borderPaint
                )

                val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1F6BFF")
                    textSize = 20f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                }
                canvas.drawText(headerBrand.take(64), 72f, 94f, brandPaint)

                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#111317")
                    textSize = 34f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(headerTitle.take(46), 72f, 144f, titlePaint)

                val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#5C6069")
                    textSize = 19f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                }
                canvas.drawText(headerMeta.take(78), 72f, 184f, metaPaint)

                val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1F6BFF")
                    textSize = 25f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#2C3038")
                    textSize = 23f
                }

                var yCursor = 244f
                val pageLines = lines.drop(pageIdx * linesPerPage).take(linesPerPage)
                for (rawLine in pageLines) {
                    if (yCursor > pageHeight - 120f) break
                    val trimmed = rawLine.trim()
                    if (trimmed.startsWith("===")) {
                        yCursor += 12f
                        canvas.drawText(trimmed.trim('=').trim(), 72f, yCursor, headingPaint)
                        yCursor += 36f
                    } else {
                        val wrapped = trimmed.chunked(70)
                        if (wrapped.isEmpty()) {
                            yCursor += 18f
                        } else {
                            for (chunk in wrapped) {
                                canvas.drawText(chunk, 72f, yCursor, bodyPaint)
                                yCursor += 32f
                            }
                        }
                    }
                }

                canvas.drawText(
                    "DOCFLOW PDF EXPORT   |   PAGE ${pageIdx + 1} / $totalPages",
                    72f,
                    pageHeight - 56f,
                    metaPaint
                )

                pdfDocument.finishPage(page)
            }
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            wroteViaPdfDocument = outFile.exists() && outFile.length() > 0L
        } catch (_: Throwable) {
            wroteViaPdfDocument = false
        }

        if (!wroteViaPdfDocument) {
            writeStructuredPdfFallback(outFile, headerTitle, headerMeta, bodyContent, totalPages)
        }

        val shareUri = runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
        }.getOrElse { Uri.fromFile(outFile) }

        return AnnotatedPdfExportResult(
            pdfFile = outFile,
            shareUri = shareUri,
            annotationCount = annotationCount,
            isBiometricSigned = false
        )
    }

    private fun writeStructuredPdfFallback(
        outFile: File,
        headerTitle: String,
        headerMeta: String,
        bodyContent: String,
        totalPages: Int
    ) {
        val safeText = (headerTitle + "\n" + headerMeta + "\n\n" + bodyContent)
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("\r", " ")
        val streamBody = "BT /F1 11 Tf 36 750 Td (${safeText.take(3000)}) Tj ET"
        val pdfString = buildString {
            append("%PDF-1.4\n")
            append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
            append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count ${totalPages.coerceAtLeast(1)} >>\nendobj\n")
            append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >>\nendobj\n")
            append("4 0 obj\n<< /Length ${streamBody.length} >>\nstream\n$streamBody\nendstream\nendobj\n")
            append("trailer\n<< /Root 1 0 R >>\n%%EOF\n")
        }
        outFile.writeBytes(pdfString.toByteArray(Charsets.UTF_8))
    }

    suspend fun exportAnnotatedPdf(
        context: Context,
        document: DocEntity,
        plainContent: String,
        annotations: List<PdfAnnotationYMapEntity>,
        isBiometricSigned: Boolean,
        signerName: String,
        signerEmail: String
    ): Result<AnnotatedPdfExportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val pageWidth = 1080
            val pageHeight = 1520
            val totalPages = document.pageCount.toInt().coerceIn(1, 8)
            val lines = plainContent.ifBlank { document.content }.lines()
            val linesPerPage = (lines.size / totalPages).coerceAtLeast(18)

            val pdfDocument = PdfDocument()

            for (pageIdx in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIdx + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // 1. Deep Crystal Canvas / A4 Document Background
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#0E1222")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

                // Outer Specular Border
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#00F4FE")
                    alpha = 90
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                canvas.drawRoundRect(
                    RectF(36f, 36f, pageWidth - 36f, pageHeight - 36f),
                    28f,
                    28f,
                    borderPaint
                )

                // 2. Document Header & Certified Badge
                val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#00F4FE")
                    textSize = 22f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                }
                canvas.drawText("DOCFLOW LEGAL ENTERPRISE STANDARD • Y.MAP CRDT SYNCED", 72f, 96f, brandPaint)

                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 38f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(document.title.take(42).uppercase(Locale.US), 72f, 148f, titlePaint)

                val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#C0C6D6")
                    textSize = 21f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                }
                val dateLabel = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
                canvas.drawText(
                    "EFFECTIVE DATE: $dateLabel   |   DOC ID: #DF-${document.id.take(8).uppercase(Locale.US)}",
                    72f,
                    190f,
                    metaPaint
                )

                // 3. Body Clauses & Table Blocks
                val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#E1E1F3")
                    textSize = 24f
                }
                val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#00F4FE")
                    textSize = 26f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                var yCursor = 250f
                val pageLines = lines.drop(pageIdx * linesPerPage).take(linesPerPage)
                for (rawLine in pageLines) {
                    if (yCursor > pageHeight - 280f) break
                    val trimmed = rawLine.trim()
                    if (trimmed.startsWith("===")) {
                        yCursor += 14f
                        canvas.drawText(trimmed.trim('=').trim(), 72f, yCursor, headingPaint)
                        yCursor += 38f
                    } else {
                        val wrapped = trimmed.chunked(68)
                        if (wrapped.isEmpty()) {
                            yCursor += 20f
                        } else {
                            for (chunk in wrapped) {
                                canvas.drawText(chunk, 72f, yCursor, bodyPaint)
                                yCursor += 34f
                            }
                        }
                    }
                }

                // 4. Bake Y.Map Annotations (Draw, Highlight, Marker, Sticky Notes) onto PDF Page
                val pageAnnotations = annotations.filter { it.pageIndex == pageIdx || totalPages == 1 }
                drawYMapAnnotationsOnCanvas(
                    canvas = canvas,
                    canvasWidth = pageWidth.toFloat(),
                    canvasHeight = pageHeight.toFloat(),
                    annotations = pageAnnotations
                )

                // 5. Biometric Cryptographic Signature Stamp if signed
                if (isBiometricSigned && pageIdx == totalPages - 1) {
                    val sigBox = RectF(72f, pageHeight - 230f, pageWidth - 72f, pageHeight - 100f)
                    val sigFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#102A45")
                        style = Paint.Style.FILL
                    }
                    val sigBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#00F4FE")
                        style = Paint.Style.STROKE
                        strokeWidth = 2.5f
                    }
                    canvas.drawRoundRect(sigBox, 20f, 20f, sigFill)
                    canvas.drawRoundRect(sigBox, 20f, 20f, sigBorder)

                    val sigTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#00F4FE")
                        textSize = 23f
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    }
                    val sigDetailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        textSize = 22f
                    }
                    canvas.drawText(
                        "✓ CRYPTOGRAPHICALLY SIGNED VIA ANDROIDX.BIOMETRIC",
                        100f,
                        pageHeight - 178f,
                        sigTitlePaint
                    )
                    canvas.drawText(
                        "Signatory: $signerName ($signerEmail) • Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}",
                        100f,
                        pageHeight - 135f,
                        sigDetailPaint
                    )
                }

                // Footer
                canvas.drawText(
                    "CONFIDENTIAL - DOCFLOW CERTIFIED SECURE   |   PAGE ${pageIdx + 1} / $totalPages",
                    72f,
                    pageHeight - 56f,
                    metaPaint
                )

                pdfDocument.finishPage(page)
            }

            val exportDir = File(context.filesDir, "annotated_pdfs").apply { mkdirs() }
            val cleanBase = document.title.lowercase(Locale.US)
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .ifBlank { "docflow_annotated" }
            val outFile = File(exportDir, "${cleanBase}_annotated.pdf")
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val shareUri = runCatching {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    outFile
                )
            }.getOrElse { Uri.fromFile(outFile) }

            AnnotatedPdfExportResult(
                pdfFile = outFile,
                shareUri = shareUri,
                annotationCount = annotations.size,
                isBiometricSigned = isBiometricSigned
            )
        }
    }

    private fun drawYMapAnnotationsOnCanvas(
        canvas: Canvas,
        canvasWidth: Float,
        canvasHeight: Float,
        annotations: List<PdfAnnotationYMapEntity>
    ) {
        annotations.forEach { entry ->
            val tool = runCatching { PdfMarkupTool.valueOf(entry.toolType) }.getOrDefault(PdfMarkupTool.PEN)
            val parsedColor = runCatching { Color.parseColor(entry.colorHex) }.getOrDefault(Color.parseColor("#FBBF24"))
            val points = entry.decodeNormalizedPoints()

            if ((tool == PdfMarkupTool.STICKY_NOTE || tool == PdfMarkupTool.TEXT_BOX) && entry.stickyNoteText.isNotBlank()) {
                val anchor = points.firstOrNull()
                val nx = (anchor?.x ?: 0.55f) * canvasWidth
                val ny = (anchor?.y ?: 0.45f) * canvasHeight
                val noteRect = RectF(
                    nx.coerceIn(60f, canvasWidth - 460f),
                    ny.coerceIn(120f, canvasHeight - 240f),
                    (nx + 400f).coerceAtMost(canvasWidth - 60f),
                    (ny + 140f).coerceAtMost(canvasHeight - 100f)
                )
                val noteBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (tool == PdfMarkupTool.TEXT_BOX) Color.parseColor("#0F172A") else Color.parseColor("#1D2238")
                    alpha = 240
                    style = Paint.Style.FILL
                }
                val noteStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = parsedColor
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                canvas.drawRoundRect(noteRect, 18f, 18f, noteBg)
                canvas.drawRoundRect(noteRect, 18f, 18f, noteStroke)

                val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = parsedColor
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 20f
                }
                val headerLabel = if (tool == PdfMarkupTool.TEXT_BOX) {
                    "${entry.authorName.ifBlank { "Collaborator" }} (Text Box):"
                } else {
                    "${entry.authorName.ifBlank { "Collaborator" }} (Y.Map Note):"
                }
                canvas.drawText(
                    headerLabel,
                    noteRect.left + 20f,
                    noteRect.top + 38f,
                    authorPaint
                )
                canvas.drawText(
                    entry.stickyNoteText.take(44),
                    noteRect.left + 20f,
                    noteRect.top + 78f,
                    textPaint
                )
                if (entry.stickyNoteText.length > 44) {
                    canvas.drawText(
                        entry.stickyNoteText.drop(44).take(44),
                        noteRect.left + 20f,
                        noteRect.top + 110f,
                        textPaint
                    )
                }
            } else if (points.size >= 2) {
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = parsedColor
                    style = Paint.Style.STROKE
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                    strokeWidth = when (tool) {
                        PdfMarkupTool.HIGHLIGHT -> 28f
                        PdfMarkupTool.MARKER -> 16f
                        else -> 7f
                    }
                    alpha = when (tool) {
                        PdfMarkupTool.HIGHLIGHT -> 115
                        PdfMarkupTool.MARKER -> 185
                        else -> 255
                    }
                }
                val path = Path()
                points.forEachIndexed { idx, pt ->
                    val px = pt.x * canvasWidth
                    val py = pt.y * canvasHeight
                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                canvas.drawPath(path, strokePaint)
            }
        }
    }

    /**
     * Launches the Android system Share Sheet (`Intent.ACTION_SEND`) for a real exported PDF or document.
     */
    fun launchSystemShareSheet(
        context: Context,
        document: DocEntity,
        plainContent: String,
        exportedPdfResult: AnnotatedPdfExportResult?
    ) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            if (exportedPdfResult != null && exportedPdfResult.pdfFile.exists()) {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, exportedPdfResult.shareUri)
                putExtra(Intent.EXTRA_SUBJECT, "DocFlow Certified Document: ${document.title}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Shared from DocFlow (${document.fileName})\nAnnotations: ${exportedPdfResult.annotationCount} entries\n\n${plainContent.take(500)}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "DocFlow Document: ${document.title}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "=== ${document.title} (${document.fileName}) ===\n\n$plainContent"
                )
            }
        }
        val chooser = Intent.createChooser(sendIntent, "Share '${document.title}' via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
    }
}

