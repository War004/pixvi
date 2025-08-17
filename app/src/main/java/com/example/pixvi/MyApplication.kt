package com.example.pixvi

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.ImageDecoderDecoder
import com.example.pixvi.utils.NetworkPerformanceMonitor

class MyApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        NetworkPerformanceMonitor.register(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                // Add the modern animated image decoder
                add(ImageDecoderDecoder.Factory())
            }
            .build()
    }
}