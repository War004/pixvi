package com.example.pixvi.viewModels

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pixvi.network.BookmarkRestrict
import com.example.pixvi.network.response.Home.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailViewModel(
    private val homeIllustViewModel: HomeIllustViewModel,
    private val mangaViewModel: MangaViewModel,
    private val contentType: ContentType,
    initialState: DetailScreenState
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    fun loadMore() {
        when (contentType) {
            ContentType.ILLUST -> homeIllustViewModel.loadMoreRecommendations()
            ContentType.MANGA -> mangaViewModel.loadMoreMangaRecommendations()
        }
    }

    fun toggleBookmark(illustId: Long) {
        val currentItem = _uiState.value.items.find { it.id == illustId } ?: return
        val newBookmarkState = !currentItem.isBookmarked

        _uiState.update { currentState ->
            val updatedItems = currentState.items.map { item ->
                if (item.id == illustId) {
                    item.copy(
                        isBookmarked = newBookmarkState,
                        totalBookmarks = if (newBookmarkState) item.totalBookmarks + 1 else (item.totalBookmarks - 1).coerceAtLeast(0)
                    )
                } else {
                    item
                }
            }
            currentState.copy(items = updatedItems)
        }

        viewModelScope.launch {
            when (contentType) {
                ContentType.ILLUST -> {
                    homeIllustViewModel.toggleBookmark(illustId, !newBookmarkState, BookmarkRestrict.PUBLIC)
                }
                ContentType.MANGA -> {
                    mangaViewModel.toggleBookmark(illustId, !newBookmarkState, BookmarkRestrict.PUBLIC)
                }
            }
        }
    }

    fun saveOrginalImageToDevice(
        context: Context,
        originalImageUrl: String,
        illustId: Int,
        displayName: String?,
        currentPageIndex: Int
    ){
        viewModelScope.launch {
            try {
                val bitmap = ImageUtils.loadBitmapFromUrl(context, originalImageUrl)
                if (bitmap != null) {
                    launch(Dispatchers.IO) {
                        ImageUtils.saveBitmapToMediaStore(
                            context = context,
                            bitmap = bitmap,
                            illustId = illustId,
                            pageIndex = currentPageIndex,
                            displayName = displayName
                        )
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load image for saving", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("PixivImageRow", "Failed to load original bitmap for saving device.")
                }
            } catch (e: Exception) {
                Log.e("PixivImageRow", "Error saving image: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            homeIllustViewModel: HomeIllustViewModel,
            mangaViewModel: MangaViewModel,
            contentType: ContentType,
            initialState: DetailScreenState
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
                    return DetailViewModel(
                        homeIllustViewModel = homeIllustViewModel,
                        mangaViewModel = mangaViewModel,
                        contentType = contentType,
                        initialState = initialState
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}