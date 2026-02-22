package com.cryptic.pixvi.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cryptic.pixvi.core.downloader.image.DownloadImageRepo
import com.cryptic.pixvi.core.downloader.image.saveImages
import com.cryptic.pixvi.core.network.repo.PixivApiRepo
import com.cryptic.pixvi.core.storage.AppSettings

/*
SignalDownloadWorker will exist as an singleton
Download repo is a single
DownloadRepo will have a property called queue where it will have the image url and etc, read.
IN the worker, we would have defined that these property would be be default,
When the singalDownload doWork would become active, it will automactically take the data, that was in it;s parameter by default
 */

/*
Central download repo, it will have a central property that will always display the current queue
SingleDownloadWorker will have the always listen to this property on the doWork,

 */
class SignalDownloadWorker (
    private val appContext: Context, workerParams: WorkerParameters,
    private val pixivRepo: PixivApiRepo,
): CoroutineWorker(appContext, workerParams){

    override suspend fun doWork(): Result {
        return try{
            //for now only download one image
            //for a single download item we would just check for complete status or not,
            //at the end we would provide a option to view the image or open it's file location using uri.
            val imageUrl = inputData.getString("KEY_URL")
                ?: return Result.failure(workDataOf("ERROR" to "No URL provided"))

            val fileName = inputData.getString("KEY_FILENAME")
                ?: "Pixvi_${System.currentTimeMillis()}.png"

            val result = saveImages(
                context = appContext,
                imageUrl = imageUrl,
                fileName = fileName,
                repo = pixivRepo
            )

            val outputData = workDataOf(
                "KEY_IMAGE_URI" to result.toString(),
                "KEY_MESSAGE" to "Download successful"
            )

            Result.success(outputData)

        } catch (e: Exception){
            Result.failure()
        }
    }
}