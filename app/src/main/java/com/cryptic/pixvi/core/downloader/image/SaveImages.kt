package com.cryptic.pixvi.core.downloader.image

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.toBitmap
import com.cryptic.pixvi.core.network.repo.PixivApiRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

suspend fun saveImages(
    context: Context,
    imageUrl: String,
    basePdfFolder: String? = null,
    fileName: String,
    repo: PixivApiRepo
): Result<Uri> = withContext(Dispatchers.IO) {

    val appContext = context.applicationContext
    val imageLoader = SingletonImageLoader.get(appContext)

    try {
        var extension = MimeTypeMap.getFileExtensionFromUrl(imageUrl).lowercase()
        if (extension.isEmpty()) extension = "jpg" // Fallback

        var mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/jpeg"

        // 2. The Provider: Logic to get the data as an InputStream
        val inputStreamProvider: () -> Pair<InputStream, String>? = {
            val memoryCached = imageLoader.memoryCache?.get(MemoryCache.Key(imageUrl))
            if (memoryCached != null) {
                val bitmap = memoryCached.image.toBitmap()
                val bos = ByteArrayOutputStream()

                // 1. Decide Format based on Extension
                val format = when (extension.lowercase()) {
                    "png" -> Bitmap.CompressFormat.PNG // Lossless, perfect quality
                    "webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS // API 30+
                    } else {
                        Bitmap.CompressFormat.WEBP // Older phones
                    }
                    "jpg", "jpeg" -> Bitmap.CompressFormat.JPEG // Lossy, but max quality
                    else -> Bitmap.CompressFormat.PNG // Safe fallback for unknown types
                }

                // 2. Compress with "100" to keep max detail
                // Note: For PNG, the 'quality' number is ignored (it's always 100%)
                bitmap.compress(format, 100, bos)

                Pair(ByteArrayInputStream(bos.toByteArray()), extension)
            } else {
                // B. Check Disk
                val diskSnapshot = imageLoader.diskCache?.openSnapshot(imageUrl)
                if (diskSnapshot != null) {
                    // Read bytes immediately and close the snapshot properly
                    diskSnapshot.use { snapshot ->
                        val bytes = snapshot.data.toFile().readBytes()
                        Pair(ByteArrayInputStream(bytes), extension)
                    }
                } else {
                    // C. Network via Retrofit Repo
                    val response = repo.loadImage(imageUrl).execute()
                    val body = response.body()
                    if (response.isSuccessful && body != null) {
                        // Update extension/mime based on REAL server response
                        val serverMime = body.contentType()?.toString()
                        val serverExt = MimeTypeMap.getSingleton().getExtensionFromMimeType(serverMime)
                        Pair(body.byteStream(), serverExt ?: extension)
                    } else {
                        null
                    }
                }
            }
        }

        // 3. Get the data and potentially update the filename extension
        val providerResult = inputStreamProvider() ?: throw Exception("Source not found")
        val finalStream = providerResult.first
        val finalExtension = providerResult.second
        val fullFileName = "$fileName.$finalExtension"
        val finalMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(finalExtension) ?: mimeType

        if(basePdfFolder.isNullOrEmpty()){
            // 4. Create MediaStore Entry
            val resolver = appContext.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fullFileName)
                put(MediaStore.Images.Media.MIME_TYPE, finalMimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Pixvi")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Failed to create MediaStore entry")

            // 5. Copying the data
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    finalStream.use { input ->
                        input.copyTo(outputStream)
                    }
                }

                // 6. Finalize MediaStore entry
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                Result.success(uri)

            } catch (e: Exception) {
                resolver.delete(uri, null, null) // Cleanup
                throw e
            }
        }else{
            return@withContext try {
                val directory = File(appContext.filesDir, basePdfFolder)
                //val innerDirection = File(directory, pdfSessionId)

                // Create the directory if it doesn't exist
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                val destinationFile = File(directory, fullFileName)

                finalStream.use { input ->
                    destinationFile.outputStream().use { outputStream ->
                        input.copyTo(outputStream)
                    }
                }
                Result.success(destinationFile.toUri())

            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    } catch (e: Exception) {
        Result.failure(e)
    }
}
