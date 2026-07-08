package com.cryptic.piyek.feature.iLLust.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.memory.MemoryCache
import com.cryptic.piyek.core.content.data.model.Artwork
import com.cryptic.piyek.core.content.data.model.ImageQuality

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

/** Default aspect ratio when no specific dimensions are available. */
private const val DEFAULT_ASPECT_RATIO = 3f / 4f

// ---------------------------------------------------------------------------
// ArtworkFeedItem — single feed entry that supports multi-page artwork
// ---------------------------------------------------------------------------

/**
 * Displays a single artwork entry in the vertical feed.
 *
 * - **Single-page artwork**: renders the first page directly.
 * - **Multi-page artwork**: wraps pages in a [HorizontalPager] with a
 *   [CircularPageProgressIndicator] overlay in the top-end corner.
 *
 * Aspect ratio defaults to [DEFAULT_ASPECT_RATIO] since the domain model
 * does not carry dimension metadata.
 *
 * @param artwork     The artwork to render.
 * @param isFocused   Whether this item is the "active" item closest to the
 *                    viewport center — used for a subtle highlight border.
 * @param onClick     Callback invoked on a regular tap.
 * @param onLongPress Callback invoked on a long-press, receiving the
 *                    raw image URL and the current page index.
 */
@Composable
fun ArtworkFeedItem(
    artwork: Artwork,
    isFocused: Boolean,
    onClick: () -> Unit,
    onLongPress: (rawUrl: String, pageIndex: Int) -> Unit,
) {
    val context = LocalPlatformContext.current
    val imageLoader = SingletonImageLoader.get(context)
    val memoryCache = imageLoader.memoryCache

    val isMultiPage = artwork.pageCount > 1
    val pageQualities = artwork.quality.all

    Column(modifier = Modifier.fillMaxWidth()) {

        // -- Pager state (only allocated for multi-page artwork) ---------------
        val pagerState = if (isMultiPage) {
            rememberPagerState(pageCount = { pageQualities.size })
        } else {
            null
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(DEFAULT_ASPECT_RATIO, matchHeightConstraintsFirst = false)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        val currentPage = pagerState?.currentPage ?: 0
                        val url = getOptimalUrl(pageQualities[currentPage], memoryCache)
                        onLongPress(url, currentPage)
                    },
                )
                .border(
                    width = if (isFocused) 3.dp else 0.dp,
                    color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(4.dp),
                )
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (isMultiPage) {
                MultiPageContent(
                    pages = pageQualities,
                    pagerState = pagerState!!,
                    memoryCache = memoryCache,
                    contentDescription = artwork.title,
                )
            } else {
                SinglePageContent(
                    quality = pageQualities.first(),
                    memoryCache = memoryCache,
                    contentDescription = artwork.title,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Internal composables
// ---------------------------------------------------------------------------

@Composable
private fun MultiPageContent(
    pages: List<ImageQuality>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    memoryCache: MemoryCache?,
    contentDescription: String,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { pageIndex ->
            val url = remember(pageIndex) {
                getOptimalUrl(pages[pageIndex], memoryCache)
            }
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        CircularPageProgressIndicator(
            currentPage = pagerState.currentPage,
            pageCount = pagerState.pageCount,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        )
    }
}

@Composable
private fun SinglePageContent(
    quality: ImageQuality,
    memoryCache: MemoryCache?,
    contentDescription: String,
) {
    val url = remember { getOptimalUrl(quality, memoryCache) }
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
}

// ---------------------------------------------------------------------------
// Image quality resolution
// ---------------------------------------------------------------------------

/**
 * Pure function that selects the best available image URL.
 *
 * Priority:
 * 1. If a higher-quality variant is already in the memory cache, use it.
 * 2. Otherwise fall back to **medium** quality (best balance for feed).
 *
 * This is intentionally **not** a composable so it can be unit-tested
 * without a Compose context.
 */
fun getOptimalUrl(
    quality: ImageQuality,
    memoryCache: MemoryCache?,
): String {
    val originalKey = MemoryCache.Key(quality.original)
    val highKey = MemoryCache.Key(quality.high)
    return when {
        memoryCache?.get(originalKey) != null -> quality.original
        memoryCache?.get(highKey) != null -> quality.high
        else -> quality.medium
    }
}
