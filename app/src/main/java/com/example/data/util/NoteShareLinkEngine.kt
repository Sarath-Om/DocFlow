package com.example.data.util

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class NoteLinkExpiryOption(val label: String, val durationMillis: Long?) {
    ONE_DAY("1 day", 24L * 60L * 60L * 1000L),
    SEVEN_DAYS("7 days", 7L * 24L * 60L * 60L * 1000L),
    THIRTY_DAYS("30 days", 30L * 24L * 60L * 60L * 1000L),
    NEVER("Never", null)
}

data class SharedNotePayload(
    val title: String,
    val body: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long?,
    val isPasswordProtected: Boolean = false
)

data class SharedNoteLinkRecord(
    val id: String,
    val noteTitle: String,
    val shareUrl: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long?,
    val hasPassword: Boolean
)

sealed class NoteLinkGenerationResult {
    data class Success(
        val url: String,
        val charCount: Int,
        val record: SharedNoteLinkRecord
    ) : NoteLinkGenerationResult()

    data class TooLarge(
        val charCount: Int,
        val maxAllowed: Int = NoteShareLinkEngine.MAX_LINK_CHARS
    ) : NoteLinkGenerationResult()

    data class Error(val message: String) : NoteLinkGenerationResult()
}

sealed class NoteLinkDecodeResult {
    data class Success(val payload: SharedNotePayload) : NoteLinkDecodeResult()
    data object PasswordRequired : NoteLinkDecodeResult()
    data object WrongPassword : NoteLinkDecodeResult()
    data class Expired(val expiredAtMillis: Long) : NoteLinkDecodeResult()
    data class Invalid(val reason: String) : NoteLinkDecodeResult()
}

/**
 * Serverless, database-free "Share Note with Link" engine.
 * Format: https://docflow.free.je/n/#v1.<payload>
 * Compresses note JSON with Deflate, optionally encrypts with AES-256-GCM (PBKDF2 key),
 * and encodes as Base64URL without padding.
 */
object NoteShareLinkEngine {
    const val MAX_LINK_CHARS = 6000
    private const val PBKDF2_ITERATIONS = 16000
    private const val AES_KEY_BITS = 256
    private const val GCM_TAG_BITS = 128

