package com.example.data.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val IV_SIZE = 16

    private fun deriveKey(password: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    fun encrypt(plainText: String, keyPhrase: String): String {
        if (keyPhrase.isBlank()) return plainText
        val keySpec = deriveKey(keyPhrase)
        val ivBytes = ByteArray(IV_SIZE).apply { SecureRandom().nextBytes(this) }
        val ivSpec = IvParameterSpec(ivBytes)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Prepend IV to ciphertext
        val combined = ByteArray(ivBytes.size + encryptedBytes.size)
        System.arraycopy(ivBytes, 0, combined, 0, ivBytes.size)
        System.arraycopy(encryptedBytes, 0, combined, ivBytes.size, encryptedBytes.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(cipherTextBase64: String, keyPhrase: String): Result<String> {
        return try {
            if (keyPhrase.isBlank()) return Result.success(cipherTextBase64)
            val combined = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            if (combined.size < IV_SIZE) {
                return Result.failure(IllegalArgumentException("Invalid encrypted payload"))
            }

            val ivBytes = ByteArray(IV_SIZE)
            val encryptedBytes = ByteArray(combined.size - IV_SIZE)
            System.arraycopy(combined, 0, ivBytes, 0, IV_SIZE)
            System.arraycopy(combined, IV_SIZE, encryptedBytes, 0, encryptedBytes.size)

            val keySpec = deriveKey(keyPhrase)
            val ivSpec = IvParameterSpec(ivBytes)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            Result.success(String(decryptedBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
