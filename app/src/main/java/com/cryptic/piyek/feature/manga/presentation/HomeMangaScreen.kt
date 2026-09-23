package com.cryptic.piyek.feature.manga.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptic.piyek.core.content.presentation.ArtworkFeedScreen

/**
 * Top-level composable for the Manga home screen.
 * Delegates feed rendering, pagination, and focus-tracking to [ArtworkFeedScreen].
 */
@Composable
fun HomeMangaScreen(
    homeMangaViewModel: HomeMangaViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by homeMangaViewModel.uiState.collectAsStateWithLifecycle()

    ArtworkFeedScreen(
        uiState = uiState,
        onAction = homeMangaViewModel::onAction,
        modifier = modifier,
        rankingTitle = "Manga Ranking",
    )
}
