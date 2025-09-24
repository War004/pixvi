package com.example.pixvi.utils

import com.example.pixvi.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage

@Composable
fun AnimatedWebp(
    modifier: Modifier = Modifier,
    contentScale: ContentScale,
    effectUrl: String
) {
    val context = LocalContext.current

    // This logic for mapping a URL to a local resource remains the same
    val effectMap: Map<String, Int> = mapOf(
        "https://source.pixiv.net/special/seasonal-effect-tag/pixiv-glow-effect/effect.png" to R.drawable.pixivgloweffect10_july
    )
    val resourceId: Int = effectMap[effectUrl] ?: throw IllegalArgumentException("No resource found for URL: $effectUrl")

    //image builder is now handled in MyApplication.kt

    // 2. Pass this specific imageLoader to AsyncImage
    AsyncImage(
        model = resourceId,
        contentDescription = "Animated effect",
        modifier = modifier,
        contentScale = contentScale
    )
}