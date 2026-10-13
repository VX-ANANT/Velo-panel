package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed class ImageUploadResult {
    data class Success(val url: String, val provider: String) : ImageUploadResult()
    data class Failure(val errorMessage: String, val fallbackBase64: String?) : ImageUploadResult()
}

object TournamentImageUploader {
    private const val TAG = "TournamentImageUploader"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Resolves an image model for Coil AsyncImage.
     * If the input is a base64 data URI (data:image/jpeg;base64,...), it decodes
     * it to a ByteArray which Coil natively decodes with zero overhead.
     */
    fun resolveBannerModel(rawUrl: String?): Any? {
        if (rawUrl.isNullOrBlank()) return null
        val trimmed = rawUrl.trim()
        if (trimmed.startsWith("data:image/") && trimmed.contains("base64,")) {
            val base64Data = trimmed.substringAfter("base64,").trim()
            return try {
                Base64.decode(base64Data, Base64.DEFAULT)
            } catch (e: Exception) {
                trimmed
            }
        }
        return trimmed
    }

    /**
     * Encodes a local Uri to a compact Base64 Data URI (e.g. for offline fallback).
     */
    fun encodeUriToBase64(context: Context, uri: Uri): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

            var sampleSize = 1
            val maxDim = 800
            while ((bounds.outWidth / sampleSize) > maxDim || (bounds.outHeight / sampleSize) > maxDim) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null
            val bos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 75, bos)
            val compressedBytes = bos.toByteArray()
            "data:image/jpeg;base64," + Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.w(TAG, "encodeUriToBase64 error: ${e.message}")
            null
        }
    }

    /**
     * Uploads an image from a local Uri through a multi-tier pipeline:
     * 1. Firebase Cloud Storage (primary)
     * 2. Direct Public Image Hosting CDN (Catbox / FreeImage fallback)
     * 3. Highly-compressed Base64 Data URI (guaranteed offline fallback)
     */
    suspend fun uploadTournamentBanner(
        context: Context,
        uri: Uri,
        onProgress: ((String) -> Unit)? = null
    ): ImageUploadResult = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke("Optimizing banner image...")
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext ImageUploadResult.Failure("Cannot read image data", null)

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)

            var sampleSize = 1
            val maxDim = 1280
            while ((bounds.outWidth / sampleSize) > maxDim || (bounds.outHeight / sampleSize) > maxDim) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val originalBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                ?: return@withContext ImageUploadResult.Failure("Could not decode image", null)

            uploadBitmapInternal(context, originalBitmap, onProgress)
        } catch (e: Exception) {
            Log.e(TAG, "uploadTournamentBanner failed: ${e.message}", e)
            ImageUploadResult.Failure(e.message ?: "Unknown error", null)
        }
    }

    /**
     * Uploads a Bitmap directly (e.g. from Camera TakePicturePreview).
     */
    suspend fun uploadTournamentBannerBitmap(
        context: Context,
        bitmap: Bitmap,
        onProgress: ((String) -> Unit)? = null
    ): ImageUploadResult = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke("Optimizing camera photo...")
            uploadBitmapInternal(context, bitmap, onProgress)
        } catch (e: Exception) {
            Log.e(TAG, "uploadTournamentBannerBitmap failed: ${e.message}", e)
            ImageUploadResult.Failure(e.message ?: "Unknown error", null)
        }
    }

    private suspend fun uploadBitmapInternal(
        context: Context,
        bitmap: Bitmap,
        onProgress: ((String) -> Unit)?
    ): ImageUploadResult = withContext(Dispatchers.IO) {
        // Step 1: Compress Bitmap to optimal JPEG for upload
        val targetWidth = 1200
        val targetHeight = if (bitmap.width > 0) {
            (1200f * bitmap.height / bitmap.width).toInt().coerceAtLeast(1)
        } else 675

        val scaled = if (bitmap.width > targetWidth) {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }

        val bos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, bos)
        val jpegBytes = bos.toByteArray()

        // Prepare compact fallback Base64 data URI
        val compactBmp = if (scaled.width > 640) {
            val compactH = (640f * scaled.height / scaled.width).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(scaled, 640, compactH, true)
        } else scaled
        val compactBos = ByteArrayOutputStream()
        compactBmp.compress(Bitmap.CompressFormat.JPEG, 70, compactBos)
        val compactBase64 = "data:image/jpeg;base64," + Base64.encodeToString(compactBos.toByteArray(), Base64.NO_WRAP)

        // Tier 1: Try Firebase Cloud Storage
        onProgress?.invoke("Uploading to Cloud Storage...")
        val firebaseUrl = tryUploadFirebaseStorage(jpegBytes)
        if (firebaseUrl != null) {
            Log.d(TAG, "Firebase Storage upload succeeded: $firebaseUrl")
            return@withContext ImageUploadResult.Success(firebaseUrl, "Firebase Storage")
        }

        // Tier 2: Try Direct Public Image Hosting CDN (Catbox.moe)
        onProgress?.invoke("Uploading to Cloud Image CDN...")
        val catboxUrl = tryUploadCatbox(jpegBytes)
        if (catboxUrl != null) {
            Log.d(TAG, "Catbox CDN upload succeeded: $catboxUrl")
            return@withContext ImageUploadResult.Success(catboxUrl, "Cloud CDN")
        }

        // Tier 2b: Try FreeImage CDN
        val freeImageUrl = tryUploadFreeImage(jpegBytes)
        if (freeImageUrl != null) {
            Log.d(TAG, "FreeImage CDN upload succeeded: $freeImageUrl")
            return@withContext ImageUploadResult.Success(freeImageUrl, "FreeImage CDN")
        }

        // Tier 3: Guaranteed local high-density Base64 Data URI
        onProgress?.invoke("Embedding image data...")
        Log.w(TAG, "Cloud hosts unavailable. Falling back to embedded Base64 data URI.")
        ImageUploadResult.Success(compactBase64, "Embedded High-Speed")
    }

    private suspend fun tryUploadFirebaseStorage(bytes: ByteArray): String? {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            try {
                auth.signInAnonymously().await()
            } catch (e: Exception) {
                Log.w(TAG, "Anonymous auth failed: ${e.message}")
            }
        }

        val fileName = "tournament_banners/${UUID.randomUUID()}.jpg"
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .setCacheControl("public,max-age=86400")
            .build()

        // List of bucket instances to try
        val storageInstances = listOf(
            try { FirebaseStorage.getInstance("gs://velorix-tournaments.firebasestorage.app") } catch (_: Exception) { null },
            try { FirebaseStorage.getInstance() } catch (_: Exception) { null },
            try { FirebaseStorage.getInstance("gs://velorix-tournaments.appspot.com") } catch (_: Exception) { null }
        ).filterNotNull()

        for (storage in storageInstances) {
            try {
                val ref = storage.reference.child(fileName)
                ref.putBytes(bytes, metadata).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                if (downloadUrl.isNotBlank() && (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://"))) {
                    return downloadUrl
                }
            } catch (e: Exception) {
                Log.w(TAG, "Storage bucket attempt error: ${e.message}")
            }
        }
        return null
    }

    private fun tryUploadCatbox(bytes: ByteArray): String? {
        return try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("reqtype", "fileupload")
                .addFormDataPart(
                    "fileToUpload",
                    "banner_${UUID.randomUUID().toString().take(8)}.jpg",
                    bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("https://catbox.moe/user/api.php")
                .header("User-Agent", "VelorixTournaments/2.0")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()?.trim()
                    if (!body.isNullOrBlank() && (body.startsWith("http://") || body.startsWith("https://"))) {
                        return body
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Catbox upload error: ${e.message}")
            null
        }
    }

    private fun tryUploadFreeImage(bytes: ByteArray): String? {
        return try {
            val base64Img = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", "6d207e02198a847aa98d0a2a901485a5")
                .addFormDataPart("action", "upload")
                .addFormDataPart("source", base64Img)
                .addFormDataPart("format", "json")
                .build()

            val request = Request.Builder()
                .url("https://freeimage.host/api/1/upload")
                .header("User-Agent", "VelorixTournaments/2.0")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val imageObj = json.optJSONObject("image")
                        val directUrl = imageObj?.optString("url")
                        if (!directUrl.isNullOrBlank() && directUrl.startsWith("http")) {
                            return directUrl
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "FreeImage upload error: ${e.message}")
            null
        }
    }
}
