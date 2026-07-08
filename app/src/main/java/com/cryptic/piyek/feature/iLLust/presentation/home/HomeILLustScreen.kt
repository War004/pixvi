package com.cryptic.piyek.feature.iLLust.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.CarouselItemScope
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cryptic.piyek.core.content.data.model.Artwork
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

/** How many items before the end of the list to start prefetching more. */
private const val PAGINATION_BUFFER = 5

// ---------------------------------------------------------------------------
// Screen entry-point (stateful)
// ---------------------------------------------------------------------------

/**
 * Top-level composable that owns list state, collects the ViewModel's
 * [HomeILLustUiState], and wires pagination + focus-tracking effects.
 *
 * All user interactions are funnelled through [HomeILLustAction] via the
 * ViewModel's `onAction()`.
 */
@OptIn(FlowPreview::class)
@Composable
fun HomeILLustScreen(
    homeILLustViewModel: HomeILLustViewModel,
) {
    val uiState by homeILLustViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // -- Pagination trigger ---------------------------------------------------
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItemCount = layoutInfo.totalItemsCount
            val lastVisibleItemIndex =
                layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            val isNearBottom =
                totalItemCount > 0 && lastVisibleItemIndex >= (totalItemCount - PAGINATION_BUFFER)
            val canLoadMore = uiState.status == ScreenStatus.Idle

            isNearBottom && canLoadMore
        }
            .distinctUntilChanged()
            .filter { shouldLoad -> shouldLoad }
            .collect {
                homeILLustViewModel.onAction(HomeILLustAction.LoadMore)
            }
    }

    // -- Active-index tracking ------------------------------------------------
    LaunchedEffect(listState) {
        // Offset to skip the ranking carousel header item.
        val rankingHeaderOffset = if (uiState.rankingList.isNotEmpty()) 1 else 0

        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@snapshotFlow null

            val viewportCenterY = layoutInfo.viewportSize.height / 2f

            visibleItems
                .filter { it.index >= rankingHeaderOffset }
                .minByOrNull { item ->
                    val itemCenterY = item.offset + (item.size / 2f)
                    abs(itemCenterY - viewportCenterY)
                }
                ?.let { it.index - rankingHeaderOffset }
        }
            .debounce(200L)
            .distinctUntilChanged()
            .filterNotNull()
            .collect { index ->
                homeILLustViewModel.onAction(HomeILLustAction.UpdateFocusedIndex(index))
            }
    }

    // -- Render ---------------------------------------------------------------
    HomeILLustContent(
        uiState = uiState,
        listState = listState,
        onAction = homeILLustViewModel::onAction,
    )
}

// ---------------------------------------------------------------------------
// Content (stateless layout)
// ---------------------------------------------------------------------------

@Composable
private fun HomeILLustContent(
    uiState: HomeILLustUiState,
    listState: LazyListState,
    onAction: (HomeILLustAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        when {
            // ---------- Full-screen loading ----------
            uiState.status == ScreenStatus.Loading && uiState.artworkList.isEmpty() -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CircularProgressIndicator()
                }
            }

            // ---------- Full-screen error ----------
            uiState.errorMessage != null && uiState.artworkList.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = uiState.errorMessage,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { onAction(HomeILLustAction.Retry) }) {
                        Text("Retry")
                    }
                }
            }

            // ---------- Artwork feed ----------
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = 8.dp,
                    ),
                ) {
                    // ---- Ranking carousel header ----
                    if (uiState.rankingList.isNotEmpty()) {
                        item(key = "ranking-carousel") {
                            RankingCarousel(
                                artworkList = uiState.rankingList,
                                onItemClick = { /* TODO */ },
                                onSeeMoreClick = { /* TODO */ },
                            )
                        }
                    }

                    // ---- Artwork feed items ----
                    itemsIndexed(
                        items = uiState.artworkList,
                        key = { _, artwork -> artwork.id },
                    ) { index, artwork ->
                        ArtworkFeedItem(
                            artwork = artwork,
                            isFocused = index == uiState.focusedIndex,
                            onClick = { /* TODO */ },
                            onLongPress = { _, _ -> /* TODO */ },
                        )
                    }

                    // ---- Loading-more spinner ----
                    if (uiState.status == ScreenStatus.LoadingMore) {
                        item(key = "loading-more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    // ---- Inline error at bottom ----
                    if (uiState.errorMessage != null && uiState.artworkList.isNotEmpty()) {
                        item(key = "inline-error") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Error loading more: ${uiState.errorMessage}",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { onAction(HomeILLustAction.LoadMore) }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// RankingCarousel
// ---------------------------------------------------------------------------

/**
 * Horizontal carousel of top-ranked artworks displayed at the head of the feed.
 * Uses Material 3 [HorizontalUncontainedCarousel] for a peek-next-item UX.
 */
@Composable
fun RankingCarousel(
    artworkList: List<Artwork>,
    modifier: Modifier = Modifier,
    onItemClick: (artworkIndex: Int) -> Unit,
    onSeeMoreClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // -- Title row --------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = "Ranking",
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Ranking",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onSeeMoreClick) {
                Text("See more")
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "See more ranking illustrations",
                )
            }
        }

        // -- Carousel ---------------------------------------------------------
        val carouselState = rememberCarouselState { artworkList.size }

        HorizontalUncontainedCarousel(
            state = carouselState,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            itemWidth = 186.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) { index ->
            CarouselArtworkItem(
                artwork = artworkList[index],
                onItemClick = { onItemClick(index) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// CarouselArtworkItem
// ---------------------------------------------------------------------------

/**
 * Individual card within the [RankingCarousel].
 * Shows the artwork image with the author's name overlaid at the bottom.
 */
@Composable
fun CarouselItemScope.CarouselArtworkItem(
    artwork: Artwork,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .maskClip(MaterialTheme.shapes.extraLarge)
            .clickable { onItemClick() },
    ) {
        AsyncImage(
            model = artwork.quality.firstPage.medium,
            contentDescription = artwork.title,
            contentScale = ContentScale.Crop,
        )

        Text(
            text = "@ ${artwork.user.userProfile}",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    offset = Offset(2f, 2f),
                    blurRadius = 4f,
                ),
            ),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 15.dp, bottom = 10.dp),
        )
    }
}