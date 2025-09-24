package com.example.pixvi.viewModels


import android.app.Application
import android.util.Log
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pixvi.data.local.AppDatabase
import com.example.pixvi.data.local.NovelHistory.NovelHistoryRepository
import com.example.pixvi.network.BookmarkRestrict
import com.example.pixvi.network.api.PixivApiService
import com.example.pixvi.network.response.Home.Novels.Novel
import com.example.pixvi.network.response.Home.Novels.NovelForDisplay
import com.example.pixvi.network.response.Home.Novels.RankingNovel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class HomeNovelViewModel(
    private val pixivApiService: PixivApiService,
    private val historyRepository: NovelHistoryRepository
) : ViewModel() {

    data class NovelUiState(
        val isLoading: Boolean = false,
        val recommendations: List<NovelForDisplay> = emptyList(),
        val rankingNovel: List<RankingNovel> = emptyList(),
        val nextUrl: String? = null,
        val isLoadingMore: Boolean = false,
        val errorMessage: String? = null
    )

    private val TAG = "HomeNovelViewModel"

    private val _uiState = MutableStateFlow(NovelUiState())
    val uiState: StateFlow<NovelUiState> = _uiState.asStateFlow()

    init {
        loadInitialRecommendations()
    }

    private fun Novel.toNovelForDisplay(): NovelForDisplay {

        return NovelForDisplay(
            id = this.id,
            title = this.title,
            user = this.user,
            image_urls = this.image_urls,
            caption = this.caption,
            page_count = this.page_count,
            text_length = this.text_length,
            total_view = this.total_view,
            total_bookmarks = this.total_bookmarks,
            total_comments = this.total_comments,
            is_bookmarked = this.is_bookmarked,
            tags = this.tags,
        )
    }

    /**
     * Fetch the initial set of recommendations.
     * Headers are now handled by the Interceptor, no need to pass them here.
     */
    fun loadInitialRecommendations() {

        viewModelScope.launch {
            Log.d(TAG, "Loading the intial recommedation")
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val historyPayload = historyRepository.prepareHistoryForApi()
                Log.d(TAG, "$historyPayload")
                val response = pixivApiService.getRecommendedNovel(
                    readNovelIds = historyPayload.readNovelIds,
                    readNovelDatetimes = historyPayload.readNovelDatetimes,
                    viewNovelIds = historyPayload.viewNovelIds,
                    viewNovelDatetimes = historyPayload.viewNovelDatetimes
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val novelsFromNetwork = body.novels ?: emptyList()

                        val novelsForDisplay = withContext(Dispatchers.Default) {
                            novelsFromNetwork.map { it.toNovelForDisplay() }
                        }

                        _uiState.update {

                            it.copy( // Using `it.copy` to avoid resetting other state fields
                                isLoading = false,
                                recommendations = novelsForDisplay,
                                rankingNovel = body.ranking_novels ?: emptyList(),
                                nextUrl = body.next_url
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Received empty response") }
                    }
                } else {
                    val errorMsg = "API Error ${response.code()}: ${response.message()}"
                    Log.d(TAG, "Error while response: $response")
                    _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error: ${e.message}") }
            }
        }
    }

    /**
     * Fetch additional recommendations (pagination).
     * Headers are handled by the Interceptor.
     */
    fun loadMoreRecommendations() {
        val currentNextUrl = _uiState.value.nextUrl
        // Check if there's a next page URL and if we're not already loading more or initial data.
        if (currentNextUrl == null || _uiState.value.isLoadingMore || _uiState.value.isLoading) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) } // Clear previous errors on new attempt
            try {
                val response = pixivApiService.getNextNovel(currentNextUrl)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.novels != null) {
                        // Append new items and update next URL
                        val newNovelsFromNetwork = body.novels

                        val newNovelsForDisplay = withContext(Dispatchers.Default) {
                            newNovelsFromNetwork.map { it.toNovelForDisplay() }
                        }

                        _uiState.update { currentState ->
                            currentState.copy(
                                recommendations = currentState.recommendations + newNovelsForDisplay,
                                nextUrl = body.next_url,
                                isLoadingMore = false
                            )
                        }
                    } else {
                        // Handle successful response but null body or null novel
                        // Keep the current nextUrl, stop loading more
                        _uiState.update { it.copy(isLoadingMore = false, errorMessage = if (body == null) "Received empty response" else "No more novels found") }
                    }
                } else {
                    // Handle HTTP error for pagination
                    val errorMsg = "API Error ${response.code()}: ${response.message()}"
                    _uiState.update { it.copy(isLoadingMore = false, errorMessage = errorMsg) }
                }

            } catch (e: kotlinx.io.IOException) {
                _uiState.update { it.copy(isLoadingMore = false, errorMessage = "Network Error: ${e.message}") }
            } catch (e: HttpException) {
                _uiState.update { it.copy(isLoadingMore = false, errorMessage = "HTTP Error: ${e.message}") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false, errorMessage = "Error: ${e.message}") }
            }
        }
    }

    /**
     * Records a "VIEW" event for a specific novel in the local history database.
     *
     * @param novelId The ID of the novel that was viewed.
     */
    fun onNovelViewed(novelId: Int) { // <-- CHANGED from String to Int
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to add VIEW event for novel ID: $novelId")

                historyRepository.addViewEvent(novelId)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to add VIEW event for novel ID: $novelId", e)
            }
        }
    }

    fun toggleBookmark(illustId: Long, isCurrentlyBookmarked: Boolean, visibility: BookmarkRestrict) {
        viewModelScope.launch {
            //Calling the api based on visibility,

            val response: retrofit2.Response<Unit>

            if (!isCurrentlyBookmarked) { //the image is not bookmarked
                try{
                    if(visibility == BookmarkRestrict.PUBLIC){
                        response = pixivApiService.addBookmarkNovel(illustId = illustId, restrict = "public")
                    }
                    else{
                        response = pixivApiService.addBookmarkNovel(illustId = illustId, restrict = "private")
                    }

                    if(response.isSuccessful){
                        updateNovelBookmarkStateInList(illustId, newBookmarkState = true)
                        Log.e("Bookmark", "Done")
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
                    response = pixivApiService.deleteBookmarkNovel(illustId = illustId)
                    if (response.isSuccessful){
                        updateNovelBookmarkStateInList(illustId, newBookmarkState = false)
                        Log.e("Bookmark", "Done")
                    }
                }catch (e: Exception){
                    Log.e("Bookmark", "Network error while adding bookmark: $e")
                }
            }
        }
    }

    private fun updateNovelBookmarkStateInList(illustId: Long, newBookmarkState: Boolean) {
        _uiState.update { currentState ->
            val updatedRecommendations = currentState.recommendations.map { novel  ->
                // Search for the correct novel by its stable ID
                if (novel .id.toLong() == illustId) {
                    // Create a new copy of the novel with the updated value for is_bookmarked and the total_bookmarks
                    novel .copy(
                        is_bookmarked = newBookmarkState,
                        total_bookmarks = if (newBookmarkState) novel .total_bookmarks + 1 else novel .total_bookmarks - 1
                    )
                } else {
                    // Leave all other novel unchanged
                    novel
                }
            }
            // Return a new copy of the UI state containing the new list
            currentState.copy(recommendations = updatedRecommendations)
        }
    }

}

class HomeNovelViewModelFactory(
    private val application: Application,
    private val pixivApiService: PixivApiService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeNovelViewModel::class.java)) {
            // 1. Create the dependencies here
            val historyDao = AppDatabase.getDatabase(application).novelHistoryDao()
            val historyRepository = NovelHistoryRepository(historyDao)

            // 2. Create the ViewModel and pass the dependencies in
            @Suppress("UNCHECKED_CAST")
            return HomeNovelViewModel(pixivApiService, historyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}