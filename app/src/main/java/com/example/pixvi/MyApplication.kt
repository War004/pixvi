package com.example.pixvi

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.ImageDecoderDecoder

class MyApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                // Add the animation decoder to the default loader
                add(ImageDecoderDecoder.Factory())
            }
            .build()
    }
}