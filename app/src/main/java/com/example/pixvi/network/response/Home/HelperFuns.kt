package com.example.pixvi.network.response.Home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.example.pixvi.utils.NormalImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

enum class SaveDestination(val displayName: String) { Device("Device"), Clipboard("Clipboard") }

enum class SaveAllFormat(val displayName: String) { Image("Image"), Pdf("PDF") } // Keep if needed for Save All

object ImageUtils {

    /**
     * Saves a Bitmap to the device's MediaStore (Pictures directory).
     * Must be called from a CoroutineScope, preferably with Dispatchers.IO.
     *
     * @param context Context
     * @param bitmap The Bitmap to save.
     * @param illustId Illustration ID for filename.
     * @param pageIndex Page index (0-based) for filename.
     * @param displayName Optional display name (title) for filename.
     * @return The Uri of the saved image, or null on failure.
     */
    suspend fun saveBitmapToMediaStore(
        context: Context,
        bitmap: Bitmap,
        illustId: Int,
        pageIndex: Int,
        displayName: String? = null
    ): Uri? {
        val safeDisplayName = displayName?.replace(Regex("[\\\\/:*?\"<>|]"), "_") ?: "image"
        val filename = "pixiv_${illustId}_p${pageIndex + 1}_${safeDisplayName}.jpg"
        val mimeType = "image/jpeg"
        val directory = Environment.DIRECTORY_PICTURES // Standard directory

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$directory/PixivDownloads") // Subfolder
                put(MediaStore.MediaColumns.IS_PENDING, 1) // Mark as pending
            }
        }

        val resolver = context.contentResolver
        var uri: Uri? = null

        try {
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri == null) {
                throw IOException("Failed to create new MediaStore record.")
            }

            resolver.openOutputStream(uri)?.use { outputStream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                    throw IOException("Failed to save bitmap.")
                }
            } ?: throw IOException("Failed to get output stream.")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0) // Mark as not pending
                resolver.update(uri, contentValues, null, null)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Image saved to Pictures/PixivDownloads", Toast.LENGTH_SHORT).show()
            }
            return uri

        } catch (e: Exception) { // Catch specific exceptions later if needed
            // Cleanup: If URI was created but save failed, delete the pending entry
            uri?.let { orphanUri ->
                try {
                    resolver.delete(orphanUri, null, null)
                } catch (deleteEx: Exception) {
                    // Log delete error if necessary
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to save image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            return null
        }
    }

    /**
     * Saves bitmap temporarily to cache and copies a content URI (via FileProvider)
     * to the clipboard. Does NOT save permanently to MediaStore.
     * Must be called from a CoroutineScope, preferably with Dispatchers.IO.
     */
    suspend fun copyBitmapToClipboardViaCache(
        context: Context,
        bitmap: Bitmap,
        illustId: Int, // Used for temp filename uniqueness
        pageIndex: Int
    ) {
        var tempFileUri: Uri? = null
        var tempFile: File? = null

        try {
            // 1. Define target directory and file in cache
            // Ensure this path matches what's in file_paths.xml (<cache-path path="images/")
            val cacheImageDir = File(context.cacheDir, "images")
            cacheImageDir.mkdirs() // Create directory if it doesn't exist
            tempFile = File(cacheImageDir, "pixiv_${illustId}_p${pageIndex}_temp_copy.jpg")

            // 2. Save bitmap to the cache file
            withContext(Dispatchers.IO) {
                FileOutputStream(tempFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
            }

            // 3. Get content URI using FileProvider
            // Authority must match AndroidManifest.xml
            val authority = "${context.packageName}.fileprovider"
            tempFileUri = FileProvider.getUriForFile(context, authority, tempFile)

            // 4. Copy URI to Clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newUri(context.contentResolver, "Image", tempFileUri)
            // Grant read permission to receiving apps
            clip.description.mimeTypeCount > 0 // Ensure mime type if needed? Generally okay.

            clipboard.setPrimaryClip(clip)

            //annoying
            /*withContext(Dispatchers.Main) {
                Toast.makeText(context, "Image copied to clipboard", Toast.LENGTH_SHORT).show()
            }*/

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to copy image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            // Clean up temp file if created but failed later? Optional.
            // tempFile?.delete()

        } finally {
            // Optional: Consider deleting the temp file immediately after copying
            // if you are SURE no other app needs prolonged access.
            // However, clipboard content might be cached differently by apps,
            // so leaving the file temporarily might be safer for compatibility.
            // The system will eventually clean up the cache dir.
            // tempFile?.delete() // Uncomment cautiously
        }
    }


    /**
     * Loads an image URL as a Bitmap using Coil.
     * Handles showing Toast messages for loading status.
     * Must be called from a CoroutineScope.
     *
     * @param context Context
     * @param imageUrl The URL of the image to load.
     * @param headers Optional headers for the request.
     * @return The loaded Bitmap, or null on failure.
     */
    suspend fun loadBitmapFromUrl(
        context: Context,
        imageUrl: String?,
    ): Bitmap? {
        if (imageUrl.isNullOrBlank()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Invalid image URL", Toast.LENGTH_SHORT).show()
            }
            return null
        }

        val request = NormalImageRequest.normalImageRequest(context, imageUrl) { builder ->
            // Add the specific configuration this function needs
            builder.allowHardware(false)
        }

        return when (val result = context.imageLoader.execute(request)) {
            is SuccessResult -> {
                withContext(Dispatchers.Main) {
                    //  Toast.makeText(context, "Image loaded", Toast.LENGTH_SHORT).show() // Optional success indicator
                }
                // Ensure we get a mutable bitmap if needed, though toBitmap() often suffices
                result.image.toBitmap()
            }
            is ErrorResult -> {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to load image: ${result.throwable.localizedMessage}", Toast.LENGTH_LONG).show()
                }
                null
            }
        }
    }
}


// No changes needed here, but adding comments for clarity based on API 31+ target
suspend fun saveBitmapToMediaStore(
    context: Context,
    bitmap: Bitmap,
    illustId: Int,
    pageIndex: Int,
    displayName: String? = null
): Uri? {
    val safeDisplayName = displayName?.replace(Regex("[\\\\/:*?\"<>|]"), "_") ?: "image"
    val filename = "pixiv_${illustId}_p${pageIndex + 1}_${safeDisplayName}.jpg"
    val mimeType = "image/jpeg"
    val directory = Environment.DIRECTORY_PICTURES // Standard directory

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        // This check is always true for API 31+, but kept for clarity/robustness
        //Moidfy here targetting lower api level
        // RELATIVE_PATH directs the save location within Pictures
        put(MediaStore.MediaColumns.RELATIVE_PATH, "$directory/PixviDownloads")
        // IS_PENDING pattern is required for API 29+
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }

    val resolver = context.contentResolver
    var uri: Uri? = null

    try {
        // This uses MediaStore, no direct file path permissions needed on API 31+
        uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri == null) {
            throw IOException("Failed to create new MediaStore record.")
        }

        resolver.openOutputStream(uri)?.use { outputStream ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                throw IOException("Failed to save bitmap.")
            }
        } ?: throw IOException("Failed to get output stream.")

        // This check is always true for API 31+
        //Moidfy here targetting lower api level
        contentValues.clear()
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0) // Finalize the save
        resolver.update(uri, contentValues, null, null)

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Image saved to Pictures/PixivDownloads", Toast.LENGTH_SHORT).show()
        }
        return uri

    } catch (e: Exception) {
        uri?.let { orphanUri ->
            try { resolver.delete(orphanUri, null, null) } catch (deleteEx: Exception) { /* Log if needed */ }
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to save image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
        return null
    }
}