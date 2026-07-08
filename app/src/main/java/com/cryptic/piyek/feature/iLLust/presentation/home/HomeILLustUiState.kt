package com.cryptic.piyek.feature.iLLust.presentation.home

import com.cryptic.piyek.core.content.data.model.Artwork

data class HomeILLustUiState(
    val status: ScreenStatus,
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