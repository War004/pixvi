package com.example.pixvi.utils

import androidx.palette.graphics.Palette
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.scale
import androidx.core.graphics.get

/**
 * Extracts the dominant color from a specified position in a bitmap image.
 * Handles HARDWARE bitmaps by converting them to a compatible format.
 *
 * @param bitmap The source bitmap to extract color from
 * @param position The relative vertical position in the image (0.0 = top, 0.5 = middle, 1.0 = bottom)
 * @param sampleHeight The height of the region to sample as a fraction of the image height
 * @param callback Callback function that receives the dominant color as an Int
 */
fun extractDominantColor(
    bitmap: Bitmap,
    position: Float = 0.0f,
    sampleHeight: Float = 0.1f,
    callback: (Int) -> Unit
) {
    try {
        // Ensure position is in valid range
        val normalizedPosition = position.coerceIn(0.0f, 1.0f)
        val normalizedSampleHeight = sampleHeight.coerceIn(0.05f, 0.25f) // Between 5% and 25%

        Log.d("ColorExtraction", "Processing bitmap: ${bitmap.width}x${bitmap.height}, " +
                "format: ${bitmap.config}, position: $normalizedPosition")

        // Check if we need to convert the bitmap to a supported format
        val processableBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            // Convert HARDWARE bitmap to ARGB_8888 for pixel access
            Log.d("ColorExtraction", "Converting HARDWARE bitmap to ARGB_8888")
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }

        // Calculate the region to sample
        val regionHeight = (processableBitmap.height * normalizedSampleHeight).toInt().coerceAtLeast(1)

        // Calculate the vertical starting position, centered on the requested position
        // This centers the sample region on the requested position
        val startY = ((processableBitmap.height * normalizedPosition) - (regionHeight / 2)).toInt()
            .coerceIn(0, processableBitmap.height - regionHeight)

        // Create a cropped bitmap of just the desired region to analyze
        val regionBitmap = Bitmap.createBitmap(
            processableBitmap,
            0, startY,
            processableBitmap.width, regionHeight
        )

        // Configure Palette with optimized settings
        val builder = Palette.Builder(regionBitmap)
            .maximumColorCount(24)
            .clearFilters() // Don't filter out any colors

        // Generate palette
        builder.generate { palette ->
            // Extract the dominant color or fallback
            val dominantColor = palette?.let {
                it.dominantSwatch?.rgb
                    ?: it.vibrantSwatch?.rgb
                    ?: it.lightVibrantSwatch?.rgb
                    ?: Color(0xFF90CAF9).toArgb() // Light blue fallback
            } ?: Color(0xFF90CAF9).toArgb()

            // Clean up temporary bitmaps to avoid memory leaks
            if (regionBitmap != processableBitmap) {
                regionBitmap.recycle()
            }
            if (processableBitmap != bitmap) {
                processableBitmap.recycle()
            }

            // Log the extracted color
            Log.d("ColorExtraction", "Successfully extracted color at position $normalizedPosition: " +
                    "#${Integer.toHexString(dominantColor)}")

            // Return the dominant color via callback
            callback(dominantColor)
        }
    } catch (e: Exception) {
        Log.e("ColorExtraction", "Error extracting dominant color at position $position", e)
        callback(Color(0xFF90CAF9).toArgb()) // Light blue fallback
    }
}

/**
 * Determines if the text color should be light or dark based on the background color
 *
 * @param backgroundColor The background color to check
 * @return true if text should be light (white), false if text should be dark (black)
 */
fun shouldUseWhiteText(backgroundColor: Color): Boolean {
    // Calculate luminance - standard formula for perceived brightness
    val luminance = (0.299 * backgroundColor.red +
            0.587 * backgroundColor.green +
            0.114 * backgroundColor.blue)

    // Use white text on dark backgrounds, black text on light backgrounds
    return luminance < 0.5
}

fun calculateAverageColorFromTop(bitmap: Bitmap, topPercent: Float): Color {
    val width = bitmap.width
    val height = bitmap.height
    val topHeight = (height * topPercent).toInt()
    if (topHeight <= 0) return Color.White

    var totalRed = 0
    var totalGreen = 0
    var totalBlue = 0
    var pixelCount = 0

    val scaleFactor = 4
    val scaledWidth = width / scaleFactor
    val scaledHeight = topHeight / scaleFactor
    val scaledBitmap = bitmap.scale(scaledWidth, scaledHeight, false)

    for (x in 0 until scaledWidth) {
        for (y in 0 until scaledHeight) {
            val pixelColor = scaledBitmap[x, y]
            totalRed += android.graphics.Color.red(pixelColor)
            totalGreen += android.graphics.Color.green(pixelColor)
            totalBlue += android.graphics.Color.blue(pixelColor)
            pixelCount++
        }
    }
    scaledBitmap.recycle()

    if (pixelCount == 0) return Color.White

    val averageRed = totalRed / pixelCount
    val averageGreen = totalGreen / pixelCount
    val averageBlue = totalBlue / pixelCount

    return Color(averageRed, averageGreen, averageBlue)
}