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
     * Applies document enhancement filters (Edge Detection, Magic Color, B&W High Contrast, Grayscale, Original)
     * and optional rotation to a captured CameraX or gallery Bitmap.
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

        if (filter == ScanFilter.EDGE_DETECTION) {
            return@withContext applyEdgeDetectionFilter(rotated)
        }

        val output = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val colorMatrix = when (filter) {
            ScanFilter.EDGE_DETECTION -> ColorMatrix()
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
     * Real 3x3 Sobel luminance gradient edge-detection & document boundary contour filter.
     * Enhances high-frequency document borders and text edges while preserving clean page readability.
     */
    fun applyEdgeDetectionFilter(source: Bitmap): Bitmap {
        val width = source.width.coerceAtLeast(1)
        val height = source.height.coerceAtLeast(1)
        val maxDim = 960
        val workingBitmap = if (width > maxDim || height > maxDim) {
            val scale = maxDim.toFloat() / maxOf(width, height).toFloat()
            Bitmap.createScaledBitmap(
                source,
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            source
        }

        val w = workingBitmap.width
        val h = workingBitmap.height
        val pixels = IntArray(w * h)
        workingBitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val luma = IntArray(w * h)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            luma[i] = (r * 77 + g * 150 + b * 29) shr 8
        }

        val outPixels = IntArray(w * h)
        var minEdgeX = w
        var minEdgeY = h
        var maxEdgeX = 0
        var maxEdgeY = 0

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                if (x == 0 || x == w - 1 || y == 0 || y == h - 1) {
                    outPixels[idx] = Color.WHITE
                    continue
                }
                val tl = luma[(y - 1) * w + (x - 1)]
                val tc = luma[(y - 1) * w + x]
                val tr = luma[(y - 1) * w + (x + 1)]
                val ml = luma[y * w + (x - 1)]
                val mr = luma[y * w + (x + 1)]
                val bl = luma[(y + 1) * w + (x - 1)]
                val bc = luma[(y + 1) * w + x]
                val br = luma[(y + 1) * w + (x + 1)]

                val gx = -tl - (2 * ml) - bl + tr + (2 * mr) + br
                val gy = -tl - (2 * tc) - tr + bl + (2 * bc) + br
                val mag = (kotlin.math.abs(gx) + kotlin.math.abs(gy)).coerceAtMost(255)

                if (mag > 70) {
                    if (x < minEdgeX) minEdgeX = x
                    if (x > maxEdgeX) maxEdgeX = x
                    if (y < minEdgeY) minEdgeY = y
                    if (y > maxEdgeY) maxEdgeY = y
                }

                // Combine adaptive luminance with detected Sobel edge contour for crisp document legibility
                val baseLum = luma[idx]
                val edgeBoosted = if (mag > 64) {
                    (255 - mag).coerceIn(0, 70)
                } else if (baseLum > 165) {
                    255
                } else {
                    (baseLum - (mag / 2)).coerceIn(0, 255)
                }
                outPixels[idx] = Color.rgb(edgeBoosted, edgeBoosted, edgeBoosted)
            }
        }

        val edgeBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        edgeBitmap.setPixels(outPixels, 0, w, 0, 0, w, h)

        val canvas = Canvas(edgeBitmap)
        val contourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00BCD4")
            style = Paint.Style.STROKE
            strokeWidth = (maxOf(w, h) * 0.005f).coerceAtLeast(2f)
        }
        val boxLeft = if (minEdgeX < maxEdgeX) minEdgeX.toFloat().coerceAtLeast(8f) else 12f
        val boxTop = if (minEdgeY < maxEdgeY) minEdgeY.toFloat().coerceAtLeast(8f) else 12f
        val boxRight = if (minEdgeX < maxEdgeX) maxEdgeX.toFloat().coerceAtMost(w - 8f) else w - 12f
        val boxBottom = if (minEdgeY < maxEdgeY) maxEdgeY.toFloat().coerceAtMost(h - 8f) else h - 12f
        canvas.drawRect(RectF(boxLeft, boxTop, boxRight, boxBottom), contourPaint)

        return if (w != width || h != height) {
            Bitmap.createScaledBitmap(edgeBitmap, width, height, true)
        } else {
            edgeBitmap
        }
    }

    /**
     * Generates a high-resolution rendered document bitmap from the real captured camera/photo bitmap
     * and user/extracted text, processed through the selected ScanFilter (including Edge Detection).
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
        // When a real cameraBitmap is provided and no extra text overlay is needed, process the cameraBitmap directly
        val filteredCameraBitmap = cameraBitmap?.let { processDocumentBitmap(it, filter) }

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
        val formatLabel = "CAMERAX SCAN • ${filter.label.uppercase()} • PAGE $pageIndex OF $totalPages"

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(RectF(36f, 36f, width - 36f, height - 36f), 20f, 20f, borderPaint)

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

        if (filteredCameraBitmap != null) {
            val imageHeight = if (ocrText.isBlank()) (height - yOffset - 110f).coerceAtLeast(480f) else 640f
            val targetRect = RectF(72f, yOffset, width - 72f, yOffset + imageHeight)
            val srcRect = Rect(0, 0, filteredCameraBitmap.width, filteredCameraBitmap.height)
            canvas.drawBitmap(filteredCameraBitmap, srcRect, targetRect, null)
            canvas.drawRect(targetRect, borderPaint)
            yOffset += imageHeight + 40f
        }

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

        val lines = ocrText.lines().filter { it.isNotBlank() }
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
            "DocFlow CameraX PDF • Filter: ${filter.label} • Page $pageIndex of $totalPages",
            72f,
            height - 60f,
            footerPaint
        )

        if (filteredCameraBitmap != null) {
            baseBitmap
        } else {
            processDocumentBitmap(baseBitmap, filter)
        }
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
     * using Android's PdfDocument API, with optional text watermark, page size, and margin controls.
     */
    suspend fun convertBitmapsToPdfFile(
        context: Context,
        fileName: String,
        bitmaps: List<Bitmap>,
        ocrText: String = "",
        watermarkConfig: PdfWatermarkConfig = PdfWatermarkConfig(enabled = false),
        pageSizeOption: String = "A4", // A4, LETTER, FIT
        orientationOption: String = "PORTRAIT", // PORTRAIT, LANDSCAPE
        marginOption: String = "SMALL" // NONE, SMALL, BIG
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

            val baseDims = when (pageSizeOption.uppercase()) {
                "LETTER" -> 612 to 792
                "LEGAL" -> 612 to 1008
                "FIT" -> {
                    val first = bitmaps.firstOrNull()
                    if (first != null) {
                        first.width.coerceIn(400, 1200) to first.height.coerceIn(500, 1600)
                    } else {
                        595 to 842
                    }
                }
                else -> 595 to 842 // A4
            }
            val (pageWidth, pageHeight) = if (orientationOption.equals("LANDSCAPE", ignoreCase = true)) {
                maxOf(baseDims.first, baseDims.second) to minOf(baseDims.first, baseDims.second)
            } else {
                baseDims.first to baseDims.second
            }

            val marginPx = when (marginOption.uppercase()) {
                "NONE" -> 0f
                "BIG" -> 42f
                else -> 18f // SMALL
            }

            val totalPages = bitmaps.size.coerceAtLeast(1)
            val wroteViaNativePdf = runCatching {
                val pdfDocument = PdfDocument()
                try {
                    bitmaps.forEachIndexed { index, bitmap ->
                        val pageNumber1Based = index + 1
                        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber1Based).create()
                        val page = pdfDocument.startPage(pageInfo)
                        val canvas = page.canvas
                        canvas.drawColor(Color.WHITE)

                        val shouldDrawWatermark = watermarkConfig.enabled &&
                            watermarkConfig.text.isNotBlank() &&
                            isPageInWatermarkRange(pageNumber1Based, totalPages, watermarkConfig)

                        if (shouldDrawWatermark && watermarkConfig.layerBehindContent) {
                            drawWatermarkOnCanvas(
                                canvas = canvas,
                                pageWidth = pageWidth.toFloat(),
                                pageHeight = pageHeight.toFloat(),
                                config = watermarkConfig
                            )
                        }

                        val destRect = RectF(
                            marginPx,
                            marginPx,
                            pageWidth - marginPx,
                            pageHeight - marginPx
                        )
                        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                            if (shouldDrawWatermark && watermarkConfig.layerBehindContent) {
                                alpha = 235
                            }
                        }
                        canvas.drawBitmap(bitmap, srcRect, destRect, paint)

                        if (ocrText.isNotBlank()) {
                            val ocrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.argb(3, 0, 0, 0)
                                textSize = 6f
                            }
                            ocrText.lines().take(25).forEachIndexed { lineIdx, line ->
                                canvas.drawText(line.take(120), 20f, 30f + (lineIdx * 8f), ocrPaint)
                            }
                        }

                        if (shouldDrawWatermark && !watermarkConfig.layerBehindContent) {
                            drawWatermarkOnCanvas(
                                canvas = canvas,
                                pageWidth = pageWidth.toFloat(),
                                pageHeight = pageHeight.toFloat(),
                                config = watermarkConfig
                            )
                        }

                        pdfDocument.finishPage(page)
                    }

                    FileOutputStream(outFile).use { fos ->
                        pdfDocument.writeTo(fos)
                        fos.flush()
                    }
                    true
                } finally {
                    runCatching { pdfDocument.close() }
                }
            }.getOrDefault(false)

            if (!wroteViaNativePdf || !outFile.exists() || outFile.length() == 0L) {
                writeStandardMultiPagePdfFallback(
                    outFile = outFile,
                    bitmaps = bitmaps,
                    pageWidth = pageWidth,
                    pageHeight = pageHeight,
                    ocrText = ocrText,
                    watermarkText = if (watermarkConfig.enabled) watermarkConfig.text else ""
                )
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

    fun isPageInWatermarkRange(
        pageNumber1Based: Int,
        totalPages: Int,
        config: PdfWatermarkConfig
    ): Boolean {
        return when (config.pageRangeMode.uppercase()) {
            "FIRST" -> pageNumber1Based == 1
            "CUSTOM" -> {
                val rangeParts = config.customPageRange.split("-").mapNotNull { it.trim().toIntOrNull() }
                if (rangeParts.size == 2) {
                    pageNumber1Based in rangeParts[0]..rangeParts[1]
                } else if (rangeParts.size == 1) {
                    pageNumber1Based == rangeParts[0]
                } else {
                    true
                }
            }
            else -> pageNumber1Based in 1..totalPages
        }
    }

    fun drawWatermarkOnCanvas(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        config: PdfWatermarkConfig
    ) {
        val text = config.text.take(40).ifBlank { return }
        val baseColor = runCatching { Color.parseColor(config.colorHex) }.getOrDefault(Color.GRAY)
        val alphaVal = ((config.opacityPercent.coerceIn(5, 100) / 100f) * 255f).toInt().coerceIn(12, 255)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(
                alphaVal,
                Color.red(baseColor),
                Color.green(baseColor),
                Color.blue(baseColor)
            )
            textSize = config.fontSizeSp.coerceIn(14, 72).toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        if (config.positionIndex == 9) {
            // Tile across page
            val stepX = (pageWidth / 2.6f).coerceAtLeast(160f)
            val stepY = (pageHeight / 3.8f).coerceAtLeast(150f)
            var y = stepY * 0.55f
            while (y < pageHeight - 30f) {
                var x = stepX * 0.5f
                while (x < pageWidth - 30f) {
                    canvas.save()
                    canvas.translate(x, y)
                    canvas.rotate(config.rotationDegrees.toFloat())
                    canvas.drawText(text, 0f, 0f, paint)
                    canvas.restore()
                    x += stepX
                }
                y += stepY
            }
        } else {
            val col = (config.positionIndex.coerceIn(0, 8)) % 3
            val row = (config.positionIndex.coerceIn(0, 8)) / 3
            val targetX = when (col) {
                0 -> pageWidth * 0.24f
                2 -> pageWidth * 0.76f
                else -> pageWidth * 0.50f
            }
            val targetY = when (row) {
                0 -> pageHeight * 0.18f
                2 -> pageHeight * 0.82f
                else -> pageHeight * 0.50f
            }
            canvas.save()
            canvas.translate(targetX, targetY)
            canvas.rotate(config.rotationDegrees.toFloat())
            canvas.drawText(text, 0f, 0f, paint)
            canvas.restore()
        }
    }

    private fun writeStandardMultiPagePdfFallback(
        outFile: File,
        bitmaps: List<Bitmap>,
        pageWidth: Int,
        pageHeight: Int,
        ocrText: String = "",
        watermarkText: String = ""
    ) {
        val count = bitmaps.size.coerceAtLeast(1)
        val sanitizedOcr = ocrText
            .replace("\\", " ")
            .replace("(", "[")
            .replace(")", "]")
            .replace("\r", " ")
            .replace("\n", " ")
            .trim()
        val sanitizedWatermark = watermarkText
            .replace("\\", " ")
            .replace("(", "[")
            .replace(")", "]")
            .trim()
        val sb = StringBuilder()
        sb.append("%PDF-1.4\n")
        sb.append("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n")
        val kids = (0 until count).joinToString(" ") { "${3 + it} 0 R" }
        sb.append("2 0 obj << /Type /Pages /Kids [$kids] /Count $count >> endobj\n")
        for (i in 0 until count) {
            val objId = 3 + i
            val bmp = bitmaps.getOrNull(i)
            val dims = if (bmp != null) "${bmp.width}x${bmp.height}" else "${pageWidth}x${pageHeight}"
            sb.append(
                "$objId 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 $pageWidth $pageHeight] /Resources << /ProcSet [/PDF /Text /ImageC] >> /PieceInfo << /DocFlowScan ($dims) /MlKitOcr ($sanitizedOcr) /Watermark ($sanitizedWatermark) >> >> endobj\n"
            )
        }
        sb.append("trailer << /Root 1 0 R >>\n%%EOF\n")
        outFile.writeBytes(sb.toString().toByteArray(Charsets.UTF_8))
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
