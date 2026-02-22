package com.cryptic.pixvi.artwork.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.memory.MemoryCache
import com.cryptic.pixvi.LocalImmersiveMode
import com.cryptic.pixvi.appShell.SettingAction
import com.cryptic.pixvi.artwork.data.ArtworkInfo
import com.cryptic.pixvi.artwork.data.PageIndices
import com.cryptic.pixvi.artwork.ui.components.FloatingImageInfoToolbar
import com.cryptic.pixvi.artwork.ui.components.PixivImageRow
import com.cryptic.pixvi.artwork.ui.components.RankingCarousel
import com.cryptic.pixvi.artwork.viewmodel.ArtworkPageActions
import com.cryptic.pixvi.artwork.viewmodel.ArtworkPageViewModel
import com.cryptic.pixvi.artwork.viewmodel.ViewerType
import com.cryptic.pixvi.core.network.model.BookmarkTypes
import com.cryptic.pixvi.core.network.model.artwork.Artwork
import com.cryptic.pixvi.core.storage.AppSettings
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

//this screen would be same for manga and artwork
//floating bar would be init
//bookmark repo to update the bookmark status all the copies,

//For now just make it without floating bar,

@OptIn(FlowPreview::class)
@Composable
fun ArtworkPageScreen(
    artworkPageViewModel: ArtworkPageViewModel,
    displaySetting: AppSettings,
    displaySettingAction: (SettingAction) -> Unit,
    parentPadding: PaddingValues
){
    Log.d("Screen","Loaded the screen")
    val setImmersiveMode = LocalImmersiveMode.current
    val uiState by artworkPageViewModel.uiState.collectAsStateWithLifecycle()
    //view model have uiState.indices.recommendationsIndex
    val listState = rememberLazyListState()

    var wasInImmersiveMode by remember { mutableStateOf(false) }


    LaunchedEffect(uiState.viewerTyper) {
        // Check if current screen is normal AND previous was immersive
        if (uiState.viewerTyper is ViewerType.NORMAL && wasInImmersiveMode) {
            uiState.indices.recommendationsIndex?.let { index ->
                val offset = if (uiState.rankingArtwork.isNotEmpty()) 1 else 0
                listState.scrollToItem(index + offset)
            }
        }

        // Update flag for next change
        wasInImmersiveMode = uiState.viewerTyper is ViewerType.IMMERSIVE

        // Set system immersive mode
        setImmersiveMode(uiState.viewerTyper is ViewerType.IMMERSIVE)
    }

    BackHandler(enabled = uiState.viewerTyper is ViewerType.IMMERSIVE) {
        artworkPageViewModel.artworkActions(ArtworkPageActions.ManageUiMode(ViewerType.NORMAL, uiState.indices.recommendationsIndex?:0))
        setImmersiveMode(false)
    }


    LaunchedEffect(uiState.indices.recommendationsIndex, uiState.recommendations.size) {
        val currentIndex = uiState.indices.recommendationsIndex ?: 0
        val totalItems = uiState.recommendations.size

        // load more when different is around 5
        if (totalItems > 0 && currentIndex >= totalItems - 5 && !uiState.isLoadingMore) {
            artworkPageViewModel.loadMoreData()
        }
    }

    //add a launched effect to debounce the requests
    //add a listeners for event,
    var focusedIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(listState, uiState.recommendations.isNotEmpty()) {
        // 1. Calculate offset once per composition, not every scroll frame.
        val recommendationItemOffset = if (uiState.rankingArtwork.isNotEmpty()) 1 else 0

        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo

            if (visibleItems.isEmpty()) return@snapshotFlow null

            // 2. Optimization: specific Viewport calculations moved outside the item loop.
            val viewportHeight = layoutInfo.viewportSize.height
            val biasedCenterY = viewportHeight * 0.5f

            // 3. Filter first, then find the minimum.
            // This avoids the Float.MAX_VALUE hack and makes the intent clearer.
            val focusedItem = visibleItems
                .filter { it.index >= recommendationItemOffset }
                .minByOrNull { item ->
                    val itemCenterY = item.offset + (item.size / 2f)
                    abs(itemCenterY - biasedCenterY)
                }

            // 4. Return the relative index safely
            focusedItem?.let { it.index - recommendationItemOffset }
        }
            .debounce(250L)
            .distinctUntilChanged()
            .collect { recommendationIndex ->
                // recommendationIndex is nullable (null if only the header is visible)
                if (recommendationIndex != null) {
                    focusedIndex = recommendationIndex

                    artworkPageViewModel.artworkActions(
                        ArtworkPageActions.ManageUiMode(ViewerType.NORMAL, recommendationIndex)
                    )
                }
            }
    }

    when(uiState.viewerTyper){
        is ViewerType.NORMAL -> {

            ArtworkViewerWindow(
                rankingArtworkList = uiState.rankingArtwork,
                feedArtworkList = uiState.recommendations,

                isLoading = uiState.isLoading,
                isLoadingMore = uiState.isLoadingMore,

                errorMessage = uiState.errorMessage,
                errorCode = uiState.errorCode.toString(),

                pageIndices = uiState.indices,
                focusedIndex = focusedIndex,

                listState = listState,
                displaySetting = displaySetting,

                contentPadding = parentPadding,

                artworkAction = {
                    artworkPageViewModel.artworkActions(it)
                }

            )
        }
        is ViewerType.IMMERSIVE ->{
            //add the immersive artwork page
            ImmersiveArtworkPage(
                rankingArtworkList = uiState.rankingArtwork,
                feedArtworkList = uiState.recommendations,

                isLoading = uiState.isLoading,
                isLoadingMore = uiState.isLoadingMore,

                errorMessage = uiState.errorMessage,
                errorCode = uiState.errorCode.toString(),

                pageIndices = uiState.indices,
                focusedIndex = focusedIndex,
                displaySetting = displaySetting,

                displaySettingAction = {
                    displaySettingAction(it)
                },
                artworkAction = {
                    artworkPageViewModel.artworkActions(it)
                }
            )
        }
    }

    //add a function to determine the focus/the active image in the url with debounce so it remains within limit.

}

