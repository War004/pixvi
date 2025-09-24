package com.example.pixvi.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pixvi.MyApplication.Companion.connectivityObserver
import com.example.pixvi.bookmark.BookmarkRepository
import com.example.pixvi.network.BookmarkRestrict
import com.example.pixvi.network.response.Home.Illust
import com.example.pixvi.network.response.Home.basePost
import com.example.pixvi.settings.SettingsRepository
import com.example.pixvi.utils.PageIndicies
import com.example.pixvi.utils.Result
import com.example.pixvi.utils.UiEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

// Common UI state using the shared Illust data class
data class ContentUiState(
    val isLoading: Boolean = false,
    val recommendations: List<Illust> = emptyList(),
    val rankingIllusts: List<Illust> = emptyList(),
    val nextUrl: String? = null,
    val indices: PageIndicies = PageIndicies(null, null, null),
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null
)

abstract class BaseContentViewModel(
    protected val bookMarkRepository: BookmarkRepository? = null,
    protected val settingsRepository: SettingsRepository
) : ViewModel() {

    protected val _uiState = MutableStateFlow(ContentUiState())
    val uiState: StateFlow<ContentUiState> = _uiState.asStateFlow()

    // For showing events like bookmark
    protected val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    // Abstract methods for specific implementations
    protected abstract suspend fun fetchInitialData(): Response<basePost>
    protected abstract suspend fun fetchMoreData(nextUrl: String): Response<basePost>
    protected abstract fun getContentTypeName(): String

    // Common loading logic
    fun loadInitialContent() {
        Log.d(
            "ViewModelDebug",
            "${getContentTypeName()} loadInitialContent called. " +
                    "HashCode: ${this.hashCode()}, " +
                    "Recommendations size: ${_uiState.value.recommendations.size}, " +
                    "isLoading: ${_uiState.value.isLoading}"
        )

        if (_uiState.value.recommendations.isNotEmpty() || _uiState.value.isLoading) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = fetchInitialData()

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _uiState.update {
                            ContentUiState(
                                isLoading = false,
                                recommendations = body.illusts,
                                rankingIllusts = body.ranking_illusts,
                                nextUrl = body.next_url
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Received empty response") }
                    }
                } else {
                    val errorMsg = "API Error ${response.code()}: ${response.message()}"
                    _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error: ${e.message}") }
            }
        }
    }

    fun loadMoreContent() {
        val currentNextUrl = _uiState.value.nextUrl
        if (currentNextUrl == null || _uiState.value.isLoadingMore || _uiState.value.isLoading) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
            try {
                val response = fetchMoreData(currentNextUrl)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.illusts != null) {
                        _uiState.update { currentState ->
                            currentState.copy(
                                recommendations = currentState.recommendations + body.illusts,
                                nextUrl = body.next_url,
                                isLoadingMore = false
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(isLoadingMore = false, errorMessage = "No more content found")
                        }
                    }
                } else {
                    val errorMsg = "API Error ${response.code()}: ${response.message()}"
                    _uiState.update { it.copy(isLoadingMore = false, errorMessage = errorMsg) }
                }
            } catch (e: IOException) {
                _uiState.update { it.copy(isLoadingMore = false, errorMessage = "Network Error: ${e.message}") }
            } catch (e: HttpException) {
                _uiState.update { it.copy(isLoadingMore = false, errorMessage = "HTTP Error: ${e.message}") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false, errorMessage = "Error: ${e.message}") }
            }
        }
    }

    // Common bookmark functionality
    fun bookmarkToggled(illustId: Long, isCurrentlyBookmarked: Boolean, visibility: BookmarkRestrict) {
        if (bookMarkRepository == null) {
            Log.w("Bookmark", "BookmarkRepository not available for this ViewModel")
            return
        }

        Log.d("BookmarkDebug", "Function bookmarkToggled was CALLED for illustId: $illustId")

        viewModelScope.launch {
            val status = connectivityObserver.observe().first()
            if (status != ConnectivityObserver.Status.Available) {
                val message = "No internet connection"
                _uiEvents.emit(UiEvent.ShowToast(message))
                return@launch
            }

            val result = bookMarkRepository.toggleBookmark(illustId, isCurrentlyBookmarked, visibility)

            when (result) {
                is Result.Success -> {
                    val newBookmarkState = result.data
                    updateIllustBookmarkStateInList(illustId, newBookmarkState)
                }
                is Result.Error -> {
                    val message = result.message
                    _uiEvents.emit(UiEvent.ShowToast(message))
                }
            }
        }
    }

    suspend fun performToggleBookmark(
        illustId: Long,
        isCurrentlyBookmarked: Boolean,
        visibility: BookmarkRestrict
    ): Result<Boolean> {
        return try {//////////
            bookMarkRepository?.toggleBookmark(illustId, isCurrentlyBookmarked, visibility)
            Result.Success(!isCurrentlyBookmarked)
        } catch (e: Exception) {
            Log.e(getContentTypeName(), "Failed to toggle bookmark for $illustId", e)
            Result.Error(
                -2,
                message = "Failed to update bookmark: ${e.message}"
            )
        }
    }

    fun updateIllustBookmarkStateInList(illustId: Long, newBookmarkState: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                recommendations = currentState.recommendations.map { illust ->
                    if (illust.id.toLong() == illustId) {
                        illust.copy(is_bookmarked = newBookmarkState)
                    } else illust
                },
                rankingIllusts = currentState.rankingIllusts.map { illust ->
                    if (illust.id.toLong() == illustId) {
                        illust.copy(is_bookmarked = newBookmarkState)
                    } else illust
                }
            )
        }
    }

    fun updateNavigationIndices(recommendationsIndex: Int?, subPageIndex: Int?, rankingIndex: Int?) {
        _uiState.update { currentState ->
            val newIndices = PageIndicies(
                recommendationsCurrentIndex = recommendationsIndex,
                subRecommendationsCurrentIndex = subPageIndex,
                rankingCurrentIndex = rankingIndex
            )
            currentState.copy(indices = newIndices)
        }
    }

    fun updateLastViewedIndex(index: Int) {
        _uiState.update { currentState ->
            if (currentState.indices.rankingCurrentIndex != null) {
                currentState.copy(indices = currentState.indices.copy(rankingCurrentIndex = index))
            } else {
                currentState.copy(indices = currentState.indices.copy(recommendationsCurrentIndex = index))
            }
        }
    }
}