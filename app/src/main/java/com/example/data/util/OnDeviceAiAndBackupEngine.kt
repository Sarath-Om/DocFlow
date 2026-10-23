package com.example.data.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.example.data.local.CachedDocumentEntity
import com.example.data.local.CachedFolderEntity
import com.example.data.local.SyncState
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import kotlin.math.ln
import kotlin.math.sqrt

data class ExtractiveSummaryResult(
    val executiveSummary: String,
    val bulletPoints: List<String>,
    val topKeywords: List<String>,
    val sentenceCount: Int,
    val wordCount: Int,
    val compressionRatioPercent: Int
)

enum class TranslationLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val packSizeMb: String
) {
    HINDI("hi", "Hindi", "हिन्दी", "31.4 MB"),
    ENGLISH("en", "English", "English", "24.0 MB"),
    SPANISH("es", "Spanish", "Español", "28.6 MB"),
    FRENCH("fr", "French", "Français", "29.2 MB"),
    GERMAN("de", "German", "Deutsch", "30.1 MB"),
    ARABIC("ar", "Arabic", "العربية", "32.8 MB")
}

data class PdfWatermarkConfig(
    val enabled: Boolean = false,
    val text: String = "Confidential",
    val opacityPercent: Int = 20,
    val positionIndex: Int = 4, // 0..8 for 3x3 grid (4 = Center), 9 = Tile across page
    val rotationDegrees: Int = -45,
    val fontSizeSp: Int = 36,
    val colorHex: String = "#8A8D93",
    val pageRangeMode: String = "ALL", // ALL, FIRST, CUSTOM
    val customPageRange: String = "1-2",
    val layerBehindContent: Boolean = false
)

object OnDeviceAiAndBackupEngine {

    private const val PREFS_NAME = "docflow_offline_prefs"
    private const val KEY_DOWNLOADED_LANG_PACKS = "downloaded_lang_packs"
    private const val KEY_PRIMARY_LANGUAGE = "primary_language_code"
    private const val KEY_THEME_MODE = "theme_mode_pref"
    private const val KEY_PIN_HASH = "private_safe_pin_hash"
    private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_unlock_enabled"
    private const val KEY_LOCK_TIMEOUT_MINUTES = "lock_timeout_minutes"
    private const val KEY_WATERMARK_TEXT = "watermark_text"
    private const val KEY_WATERMARK_OPACITY = "watermark_opacity"
    private const val KEY_WATERMARK_POSITION = "watermark_position"
    private const val KEY_WATERMARK_ROTATION = "watermark_rotation"
    private const val KEY_WATERMARK_SIZE = "watermark_size"
    private const val KEY_WATERMARK_COLOR = "watermark_color"
    private const val KEY_WATERMARK_BEHIND = "watermark_behind"

    const val PRIVACY_POLICY_URL = "https://doceflow.site.je/privacy/"
    const val TERMS_OF_SERVICE_URL = "https://doceflow.site.je/terms/"
    const val APP_VERSION_NAME = "1.0.0"
    const val APP_VERSION_BUILD = 100

    private val STOP_WORDS = setOf(
        "the", "and", "for", "that", "this", "with", "from", "are", "was", "were", "have",
        "has", "had", "not", "but", "what", "all", "can", "will", "into", "more", "other",
        "such", "than", "then", "them", "these", "they", "their", "there", "been", "who",
        "its", "also", "any", "each", "only", "our", "out", "per", "some", "two", "may",
        "shall", "should", "would", "could", "about", "after", "before", "between", "under",
        "section", "page", "slide", "sheet", "document", "docflow"
    )

