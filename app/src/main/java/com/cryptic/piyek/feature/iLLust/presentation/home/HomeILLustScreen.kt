package com.cryptic.piyek.feature.iLLust.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptic.piyek.core.content.presentation.ArtworkFeedScreen

/**
 * Top-level composable for the Illustration home screen.
 * Delegates feed rendering, pagination, and focus-tracking to [ArtworkFeedScreen].
 */
@Composable
fun HomeILLustScreen(
    homeILLustViewModel: HomeILLustViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by homeILLustViewModel.uiState.collectAsStateWithLifecycle()

    ArtworkFeedScreen(
        uiState = uiState,
        onAction = homeILLustViewModel::onAction,
        modifier = modifier,
        rankingTitle = "Ranking",
    )
}