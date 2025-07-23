package com.example.pixvi.workers

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pixvi.network.response.Home.ImageUtils // Your ImageUtils
import com.example.pixvi.viewModels.AppNotificationDatabase // Your DB
import com.example.pixvi.viewModels.NotificationDao
import com.example.pixvi.viewModels.TaskStatus // Your TaskStatus object
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers as OkHttpHeaders // Alias if needed
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PdfExportWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val notificationDao: NotificationDao = AppNotificationDatabase.getDatabase(applicationContext).notificationDao()
    private val gson = Gson()

    companion object {
        const val KEY_NOTIFICATION_ID = "notification_id"
        const val KEY_ILLUST_ID = "illust_id"
        const val KEY_ILLUST_TITLE = "illust_title"
        const val KEY_TOTAL_PAGES = "total_pages"
        const val KEY_IMAGE_URLS = "image_urls"
        private const val TAG = "PdfExportWorker"
    }

    override suspend fun doWork(): Result {
        val notificationId = inputData.getLong(KEY_NOTIFICATION_ID, -1L)
        val illustId = inputData.getInt(KEY_ILLUST_ID, -1)
        val illustTitle = inputData.getString(KEY_ILLUST_TITLE) ?: "Untitled"
        val totalPages = inputData.getInt(KEY_TOTAL_PAGES, 0)
        val imageUrls = inputData.getStringArray(KEY_IMAGE_URLS) ?: emptyArray()

        if (notificationId == -1L || illustId == -1 || imageUrls.isEmpty() || totalPages == 0) {
            Log.e(TAG, "Invalid input data for PdfExportWorker. NotificationId: $notificationId, IllustId: $illustId")
            // Optionally try to update notification to FAILED if notificationId is valid
            if (notificationId != -1L) {
                notificationDao.updateTaskCompletionOrFailure(
                    notificationId, TaskStatus.FAILED, "Invalid worker input",
                    "PDF creation failed for '$illustTitle'", "Retry", true, System.currentTimeMillis()
                )
            }
            return Result.failure()
        }


        Log.d(TAG, "Starting PDF export for illust '$illustTitle' (Job ID: $notificationId), $totalPages pages.")
        val downloadedImagePaths = mutableListOf<String>()

        try {
            // Initial progress update
            notificationDao.updateTaskProgress(
                notificationId, 0, TaskStatus.DOWNLOADING,
                "Downloading for PDF: '$illustTitle' (0/$totalPages pages)", System.currentTimeMillis()
            )

            imageUrls.forEachIndexed { index, url ->
                Log.d(TAG, "Downloading page ${index + 1} from $url")
                val bitmap = ImageUtils.loadBitmapFromUrl(applicationContext, url) // Pass headers
                if (bitmap != null) {
                    val tempFile = saveBitmapToTempCacheForPdf(applicationContext, bitmap, illustId, index)
                    if (tempFile != null) {
                        downloadedImagePaths.add(tempFile.absolutePath)
                        Log.d(TAG, "Saved page ${index + 1} to ${tempFile.absolutePath}")
                    } else {
                        throw IOException("Failed to save downloaded bitmap for page ${index + 1}")
                    }
                    bitmap.recycle() // Recycle bitmap if no longer needed by save function

                    // Update progress
                    notificationDao.updateTaskProgress(
                        notificationId, index + 1, TaskStatus.DOWNLOADING,
                        "Downloading for PDF: '$illustTitle' (${index + 1}/$totalPages pages)",
                        System.currentTimeMillis()
                    )
                } else {
                    throw IOException("Failed to download image from URL: $url for page ${index + 1}")
                }
            }

            // All images downloaded successfully
            val imagePathsJson = gson.toJson(downloadedImagePaths)
            notificationDao.updateTaskCompletionOrFailure(
                notificationId, TaskStatus.DOWNLOAD_COMPLETE, imagePathsJson,
                "Pages for '$illustTitle' downloaded ($totalPages/$totalPages). Ready to generate PDF.",
                "Save as PDF", true, System.currentTimeMillis()
            )
            Log.d(TAG, "All pages downloaded for '$illustTitle'. Payload: $imagePathsJson")
            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Error during PDF export for illust '$illustTitle' (Job ID: $notificationId)", e)
            notificationDao.updateTaskCompletionOrFailure(
                notificationId, TaskStatus.FAILED, e.localizedMessage ?: "Unknown error during download",
                "PDF creation failed for '$illustTitle'", "Retry", true, System.currentTimeMillis()
            )
            return Result.failure()
        }
    }

    // Helper to save bitmap to app's internal cache for PDF generation
    private suspend fun saveBitmapToTempCacheForPdf(
        context: Context,
        bitmap: Bitmap,
        illustId: Int,
        pageIndex: Int
    ): File? {
        return withContext(Dispatchers.IO) {
            val outputDir = File(context.cacheDir, "pdf_temp_images")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            // Sanitize illustId and pageIndex for filename if needed, but usually fine
            val fileName = "illust_${illustId}_page_${pageIndex}_${System.currentTimeMillis()}.jpg"
            val file = File(outputDir, fileName)
            try {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out) // Adjust quality as needed
                }
                file
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save bitmap to temp cache: ${file.absolutePath}", e)
                null
            }
        }
    }
}