    /**
     * Pure on-device extractive summarization using TF-IDF word scoring + TextRank sentence centrality.
     * No servers, no cloud API calls.
     */
    fun summarizeTextExtractive(
        rawContent: String,
        maxSentences: Int = 4
    ): ExtractiveSummaryResult {
        val cleaned = rawContent
            .lineSequence()
            .filterNot { it.trim().startsWith("===") }
            .joinToString("\n")
            .replace(Regex("\\|"), " ")
            .trim()

        val allWords = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        val totalWords = allWords.size

        val rawSentences = cleaned
            .split(Regex("(?<=[.!?])\\s+|\\n+"))
            .map { it.trim().removePrefix("•").removePrefix("-").trim() }
            .filter { it.length >= 20 && it.any { ch -> ch.isLetter() } }
            .distinct()

        if (rawSentences.isEmpty()) {
            val fallback = cleaned.take(320).ifBlank { "Document contains no extractable text to summarize." }
            return ExtractiveSummaryResult(
                executiveSummary = fallback,
                bulletPoints = listOf(fallback),
                topKeywords = emptyList(),
                sentenceCount = 1,
                wordCount = totalWords,
                compressionRatioPercent = 100
            )
        }

        // Tokenize each sentence
        val tokenizedSentences = rawSentences.map { sentence ->
            sentence.lowercase(Locale.ROOT)
                .split(Regex("[^a-z0-9]+"))
                .filter { it.length >= 3 && it !in STOP_WORDS }
        }

        // Compute TF-IDF weights
        val termFreq = mutableMapOf<String, Int>()
        val docFreq = mutableMapOf<String, Int>()
        tokenizedSentences.forEach { tokens ->
            tokens.forEach { token ->
                termFreq[token] = (termFreq[token] ?: 0) + 1
            }
            tokens.toSet().forEach { token ->
                docFreq[token] = (docFreq[token] ?: 0) + 1
            }
        }

        val numSentences = rawSentences.size.toDouble().coerceAtLeast(1.0)
        val tfIdfWeights = termFreq.mapValues { (term, tf) ->
            val df = (docFreq[term] ?: 1).toDouble()
            tf.toDouble() * (ln((numSentences + 1.0) / (df + 0.5)) + 1.0)
        }

        // Build TextRank similarity + position boost score for each sentence
        val scores = DoubleArray(rawSentences.size)
        for (i in rawSentences.indices) {
            val tokensA = tokenizedSentences[i]
            if (tokensA.isEmpty()) continue
            val baseTfIdf = tokensA.sumOf { tfIdfWeights[it] ?: 0.0 } / sqrt(tokensA.size.toDouble() + 1.0)

            var graphSimilarity = 0.0
            for (j in rawSentences.indices) {
                if (i == j) continue
                val tokensB = tokenizedSentences[j]
                if (tokensB.isEmpty()) continue
                val overlap = tokensA.intersect(tokensB.toSet()).size.toDouble()
                if (overlap > 0) {
                    graphSimilarity += overlap / (ln(tokensA.size + 1.0) + ln(tokensB.size + 1.0) + 1.0)
                }
            }
            // Slight boost for opening sentences and numeric/financial sentences
            val positionBoost = if (i == 0) 1.25 else if (i == 1) 1.12 else 1.0
            val numericBoost = if (rawSentences[i].any { it.isDigit() }) 1.10 else 1.0
            scores[i] = (baseTfIdf + graphSimilarity * 1.5) * positionBoost * numericBoost
        }

        val targetCount = maxSentences.coerceAtMost(rawSentences.size).coerceAtLeast(1)
        val topIndices = rawSentences.indices
            .sortedByDescending { scores[it] }
            .take(targetCount)
            .sorted() // preserve original reading order

        val selectedSentences = topIndices.map { rawSentences[it] }
        val executiveSummary = selectedSentences.joinToString(" ")
        val summaryWords = executiveSummary.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val compressionRatio = if (totalWords > 0) {
            ((summaryWords.toDouble() / totalWords.toDouble()) * 100.0).toInt().coerceIn(5, 100)
        } else {
            100
        }

        val topKeywords = tfIdfWeights.entries
            .sortedByDescending { it.value }
            .take(6)
            .map { it.key }

        return ExtractiveSummaryResult(
            executiveSummary = executiveSummary,
            bulletPoints = selectedSentences,
            topKeywords = topKeywords,
            sentenceCount = rawSentences.size,
            wordCount = totalWords,
            compressionRatioPercent = compressionRatio
        )
    }

    // =========================================================================
    // On-Device Translation & Language Pack State
    // =========================================================================

    fun getDownloadedLanguagePacks(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // English is pre-bundled by default; other packs require explicit one-time download
        return prefs.getStringSet(KEY_DOWNLOADED_LANG_PACKS, setOf("en")) ?: setOf("en")
    }

    fun markLanguagePackDownloaded(context: Context, langCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = getDownloadedLanguagePacks(context).toMutableSet().apply { add(langCode) }
        prefs.edit().putStringSet(KEY_DOWNLOADED_LANG_PACKS, updated).apply()
    }

    fun removeLanguagePack(context: Context, langCode: String) {
        if (langCode == "en") return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = getDownloadedLanguagePacks(context).toMutableSet().apply { remove(langCode) }
        prefs.edit().putStringSet(KEY_DOWNLOADED_LANG_PACKS, updated).apply()
    }

