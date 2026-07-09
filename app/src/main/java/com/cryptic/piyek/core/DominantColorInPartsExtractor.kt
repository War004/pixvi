package com.cryptic.piyek.core

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A single-pass dominant color extractor.
 * Bypasses native scaling and 3D quantization in favor of an on-the-fly,
 * pruned 15-bit histogram, and epoch-stamped O(1) state clearance.
 */
class DominantColorInPartsExtractor {

    // Flat 32,768 element array representing the 15-bit color space (131 KB).
    private val histogram = IntArray(32768)

    // Generation/Epoch tracking array to enable O(1) zero-overhead clears (131 KB).
    private val epoch = IntArray(32768)
    private var currentEpoch = 0

    // Reusable buffer to hold a single row of pixels.
    private var rowBuffer: IntArray? = null

    // Mutex to protect internal state during concurrent extractions.
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
     * Extracts the dominant color from the bitmap or a specific region.
     * Processes pixels in a single pass with O(1) finalization.
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
                    if (regionWidth <= 0 || endY <= startY) return@withLock Color.BLACK

                    // 2. Ensure row buffer is large enough for the requested region width
                    var buffer = rowBuffer
                    if (buffer == null || buffer.size < regionWidth) {
                        buffer = IntArray(regionWidth)
                        rowBuffer = buffer
                    }

                    // 3. Track running winners on the fly
                    var maxScore = -1f
                    var dominantIndex = 0

                    for (y in startY until endY step safeStride) {
                        input.getPixels(buffer, 0, regionWidth, startX, y, regionWidth, 1)

                        for (x in 0 until regionWidth step safeStride) {
                            val pixel = buffer[x]

                            // Gatekeeper: Ignore transparent and highly translucent pixels
                            val alpha = (pixel shr 24) and 0xFF
                            if (alpha < 128) continue

                            val r5 = (pixel shr 19) and 0x1F
                            val g5 = (pixel shr 11) and 0x1F
                            val b5 = (pixel shr 3)  and 0x1F
                            val index = (r5 shl 10) or (g5 shl 5) or b5

                            // Read or initialize the histogram bucket depending on the epoch
                            val count = if (epoch[index] == currentEpoch) {
                                ++histogram[index]
                            } else {
                                epoch[index] = currentEpoch
                                histogram[index] = 1
                                1
                            }

                            // Pruning Gate: Since penalty <= 1.0, score can never exceed count.
                            // Bypasses float calculations for ~95%+ of low-frequency noise pixels.
                            if (count > maxScore) {
                                // Compiles to branchless hardware-level ARM64 instructions (CSEL)
                                val lightness = (maxOf(r5, g5, b5) + minOf(r5, g5, b5)) / 2
                                val score = count * lightnessPenaltyLUT[lightness]

                                if (score > maxScore) {
                                    maxScore = score
                                    dominantIndex = index
                                }
                            }
                        }
                    }

                    if (maxScore < 0) return@withLock Color.BLACK

                    // 4. Unpack the winning index back into a standard 32-bit ARGB color
                    val finalR = ((dominantIndex shr 10) and 0x1F) shl 3
                    val finalG = ((dominantIndex shr 5) and 0x1F) shl 3
                    val finalB = (dominantIndex and 0x1F) shl 3

                    Color.rgb(finalR, finalG, finalB)

                } finally {
                    // Instantly invalidate all writes from this run in O(1) time.
                    // If currentEpoch wraps around, we run a defensive fill(0) on the epoch array.
                    if (currentEpoch == Int.MAX_VALUE) {
                        epoch.fill(0)
                        currentEpoch = 1
                    } else {
                        currentEpoch++
                    }
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
