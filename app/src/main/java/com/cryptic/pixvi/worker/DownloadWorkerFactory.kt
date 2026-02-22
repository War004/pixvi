package com.cryptic.pixvi.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.cryptic.pixvi.core.downloader.image.DownloadImageRepo
import com.cryptic.pixvi.core.network.repo.PixivApiRepo
import com.cryptic.pixvi.database.notification.NotificationRepo

class DownloadWorkerFactory(
    private val repository: DownloadImageRepo,
    private val pixivRepo: PixivApiRepo,
    private val notificationRepo: NotificationRepo
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            SignalDownloadWorker::class.java.name ->
                SignalDownloadWorker(appContext, workerParameters, pixivRepo)
            MultipleDownloadWorker::class.java.name ->
                MultipleDownloadWorker(appContext, workerParameters, pixivRepo, notificationRepo)
            PdfImageDownloader::class.java.name ->
                PdfImageDownloader(appContext, workerParameters, pixivRepo, notificationRepo)

            else -> null // Let the default factory handle it
        }
    }
}