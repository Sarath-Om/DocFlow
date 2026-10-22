package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.ui.screens.ScanFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class PdfConversionResult(
    val pdfFile: File,
    val pdfUri: Uri,
    val fileSize: Long,
    val pageCount: Int
)

data class PipelineDocToPicResult(
    val pageBitmaps: List<Bitmap>,
    val pageFiles: List<File>,
    val stitchedBitmap: Bitmap,
    val stitchedFile: File,
    val stitchedUri: Uri
)

object ImageProcessingPdfPipeline {

    /**
     * Applies document enhancement filters (Magic Color, B&W High Contrast, Grayscale, Original)
     * and optional rotation to a captured camera or gallery Bitmap.
     */
    suspend fun processDocumentBitmap(
        source: Bitmap,
        filter: ScanFilter,
        rotationDegrees: Float = 0f
    ): Bitmap = withContext(Dispatchers.Default) {
        val rotated = if (rotationDegrees % 360f != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        if (filter == ScanFilter.ORIGINAL) {
            return@withContext rotated
        }

        val output = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val colorMatrix = when (filter) {
            ScanFilter.MAGIC_COLOR -> {
                val saturation = ColorMatrix().apply { setSaturation(1.3f) }
                val contrast = 1.32f
                val translate = (-0.5f * contrast + 0.5f) * 255f + 12f
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                saturation.postConcat(contrastMatrix)
                saturation
            }
            ScanFilter.BW_CONTRAST -> {
                val grayscale = ColorMatrix().apply { setSaturation(0f) }
                val contrast = 2.4f
                val translate = (-0.5f * contrast + 0.5f) * 255f + 18f
                val thresholdMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                grayscale.postConcat(thresholdMatrix)
                grayscale
            }
            ScanFilter.GRAYSCALE -> {
                ColorMatrix().apply { setSaturation(0f) }
            }
            ScanFilter.ORIGINAL -> ColorMatrix()
        }

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(rotated, 0f, 0f, paint)
        output
    }

    /**
     * Generates a high-resolution rendered document bitmap combining the embedded camera/photo bitmap
     * AND extracted OCR text/table blocks, tailored to the target format (PDF, Word, Excel, PowerPoint).
     */
    suspend fun createHighResDocumentBitmap(
        cameraBitmap: Bitmap?,
        title: String,
        ocrText: String,
        filter: ScanFilter,
        pageIndex: Int,
        totalPages: Int,
        targetFormat: String = "pdf"
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = 1080
        val height = 1520
        val baseBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(baseBitmap)

        canvas.drawColor(Color.WHITE)

        val formatColorHex = when (targetFormat.lowercase()) {
            "pdf" -> "#DC2626"
            "docx", "doc" -> "#0066FF"
            "xlsx", "xls", "csv" -> "#059669"
            "pptx", "ppt" -> "#EA580C"
            else -> "#6200EA"
        }
        val formatLabel = when (targetFormat.lowercase()) {
            "pdf" -> "PDF DOCUMENT • HYBRID OCR + EMBEDDED IMAGE"
            "docx", "doc" -> "WORD (.DOCX) • STRUCTURED BLOCKS + EMBEDDED IMAGE"
            "xlsx", "xls", "csv" -> "EXCEL (.XLSX) • TABLE GRID + EMBEDDED SCAN"
            "pptx", "ppt" -> "POWERPOINT (.PPTX) • SLIDE DECK + VISUAL FRAME"
            else -> "DOCFLOW SCAN • HYBRID OCR + IMAGE"
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(RectF(36f, 36f, width - 36f, height - 36f), 20f, 20f, borderPaint)

        // Format Top Ribbon
        val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(formatColorHex)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(36f, 36f, width - 36f, 112f), 20f, 20f, ribbonPaint)

        val ribbonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 22f
        }
        canvas.drawText(formatLabel, 68f, 82f, ribbonTextPaint)

        var yOffset = 165f

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 36f
        }
        canvas.drawText(title.take(40), 72f, yOffset, headerPaint)
        yOffset += 26f

        canvas.drawRect(72f, yOffset, 440f, yOffset + 6f, ribbonPaint)
        yOffset += 44f

