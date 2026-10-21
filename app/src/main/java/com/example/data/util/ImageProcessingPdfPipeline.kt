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

object ImageProcessingPdfPipeline {

    /**
     * Applies document enhancement filters (Magic Color, B&W High Contrast, Grayscale, Original)
     * and optional rotation to a captured camera Bitmap.
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
     * Generates a high-resolution rendered document bitmap from a captured frame or OCR text
     * so every scanned page has a crisp visual representation ready for PDF conversion.
     */
    suspend fun createHighResDocumentBitmap(
        cameraBitmap: Bitmap?,
        title: String,
        ocrText: String,
        filter: ScanFilter,
        pageIndex: Int,
        totalPages: Int
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = 1080
        val height = 1520
        val baseBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(baseBitmap)

        // Fill clean paper background
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(RectF(36f, 36f, width - 36f, height - 36f), 16f, 16f, borderPaint)

        var yOffset = 100f

        // Header banner
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 36f
        }
        canvas.drawText(title.take(40), 72f, yOffset, headerPaint)
        yOffset += 30f

        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2563EB")
            style = Paint.Style.FILL
        }
        canvas.drawRect(72f, yOffset, 420f, yOffset + 8f, accentPaint)
        yOffset += 48f

        // If a real camera frame was captured, embed a cropped preview of the physical capture
        if (cameraBitmap != null) {
            val targetRect = RectF(72f, yOffset, width - 72f, yOffset + 460f)
            val srcRect = Rect(0, 0, cameraBitmap.width, cameraBitmap.height)
            canvas.drawBitmap(cameraBitmap, srcRect, targetRect, null)
            canvas.drawRect(targetRect, borderPaint)
            yOffset += 500f
        }

        // Draw OCR text lines onto the document page
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            typeface = Typeface.MONOSPACE
            textSize = 24f
        }

        val lines = ocrText.lines()
        for (rawLine in lines) {
            val wrappedLines = rawLine.chunked(58).ifEmpty { listOf("") }
            for (segment in wrappedLines) {
                if (yOffset < height - 110f) {
                    canvas.drawText(segment, 72f, yOffset, bodyPaint)
                    yOffset += 34f
                }
            }
        }

        // Footer stamp
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 20f
        }
        canvas.drawText(
            "DocFlow Physical Scanner • Filter: ${filter.label} • Page $pageIndex of $totalPages",
            72f,
            height - 64f,
            footerPaint
        )

        processDocumentBitmap(baseBitmap, filter)
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
            val pageWidth = 595 // Standard A4 width in PostScript points
            val pageHeight = 842 // Standard A4 height in PostScript points

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
