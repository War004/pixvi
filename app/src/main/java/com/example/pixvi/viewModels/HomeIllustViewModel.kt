package com.example.pixvi.viewModels


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pixvi.network.BookmarkRestrict
import com.example.pixvi.network.api.PixivApiService
import com.example.pixvi.network.response.Home.Illust.Illust
import com.example.pixvi.utils.PageIndicies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class HomeIllustViewModel(
    private val pixivApiService: PixivApiService
) : ViewModel() {

    data class HomePageUiState(
        val isLoading: Boolean = false,
        val recommendations: List<Illust> = emptyList(),
        val rankingIllusts: List<Illust> = emptyList(),
        val nextUrl: String? = null,
        val indices: PageIndicies = PageIndicies(null,null,null),
        val isLoadingMore: Boolean = false,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(HomePageUiState())
    val uiState: StateFlow<HomePageUiState> = _uiState.asStateFlow()



    /*
    init {
        fetchInitialUserState()
    }

    private fun fetchInitialUserState() {
        viewModelScope.launch {
            CurrentAccountManager.setLoadingState(true) // Inform global listeners we're loading
            try {
                val userState = pixivApiService.getUserState() // Replace ApiUserStateResponse

                userState.body()?.profile?.let { CurrentAccountManager.loginAccount(it) }
            } catch (e: Exception) {
                CurrentAccountManager.setErrorState("Network error: ${e.message}")
                CurrentAccountManager.logoutAccount() // Ensure account is cleared on error
            }
        }
    }
     */


    /**
     * Fetch the initial set of recommendations.
     * Headers are now handled by the Interceptor, no need to pass them here.
     */
    fun loadInitialRecommendations() {

        Log.d(
            "ViewModelDebug",
            "HomePageViewModel loadInitialRecommendations called. " +
                    "HashCode: ${this.hashCode()}, " +
                    "Recommendations size: ${_uiState.value.recommendations.size}, " +
                    "isLoading: ${_uiState.value.isLoading}"
        )

        if (_uiState.value.recommendations.isNotEmpty() || _uiState.value.isLoading) {
            Log.d("ViewModelDebug", "HomePageViewModel: Guard clause is TRUE. Returning.")
            return
        }
        Log.d("ViewModelDebug", "HomePageViewModel: Guard clause is FALSE. Proceeding to load.")

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = pixivApiService.getRecommendedIllusts()

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _uiState.update {
                            HomePageUiState(
                                isLoading = false,
                                recommendations = body.illusts ?: emptyList(),
                                rankingIllusts = body.ranking_illusts ?: emptyList(),
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
                // Simplified error handling
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
                // Call the Retrofit service method using the next URL
                val response = pixivApiService.getNextIllusts(currentNextUrl)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.illusts != null) {
                        // Append new items and update next URL
                        _uiState.update { currentState ->
                            currentState.copy(
                                recommendations = currentState.recommendations + body.illusts,
                                nextUrl = body.next_url,
                                isLoadingMore = false
                            )
                        }
                    } else {
                        // Handle successful response but null body or null illusts
                        // Keep the current nextUrl, stop loading more
                        _uiState.update { it.copy(isLoadingMore = false, errorMessage = if (body == null) "Received empty response" else "No more illustrations found") }
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
                    response = pixivApiService.deleteBookmarkIllust(illustId = illustId)
                    if (response.isSuccessful){
                        updateIllustBookmarkStateInList(illustId, newBookmarkState = false)
                        Log.e("Bookmark", "Done")
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
            // Decide whether to update the ranking or recommendations index
            if (currentState.indices.rankingCurrentIndex != null) {
                currentState.copy(indices = currentState.indices.copy(rankingCurrentIndex = index))
            } else {
                currentState.copy(indices = currentState.indices.copy(recommendationsCurrentIndex = index))
            }
        }
    }

}

class HomePageViewModelFactory(
    private val pixivApiService: PixivApiService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeIllustViewModel::class.java)) {
            // Pass the PixivApiService to the ViewModel constructor
            return HomeIllustViewModel(pixivApiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

