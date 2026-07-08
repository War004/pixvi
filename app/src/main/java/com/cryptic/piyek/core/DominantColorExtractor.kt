package com.cryptic.piyek.core

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A hyper-optimized, single-pass dominant color extractor.
 * Bypasses native scaling and 3D quantization in favor of a flat 15-bit histogram,
 * row-by-row pixel sampling, and a precomputed lightness penalty curve.
 * Includes zero-allocation region targeting (e.g., extracting only the bottom 5%).
 */
class DominantColorExtractor {

    // Flat 32,768 element array representing the 15-bit color space (131 KB).
    private val histogram = IntArray(32768)

    // Reusable buffer to hold a single row of pixels.
    private var rowBuffer: IntArray? = null

    // Mutex to prevent concurrent mutation of the histogram and rowBuffer.
    private val mutex = Mutex()

    /**
     * Precomputed Lookup Table (LUT) for the Extremes Penalty.
     * Evaluates a lightness value (0-31) and returns a float multiplier.
     */
    private val lightnessPenaltyLUT = FloatArray(32) { i ->
        val normalized = (i - 15.5f) / 15.5f
        1f - (normalized * normalized * 0.9f)
    }

    /**
     * Extracts the dominant color from the bitmap or a specific region using a background thread.
     * Does not recycle the input bitmap.
     *
     * @return The dominant ARGB color as a packed Int, or Color.BLACK if extraction fails.
     */
    suspend fun extract(
        input: Bitmap,
        stride: Int = 4,
        startYPercent: Float = 0.0f,
        endYPercent: Float = 1.0f,
        startXPercent: Float = 0.0f,
        endXPercent: Float = 1.0f
    ): Int {
        return withContext(Dispatchers.Default) {
            mutex.withLock {
                try {
                    val width = input.width
                    val height = input.height
                    val safeStride = stride.coerceAtLeast(1)

                    // 1. Calculate absolute pixel boundaries, clamping inputs for safety
                    val startY = (height * startYPercent.coerceIn(0f, 1f)).toInt().coerceIn(0, height - 1)
                    val endY = (height * endYPercent.coerceIn(0f, 1f)).toInt().coerceIn(0, height)
                    val startX = (width * startXPercent.coerceIn(0f, 1f)).toInt().coerceIn(0, width - 1)
                    val endX = (width * endXPercent.coerceIn(0f, 1f)).toInt().coerceIn(0, width)

                    val regionWidth = endX - startX

                    // Failsafe: If the region is 0 pixels wide/tall, abort.
                    if (regionWidth <= 0 || endY <= startY) return@withLock Color.BLACK

                    // 2. Ensure row buffer is large enough for the requested region width
                    var buffer = rowBuffer
                    if (buffer == null || buffer.size < regionWidth) {
                        buffer = IntArray(regionWidth)
                        rowBuffer = buffer
                    }

                    // 3. Build the 15-bit histogram strictly within the targeted bounds
                    for (y in startY until endY step safeStride) {
                        // Tell C++ to jump to X=startX and only read 'regionWidth' pixels
                        input.getPixels(buffer, 0, regionWidth, startX, y, regionWidth, 1)

                        for (x in 0 until regionWidth step safeStride) {
                            val pixel = buffer[x]

                            // Gatekeeper: Ignore transparent and highly translucent pixels
                            val alpha = (pixel shr 24) and 0xFF
                            if (alpha < 128) continue

                            // Extract 8-bit channels and shift down to 5-bit
                            val r5 = (pixel shr 19) and 0x1F
                            val g5 = (pixel shr 11) and 0x1F
                            val b5 = (pixel shr 3)  and 0x1F

                            // Pack into a single 15-bit index (0 to 32767)
                            val index = (r5 shl 10) or (g5 shl 5) or b5
                            histogram[index]++
                        }
                    }

                    // 4. Find the highest scored bucket using the LUT
                    var maxScore = -1f
                    var dominantIndex = 0

                    for (i in 0 until 32768) {
                        val count = histogram[i]
                        if (count == 0) continue

                        val r5 = (i shr 10) and 0x1F
                        val g5 = (i shr 5) and 0x1F
                        val b5 = i and 0x1F

                        // Fast lightness approximation for 5-bit colors (0 to 31)
                        val lightness = (maxOf(r5, g5, b5) + minOf(r5, g5, b5)) / 2
                        val score = count * lightnessPenaltyLUT[lightness]

                        if (score > maxScore) {
                            maxScore = score
                            dominantIndex = i
                        }
                    }

                    if (maxScore < 0) return@withLock Color.BLACK

                    // 5. Unpack the winning index back into a standard 32-bit ARGB color
                    val finalR = ((dominantIndex shr 10) and 0x1F) shl 3
                    val finalG = ((dominantIndex shr 5) and 0x1F) shl 3
                    val finalB = (dominantIndex and 0x1F) shl 3

                    Color.rgb(finalR, finalG, finalB)

                } finally {
                    // Guaranteed to clean state even if the coroutine is cancelled mid-extraction
                    histogram.fill(0)
                }
            }
        }
    }

    /**
     * Clears internal buffers to free memory.
     */
    fun clearCache() {
        rowBuffer = null
    }
}