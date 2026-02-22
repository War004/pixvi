package com.cryptic.pixvi.utlits

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.cryptic.pixvi.experimental.SlidingWindowBoxBlur

/**
 * Creates and remembers a [SlidingWindowBoxBlur] instance.
 * * Lifecycle:
 * - Created when the Composable enters the screen.
 * - Survived across recompositions.
 * - Clears its memory cache automatically when the Composable leaves the screen.
 */
@Composable
fun rememberBoxBlurState(): SlidingWindowBoxBlur {
    // 1. Create the instance once
    val blurrer = remember { SlidingWindowBoxBlur() }

    // 2. Automatically clean up when this specific UI element is destroyed
    DisposableEffect(Unit) {
        onDispose {
            blurrer.clearCache()
        }
    }
    return blurrer
}