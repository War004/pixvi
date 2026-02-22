package com.cryptic.pixvi.artwork.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.cryptic.pixvi.appShell.SettingAction
import com.cryptic.pixvi.artwork.data.ArtworkInfo
import kotlin.math.absoluteValue

@Composable
fun FadingBackgroundImage(
    imageQuality: Int,
    items: List<ArtworkInfo>,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {

    Box(modifier) {

        val pageState by remember {
            derivedStateOf {
                val current = pagerState.currentPage
                val offset = pagerState.currentPageOffsetFraction
                val target = when {
                    offset > 0.001f -> current + 1
                    offset < -0.001f -> current - 1
                    else -> current
                }
                current to target
            }
        }

        val (currentPage, targetPage) = pageState

        val currentArtwork = items.getOrNull(currentPage)
        val nextArtwork = items.getOrNull(targetPage)

        Box(Modifier.fillMaxSize()) {
            if (nextArtwork != null && currentArtwork != null) {

                // --- 1. Request for the BOTTOM Image (Current Page) ---
                val currentRequest = ImageRequest.Builder(LocalContext.current)
                    .data(when(imageQuality) {
                        -1 -> currentArtwork.pages[0].quality.original
                        0 -> currentArtwork.pages[0].quality.large
                        1 -> currentArtwork.pages[0].quality.medium
                        else -> currentArtwork.pages[0].quality.large
                    })
                    .memoryCacheKey(currentArtwork.data.id.toString())
                    .build()

                AsyncImage(
                    model = currentRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                val nextRequest = ImageRequest.Builder(LocalContext.current)
                    .data(when(imageQuality) {
                        -1 -> nextArtwork.pages[0].quality.original
                        0 -> nextArtwork.pages[0].quality.large
                        1 -> nextArtwork.pages[0].quality.medium
                        else -> nextArtwork.pages[0].quality.large
                    })
                    .memoryCacheKey(nextArtwork.data.id.toString())
                    .build()

                AsyncImage(
                    model = nextRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = pagerState.currentPageOffsetFraction.absoluteValue }
                )
            }
        }
    }
}