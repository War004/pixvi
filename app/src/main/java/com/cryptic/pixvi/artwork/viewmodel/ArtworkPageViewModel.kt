package com.cryptic.pixvi.artwork.viewmodel

import android.util.Log
import androidx.compose.ui.text.font.FontVariation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.memory.MemoryCache
import coil3.toBitmap
import com.cryptic.pixvi.appShell.SettingAction
import com.cryptic.pixvi.artwork.data.ArtworkInfo
import com.cryptic.pixvi.artwork.data.ArtworkPage
import com.cryptic.pixvi.artwork.data.PageIndices
import com.cryptic.pixvi.artwork.data.Quality
import com.cryptic.pixvi.core.downloader.image.DownloadImageRepo
import com.cryptic.pixvi.core.downloader.pdf.DownloadPdfRepo
import com.cryptic.pixvi.printer.PdfInfo
import com.cryptic.pixvi.core.network.NetworkResult
import com.cryptic.pixvi.core.network.model.ArtworkRequest
import com.cryptic.pixvi.core.network.model.BookmarkTypes
import com.cryptic.pixvi.core.network.model.artwork.Artwork
import com.cryptic.pixvi.core.network.model.artwork.toArtworkPage
import com.cryptic.pixvi.core.network.repo.PixivApiRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Objects

