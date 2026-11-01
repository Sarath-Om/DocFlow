package com.example.data.util

import android.content.Context
import android.net.Uri
import com.pdftron.common.PDFNetException
import com.pdftron.pdf.ColorPt
import com.pdftron.pdf.Font
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.PDFNet
import com.pdftron.pdf.PageSet
import com.pdftron.pdf.Stamper
import com.pdftron.sdf.SDFDoc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Position presets for Apryse SDK programmatic PDF watermarking.
 */
enum class ApryseWatermarkPosition(
    val label: String,
    val hAlign: Int,
    val vAlign: Int,
    val defaultRotation: Double
) {
    CENTER_DIAGONAL("Center (Diagonal)", Stamper.e_horizontal_center, Stamper.e_vertical_center, 45.0),
    CENTER_HORIZONTAL("Center (Horizontal)", Stamper.e_horizontal_center, Stamper.e_vertical_center, 0.0),
    TOP_CENTER("Top Center", Stamper.e_horizontal_center, Stamper.e_vertical_top, 0.0),
    BOTTOM_CENTER("Bottom Center", Stamper.e_horizontal_center, Stamper.e_vertical_bottom, 0.0),
    TOP_LEFT("Top Left", Stamper.e_horizontal_left, Stamper.e_vertical_top, 0.0),
    TOP_RIGHT("Top Right", Stamper.e_horizontal_right, Stamper.e_vertical_top, 0.0),
    BOTTOM_LEFT("Bottom Left", Stamper.e_horizontal_left, Stamper.e_vertical_bottom, 0.0),
    BOTTOM_RIGHT("Bottom Right", Stamper.e_horizontal_right, Stamper.e_vertical_bottom, 0.0)
}

/**
 * Configuration options for applying a text watermark programmatically via Apryse SDK.
 */
data class ApryseWatermarkOptions(
    val text: String = "CONFIDENTIAL",
    val opacityPercent: Int = 30, // 5..100
    val position: ApryseWatermarkPosition = ApryseWatermarkPosition.CENTER_DIAGONAL,
    val customRotationDegrees: Double? = null,
    val xOffsetPt: Double = 0.0,
    val yOffsetPt: Double = 0.0,
    val fontSizePt: Double = 42.0,
    val colorHex: String = "#E53935",
    val asBackground: Boolean = false,
    val startPage: Int = 1,
    val endPage: Int? = null
)

/**
 * Engine utilizing Apryse SDK (PDFNet Stamper API) to programmatically overlay
 * custom text watermarks with exact opacity, alignment, rotation, and offsets
 * on background threads.
 */
object ApryseWatermarkEngine {

    /**
     * Applies a text watermark programmatically to a PDF document using Apryse SDK on a background thread.
     *
     * @param context Android context for file / content access.
     * @param inputUri Uri pointing to the source PDF.
     * @param outputFile Destination file where the watermarked PDF will be stored.
     * @param options Watermark parameters (text, opacity, position, rotation, offset, color, etc.).
     * @return Result containing the watermarked File on success, or an Exception on failure.
     */
    suspend fun applyWatermark(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        options: ApryseWatermarkOptions
    ): Result<File> = withContext(Dispatchers.IO) {
        var tempInputFile: File? = null
        var pdfDoc: PDFDoc? = null
        var stamper: Stamper? = null
        var pageSet: PageSet? = null

        try {
            ensureApryseInitialized(context)

            // Resolve input URI into a readable local file
            val sourceFile = if (inputUri.scheme == "file" && inputUri.path != null) {
                File(inputUri.path!!)
            } else {
                tempInputFile = File.createTempFile("apryse_wm_in_", ".pdf", context.cacheDir)
                context.contentResolver.openInputStream(inputUri)?.use { input ->
                    FileOutputStream(tempInputFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalArgumentException("Could not open input stream from URI: $inputUri")
                tempInputFile
            }

            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                return@withContext Result.failure(IllegalStateException("Source PDF file does not exist or is empty"))
            }

            // Open document with Apryse PDFNet
            pdfDoc = PDFDoc(sourceFile.absolutePath)
            pdfDoc.initSecurityHandler()
            pdfDoc.lock()

            val pageCount = pdfDoc.pageCount
            if (pageCount < 1) {
                return@withContext Result.failure(IllegalStateException("PDF contains zero pages"))
            }

            // Configure Apryse Stamper with font size
            val fontSize = options.fontSizePt.coerceIn(12.0, 120.0)
            stamper = Stamper(Stamper.e_font_size, fontSize, fontSize)

            // Set alignment and position offsets
            stamper.setAlignment(options.position.hAlign, options.position.vAlign)
            stamper.setPosition(options.xOffsetPt, options.yOffsetPt)

            // Set opacity (0.05 to 1.0)
            val clampedOpacity = (options.opacityPercent.coerceIn(5, 100) / 100.0)
            stamper.setOpacity(clampedOpacity)

            // Set rotation
            val rotation = options.customRotationDegrees ?: options.position.defaultRotation
            stamper.setRotation(rotation)

            // Set text alignment
            stamper.setTextAlignment(Stamper.e_align_center)

            // Set font to Helvetica Bold
            val font = Font.create(pdfDoc.sdfDoc, Font.e_helvetica_bold)
            stamper.setFont(font)

            // Set font color
            val (r, g, b) = parseHexToRgb(options.colorHex)
            stamper.setFontColor(ColorPt(r, g, b))

            // Background vs foreground overlay
            stamper.setAsBackground(options.asBackground)
            stamper.showsOnScreen(true)
            stamper.showsOnPrint(true)

            // Determine target pages
            val start = options.startPage.coerceIn(1, pageCount)
            val end = (options.endPage ?: pageCount).coerceIn(start, pageCount)
            pageSet = PageSet(start, end, PageSet.e_all)

            // Programmatically stamp text onto pages
            val watermarkText = options.text.ifBlank { "CONFIDENTIAL" }
            stamper.stampText(pdfDoc, watermarkText, pageSet)

            // Save watermarked PDF
            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) {
                outputFile.delete()
            }
            pdfDoc.save(outputFile.absolutePath, SDFDoc.SaveMode.LINEARIZED, null)

            Result.success(outputFile)
        } catch (e: PDFNetException) {
            Result.failure(Exception("Apryse PDFNet error: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                pageSet?.destroy()
            } catch (_: Exception) {}
            try {
                stamper?.destroy()
            } catch (_: Exception) {}
            try {
                pdfDoc?.unlock()
                pdfDoc?.close()
            } catch (_: Exception) {}
            try {
                tempInputFile?.delete()
            } catch (_: Exception) {}
        }
    }

    private fun ensureApryseInitialized(context: Context) {
        try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_META_DATA
            )
            val metaLicenseKey = appInfo.metaData?.getString("pdftron_license_key") ?: ""
            PDFNet.initialize(metaLicenseKey)
        } catch (_: Exception) {
            // Already initialized or fallback
        }
    }

    private fun parseHexToRgb(hex: String): Triple<Double, Double, Double> {
        val clean = hex.removePrefix("#")
        return try {
            val colorInt = clean.toLong(16).toInt()
            val r = ((colorInt shr 16) and 0xFF) / 255.0
            val g = ((colorInt shr 8) and 0xFF) / 255.0
            val b = (colorInt and 0xFF) / 255.0
            Triple(r, g, b)
        } catch (_: Exception) {
            Triple(0.89, 0.22, 0.21) // fallback red
        }
    }
}
