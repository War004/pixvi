package com.cryptic.piyek.core.image.blur

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.createBitmap

/**
 * Pre-allocated zero-allocation workspace using 32-bit ARGB_8888.
 */
class BlurWorkspace {
    val bitmap: Bitmap = createBitmap(CpuBoxBlurEngine.MICRO_WIDTH, CpuBoxBlurEngine.MICRO_HEIGHT)
    val canvas: Canvas = Canvas(bitmap)
    val paint: Paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    val bufferA: IntArray = IntArray(CpuBoxBlurEngine.PIXEL_COUNT)
    val bufferB: IntArray = IntArray(CpuBoxBlurEngine.PIXEL_COUNT)
}