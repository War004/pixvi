package com.cryptic.pixvi.experimental

import android.graphics.Bitmap
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap

/**
 * Fast box blur implementation for Android bitmaps.
 *
 * Uses a separable blur approach (horizontal + vertical passes) with a sliding window
 * technique that efficiently computes blur by updating running sums instead of
 * recalculating from scratch for each pixel.
 *
 * ## Usage
 * ```kotlin
 * val blurrer = SlidingWindowBoxBlur()
 *
 * // Blur with default settings (25% scale, 9x9 kernel, 2 passes)
 * val blurred = blurrer.blur(bitmap)
 *
 * // Blur with custom settings
 * val blurred = blurrer.blur(bitmap, BlurConfig(
 *     scaleFactor = 0.1f,   // Process at 10% resolution for speed
 *     kernelSize = 15,      // Larger kernel = more blur
 *     passes = 3            // More passes = smoother result
 * ))
 *
 * // Free memory when done
 * blurrer.clearCache()
 * ```
 *
 * ## Performance Tips
 * - Lower `scaleFactor` = faster blur (0.1 to 0.25 recommended for backgrounds)
 * - Reuse the same instance for multiple blurs to benefit from buffer caching
 * - Call `clearCache()` when leaving a screen to free memory
 *
 * @see BlurConfig for configuration options
 */
class SlidingWindowBoxBlur {

    /**
     * Configuration for the blur effect.
     *
     * @property scaleFactor Process image at this fraction of original size (0.1 = 10%).
     *                       Lower values = faster but more pixelated. Range: 0.05 to 1.0
     * @property kernelSize  Size of the blur kernel in pixels. Must be odd (3, 5, 7, 9...).
     *                       Larger = more blur spread. Default 9 works well for most cases.
     * @property passes      Number of blur passes. More passes = smoother result.
     *                       2 passes approximates a Gaussian blur.
     */
    data class BlurConfig(
        val scaleFactor: Float = 0.25f,
        val kernelSize: Int = 9,
        val passes: Int = 2
    )

    // Cached buffers for reuse between blur calls
    private var cachedBufferA: IntArray? = null
    private var cachedBufferB: IntArray? = null
    private var cachedBufferSize: Int = 0

    /**
     * Clears cached buffers to free memory.
     * Call this when you're done blurring, e.g., when leaving a screen or activity.
     */
    fun clearCache() {
        cachedBufferA = null
        cachedBufferB = null
        cachedBufferSize = 0
    }

    /**
     * Applies box blur to the input bitmap.
     *
     * The blur is performed on a background thread (Dispatchers.Default).
     *
     * **Important:** The input bitmap will be recycled after use to save memory.
     * If you need to keep the original, pass a copy: `bitmap.copy(bitmap.config, false)`
     *
     * @param input  The bitmap to blur. Will be recycled after processing.
     * @param config Blur configuration. See [BlurConfig] for options.
     * @return A new blurred bitmap at the original size.
     */
    suspend fun blur(input: Bitmap, config: BlurConfig = BlurConfig()): Bitmap {
        return withContext(Dispatchers.Default) {
            val originalW = input.width
            val originalH = input.height

            // Step 1: Downscale for faster processing
            val smallW = (originalW * config.scaleFactor).toInt().coerceAtLeast(1)
            val smallH = (originalH * config.scaleFactor).toInt().coerceAtLeast(1)
            val smallInput = input.scale(smallW, smallH, filter = true)

            // Free the input bitmap since we no longer need it
            if (smallInput != input) {
                input.recycle()
            }

            // Step 2: Get or reuse pixel buffers
            val w = smallInput.width
            val h = smallInput.height
            val pixelCount = w * h

            val srcPixels = getOrCreateBuffer(cachedBufferA, pixelCount)
            val dstPixels = getOrCreateBuffer(cachedBufferB, pixelCount)

            // Cache buffers for next blur call
            cachedBufferA = srcPixels
            cachedBufferB = dstPixels
            cachedBufferSize = pixelCount

            // Extract pixels from bitmap
            smallInput.getPixels(srcPixels, 0, w, 0, 0, w, h)
            smallInput.recycle()

            // Step 3: Apply blur passes
            repeat(config.passes) {
                applyBlurPass(srcPixels, dstPixels, w, h, config.kernelSize)
            }

            // Step 4: Create output bitmap
            val output = createBitmap(w, h)
            output.setPixels(srcPixels, 0, w, 0, 0, w, h)

            // Step 5: Scale back to original size
            val finalResult = output.scale(originalW, originalH, filter = true)
            if (output != finalResult) output.recycle()

            finalResult
        }
    }

