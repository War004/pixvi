package com.cryptic.piyek.core.image.blur


import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-lifetime immutable configuration.
 * Pre-computes all mathematical boundaries and reciprocal multipliers on startup.
 */
class BlurConfig(
    val radius: Int = 4,
    val passes: Int = 2
) {
    init {
        require(radius in 1..<CpuBoxBlurEngine.MICRO_WIDTH / 2) {
            "Radius must be between 1 and ${(CpuBoxBlurEngine.MICRO_WIDTH / 2 - 1)}"
        }
        require(passes >= 1) { "Passes must be at least 1" }
    }

    val windowSize: Int = 2 * radius + 1
    val multiplier: Int = (1 shl 16) / windowSize
    val leftEnd: Int = radius
    val rightStart: Int = CpuBoxBlurEngine.MICRO_WIDTH - radius - 1
}