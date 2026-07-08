package com.cryptic.piyek

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.cryptic.piyek.core.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MyApplication: Application(), SingletonImageLoader.Factory {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this,applicationScope)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return container.imageLoader
    }
}