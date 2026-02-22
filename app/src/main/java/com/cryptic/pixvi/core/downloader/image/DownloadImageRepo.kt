package com.cryptic.pixvi.core.downloader.image

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.cryptic.pixvi.worker.MultipleDownloadWorker
import com.cryptic.pixvi.worker.SignalDownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

//property to display the current info about the queue that is needed by the imageDownload

//current task is just to hold the current value of the downloads
data class DownloadState(
    val imageLink: MutableList<String> = mutableListOf(),
    val fileName: String? = null,
)

class DownloadImageRepo(private val context: Context) {

    private val appContext = context.applicationContext

    private val workManager by lazy { WorkManager.getInstance(appContext) }

    private val _currentDownloadState = MutableStateFlow(DownloadState())
    val currentDownloadQueueInfo = _currentDownloadState.asStateFlow()

    companion object {
        const val KEY_SCHEDULE_TIME = "KEY_SCHEDULE_TIME"
    }

    private fun addItem(imageUrl: List<String>, fileName: String?){
        if(imageUrl.isEmpty()){
            Log.d("DownloaderViewModel","Empty list for image urls")
            return
        }
        val fileName = fileName?:"Pixvi_${System.currentTimeMillis()}"

        _currentDownloadState.update {
            it.copy(
                imageLink = imageUrl.toMutableList(),
                fileName = fileName
            )
        }
    }

    fun scheduleDownload(imageUrl: List<String>, fileName: String?, fileId: Long) {
        //for a single file,

        //saving the data that is required to save so that it can be downloaded,
        val input = workDataOf(
            "KEY_URL" to imageUrl[0],
            "KEY_FILENAME" to fileName,
            KEY_SCHEDULE_TIME to System.currentTimeMillis()
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Only run if we have internet
            .build()

        val workRequest = OneTimeWorkRequestBuilder<SignalDownloadWorker>()
            .setInputData(input)
            .setConstraints(constraints)
            .addTag("SINGLE_IMAGE_DOWNLOAD")
            .build()

        workManager.enqueueUniqueWork(
            "${fileId}_single",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Schedule a batch download for multiple pages of an artwork.
     *
     * @param baseUrl The first page URL containing _p0 pattern (e.g., "...140964616_p0.jpg")
     * @param fileName Base filename for saved images
     * @param fileId Unique artwork ID for deduplication
     * @param startIndex Starting page index (default: 0)
     * @param pageCount Number of pages to download
     */
    fun scheduleBatchDownload(
        baseUrl: String,
        fileName: String?,
        fileId: Long,
        startIndex: Int = 0,
        pageCount: Int
    ) {
        val input = workDataOf(
            MultipleDownloadWorker.KEY_BASE_URL to baseUrl,
            MultipleDownloadWorker.KEY_FILENAME to (fileName ?: "Pixvi_$fileId"),
            MultipleDownloadWorker.KEY_START_INDEX to startIndex,
            MultipleDownloadWorker.KEY_MAX_COUNT to pageCount,
            KEY_SCHEDULE_TIME to System.currentTimeMillis()
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<MultipleDownloadWorker>()
            .setInputData(input)
            .setConstraints(constraints)
            .addTag("BATCH_IMAGE_DOWNLOAD")
            .build()

        workManager.enqueueUniqueWork(
            "${fileId}_batch",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Saves bitmap temporarily to cache and copies a content URI (via FileProvider)
     * to the clipboard. Does NOT save permanently to MediaStore.
     * Must be called from a CoroutineScope, preferably with Dispatchers.IO.
     */
    suspend fun copyBitmapToClipboardViaCache(
        context: Context,
        bitmap: Bitmap,
        illustId: Long,
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
            Log.d("ImageCopy","Failed to copy image: ${e.localizedMessage}")
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
}