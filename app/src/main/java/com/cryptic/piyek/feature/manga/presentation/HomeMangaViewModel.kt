package com.cryptic.piyek.feature.manga.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptic.piyek.core.CResponse
import com.cryptic.piyek.core.content.data.model.Artwork
import com.cryptic.piyek.core.content.data.model.ArtworkContentList
import com.cryptic.piyek.core.content.data.model.RecommendationNonNovelPara
import com.cryptic.piyek.core.content.domain.repo.CoreContentApiRepo
import com.cryptic.piyek.core.data.local.BookmarkRestrict
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.cryptic.piyek.core.content.presentation.ArtworkFeedAction
import com.cryptic.piyek.core.content.presentation.ScreenStatus as CoreScreenStatus

object HomeMangaAction {
    val Retry: ArtworkFeedAction = ArtworkFeedAction.Retry
    val LoadMore: ArtworkFeedAction = ArtworkFeedAction.LoadMore
    fun UpdateFocusedIndex(index: Int): ArtworkFeedAction = ArtworkFeedAction.UpdateFocusedIndex(index)
    fun ToggleBookmark(
        postId: Long,
        isCurrentlyBookmarked: Boolean,
        restrict: BookmarkRestrict? = null,
    ): ArtworkFeedAction = ArtworkFeedAction.ToggleBookmark(postId, isCurrentlyBookmarked, restrict)
    fun NavigateToArtwork(artworkId: Long): ArtworkFeedAction = ArtworkFeedAction.NavigateToArtwork(artworkId)
    fun NavigateToUser(userId: Int): ArtworkFeedAction = ArtworkFeedAction.NavigateToUser(userId)
    val NavigateToRankingList: ArtworkFeedAction = ArtworkFeedAction.NavigateToRankingList
    fun LongPressArtwork(artwork: Artwork, pageIndex: Int): ArtworkFeedAction = ArtworkFeedAction.LongPressArtwork(artwork, pageIndex)
}

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class HomeMangaViewModel(
    private val mangaRepo: CoreContentApiRepo<ArtworkContentList, RecommendationNonNovelPara, ArtworkContentList>,
) : ViewModel() {

    private val _isLoadingMore = MutableStateFlow(false)
    private val _uiErrors = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeMangaUiState> = combine(
        mangaRepo.contentList,
        _isLoadingMore,
        _uiErrors,
    ) { contentList, isLoadingMore, error ->
        when {
            contentList == null -> {
                HomeMangaUiState(
                    status = if (error != null) ScreenStatus.Idle else ScreenStatus.Loading,
                    errorMessage = error,
                )
            }
            else -> {
                HomeMangaUiState(
                    status = if (isLoadingMore) ScreenStatus.LoadingMore else ScreenStatus.Idle,
                    artworkList = contentList.artworkList,
                    rankingList = contentList.rankingArtworkList,
                    focusedIndex = contentList.focusedIndex,
                    errorMessage = error,
                )
            }
        }
    }
        .onStart {
            if (mangaRepo.contentList.value == null) {
                getRecommendation()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = HomeMangaUiState(status = ScreenStatus.Loading),
        )

    // ---------------------------------------------------------------------------
    // Single public entry-point
    // ---------------------------------------------------------------------------

    fun onAction(action: ArtworkFeedAction) {
        when (action) {
            ArtworkFeedAction.Retry -> getRecommendation()

            ArtworkFeedAction.LoadMore -> {
                if (!_isLoadingMore.value) {
                    getMoreRecommendation()
                }
            }

            is ArtworkFeedAction.UpdateFocusedIndex -> {
                mangaRepo.changeFocusedIndex(action.index)
            }

            is ArtworkFeedAction.ToggleBookmark -> {
                onBookmarkClick(
                    postId = action.postId,
                    isCurrentlyBookmarked = action.isCurrentlyBookmarked,
                    restrict = action.restrict,
                )
            }

            // ---- Defined, not yet implemented ----
            is ArtworkFeedAction.NavigateToArtwork -> { /* TODO */ }
            is ArtworkFeedAction.NavigateToUser -> { /* TODO */ }
            ArtworkFeedAction.NavigateToRankingList -> { /* TODO */ }
            is ArtworkFeedAction.LongPressArtwork -> { /* TODO */ }
        }
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    private fun getRecommendation() {
        val para = RecommendationNonNovelPara(
            includeRanking = true,
            includePrivacyPolicy = false,
        )

        viewModelScope.launch {
            _uiErrors.update { null }

            when (val response = mangaRepo.getRecommendation(para)) {
                is CResponse.Success -> {
                    // Content list is updated internally via the repo flow.
                }
                is CResponse.Failed -> {
                    _uiErrors.update {
                        response.exception.message ?: "No error message provided!"
                    }
                }
            }
        }
    }

    private fun getMoreRecommendation() {
        viewModelScope.launch {
            _isLoadingMore.update { true }
            _uiErrors.update { null }

            when (val response = mangaRepo.getMoreRecommendation()) {
                is CResponse.Success -> {
                    // List gets updated internally via the repo.
                }
                is CResponse.Failed -> {
                    _uiErrors.update {
                        response.exception.message ?: "No error message provided!"
                    }
                }
            }
            _isLoadingMore.update { false }
        }
    }

    private fun onBookmarkClick(
        postId: Long,
        isCurrentlyBookmarked: Boolean,
        restrict: BookmarkRestrict? = null,
    ) {
        viewModelScope.launch {
            try {
                _uiErrors.update { null }
                if (isCurrentlyBookmarked) {
                    mangaRepo.deleteBookmark(postId)
                } else {
                    mangaRepo.addBookmark(postId, restrict ?: BookmarkRestrict.PUBLIC)
                }
            } catch (e: Exception) {
                _uiErrors.update { e.message ?: "Failed to update bookmark" }
            }
        }
    }
}