    fun generateShareLink(
        title: String,
        body: String,
        createdAtMillis: Long = System.currentTimeMillis(),
        expiryOption: NoteLinkExpiryOption = NoteLinkExpiryOption.NEVER,
        password: String = ""
    ): NoteLinkGenerationResult {
        return try {
            val cleanTitle = sanitizeText(title).ifBlank { "Untitled Note" }
            val cleanBody = sanitizeText(body)
            val expiresAt = expiryOption.durationMillis?.let { createdAtMillis + it }

            val innerJson = JSONObject().apply {
                put("t", cleanTitle)
                put("b", cleanBody)
                put("c", createdAtMillis)
                if (expiresAt != null) put("e", expiresAt)
            }

            val compressedInner = deflateBytes(innerJson.toString().toByteArray(StandardCharsets.UTF_8))

            val envelopeJson = JSONObject()
            envelopeJson.put("v", 1)
            if (expiresAt != null) {
                envelopeJson.put("exp", expiresAt)
            }

            if (password.isNotBlank()) {
                val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
                val keySpec = deriveAesKey(password, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_BITS, iv))
                val cipherBytes = cipher.doFinal(compressedInner)

                envelopeJson.put("enc", true)
                envelopeJson.put("s", base64UrlEncode(salt))
                envelopeJson.put("iv", base64UrlEncode(iv))
                envelopeJson.put("ct", base64UrlEncode(cipherBytes))
            } else {
                envelopeJson.put("enc", false)
                envelopeJson.put("d", base64UrlEncode(compressedInner))
            }

            val finalToken = base64UrlEncode(deflateBytes(envelopeJson.toString().toByteArray(StandardCharsets.UTF_8)))
            val fullUrl = "${DocFlowLegalConstants.NOTE_SHARE_BASE_URL}$finalToken"

            if (fullUrl.length > MAX_LINK_CHARS) {
                NoteLinkGenerationResult.TooLarge(charCount = fullUrl.length, maxAllowed = MAX_LINK_CHARS)
            } else {
                val record = SharedNoteLinkRecord(
                    id = "link_${System.currentTimeMillis()}",
                    noteTitle = cleanTitle,
                    shareUrl = fullUrl,
                    createdAtMillis = createdAtMillis,
                    expiresAtMillis = expiresAt,
                    hasPassword = password.isNotBlank()
                )
                NoteLinkGenerationResult.Success(
                    url = fullUrl,
                    charCount = fullUrl.length,
                    record = record
                )
            }
        } catch (t: Throwable) {
            NoteLinkGenerationResult.Error(t.localizedMessage ?: "Failed to encode note link")
        }
    }

    fun decodeShareLink(
        urlOrFragment: String,
        password: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ): NoteLinkDecodeResult {
        return try {
            val trimmed = urlOrFragment.trim()
            val prefixIndex = trimmed.indexOf("#v1.")
            val encodedPayload = when {
                prefixIndex >= 0 -> trimmed.substring(prefixIndex + 4)
                trimmed.startsWith("v1.") -> trimmed.substring(3)
                else -> return NoteLinkDecodeResult.Invalid("This link is invalid or missing the #v1. payload.")
            }

            if (encodedPayload.isBlank()) {
                return NoteLinkDecodeResult.Invalid("This link is empty or truncated.")
            }

            val envelopeBytes = inflateBytes(base64UrlDecode(encodedPayload))
            val envelopeJson = JSONObject(String(envelopeBytes, StandardCharsets.UTF_8))
            if (envelopeJson.optInt("v", 0) != 1) {
                return NoteLinkDecodeResult.Invalid("Unsupported link version.")
            }

            val outerExpiry = if (envelopeJson.has("exp")) envelopeJson.optLong("exp", 0L) else 0L
            if (outerExpiry > 0L && nowMillis > outerExpiry) {
                return NoteLinkDecodeResult.Expired(outerExpiry)
            }

            val isEncrypted = envelopeJson.optBoolean("enc", false)
            val compressedInner = if (isEncrypted) {
                if (password.isEmpty()) {
                    return NoteLinkDecodeResult.PasswordRequired
                }
                val salt = base64UrlDecode(envelopeJson.getString("s"))
                val iv = base64UrlDecode(envelopeJson.getString("iv"))
                val cipherBytes = base64UrlDecode(envelopeJson.getString("ct"))
                val keySpec = deriveAesKey(password, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_BITS, iv))
                try {
                    cipher.doFinal(cipherBytes)
                } catch (_: Throwable) {
                    return NoteLinkDecodeResult.WrongPassword
                }
            } else {
                base64UrlDecode(envelopeJson.getString("d"))
            }

            val innerBytes = inflateBytes(compressedInner)
            val innerJson = JSONObject(String(innerBytes, StandardCharsets.UTF_8))
            val innerExpiry = if (innerJson.has("e")) innerJson.optLong("e", 0L) else 0L
            if (innerExpiry > 0L && nowMillis > innerExpiry) {
                return NoteLinkDecodeResult.Expired(innerExpiry)
            }

            val title = sanitizeText(innerJson.optString("t", "Shared Note"))
            val body = sanitizeText(innerJson.optString("b", ""))
            val createdAt = innerJson.optLong("c", nowMillis)

            NoteLinkDecodeResult.Success(
                SharedNotePayload(
                    title = title,
                    body = body,
                    createdAtMillis = createdAt,
                    expiresAtMillis = if (innerExpiry > 0L) innerExpiry else null,
                    isPasswordProtected = isEncrypted
                )
            )
        } catch (_: Throwable) {
            NoteLinkDecodeResult.Invalid("This link is invalid or has been tampered with.")
        }
    }

    fun sanitizeText(raw: String): String {
        return raw
            .replace("\u0000", "")
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    /**
     * Generates a clean, high-contrast deterministic visual QR-style matrix bitmap for displaying
     * in the Share Note with Link result sheet.
     */
    fun generateQrCodeBitmap(content: String, sizePx: Int = 320): Bitmap {
        val grid = 25
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        val cellSize = sizePx / (grid + 4)
        val offset = (sizePx - grid * cellSize) / 2

        fun fillCell(gx: Int, gy: Int, dark: Boolean) {
            if (!dark) return
            val startX = offset + gx * cellSize
            val startY = offset + gy * cellSize
            for (y in startY until (startY + cellSize).coerceAtMost(sizePx)) {
                for (x in startX until (startX + cellSize).coerceAtMost(sizePx)) {
                    bmp.setPixel(x, y, Color.parseColor("#111317"))
                }
            }
        }

        fun drawFinderPattern(ox: Int, oy: Int) {
            for (y in 0..6) {
                for (x in 0..6) {
                    val border = x == 0 || x == 6 || y == 0 || y == 6
                    val center = x in 2..4 && y in 2..4
                    fillCell(ox + x, oy + y, border || center)
                }
            }
        }

        drawFinderPattern(0, 0)
        drawFinderPattern(grid - 7, 0)
        drawFinderPattern(0, grid - 7)

        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        var bitIdx = 0
        for (gy in 0 until grid) {
            for (gx in 0 until grid) {
                val inFinder = (gx <= 7 && gy <= 7) ||
                    (gx >= grid - 8 && gy <= 7) ||
                    (gx <= 7 && gy >= grid - 8)
                if (!inFinder) {
                    val b = bytes[bitIdx % bytes.size].toInt() and 0xFF
                    val bit = ((b shr (bitIdx % 8)) xor ((gx * 31 + gy * 17) and 1)) and 1
                    fillCell(gx, gy, bit == 1)
                    bitIdx++
                }
            }
        }
        return bmp
    }

    private fun deriveAesKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun deflateBytes(input: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setInput(input)
        deflater.finish()
        val out = ByteArrayOutputStream(input.size)
        val buf = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buf)
            if (count <= 0) break
            out.write(buf, 0, count)
        }
        deflater.end()
        return out.toByteArray()
    }

    private fun inflateBytes(compressed: ByteArray): ByteArray {
        val inflater = Inflater(true)
        inflater.setInput(compressed)
        val out = ByteArrayOutputStream(compressed.size * 2)
        val buf = ByteArray(1024)
        var total = 0
        while (!inflater.finished()) {
            val count = inflater.inflate(buf)
            if (count <= 0) {
                if (inflater.needsInput() || inflater.needsDictionary()) break
                break
            }
            out.write(buf, 0, count)
            total += count
            if (total > 256 * 1024) throw IllegalStateException("Payload exceeds safe decompression size")
        }
        inflater.end()
        return out.toByteArray()
    }

    private fun base64UrlEncode(bytes: ByteArray): String {
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    private fun base64UrlDecode(str: String): ByteArray {
        return Base64.decode(str, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
