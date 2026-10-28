package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class PhotoStorageRepository(
    private val storage: FirebaseStorage = Firebase.storage
) {
    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before uploading photos.")
    }

    /**
     * Compresses the selected image [uri] into WebP format locally and uploads it to Firebase Storage,
     * returning only the downloadUrl string to be stored in Firestore.
     * Never encodes or stores Base64 strings.
     */
    suspend fun compressAndUploadWebpPhoto(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = 1080,
        webpQuality: Int = 80
    ): Result<String> = withContext(Dispatchers.IO) {
        val webpBytes = compressUriToWebpBytes(context, uri, maxDimensionPx, webpQuality)
            ?: return@withContext Result.failure(IllegalArgumentException("Unable to decode or compress image to WebP"))

        val fileName = "photo_${UUID.randomUUID()}.webp"
        val uid = auth.currentUser?.uid

        // If user skipped Google Sign-In, save WebP file locally and return file:// URI (never Base64)
        if (uid.isNullOrBlank()) {
            return@withContext try {
                val localDir = File(context.filesDir, "webp_photos/local_guest").apply { mkdirs() }
                val localFile = File(localDir, fileName)
                localFile.writeBytes(webpBytes)
                Result.success(Uri.fromFile(localFile).toString())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val storageRef = storage.reference
            .child("users")
            .child(uid)
            .child("photos")
            .child(fileName)

        val metadata = StorageMetadata.Builder()
            .setContentType("image/webp")
            .build()

        return@withContext try {
            storageRef.putBytes(webpBytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Storage upload failed, saving WebP file URI locally", e)
            try {
                val localDir = File(context.filesDir, "webp_photos/$uid").apply { mkdirs() }
                val localFile = File(localDir, fileName)
                localFile.writeBytes(webpBytes)
                Result.success(Uri.fromFile(localFile).toString())
            } catch (fallbackEx: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun compressUriToWebpBytes(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int,
        quality: Int
    ): ByteArray? {
        return try {
            val originalBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            val width = originalBitmap.width.coerceAtLeast(1)
            val height = originalBitmap.height.coerceAtLeast(1)
            val scale = minOf(maxDimensionPx.toFloat() / width, maxDimensionPx.toFloat() / height, 1f)
            val targetWidth = (width * scale).toInt().coerceAtLeast(1)
            val targetHeight = (height * scale).toInt().coerceAtLeast(1)

            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            scaledBitmap.compress(format, quality.coerceIn(10, 100), outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress image to WebP", e)
            null
        }
    }

    private companion object {
        const val TAG = "PhotoStorageRepo"
    }
}