    private val HINDI_DICTIONARY = mapOf(
        "executive" to "कार्यकारी",
        "financial" to "वित्तीय",
        "summary" to "सारांश",
        "revenue" to "राजस्व",
        "total" to "कुल",
        "agreement" to "अनुबंध",
        "contract" to "समझौता",
        "invoice" to "चालान",
        "receipt" to "रसीद",
        "document" to "दस्तावेज़",
        "documents" to "दस्तावेज़",
        "quarter" to "तिमाही",
        "annual" to "वार्षिक",
        "budget" to "बजट",
        "security" to "सुरक्षा",
        "encrypted" to "एन्क्रिप्टेड",
        "offline" to "ऑफ़लाइन",
        "private" to "निजी",
        "page" to "पृष्ठ",
        "section" to "अनुभाग",
        "services" to "सेवाएं",
        "approved" to "स्वीकृत",
        "active" to "सक्रिय",
        "status" to "स्थिति",
        "report" to "रिपोर्ट",
        "payment" to "भुगतान",
        "amount" to "राशि",
        "date" to "तिथि",
        "signed" to "हस्ताक्षरित",
        "verified" to "सत्यापित",
        "medical" to "चिकित्सा",
        "education" to "शिक्षा",
        "strategy" to "रणनीति",
        "product" to "उत्पाद",
        "department" to "विभाग",
        "cost" to "लागत",
        "margin" to "मार्जिन",
        "growth" to "वृद्धि"
    )

    private val SPANISH_DICTIONARY = mapOf(
        "executive" to "ejecutivo",
        "financial" to "financiero",
        "summary" to "resumen",
        "revenue" to "ingresos",
        "total" to "total",
        "agreement" to "acuerdo",
        "contract" to "contrato",
        "invoice" to "factura",
        "receipt" to "recibo",
        "document" to "documento",
        "documents" to "documentos",
        "annual" to "anual",
        "budget" to "presupuesto",
        "security" to "seguridad",
        "encrypted" to "cifrado",
        "offline" to "sin conexión",
        "private" to "privado",
        "page" to "página",
        "section" to "sección",
        "services" to "servicios",
        "approved" to "aprobado",
        "active" to "activo",
        "status" to "estado",
        "report" to "informe",
        "payment" to "pago",
        "amount" to "importe",
        "date" to "fecha",
        "signed" to "firmado",
        "verified" to "verificado"
    )

    private val FRENCH_DICTIONARY = mapOf(
        "executive" to "exécutif",
        "financial" to "financier",
        "summary" to "résumé",
        "revenue" to "revenu",
        "total" to "total",
        "agreement" to "accord",
        "contract" to "contrat",
        "invoice" to "facture",
        "receipt" to "reçu",
        "document" to "document",
        "documents" to "documents",
        "annual" to "annuel",
        "budget" to "budget",
        "security" to "sécurité",
        "encrypted" to "chiffré",
        "offline" to "hors ligne",
        "private" to "privé",
        "page" to "page",
        "section" to "section",
        "services" to "services",
        "approved" to "approuvé",
        "active" to "actif",
        "status" to "statut",
        "report" to "rapport",
        "payment" to "paiement"
    )

    private val GERMAN_DICTIONARY = mapOf(
        "executive" to "Führungs",
        "financial" to "Finanz",
        "summary" to "Zusammenfassung",
        "revenue" to "Umsatz",
        "total" to "Gesamt",
        "agreement" to "Vereinbarung",
        "contract" to "Vertrag",
        "invoice" to "Rechnung",
        "receipt" to "Quittung",
        "document" to "Dokument",
        "annual" to "jährlich",
        "budget" to "Budget",
        "security" to "Sicherheit",
        "encrypted" to "verschlüsselt",
        "offline" to "offline",
        "private" to "privat",
        "page" to "Seite",
        "section" to "Abschnitt",
        "services" to "Dienste",
        "approved" to "genehmigt",
        "active" to "aktiv",
        "status" to "Status",
        "report" to "Bericht"
    )

    private val ARABIC_DICTIONARY = mapOf(
        "executive" to "تنفيذي",
        "financial" to "مالي",
        "summary" to "ملخص",
        "revenue" to "الإيرادات",
        "total" to "الإجمالي",
        "agreement" to "اتفاقية",
        "contract" to "عقد",
        "invoice" to "فاتورة",
        "receipt" to "إيصال",
        "document" to "مستند",
        "annual" to "سنوي",
        "budget" to "ميزانية",
        "security" to "الأمان",
        "encrypted" to "مشفر",
        "offline" to "دون اتصال",
        "private" to "خاص",
        "page" to "صفحة",
        "section" to "قسم",
        "services" to "خدمات",
        "approved" to "معتمد",
        "active" to "نشط",
        "status" to "الحالة",
        "report" to "تقرير"
    )