class ArtworkPageViewModel(
    private val apiService: PixivApiRepo,
    private val downloadImageRepo: DownloadImageRepo,
    val downloadPdfRepo: DownloadPdfRepo
): ViewModel(){
    //what all artwork page can do??
    /*
    Load images,
    load more images,
    keep track of the indices of the images -> Maybe make a map of Map<id, page index>
    make the image focus logic
    make the circle counter
    make the autoscroll featuree
    make the long tap
     */
    //priorty
    /*
    1. Load the images,
    2. Pagination
     */
    /*
    Ui State->
     */
    data class ContentUiState(
        val isLoading: Boolean = true,
        val bookmarkStatus: BookmarkStatus? = null,
        val viewerTyper: ViewerType = ViewerType.NORMAL,
        val recommendations: List<ArtworkInfo> = emptyList(),
        val rankingArtwork: List<ArtworkInfo> = emptyList(),
        val nextUrl: String? = null,
        val indices: PageIndices = PageIndices(null,null, emptyMap()),//temp
        val isLoadingMore: Boolean = false,
        val errorMessage: String? = null,
        val errorCode: String? = null,
    )

    private val _uiState = MutableStateFlow(ContentUiState())

    val uiState = _uiState
        .onStart {
            if(_uiState.value.isLoading){
                loadData()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ContentUiState()
        )


    private fun loadData(){
        viewModelScope.launch{
            when(val response = apiService.loadArtworkPage(ArtworkRequest.FreshLoad())){
                is NetworkResult.Success -> {
                    //update the list,
                    val recommendationFeed = response.data.toArtworkPage()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            recommendations = recommendationFeed.recommendedArt,
                            rankingArtwork = recommendationFeed.rankingArt,
                            nextUrl = recommendationFeed.nextUrl
                        )
                    }
                }

                is NetworkResult.Error ->{
                    //stop the loading and show the error message
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = response.message, errorCode = response.code.toString()
                        )
                    }
                }

                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = response.e.localizedMessage
                        )
                    }
                }
            }
        }
    }

    fun loadMoreData(){
        viewModelScope.launch{
            _uiState.update { it.copy(isLoadingMore = true) }
            if(uiState.value.nextUrl.isNullOrBlank()){
                _uiState.update {
                    it.copy(isLoadingMore = false, errorMessage = "The next urls were blank or null. Restart the app.")
                }
                return@launch
            }
            val response = apiService.loadArtworkPage(ArtworkRequest.NextPage(uiState.value.nextUrl!!)) /* checked above for null values */
            when(response){
                is NetworkResult.Success-> {
                    val moreRecommendation = response.data.toArtworkPage()
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            recommendations = it.recommendations + moreRecommendation.recommendedArt,
                            errorMessage = null,
                            errorCode = null
                            //ranking list are not fetched on next page
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            errorMessage = response.message,
                            errorCode = response.code.toString()
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            errorMessage = response.e.localizedMessage
                        )
                    }
                }
            }
        }
    }

    fun artworkActions(action: ArtworkPageActions){
        when(action){

            is ArtworkPageActions.ForceGetMoreContent -> {
                loadMoreData()
            }

            is ArtworkPageActions.DownloadImage -> {
                downloadImageRepo.scheduleDownload(
                    imageUrl = action.imageUrl,
                    fileName = action.fileName,
                    fileId = action.fileId
                )
            }

            is ArtworkPageActions.CopyImageToClipboard -> {
                //get the image loader
                //check for the cached bitmap
                Log.d("ImageCopy","Url: ${action.targetImageUrl}")
                val bitmap = action.imageLoader.memoryCache?.get(MemoryCache.Key(action.targetImageUrl))?.image?.toBitmap()
                if(bitmap==null){
                    //launch the toastbar
                }
                else{
                    viewModelScope.launch {
                        downloadImageRepo.copyBitmapToClipboardViaCache(
                            context = action.application,
                            bitmap = bitmap,
                            illustId = action.illustId,
                            pageIndex = action.pageIndex
                        )
                    }
                }
            }

            is ArtworkPageActions.DownloadAllImage -> {
                downloadImageRepo.scheduleBatchDownload(
                    baseUrl = action.imageUrl,
                    fileName = action.fileName,
                    fileId = action.fileId,
                    startIndex = action.startIndex,
                    pageCount = action.pageCount
                )
            }

            is ArtworkPageActions.UpdateBookmarkInteractive -> {

            }

            is ArtworkPageActions.UpdateBookMark -> {
                bookmarkArtwork(
                    newBookmarkStatus = action.bookmarkStatus,
                    artworkId = action.artworkId,
                    type = action.type,
                    onResult = action.onResult
                )
            }
            is ArtworkPageActions.OpenArtworkDetail ->{
                //do something
            }
            is ArtworkPageActions.OpenAuthor -> {
                //open the author page
            }
            is ArtworkPageActions.UpdateIndexes -> {
                updatePostPageIndex(id = action.artworkId, index = action.pageIndex)
            }
            is ArtworkPageActions.ManageUiMode -> {
                _uiState.update {
                    it.copy(viewerTyper = action.mode, indices = it.indices.copy(recommendationsIndex = action.currentItemIndex))
                }
            }
            is ArtworkPageActions.UpdateFocusedIndex -> {
                _uiState.update {
                    it.copy(indices = it.indices.copy(recommendationsIndex = action.index))
                }
                Log.d("Page imeersive","new index is ${action.index}")
            }
            is ArtworkPageActions.DownloadPdf -> {
                //download the pages from the artwork, actually making of the pdf would be handled in the notification screen
                viewModelScope.launch {
                    downloadPdfRepo.downloadPdf(
                        postId = action.postId,
                        postTitle = action.postTitle,
                        startIndex = action.startIndex,
                        endIndex = action.endIndex,
                        singleImageUrl = action.singleImageUrl
                    )
                }
            }
        }
    }

    private fun bookmarkArtwork(
        newBookmarkStatus: Boolean,
        artworkId: Long,
        type: BookmarkTypes,
        onResult: ((BookmarkStatus) -> Unit)? = null
    ){

        //start the processing
        _uiState.update {
            it.copy(bookmarkStatus = BookmarkStatus.Processing(artworkId))
        }
        onResult?.invoke(BookmarkStatus.Processing(artworkId))

        val currentItem = uiState.value.recommendations.find { it.data.id == artworkId }

        if (currentItem == null || currentItem.data.isBookmarked == newBookmarkStatus) {
            val error = BookmarkStatus.Error(69,"Item is null or bookmark is same as the previous")
            _uiState.update {
                it.copy(bookmarkStatus = error)
            }
            onResult?.invoke(error)
            return
        }
        //show an error

        viewModelScope.launch {
            val bookmarkResponse = apiService.artworkBookmarkToggle(artworkId,newBookmarkStatus,type)
            when(bookmarkResponse){
                is NetworkResult.Success-> {
                    val success = BookmarkStatus.Success(artworkId, newBookmarkStatus)
                    _uiState.update {
                        it.copy(
                            recommendations = it.recommendations.map { illust ->
                                if (illust.data.id == artworkId) {
                                    illust.copy(
                                        data = illust.data.copy(
                                            totalBookmarks = illust.data.totalBookmarks + if(newBookmarkStatus) 1 else -1,
                                            isBookmarked = newBookmarkStatus
                                        )
                                    )
                                } else {
                                    illust
                                }
                            },
                            bookmarkStatus = success
                        )
                    }
                    onResult?.invoke(success)
                    Log.d("Bookmarked","Active")
                }
                is NetworkResult.Error ->{
                    val error = BookmarkStatus.Error(bookmarkResponse.code,bookmarkResponse.message)
                    _uiState.update {
                        it.copy(bookmarkStatus = error)
                    }
                    onResult?.invoke(error)
                    Log.d("Bookmarked","Error")
                }

                is NetworkResult.Exception -> {
                    val error = BookmarkStatus.Error(400,bookmarkResponse.e.message?:"Undefined error")
                    _uiState.update {
                        it.copy(bookmarkStatus = error)
                    }
                    onResult?.invoke(error)
                    Log.d("Bookmarked","exception")
                }
            }
        }
    }
    fun updatePostPageIndex(id: Long, index: Int){
        _uiState.update {
            it.copy(
                indices = it.indices.copy(
                    postIndex = it.indices.postIndex + (id to index)
                )
            )
        }
    }
}

sealed class BookmarkStatus{
    data object Idle: BookmarkStatus()
    data class Processing(val id: Long): BookmarkStatus()
    data class Success(val id: Long, val isBookmarked: Boolean): BookmarkStatus()
    data class Error(val code: Int, val message: String): BookmarkStatus()
}
sealed class ViewerType{
    data object NORMAL: ViewerType()
    data object IMMERSIVE: ViewerType()
}