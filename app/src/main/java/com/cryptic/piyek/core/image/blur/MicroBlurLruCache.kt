package com.cryptic.piyek.core.image.blur

import android.graphics.Bitmap

/**
 * In-memory LRU Cache for 64x64 RGB_565 micro-bitmaps (~8 KB each).
 */
class MicroBlurLruCache(maxEntries: Int = 100) {
    private val cache = object : android.util.LruCache<String, Bitmap>(maxEntries) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    fun get(key: String): Bitmap? = synchronized(cache) { cache.get(key) }

    fun put(key: String, bitmap: Bitmap) {
        synchronized(cache) { cache.put(key, bitmap) }
    }
}