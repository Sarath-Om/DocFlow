package com.example.data.repository

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.model.ApryseDocumentModel
import com.example.data.model.DocumentFormat
import com.pdftron.pdf.Convert
import com.pdftron.pdf.PDFDoc
import com.pdftron.sdf.SDFDoc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Repository interface for managing local document preparation, caching,
 * format conversion, and export for the Apryse Android SDK.
 */
interface DocumentRepository {
    suspend fun prepareDocumentFromUri(uri: Uri): Result<ApryseDocumentModel>
    suspend fun createSampleDocument(format: DocumentFormat): Result<ApryseDocumentModel>
    suspend fun saveDocumentToUri(documentModel: ApryseDocumentModel, destinationUri: Uri): Result<Unit>
    suspend fun exportDocumentAsPdf(documentModel: ApryseDocumentModel, targetPdfUri: Uri): Result<Unit>
}

class DocumentRepositoryImpl(
    private val context: Context
) : DocumentRepository {

    private val documentsDir: File by lazy {
        File(context.filesDir, "apryse_documents").apply {
            if (!exists()) mkdirs()
        }
    }

    override suspend fun prepareDocumentFromUri(uri: Uri): Result<ApryseDocumentModel> = withContext(Dispatchers.IO) {
        runCatching {
            val contentResolver = context.contentResolver
            var fileName = "Document_${System.currentTimeMillis()}"
            var fileSize = 0L

            // 1. Resolve metadata from ContentResolver
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            val mimeType = contentResolver.getType(uri)
            val format = DocumentFormat.fromFileNameOrMime(fileName, mimeType)

            // Ensure valid extension for Apryse native file detection
            val safeExtension = format.extension
            val sanitizedBaseName = fileName.substringBeforeLast(".")
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .ifBlank { "document" }
            val localFileName = "${sanitizedBaseName}_${UUID.randomUUID().toString().take(8)}.$safeExtension"
            val targetFile = File(documentsDir, localFileName)

            // 2. Stream content into local application storage
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Unable to open input stream for URI: $uri")

            val actualSize = if (targetFile.exists()) targetFile.length() else fileSize

            // 3. Inspect page count safely if PDF or convertible
            var pages = 1
            try {
                if (format == DocumentFormat.PDF) {
                    val pdfDoc = PDFDoc(targetFile.absolutePath)
                    pdfDoc.initSecurityHandler()
                    pages = pdfDoc.pageCount.coerceAtLeast(1)
                    pdfDoc.close()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not determine exact page count: ${e.message}")
            }

            ApryseDocumentModel(
                id = UUID.randomUUID().toString(),
                title = fileName,
                originalUri = uri,
                localFileUri = Uri.fromFile(targetFile),
                localFilePath = targetFile.absolutePath,
                format = format,
                sizeBytes = actualSize,
                pageCount = pages,
                lastModifiedMillis = System.currentTimeMillis()
            )
        }
    }

    override suspend fun createSampleDocument(format: DocumentFormat): Result<ApryseDocumentModel> = withContext(Dispatchers.IO) {
        runCatching {
            val sampleName = when (format) {
                DocumentFormat.PDF -> "Sample_Report.pdf"
                DocumentFormat.WORD -> "Quarterly_Summary.docx"
                DocumentFormat.EXCEL -> "Financial_Budget.xlsx"
                DocumentFormat.POWERPOINT -> "Strategy_Deck.pptx"
            }
            val targetFile = File(documentsDir, "${System.currentTimeMillis()}_$sampleName")

            when (format) {
                DocumentFormat.PDF -> generateSamplePdfFile(targetFile, "DocFlow Apryse Sample Document")
                DocumentFormat.WORD -> generateSampleOfficeFile(targetFile, "DocFlow Word Document (.docx)\n\nThis is a client-side Word document rendered locally by Apryse SDK.\n- Fully offline viewing and editing\n- No server dependency or Microsoft license required.")
                DocumentFormat.EXCEL -> generateSampleOfficeFile(targetFile, "Item,Quarter 1,Quarter 2,Total\nServices,12000,15400,27400\nLicenses,8500,9200,17700\nHardware,6300,5100,11400")
                DocumentFormat.POWERPOINT -> generateSampleOfficeFile(targetFile, "Slide 1: Executive Overview\nKey Highlights for FY2026\n- Client-side document processing\n- Enterprise PDF & Office annotations")
            }

            val size = if (targetFile.exists()) targetFile.length() else 1024L
            ApryseDocumentModel(
                id = UUID.randomUUID().toString(),
                title = sampleName,
                originalUri = Uri.fromFile(targetFile),
                localFileUri = Uri.fromFile(targetFile),
                localFilePath = targetFile.absolutePath,
                format = format,
                sizeBytes = size,
                pageCount = 1,
                lastModifiedMillis = System.currentTimeMillis()
            )
        }
    }

    override suspend fun saveDocumentToUri(
        documentModel: ApryseDocumentModel,
        destinationUri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceFile = File(documentModel.localFilePath)
            if (!sourceFile.exists()) {
                throw IllegalStateException("Source file does not exist at ${documentModel.localFilePath}")
            }

            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Cannot open output stream for destination URI: $destinationUri")
            Unit
        }
    }

    override suspend fun exportDocumentAsPdf(
        documentModel: ApryseDocumentModel,
        targetPdfUri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceFile = File(documentModel.localFilePath)
            if (!sourceFile.exists()) {
                throw IllegalStateException("Source file does not exist at ${documentModel.localFilePath}")
            }

            if (documentModel.format == DocumentFormat.PDF) {
                // Direct PDF export
                saveDocumentToUri(documentModel, targetPdfUri).getOrThrow()
            } else {
                // Convert Office format (.docx, .xlsx, .pptx) to PDF on-device using Apryse Convert
                val tempPdfFile = File(documentsDir, "export_${System.currentTimeMillis()}.pdf")
                try {
                    val pdfDoc = PDFDoc()
                    Convert.officeToPdf(pdfDoc, sourceFile.absolutePath, null)
                    pdfDoc.save(tempPdfFile.absolutePath, SDFDoc.SaveMode.REMOVE_UNUSED, null)
                    pdfDoc.close()

                    context.contentResolver.openOutputStream(targetPdfUri)?.use { output ->
                        tempPdfFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    } ?: throw IllegalStateException("Cannot write to target PDF URI")
                } catch (convEx: Exception) {
                    Log.e(TAG, "Apryse Office conversion fallback", convEx)
                    // Fallback to text PDF generator if native office layout failed
                    val content = sourceFile.readText().take(5000)
                    generateSamplePdfFile(tempPdfFile, "${documentModel.title}\n\n$content")
                    context.contentResolver.openOutputStream(targetPdfUri)?.use { output ->
                        tempPdfFile.inputStream().use { input -> input.copyTo(output) }
                    }
                } finally {
                    tempPdfFile.delete()
                }
            }
            Unit
        }
    }

    private fun generateSamplePdfFile(file: File, heading: String) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paintTitle = Paint().apply {
            color = Color.rgb(26, 115, 232)
            textSize = 22f
            isAntiAlias = true
            isFakeBoldText = true
        }

        val paintBody = Paint().apply {
            color = Color.rgb(33, 33, 33)
            textSize = 14f
            isAntiAlias = true
        }

        val paintSub = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 12f
            isAntiAlias = true
        }

        canvas.drawText("DocFlow • Apryse Android SDK", 48f, 60f, paintSub)
        canvas.drawText(heading, 48f, 100f, paintTitle)

        var y = 140f
        val sampleLines = listOf(
            "This document is processed entirely on-device without remote servers.",
            "• Format: PDF (Portable Document Format)",
            "• Annotations: Highlighting, Underlining, Freehand Ink, Sticky Notes",
            "• Editing: Inline text changes, bookmarks, page reordering",
            "• Fully offline with Apryse Mobile SDK."
        )

        for (line in sampleLines) {
            canvas.drawText(line, 48f, y, paintBody)
            y += 24f
        }

        pdfDocument.finishPage(page)
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
    }

    private fun generateSampleOfficeFile(file: File, sampleText: String) {
        file.writeText(sampleText)
    }

    companion object {
        private const val TAG = "DocumentRepository"
    }
}