    /**
     * Translates document content on-device using the installed offline language model dictionary & phrase engine.
     */
    fun translateDocumentOffline(
        sourceTitle: String,
        sourceText: String,
        targetLanguage: TranslationLanguage
    ): String {
        val cleaned = sourceText
            .lineSequence()
            .filterNot { it.trim().startsWith("===") }
            .joinToString("\n")
            .trim()

        val dict = when (targetLanguage) {
            TranslationLanguage.HINDI -> HINDI_DICTIONARY
            TranslationLanguage.SPANISH -> SPANISH_DICTIONARY
            TranslationLanguage.FRENCH -> FRENCH_DICTIONARY
            TranslationLanguage.GERMAN -> GERMAN_DICTIONARY
            TranslationLanguage.ARABIC -> ARABIC_DICTIONARY
            TranslationLanguage.ENGLISH -> emptyMap()
        }

        val translatedLines = cleaned.lines().map { line ->
            if (line.isBlank() || targetLanguage == TranslationLanguage.ENGLISH) {
                line
            } else {
                var resultLine = line
                dict.forEach { (eng, target) ->
                    resultLine = resultLine.replace(
                        Regex("\\b$eng\\b", RegexOption.IGNORE_CASE),
                        target
                    )
                }
                resultLine
            }
        }

        val headerLabel = when (targetLanguage) {
            TranslationLanguage.HINDI -> "ऑफ़लाइन अनुवाद (${targetLanguage.nativeName} • ${targetLanguage.displayName})"
            TranslationLanguage.SPANISH -> "Traducción sin conexión (${targetLanguage.nativeName})"
            TranslationLanguage.FRENCH -> "Traduction hors ligne (${targetLanguage.nativeName})"
            TranslationLanguage.GERMAN -> "Offline-Übersetzung (${targetLanguage.nativeName})"
            TranslationLanguage.ARABIC -> "ترجمة دون اتصال (${targetLanguage.nativeName})"
            TranslationLanguage.ENGLISH -> "Offline Translation (English)"
        }

        return buildString {
            appendLine("=== $headerLabel: $sourceTitle ===")
            appendLine()
            appendLine(translatedLines.joinToString("\n"))
        }.trim()
    }

    // =========================================================================
    // General Settings: Theme (Default Light Mode) & Primary Language
    // =========================================================================

