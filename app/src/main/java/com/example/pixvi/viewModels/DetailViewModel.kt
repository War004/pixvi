package com.example.pixvi.viewModels

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pixvi.MyApplication.Companion.connectivityObserver
import com.example.pixvi.repo.SystemInfoRepository
import com.example.pixvi.network.BookmarkRestrict
import com.example.pixvi.network.response.Home.ImageUtils
import com.example.pixvi.network.response.Home.saveBitmapToMediaStore
import com.example.pixvi.repo.BatterySaverThemeRepository
import com.example.pixvi.utils.Result
import com.example.pixvi.utils.UiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.pixvi.screens.detail.ContentType
import com.example.pixvi.settings.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn


sealed interface BookmarkEffect {
    val token: String

    data class Processing(override val token: String) : BookmarkEffect
    data class Success(override val token: String, val newBookmarkState: Boolean) : BookmarkEffect
    data class Failure(override val token: String, val errorMessage: String, val errorCode: Int) : BookmarkEffect
}

class DetailViewModel(
    private val homeIllustViewModel: HomeIllustViewModel,
    private val mangaViewModel: MangaViewModel,
    private val contentType: ContentType,
    initialState: DetailScreenState,
    isBatterySaverTheme: BatterySaverThemeRepository
) : ViewModel() {

    private val _uiEvents = MutableSharedFlow<BookmarkEffect>()
    val uiEvent = _uiEvents.asSharedFlow()

    private val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    private val _imageQuality = MutableStateFlow(ImageQuality.AUTO)
    val imageQuality = _imageQuality.asStateFlow()

    val isPowerSaverTheme: StateFlow<Boolean> = isBatterySaverTheme.batterSaver

    fun cycleImageQuality() {
        _imageQuality.update {
            when (it) {
                ImageQuality.AUTO -> ImageQuality.LARGE
                ImageQuality.LARGE -> ImageQuality.MEDIUM
                ImageQuality.MEDIUM -> ImageQuality.AUTO
            }
        }
    }

    fun loadMore() {
        when (contentType) {
            ContentType.ILLUST -> homeIllustViewModel.loadMoreRecommendations()
            ContentType.MANGA -> mangaViewModel.loadMoreMangaRecommendations()
        }
    }

    fun toggleBookmark(illustId: Long, requestToken: String) {
        val currentItem = _uiState.value.items.find { it.id == illustId } ?: return
        val newBookmarkState = !currentItem.isBookmarked

        viewModelScope.launch {
            _uiEvents.emit(BookmarkEffect.Processing(requestToken))

            when (contentType) {
                ContentType.ILLUST -> {
                    val status = connectivityObserver.observe().first()
                    if (status != ConnectivityObserver.Status.Available) {
                        _uiEvents.emit(BookmarkEffect.Failure(requestToken, "No internet. Please check device settings", -1))
                        //_uiEvents.emit(UiEvent.ShowToast("No internet connection"))
                        //_uiEvents.emit(UiEvent.isBookmarked(null))
                        //markAnimationFailureIfPending(illustId)
                        return@launch
                    }

                    val result = homeIllustViewModel.performToggleBookmark(
                        illustId,
                        !newBookmarkState,
                        BookmarkRestrict.PUBLIC
                    )

                    when (result) {
                        is Result.Success -> {
                            val updatedState = result.data
                            homeIllustViewModel.updateIllustBookmarkStateInList(
                                illustId,
                                updatedState
                            )

                            _uiState.update { currentState ->
                                currentState.copy(
                                    items = currentState.items.map { item ->
                                        if (item.id == illustId) {
                                            item.copy(
                                                isBookmarked = updatedState,
                                                totalBookmarks = if (updatedState)
                                                    item.totalBookmarks + 1
                                                else (item.totalBookmarks - 1).coerceAtLeast(0)
                                            )
                                        } else item
                                    }
                                )
                            }

                            _uiEvents.emit(BookmarkEffect.Success(requestToken,updatedState))
                            //updateAnimationResultFromToggle(illustId)
                            //_uiEvents.emit(UiEvent.isBookmarked(true))
                        }

                        is Result.Error -> {
                            _uiEvents.emit(BookmarkEffect.Failure(requestToken, result.message, result.errorCode))
                            //_uiEvents.emit(UiEvent.ShowToast(result.message))
                            //_uiEvents.emit(UiEvent.isBookmarked(null))
                            //markAnimationFailureIfPending(illustId)
                        }
                    }
                }
                ContentType.MANGA -> {
                    val status = connectivityObserver.observe().first()
                    if (status != ConnectivityObserver.Status.Available) {
                        _uiEvents.emit(BookmarkEffect.Failure(requestToken, "No internet. Please check device settings", -1))
                        //_uiEvents.emit(UiEvent.ShowToast("No internet connection"))
                        //_uiEvents.emit(UiEvent.isBookmarked(null))
                        //markAnimationFailureIfPending(illustId)
                        return@launch
                    }

                    val result = mangaViewModel.performToggleBookmark(
                        illustId,
                        !newBookmarkState,
                        BookmarkRestrict.PUBLIC
                    )

                    when (result) {
                        is Result.Success -> {
                            val updatedState = result.data
                            mangaViewModel.updateIllustBookmarkStateInList(
                                illustId,
                                updatedState
                            )

                            _uiState.update { currentState ->
                                currentState.copy(
                                    items = currentState.items.map { item ->
                                        if (item.id == illustId) {
                                            item.copy(
                                                isBookmarked = updatedState,
                                                totalBookmarks = if (updatedState)
                                                    item.totalBookmarks + 1
                                                else (item.totalBookmarks - 1).coerceAtLeast(0)
                                            )
                                        } else item
                                    }
                                )
                            }

                            _uiEvents.emit(BookmarkEffect.Success(requestToken,updatedState))

                            //updateAnimationResultFromToggle(illustId)
                            //_uiEvents.emit(UiEvent.isBookmarked(true))
                        }

                        is Result.Error -> {
                            _uiEvents.emit(BookmarkEffect.Failure(requestToken, result.message, result.errorCode))
                            //_uiEvents.emit(UiEvent.ShowToast(result.message))
                            //_uiEvents.emit(UiEvent.isBookmarked(null))
                            //markAnimationFailureIfPending(illustId)
                        }
                    }
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
    ) {
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
                        Toast.makeText(
                            context,
                            "Failed to load image for saving",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    Log.e(
                        "PixivImageRow",
                        "Failed to load original bitmap for saving device."
                    )
                }
            } catch (e: Exception) {
                Log.e("PixivImageRow", "Error saving image: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            homeIllustViewModel: HomeIllustViewModel,
            mangaViewModel: MangaViewModel,
            contentType: ContentType,
            initialState: DetailScreenState,
            isBatterySaverTheme: BatterySaverThemeRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
                    return DetailViewModel(
                        homeIllustViewModel = homeIllustViewModel,
                        mangaViewModel = mangaViewModel,
                        contentType = contentType,
                        initialState = initialState,
                        isBatterySaverTheme = isBatterySaverTheme
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}