data class SelectedArtworkState(
    val artwork: ArtworkInfo,
    val rawUrl: String,
    val pageIndex: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtworkViewerWindow(
    rankingArtworkList: List<ArtworkInfo>,
    feedArtworkList: List<ArtworkInfo>,

    isLoading: Boolean,
    isLoadingMore: Boolean,

    errorMessage: String?,
    errorCode: String,

    pageIndices: PageIndices,
    focusedIndex: Int?,

    listState: LazyListState,
    displaySetting: AppSettings,
    contentPadding: PaddingValues,

    artworkAction: (ArtworkPageActions)-> Unit
){

    val focusedArtwork = focusedIndex?.let { feedArtworkList.getOrNull(it) }
    var selectedArtwork by remember { mutableStateOf<SelectedArtworkState?>(null) }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val context = LocalPlatformContext.current

    val imageLoader = SingletonImageLoader.get(context)


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
    ){
        when{
            isLoading && feedArtworkList.isEmpty() ->{
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null && feedArtworkList.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Warning, contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Code: $errorMessage. Error Code: $errorCode",
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error
                    )
                    Log.e("Network error", errorMessage)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        /*TODO*/
                    }) {
                        Text("Retry")
                    }
                }
            }
            else ->{
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        top = contentPadding.calculateTopPadding() + 8.dp,
                        bottom = contentPadding.calculateBottomPadding() + 8.dp,
                        start = 8.dp,
                        end = 8.dp
                    )
                ) {

                    if (rankingArtworkList.isNotEmpty()) {
                        item("ranking-carousel") {
                            RankingCarousel(
                                artworkList = rankingArtworkList,
                                onItemClick = {
                                    /*NAVIGATE TO THE PARTICUALR IMAGE*/
                                }
                            )
                        }
                    }

                    itemsIndexed(
                        items = feedArtworkList,
                        key = { _, artwork -> artwork.data.id }
                    ) { index, artwork ->

                        //add the logic here? For the bottom sheet
                        PixivImageRow(
                            artwork = artwork,
                            imageQuality = displaySetting.imageQuality,
                            itemIndex = index,
                            isFocused = index == focusedIndex,
                            onClick = { it->
                                artworkAction(it)
                            },
                            onLongPress = { artworkUrl, pageIndex ->
                                selectedArtwork = SelectedArtworkState(artwork, artworkUrl, pageIndex)
                            }
                        )
                    }

                    // Loading/Error items at the bottom of the list
                    item {
                        if (isLoadingMore) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        }
                    }
                    item {
                        if (errorMessage != null && !isLoadingMore && feedArtworkList.isNotEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp, bottom = 100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Error loading more: ${errorMessage}",
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = {
                                            artworkAction(ArtworkPageActions.ForceGetMoreContent)
                                        }
                                    ) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }
                }
                if(focusedArtwork != null){
                    FloatingImageInfoToolbar(
                        artwork = focusedArtwork!!,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 30.dp)
                            .fillMaxWidth(),
                        onFavoriteClicked = {
                            artworkAction(ArtworkPageActions.UpdateBookMark(
                                artworkId = focusedArtwork.data.id,
                                bookmarkStatus = !focusedArtwork.data.isBookmarked,
                                type = BookmarkTypes.PUBLIC
                            ))
                        },
                        onLongFavorite = {
                            artworkAction(ArtworkPageActions.UpdateBookMark(
                                artworkId = focusedArtwork.data.id,
                                bookmarkStatus = true, //forced due to long press action is supposed to make bookmark private not remove any bookmark
                                type = BookmarkTypes.PRIVATE
                            ))

                            /*
                            artworkAction(ArtworkPageActions.DownloadPdf(
                                postId = focusedArtwork.data.id,
                                postTitle = focusedArtwork.data.title,
                                startIndex = 0,
                                endIndex = focusedArtwork.data.totalPage -1,
                                singleImageUrl = focusedArtwork.pages[0].quality.original
                            ))
                            */
                            /*
                            artworkAction(ArtworkPageActions.DownloadAllImage(
                                imageUrl = focusedArtwork.pages[0].quality.original,
                                fileName = focusedArtwork.data.title,
                                fileId = focusedArtwork.data.id,
                                startIndex = 0,
                                pageCount = focusedArtwork.pages.size
                            ))*/
                            /*
                            artworkAction(ArtworkPageActions.DownloadImage(
                                imageUrl = listOf(focusedArtwork.pages[0].quality.original),
                                fileName = focusedArtwork.data.title,
                                fileId = focusedArtwork.data.id
                            ))*/
                        }

                        /*
                        onLongFavorite = {
                            artworkAction(ArtworkPageActions.UpdateBookMark(
                                artworkId = focusedArtwork.data.id,
                                bookmarkStatus = true, //forced due to long press action is supposed to make bookmark private not remove any bookmark
                                type = BookmarkTypes.PRIVATE
                            ))
                        }*/
                    )
                }
                else{
                    //Log or show the first artwork index
                    Log.e("ArtworkPage","Can't load the floatingbar for the artwork screen. The focusedArtwork is nul. /nm$focusedArtwork")
                }

                //bottom sheet
                selectedArtwork?.let { artworkPost ->
                    ModalBottomSheet(
                        onDismissRequest = {
                            selectedArtwork = null
                        },
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surface,
                    ){
                        Column() {
                            ListItem(
                                headlineContent = { Text("Save to Device") },
                                leadingContent = { Icon(Icons.Filled.FileDownload, contentDescription = "Save to Device") },
                                modifier = Modifier.clickable {

                                    selectedArtwork = null

                                    artworkAction(ArtworkPageActions.DownloadImage(
                                        imageUrl = listOf(artworkPost.artwork.pages[artworkPost.pageIndex].quality.original),
                                        fileName = artworkPost.artwork.data.title,
                                        fileId = artworkPost.artwork.data.id
                                    ))
                                }
                            )
                            ListItem(
                                headlineContent = { Text("Copy to Clipboard") },
                                leadingContent = { Icon(Icons.Filled.ContentCopy, contentDescription = "Copy to Clipboard") },
                                modifier = Modifier.clickable {
                                    //make the function to copy the image via the viewModel,
                                    //use the url to check for the cache

                                    artworkAction(ArtworkPageActions.CopyImageToClipboard(
                                        targetImageUrl = selectedArtwork!!.rawUrl,
                                        application = context.applicationContext,
                                        imageLoader = imageLoader,
                                        illustId = selectedArtwork!!.artwork.data.id,
                                        pageIndex = selectedArtwork!!.pageIndex
                                    ))

                                    //if not in cached launch a toast saying image not loaded
                                    selectedArtwork = null
                                }
                            )
                            if (artworkPost.artwork.pages.size > 1) {
                                HorizontalDivider()
                                ListItem(
                                    headlineContent = { Text("Save All as Image") },
                                    leadingContent = { Icon(Icons.Filled.Image, contentDescription = "Save All as Image") },
                                    modifier = Modifier.clickable {
                                        artworkAction(ArtworkPageActions.DownloadAllImage(
                                            imageUrl = artworkPost.artwork.pages[0].quality.original,
                                            fileName = artworkPost.artwork.data.title,
                                            fileId = artworkPost.artwork.data.id,
                                            startIndex = 0, //hardcoded
                                            pageCount = artworkPost.artwork.pages.size
                                        ))

                                        selectedArtwork = null
                                    }
                                )
                                ListItem(
                                    headlineContent = { Text("Save All as PDF") },
                                    leadingContent = { Icon(Icons.Filled.PictureAsPdf, contentDescription = "Save All as PDF") },
                                    modifier = Modifier.clickable {

                                        artworkAction(ArtworkPageActions.DownloadPdf(
                                            postId = artworkPost.artwork.data.id,
                                            postTitle = artworkPost.artwork.data.title,
                                            startIndex = 0,
                                            endIndex = artworkPost.artwork.data.totalPage -1,
                                            singleImageUrl = artworkPost.artwork.pages[0].quality.original
                                        ))

                                        selectedArtwork = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}