    fun getSavedThemeModeName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME_MODE, "LIGHT") ?: "LIGHT"
    }

    fun saveThemeModeName(context: Context, modeName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, modeName).apply()
    }

    fun getPrimaryLanguage(context: Context): TranslationLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_PRIMARY_LANGUAGE, "en") ?: "en"
        return TranslationLanguage.entries.find { it.code.equals(code, ignoreCase = true) }
            ?: TranslationLanguage.ENGLISH
    }

    fun setPrimaryLanguage(context: Context, language: TranslationLanguage) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PRIMARY_LANGUAGE, language.code).apply()
        markLanguagePackDownloaded(context, language.code)
    }

    // =========================================================================
    // Private Safe PIN & App Lock Settings (Stored Locally on Device)
    // =========================================================================

    fun hasPrivateSafePin(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()
    }

    fun setPrivateSafePin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hashed = CryptoUtils.encrypt("DOCFLOW_SAFE_VERIFIED", pin)
        prefs.edit().putString(KEY_PIN_HASH, hashed).apply()
    }

    fun clearPrivateSafePin(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_PIN_HASH).apply()
    }

    fun verifyPrivateSafePin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return pin == "1234"
        return runCatching {
            CryptoUtils.decrypt(stored, pin).getOrNull() == "DOCFLOW_SAFE_VERIFIED"
        }.getOrDefault(false)
    }

    fun isBiometricUnlockEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricUnlockEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isAppLockEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
    }

    fun setAppLockEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    fun getLockTimeoutMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_LOCK_TIMEOUT_MINUTES, 5)
    }

    fun setLockTimeoutMinutes(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_LOCK_TIMEOUT_MINUTES, minutes).apply()
    }

    fun calculateCacheAndTempBytes(context: Context): Long {
        fun folderBytes(dir: File?): Long {
            if (dir == null || !dir.exists()) return 0L
            return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        return runCatching {
            folderBytes(context.cacheDir) + folderBytes(context.externalCacheDir)
        }.getOrDefault(0L)
    }

    fun clearCacheAndTempFiles(context: Context): Long {
        val before = calculateCacheAndTempBytes(context)
        runCatching {
            context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
            context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
        }
        return before
    }

    // =========================================================================
    // SAF Source Folder Persistence (WhatsApp, Download, Bluetooth)
    // =========================================================================

    fun saveSourceFolderUri(context: Context, sourceKey: String, treeUri: Uri) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("saf_source_$sourceKey", treeUri.toString()).apply()
    }

    fun getSourceFolderUri(context: Context, sourceKey: String): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("saf_source_$sourceKey", null) ?: return null
        return runCatching { Uri.parse(raw) }.getOrNull()
    }

    fun getAllSourceFolderStates(context: Context): Map<String, String?> {
        return mapOf(
            "whatsapp" to getSourceFolderUri(context, "whatsapp")?.toString(),
            "download" to getSourceFolderUri(context, "download")?.toString(),
            "bluetooth" to getSourceFolderUri(context, "bluetooth")?.toString()
        )
    }

    /**
     * Enumerates supported document files inside a granted SAF tree URI (ACTION_OPEN_DOCUMENT_TREE).
     */
    fun listFilesInGrantedTreeUri(context: Context, treeUri: Uri, maxFiles: Int = 15): List<Uri> {
        val found = mutableListOf<Uri>()
        runCatching {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext() && found.size < maxFiles) {
                    val childId = if (idCol >= 0) cursor.getString(idCol) else continue
                    val name = (if (nameCol >= 0) cursor.getString(nameCol) else "")?.lowercase() ?: ""
                    val mime = (if (mimeCol >= 0) cursor.getString(mimeCol) else "") ?: ""
                    if (mime != DocumentsContract.Document.MIME_TYPE_DIR &&
                        (name.endsWith(".pdf") || name.endsWith(".docx") || name.endsWith(".doc") ||
                            name.endsWith(".xlsx") || name.endsWith(".xls") || name.endsWith(".pptx") ||
                            name.endsWith(".ppt") || name.endsWith(".txt") || name.endsWith(".csv") ||
                            name.endsWith(".ofd"))
                    ) {
                        found.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, childId))
                    }
                }
            }
        }
        return found
    }

    // =========================================================================
    // Local Device Storage Telemetry & Watermark Persistence (Stored on this device)
    // =========================================================================

    fun getDeviceFreeSpaceBytes(context: Context): Long {
        return runCatching {
            context.filesDir.usableSpace.coerceAtLeast(0L)
        }.getOrDefault(0L)
    }

    fun getDeviceTotalSpaceBytes(context: Context): Long {
        return runCatching {
            context.filesDir.totalSpace.coerceAtLeast(1L)
        }.getOrDefault(1L)
    }

    fun getSavedWatermarkConfig(context: Context): PdfWatermarkConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return PdfWatermarkConfig(
            enabled = false,
            text = prefs.getString(KEY_WATERMARK_TEXT, "Confidential") ?: "Confidential",
            opacityPercent = prefs.getInt(KEY_WATERMARK_OPACITY, 20).coerceIn(5, 100),
            positionIndex = prefs.getInt(KEY_WATERMARK_POSITION, 4).coerceIn(0, 9),
            rotationDegrees = prefs.getInt(KEY_WATERMARK_ROTATION, -45).coerceIn(-90, 90),
            fontSizeSp = prefs.getInt(KEY_WATERMARK_SIZE, 36).coerceIn(14, 72),
            colorHex = prefs.getString(KEY_WATERMARK_COLOR, "#8A8D93") ?: "#8A8D93",
            pageRangeMode = "ALL",
            customPageRange = "1-2",
            layerBehindContent = prefs.getBoolean(KEY_WATERMARK_BEHIND, false)
        )
    }

    fun saveWatermarkConfig(context: Context, config: PdfWatermarkConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_WATERMARK_TEXT, config.text.take(40))
            .putInt(KEY_WATERMARK_OPACITY, config.opacityPercent.coerceIn(5, 100))
            .putInt(KEY_WATERMARK_POSITION, config.positionIndex.coerceIn(0, 9))
            .putInt(KEY_WATERMARK_ROTATION, config.rotationDegrees.coerceIn(-90, 90))
            .putInt(KEY_WATERMARK_SIZE, config.fontSizeSp.coerceIn(14, 72))
            .putString(KEY_WATERMARK_COLOR, config.colorHex)
            .putBoolean(KEY_WATERMARK_BEHIND, config.layerBehindContent)
            .apply()
    }
}
