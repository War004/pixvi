package com.cryptic.piyek.core.content.presentation

import com.cryptic.piyek.core.content.data.model.Artwork

data class ArtworkFeedUiState(
    val status: ScreenStatus = ScreenStatus.Loading,
    val artworkList: List<Artwork> = emptyList(),
    val rankingList: List<Artwork> = emptyList(),
    val focusedIndex: Int = 0,
    val errorMessage: String? = null
)

sealed interface ScreenStatus {
    data object Idle : ScreenStatus
    data object Loading : ScreenStatus
    data object LoadingMore : ScreenStatus
}
