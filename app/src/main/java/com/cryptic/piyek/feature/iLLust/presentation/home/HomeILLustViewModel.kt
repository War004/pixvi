package com.cryptic.piyek.feature.iLLust.presentation.home

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

// ---------------------------------------------------------------------------
// Actions – sealed interface defining every possible user/system intent
// ---------------------------------------------------------------------------

sealed interface HomeILLustAction {

    /** Initial data load / retry after error. */
    data object Retry : HomeILLustAction

    /** Triggered when the user scrolls near the end of the feed. */
    data object LoadMore : HomeILLustAction

    /** Scroll-driven: the artwork closest to the viewport center changed. */
    data class UpdateFocusedIndex(val index: Int) : HomeILLustAction

    /** Toggle bookmark state for a given artwork. */
    data class ToggleBookmark(
        val postId: Long,
        val isCurrentlyBookmarked: Boolean,
        val restrict: BookmarkRestrict? = null,
    ) : HomeILLustAction

    /** Navigate to the full artwork detail view. */
    data class NavigateToArtwork(val artworkId: Long) : HomeILLustAction

    /** Navigate to the user's profile. */
    data class NavigateToUser(val userId: Int) : HomeILLustAction

    /** Navigate to the full ranking list screen. */
    data object NavigateToRankingList : HomeILLustAction

    /** Long-press on an artwork (e.g. to show a context menu). */
    data class LongPressArtwork(
        val artwork: Artwork,
        val pageIndex: Int,
    ) : HomeILLustAction
}

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class HomeILLustViewModel(
    private val iLLustRepo: CoreContentApiRepo<ArtworkContentList, RecommendationNonNovelPara, ArtworkContentList>,
) : ViewModel() {

    private val _isLoadingMore = MutableStateFlow(false)
    private val _uiErrors = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeILLustUiState> = combine(
        iLLustRepo.contentList,
        _isLoadingMore,
        _uiErrors,
    ) { contentList, isLoadingMore, error ->
        when {
            contentList == null -> {
                HomeILLustUiState(
                    status = ScreenStatus.Loading,
                    errorMessage = error,
                )
            }
            else -> {
                HomeILLustUiState(
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
            if (iLLustRepo.contentList.value == null) {
                getRecommendation()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = HomeILLustUiState(status = ScreenStatus.Loading),
        )

    // ---------------------------------------------------------------------------
    // Single public entry-point
    // ---------------------------------------------------------------------------

    fun onAction(action: HomeILLustAction) {
        when (action) {
            is HomeILLustAction.Retry -> getRecommendation()

            is HomeILLustAction.LoadMore -> {
                if (!_isLoadingMore.value) {
                    getMoreRecommendation()
                }
            }

            is HomeILLustAction.UpdateFocusedIndex -> {
                iLLustRepo.changeFocusedIndex(action.index)
            }

            is HomeILLustAction.ToggleBookmark -> {
                onBookmarkClick(
                    postId = action.postId,
                    isCurrentlyBookmarked = action.isCurrentlyBookmarked,
                    restrict = action.restrict,
                )
            }

            // ---- Defined, not yet implemented ----
            is HomeILLustAction.NavigateToArtwork -> { /* TODO */ }
            is HomeILLustAction.NavigateToUser -> { /* TODO */ }
            is HomeILLustAction.NavigateToRankingList -> { /* TODO */ }
            is HomeILLustAction.LongPressArtwork -> { /* TODO */ }
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

            when (val response = iLLustRepo.getRecommendation(para)) {
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

            when (val response = iLLustRepo.getMoreRecommendation()) {
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
                    iLLustRepo.deleteBookmark(postId)
                } else {
                    iLLustRepo.addBookmark(postId, restrict ?: BookmarkRestrict.PUBLIC)
                }
            } catch (e: Exception) {
                _uiErrors.update { e.message ?: "Failed to update bookmark" }
            }
        }
    }
}