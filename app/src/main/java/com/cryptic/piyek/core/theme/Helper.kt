package com.cryptic.piyek.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

/**
 * Mathematically determines if White or Black text provides the best contrast.
 */
fun getContrastTextColor(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() > 0.5f) {
        Color.Black
    } else {
        Color.White
    }
}

/**
 * Generates a cohesive button color by blending the background color
 * with 20% of the high-contrast text color. This creates a perfect "Tonal" elevation.
 */
fun getTonalButtonColor(backgroundColor: Color, contrastTextColor: Color): Color {
    return contrastTextColor.copy(alpha = 0.2f).compositeOver(backgroundColor)
}