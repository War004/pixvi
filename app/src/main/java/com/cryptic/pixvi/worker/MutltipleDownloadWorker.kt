package com.cryptic.pixvi.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cryptic.pixvi.core.downloader.image.saveImages
import com.cryptic.pixvi.core.network.repo.PixivApiRepo
import com.cryptic.pixvi.core.network.util.convertLongToTime
import com.cryptic.pixvi.database.notification.MediaType
import com.cryptic.pixvi.database.notification.NotifType
import com.cryptic.pixvi.database.notification.NotificationEntity
import com.cryptic.pixvi.database.notification.NotificationRepo
import kotlinx.coroutines.currentCoroutineContext

class MultipleDownloadWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
    private val pixivRepo: PixivApiRepo,
    private val notificationRepo: NotificationRepo
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_BASE_URL = "KEY_BASE_URL"
        const val KEY_FILENAME = "KEY_FILENAME"
        const val KEY_START_INDEX = "KEY_START_INDEX"
        const val KEY_MAX_COUNT = "KEY_MAX_COUNT"

        // Regex to match the page index pattern: _p0, _p1, _p12, etc.
        private val PAGE_INDEX_REGEX = Regex("""_p(\d+)""")
    }

    override suspend fun doWork(): Result {
        return try {
            // 1. Get Input Data
            val baseUrl = inputData.getString(KEY_BASE_URL)
                ?: return Result.failure(workDataOf("ERROR" to "No base URL provided"))

            val baseFileName = inputData.getString(KEY_FILENAME)
                ?: "Pixvi_${System.currentTimeMillis()}"

            val startIndex = inputData.getInt(KEY_START_INDEX, 0)
            val lastIndex = inputData.getInt(KEY_MAX_COUNT, 1)

            // Validate that base URL contains the page pattern
            if (!PAGE_INDEX_REGEX.containsMatchIn(baseUrl)) {
                return Result.failure(workDataOf("ERROR" to "URL doesn't contain _p{index} pattern"))
            }

            val successfulUris = mutableListOf<String>()
            var failedCount = 0

            //progress indicator
            val lastPage = lastIndex - startIndex - 1

            // Will correctly work only if index is continous
            for (pageIndex in startIndex until (lastIndex)) {
                // --- BROADCAST PROGRESS ---
                val currentProgress = lastIndex - pageIndex
                setProgress(
                    workDataOf(
                        //add baseFileName here
                        "BASE_FILE_NAME" to baseFileName,
                        "PROGRESS_CURRENT" to currentProgress,
                        "PROGRESS_TOTAL" to lastPage
                    )
                )

                // 3. Generate URL by replacing the page index
                val imageUrl = PAGE_INDEX_REGEX.replace(baseUrl) { "_p$pageIndex" }

                // 4. Create unique filename
                val uniqueFileName = "${baseFileName}_p$pageIndex"

                // 5. Perform the download
                val result = saveImages(
                    context = appContext,
                    imageUrl = imageUrl,
                    fileName = uniqueFileName,
                    repo = pixivRepo
                )

                // 6. Handle the Result properly
                result.getOrNull()?.let { uri ->
                    successfulUris.add(uri.toString())
                } ?: run {
                    failedCount++
                }
            }

            // 7. Save to database only if at least one download succeeded
            if (successfulUris.isNotEmpty()) {
                val currentTime = System.currentTimeMillis()
                val notificationEntity = NotificationEntity(
                    mediaType = 0,
                    notificationType = NotifType.DOWNLOAD.id,
                    fileName = baseFileName,
                    time = currentTime,
                    formattedTime = convertLongToTime(currentTime),
                    savedFolder = "Pictures/Pixvi", //baked in folder name
                    directFileUri = successfulUris.firstOrNull(),
                )
                notificationRepo.insertDownloadNotification(notificationEntity)
                    .onFailure { e ->
                        Log.w("MultipleDownloadWorker", "DB save failed", e)
                    }
            }

            // 8. Return results with success/failure counts
            val outputData = workDataOf(
                "KEY_IMAGE_URIS" to successfulUris.toTypedArray(),
                "KEY_SUCCESS_COUNT" to successfulUris.size,
                "KEY_FAILED_COUNT" to failedCount,
                "KEY_MESSAGE" to "Downloaded ${successfulUris.size}/$lastIndex images"
            )

            Result.success(outputData)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(workDataOf("ERROR" to e.message))
        }
    }
}