    // Returns a cached buffer if large enough, otherwise creates a new one
    private fun getOrCreateBuffer(cached: IntArray?, requiredSize: Int): IntArray {
        return if (cached != null && cached.size >= requiredSize) cached else IntArray(requiredSize)
    }

    /**
     * Applies one blur pass (horizontal + vertical).
     * Uses a sliding window technique that updates running sums efficiently.
     */
    private fun applyBlurPass(src: IntArray, dst: IntArray, w: Int, h: Int, kernelSize: Int) {
        val radius = kernelSize / 2
        val windowSize = 2 * radius + 1

        // Use fixed-point math for fast division
        val shift = 16
        val multiplier = (1 shl shift) / windowSize

        // Horizontal pass: blur each row, write to dst
        for (y in 0 until h) {
            blurRow(src, dst, y * w, w, radius, multiplier, shift)
        }

        // Vertical pass: blur each column, write back to src
        for (x in 0 until w) {
            blurColumn(dst, src, x, w, h, radius, multiplier, shift)
        }
    }

    // Blurs a single row using sliding window
    private fun blurRow(
        src: IntArray, dst: IntArray,
        rowStart: Int, width: Int, radius: Int,
        multiplier: Int, shift: Int
    ) {
        var sumA = 0; var sumR = 0; var sumG = 0; var sumB = 0

        // Initialize window at left edge
        for (kx in -radius..radius) {
            val px = kx.coerceIn(0, width - 1)
            val pixel = src[rowStart + px]
            sumA += (pixel shr 24) and 0xFF
            sumR += (pixel shr 16) and 0xFF
            sumG += (pixel shr 8) and 0xFF
            sumB += pixel and 0xFF
        }

        // Slide window across the row
        for (x in 0 until width) {
            dst[rowStart + x] = packPixel(sumA, sumR, sumG, sumB, multiplier, shift)

            if (x < width - 1) {
                // Remove leftmost pixel from window
                val removeX = (x - radius).coerceIn(0, width - 1)
                val removePixel = src[rowStart + removeX]

                // Add new rightmost pixel to window
                val addX = (x + radius + 1).coerceIn(0, width - 1)
                val addPixel = src[rowStart + addX]

                sumA += ((addPixel shr 24) and 0xFF) - ((removePixel shr 24) and 0xFF)
                sumR += ((addPixel shr 16) and 0xFF) - ((removePixel shr 16) and 0xFF)
                sumG += ((addPixel shr 8) and 0xFF) - ((removePixel shr 8) and 0xFF)
                sumB += (addPixel and 0xFF) - (removePixel and 0xFF)
            }
        }
    }

    // Blurs a single column using sliding window
    private fun blurColumn(
        src: IntArray, dst: IntArray,
        x: Int, width: Int, height: Int, radius: Int,
        multiplier: Int, shift: Int
    ) {
        var sumA = 0; var sumR = 0; var sumG = 0; var sumB = 0

        // Initialize window at top edge
        for (ky in -radius..radius) {
            val py = ky.coerceIn(0, height - 1)
            val pixel = src[py * width + x]
            sumA += (pixel shr 24) and 0xFF
            sumR += (pixel shr 16) and 0xFF
            sumG += (pixel shr 8) and 0xFF
            sumB += pixel and 0xFF
        }

        // Slide window down the column
        for (y in 0 until height) {
            dst[y * width + x] = packPixel(sumA, sumR, sumG, sumB, multiplier, shift)

            if (y < height - 1) {
                // Remove topmost pixel from window
                val removeY = (y - radius).coerceIn(0, height - 1)
                val removePixel = src[removeY * width + x]

                // Add new bottommost pixel to window
                val addY = (y + radius + 1).coerceIn(0, height - 1)
                val addPixel = src[addY * width + x]

                sumA += ((addPixel shr 24) and 0xFF) - ((removePixel shr 24) and 0xFF)
                sumR += ((addPixel shr 16) and 0xFF) - ((removePixel shr 16) and 0xFF)
                sumG += ((addPixel shr 8) and 0xFF) - ((removePixel shr 8) and 0xFF)
                sumB += (addPixel and 0xFF) - (removePixel and 0xFF)
            }
        }
    }

    // Packs ARGB components into a single Int using fixed-point division
    @Suppress("NOTHING_TO_INLINE")
    private inline fun packPixel(a: Int, r: Int, g: Int, b: Int, multiplier: Int, shift: Int): Int {
        return (((a * multiplier) shr shift) shl 24) or
               (((r * multiplier) shr shift) shl 16) or
               (((g * multiplier) shr shift) shl 8) or
               ((b * multiplier) shr shift)
    }
}