package com.example.data.util

/**
 * Single source of truth for allow-listed legal URLs, policy version, bundled offline legal
 * documents, and the client-side note share link prefix.
 */
object DocFlowLegalConstants {
    const val PRIVACY_POLICY_URL = "https://docflow.free.je/privacy/"
    const val TERMS_OF_SERVICE_URL = "https://docflow.free.je/terms/"
    const val NOTE_SHARE_BASE_URL = "https://docflow.free.je/n/#v1."

    const val LEGAL_POLICY_VERSION = "2026-10-24"
    const val LEGAL_LAST_UPDATED_DATE = "October 24, 2026 (v2026-10-24)"

    private val ALLOW_LISTED_LEGAL_URLS = setOf(
        PRIVACY_POLICY_URL,
        TERMS_OF_SERVICE_URL
    )

    fun isAllowListedLegalUrl(url: String): Boolean {
        val normalized = url.trim()
        return ALLOW_LISTED_LEGAL_URLS.contains(normalized)
    }

    val OFFLINE_PRIVACY_POLICY_CONTENT = """
        DocFlow Privacy Policy
        Offline copy, last updated $LEGAL_LAST_UPDATED_DATE

        1. Data Stays on Your Device
        • All documents, scans, PDFs, Word (.docx), PowerPoint (.pptx), Excel (.xlsx) files, version snapshots, and notes created or imported in DocFlow are stored locally on your device.
        • No account data, document contents, or personal files are uploaded to any remote server or cloud database.

        2. Device Permissions We Use
        • Camera: Used exclusively when you open the Document Scanner to capture single or batch pages.
        • Microphone: Used exclusively by AI Mic while you actively tap and hold or start voice dictation. The microphone indicator is active only while listening, and listening stops immediately when you stop dictation, lock the screen, or background the app.
        • Storage / Folder Access: Used only for files and folders you explicitly pick or grant via the system file picker.

        3. Network & Offline Language Packs
        • DocFlow works offline by default.
        • Network access is used only when you explicitly download a one-time offline language model for translation/speech recognition or when you open these Privacy Policy / Terms of Service pages or a shared note link.

        4. Share Note with Link (#v1.<payload>)
        • When you share a note using "Share with link", the note content is compressed (and optionally encrypted with AES-256-GCM using your password) directly inside the URL fragment after the '#' symbol.
        • URL fragments are processed locally in the recipient's browser or DocFlow app and are never sent to or stored on any server.

        5. Zero Analytics or Tracking
        • DocFlow contains no third-party analytics SDKs, advertising trackers, or telemetry collectors.
    """.trimIndent()

    val OFFLINE_TERMS_OF_SERVICE_CONTENT = """
        DocFlow Terms of Service
        Offline copy, last updated $LEGAL_LAST_UPDATED_DATE

        1. Acceptance of Terms
        • By using DocFlow, you agree to these Terms of Service ($LEGAL_POLICY_VERSION). If our policy version changes in a future update, DocFlow will ask you to review and accept the updated terms on launch.

        2. Local-Only Document & Note Management
        • DocFlow provides on-device tools for scanning documents, converting files (Image to PDF, PDF to Word, Word to PDF, Office to PDF), editing documents, watermarking PDFs, and managing notes.
        • Because all files are stored on your device, you are responsible for maintaining your own local copies and device security (such as your Private Safe PIN).

        3. Share Note with Link Responsibility
        • Links generated via "Share with link" (https://docflow.free.je/n/#v1.<payload>) embed the note payload directly inside the link itself without a server.
        • Anyone who has the link (and password, if set) can read the note. Because no server stores the note, a shared link cannot be remotely revoked once shared.

        4. Document Conversion & OCR Notice
        • On-device conversions and OCR preserve your original text and images as closely as possible, though complex multi-column layouts or missing custom fonts may differ from the source file.

        5. No Warranty
        • DocFlow is provided "as is" for local productivity on your Android device without remote server dependencies.
    """.trimIndent()
}
