package com.cryptic.pixvi.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cryptic.pixvi.core.downloader.image.saveImages
import com.cryptic.pixvi.core.network.repo.PixivApiRepo
import com.cryptic.pixvi.core.network.util.convertLongToTime
import com.cryptic.pixvi.database.notification.NotifType
import com.cryptic.pixvi.database.notification.NotificationEntity
import com.cryptic.pixvi.database.notification.NotificationRepo

/*
We have a function to download the images to download the images to the internal function
 */
class PdfImageDownloader(
    private val appContext: Context,
    workerParams: WorkerParameters,
    private val pixivRepo: PixivApiRepo,
    private val notificationRepo: NotificationRepo
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_BASE_FOLDER = "KEY_BASE_FOLDER"
        const val KEY_POST_TITLE = "KEY_POST_TITLE"
        const val KEY_START_INDEX = "KEY_START_INDEX"
        const val KEY_END_INDEX = "KEY_END_INDEX"
        const val KEY_IMAGE_URL = "KEY_IMAGE_URL"

        // Regex to match the page index pattern: _p0, _p1, _p12, etc.
        private val PAGE_INDEX_REGEX = Regex("""_p(\d+)""")
    }

    /*

     */
    override suspend fun doWork(): Result {
        return try{

            val baseFolder = inputData.getString(KEY_BASE_FOLDER)
                ?: return Result.failure(workDataOf("ERROR" to "Base folder name was not found."))

            val baseFileName = inputData.getString(KEY_POST_TITLE)
                ?: return Result.failure(workDataOf("ERROR" to "Base filename not found"))

            val startIndex = inputData.getInt(KEY_START_INDEX, -1)

            val endIndex = inputData.getInt(KEY_END_INDEX, -1)

            val baseUrl = inputData.getString(KEY_IMAGE_URL)
                ?: return Result.failure(workDataOf("ERROR" to "No base URL provided"))


            if(startIndex == -1 || endIndex == -1) return Result.failure(workDataOf("ERROR" to "No index was provided for the select pdf"))

            // Validate that base URL contains the page pattern
            if (!PAGE_INDEX_REGEX.containsMatchIn(baseUrl)) {
                return Result.failure(workDataOf("ERROR" to "URL doesn't contain _p{index} pattern"))
            }

            val successfulUris = mutableListOf<String>()
            var failedCount = 0

            for(pageIndex in startIndex until (endIndex)) {

                val currentProgress = pageIndex - startIndex + 1
                setProgress(
                    workDataOf(
                        //add baseFileName here
                        "BASE_FILE_NAME" to baseFileName,
                        "PROGRESS_CURRENT" to currentProgress,
                        "PROGRESS_TOTAL" to endIndex - startIndex
                    )
                )

                val imageUrl = PAGE_INDEX_REGEX.replace(baseUrl) { "_p$pageIndex" }

                val uniqueFileName = "${baseFileName}_p$pageIndex"

                val result = saveImages(
                    context = appContext,
                    basePdfFolder = baseFolder,
                    imageUrl = imageUrl,
                    fileName = uniqueFileName,
                    repo = pixivRepo
                )

                result.getOrNull()?.let { uri ->
                    successfulUris.add(uri.toString())
                } ?: run {
                    failedCount++
                }
            }
            //loop ended. Save the data to the database,
            //how to store the filename
            if(successfulUris.isNotEmpty()){
                val currentTime = System.currentTimeMillis()
                Log.d("PdfInternal","Filename: $baseFileName")
                Log.d("PdfInternal","Foldername: $baseFolder")

                val notificationEntry = NotificationEntity(
                    notificationType = NotifType.DOWNLOAD.id,
                    mediaType = 1,
                    isPdfRenderingPending = true,
                    pdfSessionId = baseFolder,
                    pdfPrintStartIndex = startIndex,
                    pdfPrintEndIndex = endIndex,
                    fileName = baseFileName,
                    time = currentTime,
                    formattedTime = convertLongToTime(currentTime),
                    savedFolder = "Internal app folder",
                    directFileUri = null,

                )
                notificationRepo.insertDownloadNotification(notificationEntry)
                    .onFailure { e->
                        android.util.Log.w("MultipleDownloadWorker_pdf", "DB save failed", e)
                    }
            }

            //we can use base folder to get the data from the db if needed. not reliable
            //not handling replicated id,
            val outputData = workDataOf(
                "BASE_FOLDER" to baseFolder,
                "BASE_FILENAME" to baseFileName,
                "KEY_SUCCESS_COUNT" to successfulUris.size,
                "KEY_FAILED_COUNT" to failedCount,
                "KEY_MESSAGE" to "Downloaded ${successfulUris.size}/${endIndex-startIndex} images"
            )
            Result.success(outputData)
        }
        catch (e: Exception) {
            e.printStackTrace()
            Result.failure(workDataOf("ERROR" to e.message))
        }
    }
}