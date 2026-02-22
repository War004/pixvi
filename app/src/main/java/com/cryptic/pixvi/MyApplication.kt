package com.cryptic.pixvi

import android.app.Application
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.cryptic.pixvi.worker.DownloadWorkerFactory

class MyApplication: Application(), SingletonImageLoader.Factory, Configuration.Provider{
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.startAccountMonitoring()  // Monitor for account removal from Settings
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(DownloadWorkerFactory(
                repository = container.downloadImageRepo,
                pixivRepo = container.pixivRepo,
                notificationRepo = container.notificationRepo
            ))
            .build()

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return container.imageLoader
    }
}