        // Embedded Image Layout section
        if (cameraBitmap != null) {
            val targetRect = RectF(72f, yOffset, width - 72f, yOffset + 460f)
            val srcRect = Rect(0, 0, cameraBitmap.width, cameraBitmap.height)
            canvas.drawBitmap(cameraBitmap, srcRect, targetRect, null)
            canvas.drawRect(targetRect, borderPaint)
            yOffset += 500f
        } else {
            // Render a visual diagram frame showing Embedded Visual Layout
            val frameRect = RectF(72f, yOffset, width - 72f, yOffset + 240f)
            val frameFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F1F5F9")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(frameRect, 14f, 14f, frameFill)
            canvas.drawRoundRect(frameRect, 14f, 14f, borderPaint)
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(formatColorHex)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 24f
            }
            canvas.drawText("Embedded High-Clarity Visual Frame (${filter.label})", 110f, yOffset + 100f, badgePaint)
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
                textSize = 20f
            }
            canvas.drawText("Combined with editable structured text & table blocks below", 110f, yOffset + 145f, subPaint)
            yOffset += 280f
        }

        // Draw Structured Text & Table rows
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            typeface = Typeface.MONOSPACE
            textSize = 22f
        }
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(formatColorHex)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 22f
        }

        val lines = ocrText.lines()
        for (rawLine in lines) {
            val isTableLine = rawLine.trim().startsWith("|")
            val isSectionHeader = rawLine.trim().startsWith("===")
            val paintToUse = if (isTableLine || isSectionHeader) tableHeaderPaint else bodyPaint
            val wrappedLines = rawLine.chunked(58).ifEmpty { listOf("") }
            for (segment in wrappedLines) {
                if (yOffset < height - 105f) {
                    if (isTableLine) {
                        val rowBg = Paint().apply {
                            color = Color.parseColor("#F8FAFC")
                            style = Paint.Style.FILL
                        }
                        canvas.drawRect(68f, yOffset - 24f, width - 68f, yOffset + 8f, rowBg)
                    }
                    canvas.drawText(segment, 72f, yOffset, paintToUse)
                    yOffset += 33f
                }
            }
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 20f
        }
        canvas.drawText(
            "DocFlow Pic-to-Doc (${targetFormat.uppercase()}) • Filter: ${filter.label} • Page $pageIndex of $totalPages",
            72f,
            height - 60f,
            footerPaint
        )

        processDocumentBitmap(baseBitmap, filter)
    }

    /**
     * Converts any document (PDF, Word, Excel, PowerPoint, Scanned, Pic) into:
     * 1. Individual high-resolution PNG page/slide/sheet Bitmaps & Files
     * 2. A single vertically stitched tall PNG Bitmap & File combining all pages/slides
     */
    suspend fun convertDocumentToPictures(
        context: Context,
        docId: String,
        title: String,
        fileType: String,
        plainContent: String,
        localUri: Uri?
    ): Result<PipelineDocToPicResult> = withContext(Dispatchers.IO) {
        try {
            val outDir = File(context.filesDir, "doc_to_pic")
            if (!outDir.exists()) {
                outDir.mkdirs()
            }

            val pageBitmaps = mutableListOf<Bitmap>()

            // 1. Try rendering from local PDF if available
            if (localUri != null && (fileType == "pdf" || fileType == "scanned")) {
                var pageIndex = 0
                while (pageIndex < 12) {
                    val bmp = FileParserUtils.renderPdfPage(context, localUri, pageIndex) ?: break
                    pageBitmaps.add(bmp)
                    pageIndex++
                }
            }

            // 2. If not a binary PDF or if content has multiple sections/slides/sheets, render high-res pages
            if (pageBitmaps.isEmpty()) {
                val sections = splitContentIntoVisualPages(plainContent, title, fileType)
                sections.forEachIndexed { idx, sectionText ->
                    val bmp = createHighResDocumentBitmap(
                        cameraBitmap = null,
                        title = "$title (${idx + 1}/${sections.size})",
                        ocrText = sectionText,
                        filter = ScanFilter.ORIGINAL,
                        pageIndex = idx + 1,
                        totalPages = sections.size,
                        targetFormat = fileType
                    )
                    pageBitmaps.add(bmp)
                }
            }

            val safePrefix = title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "doc" }
            val pageFiles = mutableListOf<File>()
            pageBitmaps.forEachIndexed { index, bitmap ->
                val pageFile = File(outDir, "${safePrefix}_${docId.take(5)}_page_${index + 1}.png")
                FileOutputStream(pageFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.flush()
                }
                pageFiles.add(pageFile)
            }

            // 3. Create Vertically Stitched Single Image combining all pages/slides
            val stitchWidth = pageBitmaps.maxOf { it.width }.coerceAtLeast(800)
            val gap = 24
            val totalHeight = pageBitmaps.sumOf { it.height } + (gap * (pageBitmaps.size - 1).coerceAtLeast(0))
            val stitchedBitmap = Bitmap.createBitmap(
                stitchWidth,
                totalHeight.coerceAtMost(9000),
                Bitmap.Config.ARGB_8888
            )
            val stitchCanvas = Canvas(stitchedBitmap)
            stitchCanvas.drawColor(Color.parseColor("#E2E8F0"))

            var currentY = 0f
            val scalePaint = Paint(Paint.FILTER_BITMAP_FLAG)
            for (bmp in pageBitmaps) {
                if (currentY + bmp.height <= stitchedBitmap.height) {
                    val left = ((stitchWidth - bmp.width) / 2f).coerceAtLeast(0f)
                    stitchCanvas.drawBitmap(bmp, left, currentY, scalePaint)
                    currentY += bmp.height + gap
                }
            }

            val stitchedFile = File(outDir, "${safePrefix}_${docId.take(5)}_stitched_all.png")
            FileOutputStream(stitchedFile).use { fos ->
                stitchedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
            }

            Result.success(
                PipelineDocToPicResult(
                    pageBitmaps = pageBitmaps,
                    pageFiles = pageFiles,
                    stitchedBitmap = stitchedBitmap,
                    stitchedFile = stitchedFile,
                    stitchedUri = Uri.fromFile(stitchedFile)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun splitContentIntoVisualPages(
        plainContent: String,
        title: String,
        fileType: String
    ): List<String> {
        val rawBlocks = plainContent.split(Regex("(?====\\s*(?:SLIDE|SHEET|SECTION|PAGE))"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (rawBlocks.size > 1) {
            return rawBlocks.take(8)
        }

        val lines = plainContent.lines()
        if (lines.size <= 24) {
            return listOf(plainContent.ifBlank { "=== ${fileType.uppercase()} DOCUMENT: $title ===" })
        }
        return lines.chunked(24).take(8).map { it.joinToString("\n") }
    }

    /**
     * Converts a list of processed Bitmaps into a standard multi-page PDF file on local storage
     * using Android's PdfDocument API.
     */
    suspend fun convertBitmapsToPdfFile(
        context: Context,
        fileName: String,
        bitmaps: List<Bitmap>
    ): Result<PdfConversionResult> = withContext(Dispatchers.IO) {
        try {
            val scansDir = File(context.filesDir, "scanned_pdfs")
            if (!scansDir.exists()) {
                scansDir.mkdirs()
            }
            val safeName = if (fileName.endsWith(".pdf", ignoreCase = true)) {
                fileName
            } else {
                "$fileName.pdf"
            }
            val outFile = File(scansDir, safeName)

            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            try {
                bitmaps.forEachIndexed { index, bitmap ->
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    canvas.drawColor(Color.WHITE)

                    val destRect = RectF(16f, 16f, pageWidth - 16f, pageHeight - 16f)
                    val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                    val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                    canvas.drawBitmap(bitmap, srcRect, destRect, paint)

                    pdfDocument.finishPage(page)
                }

                FileOutputStream(outFile).use { fos ->
                    pdfDocument.writeTo(fos)
                    fos.flush()
                }
            } finally {
                pdfDocument.close()
            }

            Result.success(
                PdfConversionResult(
                    pdfFile = outFile,
                    pdfUri = Uri.fromFile(outFile),
                    fileSize = outFile.length().coerceAtLeast(1024L),
                    pageCount = bitmaps.size.coerceAtLeast(1)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }
}
