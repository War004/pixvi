package com.cryptic.piyek.feature.novel.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptic.piyek.core.CResponse
import com.cryptic.piyek.core.content.data.model.RecommendationNovelPara
import com.cryptic.piyek.core.content.domain.repo.CoreContentApiRepo
import com.cryptic.piyek.core.content.novel.domain.model.NovelHome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeNovelViewModel(
    private val novelRepo: CoreContentApiRepo<NovelHome, RecommendationNovelPara, NovelHome>
) : ViewModel() {
    private val _isLoadingMore = MutableStateFlow(false)
    private val _uiErrors = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeNovelUiState> = combine(
        novelRepo.contentList,
        _isLoadingMore,
        _uiErrors
    ) { contentList, isLoadingMore, error ->
        when {
            contentList == null -> {
                HomeNovelUiState(
                    status = if (error != null) ScreenState.Idle else ScreenState.Loading,
                    errorMessage = error
                )
            }
            else -> {
                HomeNovelUiState(
                    status = if (isLoadingMore) ScreenState.LoadingMore else ScreenState.Idle,
                    novelList = contentList.novels.map { it.toNovelCardUi() },
                    rankingList = contentList.rankingNovels.map { it.toNovelCardUi() },
                    errorMessage = error
                )
            }
        }
    }.onStart {
        if (novelRepo.contentList.value == null) {
            getRecommendation()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HomeNovelUiState(status = ScreenState.Loading)
    )

    fun onAction(action: HomeNovelScreenActions) {
        when (action) {
            HomeNovelScreenActions.Retry -> getRecommendation()
            HomeNovelScreenActions.LoadMore -> {
                if (!_isLoadingMore.value) {
                    getMoreRecommendation()
                }
            }
            is HomeNovelScreenActions.ToggleBookmark -> {
                // TODO: Wire up repository call to toggle bookmark for action.novelId
            }
            is HomeNovelScreenActions.OpenNovel -> {
                // TODO: Navigate to novel detail/reader screen for action.novelId
            }
            is HomeNovelScreenActions.OpenAuthor -> {
                // TODO: Navigate to author profile for action.userId
            }
            is HomeNovelScreenActions.PressForMoreInfo -> {
                // TODO: Show detailed bottom sheet or dialog for action.novelId
            }
            is HomeNovelScreenActions.PushToDownload -> {
                // TODO: Enqueue offline download job for action.novelId
            }
            is HomeNovelScreenActions.PushToSpeechService -> {
                // TODO: Route novel content to background TTS speech service for action.novelId
            }
            is HomeNovelScreenActions.FilterByTag -> {
                // TODO: Apply tag filter or navigate to tag search for action.tag
            }
            is HomeNovelScreenActions.CopyFeedback -> {
                // TODO: Show user notification or snackbar for action.message
            }
        }
    }

    private fun getRecommendation() {
        val para = RecommendationNovelPara(
            includeRanking = true,
            includePrivacyPolicy = true,
            readNovelIds = emptyList(),
            readNovelDateTimes = emptyList(),
            viewNovelIds = emptyList(),
            viewNovelDataTimes = emptyList()
        )
        viewModelScope.launch {
            _uiErrors.update { null }

            when (val response = novelRepo.getRecommendation(para)) {
                is CResponse.Success -> {
                    /* Nothing */
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

            when (val response = novelRepo.getMoreRecommendation()) {
                is CResponse.Success -> {
                    // Nothing
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
}
