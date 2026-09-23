package com.cryptic.piyek.core.content.presentation

import android.util.Log
import androidx.compose.foundation.BorderStroke
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
import coil3.compose.AsyncImage
import com.cryptic.piyek.core.content.data.model.Artwork
import com.cryptic.piyek.feature.iLLust.presentation.home.ArtworkFeedItem
import com.cryptic.piyek.ui.theme.GoldenRanking
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

private const val PAGINATION_BUFFER = 5

@OptIn(FlowPreview::class)
@Composable
fun ArtworkFeedScreen(
    uiState: ArtworkFeedUiState,
    onAction: (ArtworkFeedAction) -> Unit,
    modifier: Modifier = Modifier,
    rankingTitle: String = "Ranking",
) {
    val listState = rememberLazyListState()

    // -- Pagination trigger ---------------------------------------------------
    LaunchedEffect(listState, uiState.status) {
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
                onAction(ArtworkFeedAction.LoadMore)
            }
    }

    // Track which item is closest to the ~35% screen line to update active focus
    LaunchedEffect(listState, uiState.rankingList.size) {
        snapshotFlow {
            if (!listState.isScrollInProgress) return@snapshotFlow null

            val layoutInfo = listState.layoutInfo
            val viewportHeight = layoutInfo.viewportSize.height
            val rankingHeaderOffset = if (uiState.rankingList.isNotEmpty()) 1 else 0

            if (viewportHeight <= 0 || layoutInfo.totalItemsCount <= rankingHeaderOffset) {
                return@snapshotFlow null
            }

            val targetLineY = (viewportHeight * 0.35f).toInt()

            layoutInfo.visibleItemsInfo
                .filter { it.index >= rankingHeaderOffset }
                .minByOrNull { item -> abs((item.offset + item.size / 2) - targetLineY) }
                ?.let { it.index - rankingHeaderOffset }
        }
            .filterNotNull()
            .distinctUntilChanged()
            .debounce(200L.milliseconds)
            .collect { index ->
                onAction(ArtworkFeedAction.UpdateFocusedIndex(index))
            }
    }

    // -- Render ---------------------------------------------------------------
    ArtworkHomeUI(
        uiState = uiState,
        listState = listState,
        onAction = onAction,
        modifier = modifier,
        rankingTitle = rankingTitle,
    )
}

@Composable
private fun ArtworkHomeUI(
    uiState: ArtworkFeedUiState,
    listState: LazyListState,
    onAction: (ArtworkFeedAction) -> Unit,
    modifier: Modifier = Modifier,
    rankingTitle: String = "Ranking",
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when {
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
                    Button(onClick = { onAction(ArtworkFeedAction.Retry) }) {
                        Text("Retry")
                    }
                }
            }

            // ---------- Full-screen loading ----------
            uiState.status == ScreenStatus.Loading && uiState.artworkList.isEmpty() -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CircularProgressIndicator()
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
                                title = rankingTitle,
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
                                Button(onClick = { onAction(ArtworkFeedAction.LoadMore) }) {
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

@Composable
fun RankingCarousel(
    artworkList: List<Artwork>,
    modifier: Modifier = Modifier,
    title: String = "Ranking",
    onItemClick: (artworkIndex: Int) -> Unit,
    onSeeMoreClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = title,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = onSeeMoreClick,
                border = BorderStroke(
                    width = 1.dp,
                    color = GoldenRanking
                )
            ) {
                Text("See more")
                Icon(
                    modifier = Modifier.padding(start = 10.dp),
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "See more $title",
                )
            }
        }

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
            contentScale = ContentScale.Fit,
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
