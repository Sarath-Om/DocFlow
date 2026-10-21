package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

data class ParsedFileResult(
    val title: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val pageCount: Long,
    val extractedContent: String
)

object FileParserUtils {

    suspend fun parseDocumentUri(context: Context, uri: Uri): Result<ParsedFileResult> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var fileName = "document"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: "document"
                    }
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            val mimeType = contentResolver.getType(uri) ?: ""
            val lowerName = fileName.lowercase()
            val fileType = when {
                lowerName.endsWith(".pdf") || mimeType.contains("pdf") -> "pdf"
                lowerName.endsWith(".docx") || mimeType.contains("wordprocessingml") -> "docx"
                lowerName.endsWith(".doc") || mimeType.contains("msword") -> "doc"
                else -> "txt"
            }

            val title = fileName.substringBeforeLast(".")
                .replace("_", " ")
                .replace("-", " ")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                .ifBlank { "Imported Document" }

            var pageCount = 1L
            val extractedContent: String

            when (fileType) {
                "pdf" -> {
                    var pages = 1
                    try {
                        contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            PdfRenderer(pfd).use { renderer ->
                                pages = renderer.pageCount.coerceAtLeast(1)
                            }
                        }
                    } catch (_: Exception) {
                        pages = 1
                    }
                    pageCount = pages.toLong()
                    extractedContent = extractTextFromPdfStream(context, uri, fileName, pageCount)
                }
                "docx" -> {
                    val docxText = extractTextFromDocx(context, uri)
                    extractedContent = docxText.ifBlank {
                        "Word Document ($fileName)\n\nImported from local storage. You can edit and synchronize this document across all connected devices."
                    }
                    pageCount = ((extractedContent.length / 1800) + 1).toLong().coerceAtLeast(1L)
                }
                else -> {
                    val rawText = readPlainText(context, uri)
                    extractedContent = rawText.ifBlank {
                        "Document ($fileName)\n\nImported for cross-device viewing and editing."
                    }
                    pageCount = ((extractedContent.length / 1800) + 1).toLong().coerceAtLeast(1L)
                }
            }

            if (fileSize <= 0L) {
                fileSize = extractedContent.toByteArray().size.toLong().coerceAtLeast(512L)
            }

            Result.success(
                ParsedFileResult(
                    title = title.take(190),
                    fileName = fileName.take(190),
                    fileType = fileType,
                    fileSize = fileSize.coerceAtMost(49_000_000L),
                    pageCount = pageCount,
                    extractedContent = extractedContent.take(4_000_000)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractTextFromDocx(context: Context, uri: Uri): String {
        val sb = StringBuilder()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "word/document.xml") {
                            val parser = Xml.newPullParser()
                            parser.setInput(zis, "UTF-8")
                            var eventType = parser.eventType
                            while (eventType != XmlPullParser.END_DOCUMENT) {
                                if (eventType == XmlPullParser.START_TAG) {
                                    when (parser.name) {
                                        "t" -> {
                                            val text = parser.nextText()
                                            if (text.isNotEmpty()) {
                                                sb.append(text)
                                            }
                                        }
                                        "tab" -> sb.append("\t")
                                        "br", "cr" -> sb.append("\n")
                                    }
                                } else if (eventType == XmlPullParser.END_TAG && parser.name == "p") {
                                    sb.append("\n\n")
                                }
                                eventType = parser.next()
                            }
                            break
                        }
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (_: Exception) {
        }
        return sb.toString().trim()
    }

    private fun extractTextFromPdfStream(
        context: Context,
        uri: Uri,
        fileName: String,
        pageCount: Long
    ): String {
        val sb = StringBuilder()
        sb.append("=== PDF DOCUMENT: $fileName ===\n")
        sb.append("Pages Detected: $pageCount\n\n")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                val raw = String(bytes, Charsets.ISO_8859_1)
                // Extract readable parenthesized PDF text tokens if present in uncompressed streams
                val regex = Regex("\\(([^()\\\\]{3,120})\\)")
                val matches = regex.findAll(raw)
                    .map { it.groupValues[1] }
                    .filter { token -> token.all { it.code in 32..126 } && token.any { it.isLetter() } }
                    .take(150)
                    .toList()
                if (matches.isNotEmpty()) {
                    sb.append(matches.joinToString(" "))
                    sb.append("\n\n")
                }
            }
        } catch (_: Exception) {
        }
        sb.append("Document ready for viewing, annotation, and instant cloud synchronization across your devices.")
        return sb.toString()
    }

    private fun readPlainText(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun renderPdfPage(context: Context, uri: Uri, pageIndex: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex in 0 until renderer.pageCount) {
                        renderer.openPage(pageIndex).use { page ->
                            val width = (page.width * 2).coerceIn(400, 1600)
                            val height = (page.height * 2).coerceIn(500, 2200)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            return@withContext bitmap
                        }
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
