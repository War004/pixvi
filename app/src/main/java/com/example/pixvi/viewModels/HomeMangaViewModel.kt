package com.example.pixvi.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pixvi.network.BookmarkRestrict
import com.example.pixvi.network.api.PixivApiService
import com.example.pixvi.network.response.Home.Manga.Illust
import com.example.pixvi.network.response.Home.Manga.RankingIllust
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import com.example.pixvi.utils.PageIndicies

data class MangaUiState(
    val isLoading: Boolean = false,
    val recommendations: List<Illust> = emptyList(),
    val rankingIllusts: List<RankingIllust> = emptyList(),
    val nextUrl: String? = null,
    val indices: PageIndicies = PageIndicies(null,null,null),
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null
)

class MangaViewModel(
    private val pixivApiService: PixivApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MangaUiState())
    val uiState: StateFlow<MangaUiState> = _uiState.asStateFlow()

    fun loadInitialMangaRecommendations() {
        Log.d(
            "ViewModelDebug",
            "MangaViewModel loadInitialMangaRecommendations called. " +
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
                val response = pixivApiService.getRecommendedManga()

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _uiState.update {
                            MangaUiState(
                                isLoading = false,
                                recommendations = body.illusts ?: emptyList(),
                                rankingIllusts = body.ranking_illusts?: emptyList(),
                                nextUrl = body.next_url
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Received empty response for manga") }
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

    fun loadMoreMangaRecommendations() {
        val currentNextUrl = _uiState.value.nextUrl
        if (currentNextUrl == null || _uiState.value.isLoadingMore || _uiState.value.isLoading) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
            try {
                val response = pixivApiService.getNextManga(currentNextUrl)

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
                        _uiState.update { it.copy(isLoadingMore = false, errorMessage = if (body == null) "Received empty response" else "No more manga found") }
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

    fun toggleBookmark(illustId: Long, isCurrentlyBookmarked: Boolean, visibility: BookmarkRestrict) {
        viewModelScope.launch {

            val response: retrofit2.Response<Unit>

            if (!isCurrentlyBookmarked) { //the image is not bookmarked
                try{
                    if(visibility == BookmarkRestrict.PUBLIC){
                        response = pixivApiService.addBookmarkIllust(illustId = illustId, restrict = "public")
                    }
                    else{
                        response = pixivApiService.addBookmarkIllust(illustId = illustId, restrict = "private")
                    }

                    if(response.isSuccessful){
                        updateIllustBookmarkStateInList(illustId, newBookmarkState = true)
                    }
                    else{
                        Log.e("Bookmark", "Failed to add bookmark. Code: ${response.code()}, Message: ${response.message()}")
                        //add a toast later
                    }
                }catch (e: Exception){
                    Log.e("Bookmark", "Network error while adding bookmark: $e")
                    //add a toast later
                }
            }
            else{
                try{
                    response = pixivApiService.deleteBookmarkIllust(illustId = illustId)
                    if (response.isSuccessful){
                        updateIllustBookmarkStateInList(illustId, newBookmarkState = false)
                    }
                }catch (e: Exception){
                    Log.e("Bookmark", "Network error while adding bookmark: $e")
                }
            }
        }
    }

    private fun updateIllustBookmarkStateInList(illustId: Long, newBookmarkState: Boolean) {
        _uiState.update { currentState ->
            val updatedRecommendations = currentState.recommendations.map { illust ->
                // Search for the correct illust by its stable ID
                if (illust.id.toLong() == illustId) {
                    // Create a new copy of the illust with the updated value for is_bookmarked and the total_bookmarks
                    illust.copy(
                        is_bookmarked = newBookmarkState,
                        total_bookmarks = if (newBookmarkState) illust.total_bookmarks + 1 else illust.total_bookmarks - 1
                    )
                } else {
                    // Leave all other illusts unchanged
                    illust
                }
            }
            // Return a new copy of the UI state containing the new list
            currentState.copy(recommendations = updatedRecommendations)
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

class MangaViewModelFactory(
    private val pixivApiService: PixivApiService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MangaViewModel::class.java)) {
            return MangaViewModel(pixivApiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for MangaViewModelFactory")
    }
}