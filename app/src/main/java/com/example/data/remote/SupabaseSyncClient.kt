package com.example.data.remote

import com.example.BuildConfig
import com.example.data.local.CachedDocumentEntity
import com.example.data.local.CachedFolderEntity
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Real Supabase PostgREST client for uploading & reconciling pending offline documents and folders.
 * Uses `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_ANON_KEY` configured via AI Studio Secrets.
 */
class SupabaseSyncClient(
    private val baseUrl: String = BuildConfig.SUPABASE_URL.trim().trimEnd('/'),
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY.trim(),
    private val httpClient: OkHttpClient = defaultHttpClient
) {

    fun isConfigured(): Boolean {
        return baseUrl.isNotBlank() &&
            anonKey.isNotBlank() &&
            baseUrl.startsWith("http") &&
            !baseUrl.contains("your-project-id.supabase.co", ignoreCase = true) &&
            !anonKey.contains("YOUR_SUPABASE_ANON_KEY", ignoreCase = true)
    }

    suspend fun upsertDocument(doc: CachedDocumentEntity): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Supabase credentials not configured in Secrets panel")
            )
        }

        runCatching {
            val payload = JSONObject().apply {
                put("id", doc.id)
                put("user_id", doc.userId)
                put("title", doc.title)
                put("folder_id", doc.folderId)
                put("category", doc.category)
                put("file_type", doc.fileType)
                put("file_name", doc.fileName)
                put("file_size", doc.fileSize)
                put("content", doc.content)
                put("content_preview", doc.contentPreview)
                put("tags", doc.tags)
                put("is_encrypted", doc.isEncrypted)
                put("encryption_key_hint", doc.encryptionKeyHint ?: JSONObject.NULL)
                put("is_offline_cached", doc.isOfflineCached)
                put("page_count", doc.pageCount)
                put("updated_at_millis", doc.updatedAtMillis)
                put("last_accessed_at_millis", doc.lastAccessedAtMillis)
            }

            val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/documents?on_conflict=id")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Supabase PostgREST HTTP ${response.code}: ${response.message}")
                }
                "Supabase PostgREST upserted document '${doc.title}' (${response.code})"
            }
        }
    }

    suspend fun deleteDocument(docId: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Supabase credentials not configured in Secrets panel")
            )
        }

        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/documents?id=eq.$docId")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .delete()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Supabase PostgREST DELETE HTTP ${response.code}: ${response.message}")
                }
                "Supabase PostgREST deleted document $docId (${response.code})"
            }
        }
    }

    suspend fun upsertFolder(folder: CachedFolderEntity): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Supabase credentials not configured in Secrets panel")
            )
        }

        runCatching {
            val payload = JSONObject().apply {
                put("id", folder.id)
                put("user_id", folder.userId)
                put("name", folder.name)
                put("color", folder.color)
                put("icon", folder.icon)
                put("updated_at_millis", System.currentTimeMillis())
            }

            val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/folders?on_conflict=id")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Supabase PostgREST Folder HTTP ${response.code}: ${response.message}")
                }
                "Supabase PostgREST upserted folder '${folder.name}' (${response.code})"
            }
        }
    }

    suspend fun deleteFolder(folderId: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Supabase credentials not configured in Secrets panel")
            )
        }

        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/folders?id=eq.$folderId")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .delete()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Supabase PostgREST Folder DELETE HTTP ${response.code}: ${response.message}")
                }
                "Supabase PostgREST deleted folder $folderId (${response.code})"
            }
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private val defaultHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()
        }
    }
}
