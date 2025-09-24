package com.example.pixvi

import ConnectivityObserver
import android.app.Application
import android.content.Context
import coil3.SingletonImageLoader
import coil3.ImageLoader
import coil3.PlatformContext
import com.example.pixvi.bookmark.BookmarkRepository
import com.example.pixvi.bookmark.BookmarkRepositoryImpl
import com.example.pixvi.utils.NetworkPerformanceMonitor
import com.example.pixvi.network.api.RetrofitClient
import com.example.pixvi.repo.SystemInfoRepository
import com.example.pixvi.settings.SettingsRepository
import com.example.pixvi.utils.AndroidConnectivityObserver
import com.example.pixvi.repo.BatterySaverThemeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.getValue


class AppContainer(private val context: Context) {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val bookmarkRepository: BookmarkRepository = BookmarkRepositoryImpl(RetrofitClient.apiService)

    val viewModelFactory = ViewModelFactory(this)

    val settingsRepository = SettingsRepository(context.dataStore)

    val systemInfoRepository: SystemInfoRepository by lazy {
        SystemInfoRepository(context)
    }

    val BatterySaverTheme by lazy {
        BatterySaverThemeRepository(settingsRepository, systemInfoRepository, applicationScope)
    }
}

class MyApplication : Application(), SingletonImageLoader.Factory {

    companion object {
        lateinit var connectivityObserver: ConnectivityObserver
            private set
    }

    override fun onCreate() {
        super.onCreate()

        connectivityObserver = AndroidConnectivityObserver(applicationContext)
        NetworkPerformanceMonitor.register(this, connectivityObserver)
    }

    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {  // Add context parameter back
        return ImageLoader.Builder(context)
            .build()
    }
}