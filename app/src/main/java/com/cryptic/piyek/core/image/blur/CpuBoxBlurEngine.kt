package com.cryptic.piyek.core.image.blur

/**
 * 32-bit (ARGB_8888) CPU sliding-window box blur engine.
 * Fully branchless inner loops with zero coerce calls, 16 KB L1-cache residency,
 * and exact 8-bit sRGB channel alignment.
 */
class CpuBoxBlurEngine(
    private val config: BlurConfig = BlurConfig()
) {
    companion object {
        const val MICRO_WIDTH: Int = 64
        const val MICRO_HEIGHT: Int = 64
        const val PIXEL_COUNT: Int = MICRO_WIDTH * MICRO_HEIGHT
        private const val SHIFT: Int = 16
        private const val OPAQUE_ALPHA: Int = 0xFF shl 24
    }

    // Hoisted primitives for direct register caching in ART
    private val radius: Int = config.radius
    private val multiplier: Int = config.multiplier
    private val leftEnd: Int = config.leftEnd
    private val rightStart: Int = config.rightStart
    private val passes: Int = config.passes

    fun applyBlur(src: IntArray, dst: IntArray) {
        repeat(passes) {
            // Horizontal Pass: Row blur
            for (y in 0 until MICRO_HEIGHT) {
                blurRow(src, dst, y * MICRO_WIDTH)
            }
            // Transpose dst -> src (L1 Cache resident)
            transpose(dst, src, MICRO_WIDTH, MICRO_HEIGHT)

            // Vertical Pass: Row blur across transposed data
            for (y in 0 until MICRO_WIDTH) {
                blurRow(src, dst, y * MICRO_HEIGHT)
            }
            // Transpose dst -> src (Places completed pass back in src)
            transpose(dst, src, MICRO_HEIGHT, MICRO_WIDTH)
        }
    }

    private fun blurRow(src: IntArray, dst: IntArray, rowStart: Int) {
        var sumR: Int
        var sumG: Int
        var sumB: Int

        // Load edge pixels directly into CPU registers
        val firstPixel = src[rowStart]
        val lastPixel = src[rowStart + MICRO_WIDTH - 1]

        // Unpack standard 32-bit ARGB_8888 (8 bits per channel)
        val firstR = (firstPixel ushr 16) and 0xFF
        val firstG = (firstPixel ushr 8) and 0xFF
        val firstB = firstPixel and 0xFF

        // Seed sum: Left clamped portion (negative indices map to pixel 0)
        sumR = firstR * radius
        sumG = firstG * radius
        sumB = firstB * radius

        // Seed sum: In-bounds portion (0..radius guaranteed < MICRO_WIDTH by invariant)
        for (x in 0..radius) {
            val px = src[rowStart + x]
            sumR += (px ushr 16) and 0xFF
            sumG += (px ushr 8) and 0xFF
            sumB += px and 0xFF
        }

        // ── REGION 1: Left Edge (0 until radius) ──
        for (x in 0 until leftEnd) {
            dst[rowStart + x] = packPixel(sumR, sumG, sumB)
            val addPx = src[rowStart + x + radius + 1]
            sumR += ((addPx ushr 16) and 0xFF) - firstR
            sumG += ((addPx ushr 8) and 0xFF) - firstG
            sumB += (addPx and 0xFF) - firstB
        }

        // ── REGION 2: Middle Core (radius until MICRO_WIDTH - radius - 1) ──
        for (x in leftEnd until rightStart) {
            dst[rowStart + x] = packPixel(sumR, sumG, sumB)
            val remPx = src[rowStart + x - radius]
            val addPx = src[rowStart + x + radius + 1]
            sumR += ((addPx ushr 16) and 0xFF) - ((remPx ushr 16) and 0xFF)
            sumG += ((addPx ushr 8) and 0xFF) - ((remPx ushr 8) and 0xFF)
            sumB += (addPx and 0xFF) - (remPx and 0xFF)
        }

        // ── REGION 3: Right Edge (MICRO_WIDTH - radius - 1 until MICRO_WIDTH) ──
        val lastR = (lastPixel ushr 16) and 0xFF
        val lastG = (lastPixel ushr 8) and 0xFF
        val lastB = lastPixel and 0xFF

        for (x in rightStart until MICRO_WIDTH) {
            dst[rowStart + x] = packPixel(sumR, sumG, sumB)
            val remPx = src[rowStart + x - radius]
            sumR += lastR - ((remPx ushr 16) and 0xFF)
            sumG += lastG - ((remPx ushr 8) and 0xFF)
            sumB += lastB - (remPx and 0xFF)
        }
    }

    private fun transpose(src: IntArray, dst: IntArray, w: Int, h: Int) {
        for (y in 0 until h) {
            val rowOffset = y * w
            for (x in 0 until w) {
                dst[x * h + y] = src[rowOffset + x]
            }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun packPixel(r: Int, g: Int, b: Int): Int {
        val rAvg = ((r * multiplier) ushr SHIFT) and 0xFF
        val gAvg = ((g * multiplier) ushr SHIFT) and 0xFF
        val bAvg = ((b * multiplier) ushr SHIFT) and 0xFF
        return OPAQUE_ALPHA or (rAvg shl 16) or (gAvg shl 8) or bAvg
    }
}