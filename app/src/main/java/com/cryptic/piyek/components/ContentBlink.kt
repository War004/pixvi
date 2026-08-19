package com.cryptic.piyek.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun ContentBlink(
    modifier: Modifier = Modifier,
    durationMillis: Int = 500,
    targetValue: Float = 0f,
    content: @Composable (modifier: Modifier) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BlinkTransition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkAlpha"
    )
    content(modifier.graphicsLayer{
        this.alpha = alpha
    })
}