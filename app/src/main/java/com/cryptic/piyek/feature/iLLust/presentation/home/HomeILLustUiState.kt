package com.cryptic.piyek.feature.iLLust.presentation.home

import com.cryptic.piyek.core.content.presentation.ArtworkFeedUiState
import com.cryptic.piyek.core.content.presentation.ScreenStatus as CoreScreenStatus

typealias HomeILLustUiState = ArtworkFeedUiState

object ScreenStatus {
    val Idle: CoreScreenStatus = CoreScreenStatus.Idle
    val Loading: CoreScreenStatus = CoreScreenStatus.Loading
    val LoadingMore: CoreScreenStatus = CoreScreenStatus.LoadingMore
}