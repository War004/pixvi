package com.cryptic.pixvi.artwork.ui

import android.text.Layout
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RawOn
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Sd
import androidx.compose.material.icons.outlined.RawOn
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.util.lerp
import coil3.Bitmap
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.memory.MemoryCache
import com.cryptic.pixvi.appShell.SettingAction
import com.cryptic.pixvi.artwork.data.ArtworkInfo
import com.cryptic.pixvi.artwork.data.PageIndices
import com.cryptic.pixvi.artwork.data.Quality
import com.cryptic.pixvi.artwork.ui.components.FadingBackgroundImage
import com.cryptic.pixvi.artwork.viewmodel.ArtworkPageActions
import com.cryptic.pixvi.artwork.viewmodel.BookmarkStatus
import com.cryptic.pixvi.artwork.viewmodel.ViewerType
import com.cryptic.pixvi.core.network.model.BookmarkTypes
import com.cryptic.pixvi.core.network.model.artwork.Artwork
import com.cryptic.pixvi.core.storage.AppSettings
import okhttp3.internal.userAgent
import kotlin.math.absoluteValue
import kotlin.math.acos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveArtworkPage(
    rankingArtworkList: List<ArtworkInfo>,
    feedArtworkList: List<ArtworkInfo>,

    isLoading: Boolean,
    isLoadingMore: Boolean,

    errorMessage: String?,
    errorCode: String,

    pageIndices: PageIndices,
    focusedIndex: Int?,

    displaySetting: AppSettings,

    displaySettingAction: (SettingAction) -> Unit,
    artworkAction: (ArtworkPageActions)-> Unit
){

    val pagerState = rememberPagerState(
        initialPage = pageIndices.recommendationsIndex?:0,
        pageCount = {feedArtworkList.size}
    )

    var isMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        artworkAction(ArtworkPageActions.ManageUiMode(ViewerType.IMMERSIVE, pagerState.currentPage))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column{
                        Text(
                            text = feedArtworkList[pagerState.currentPage].data.title?:"No title_",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = feedArtworkList[pagerState.currentPage].author.authorName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            //switch back to the inmerssive mode
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close the immerssive viewer mode."
                        )
                    }
                },
                actions = {
                    //quality selector
                    IconButton(
                        onClick = {
                            when(displaySetting.imageQuality){
                                -1 -> displaySettingAction(SettingAction.ChangeImageQuality(0))  //-1= raw, 0=hd, 1=sd
                                0 -> displaySettingAction(SettingAction.ChangeImageQuality(1))
                                1 -> displaySettingAction(SettingAction.ChangeImageQuality(-1))
                            }
                        }
                    ) {
                        Icon(
                            imageVector = when(displaySetting.imageQuality){
                                -1 -> Icons.Default.RawOn
                                0 -> Icons.Default.Hd
                                1 -> Icons.Default.Sd
                                else -> Icons.Default.Hd
                            },
                            contentDescription = "Quality selector" //modify
                        )
                    }

                    //download button
                    IconButton(
                        onClick = {
                            val artId = feedArtworkList[pagerState.currentPage].data.id
                            val indexNumber = pageIndices.postIndex[artId] ?:0

                            artworkAction(ArtworkPageActions.DownloadImage(
                                imageUrl = listOf(feedArtworkList[pagerState.currentPage].pages[indexNumber].quality.original),
                                fileName = feedArtworkList[pagerState.currentPage].data.title,
                                fileId = feedArtworkList[pagerState.currentPage].data.id
                            ))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = null
                        )
                    }
                    IconButton(
                        onClick = {
                            isMenuExpanded = !isMenuExpanded
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null
                        )
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = !isMenuExpanded }
                    ) {
                        DropdownMenuItem(
                            enabled = feedArtworkList[pagerState.currentPage].pages.size > 1,
                            text = { Text("Save all Images") },
                            onClick = { /* Do something... */ }
                        )
                        DropdownMenuItem(
                            enabled = feedArtworkList[pagerState.currentPage].pages.size > 1,
                            text = { Text("Save as Pdf") },
                            onClick = { /* Do something... */ }
                        )
                        DropdownMenuItem(
                            text = {Text("Copy to Clipboard")},
                            onClick = {/*Copy*/}
                        )
                        DropdownMenuItem(
                            text = {Text("Show Caption")},
                            onClick = {/*Show a bottom sheet with caption info*/}
                        )
                    }
                },
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            )
        },
    ) { paddingValues->

        val layoutDirection = LocalLayoutDirection.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = paddingValues.calculateStartPadding(layoutDirection),
                    end = paddingValues.calculateEndPadding(layoutDirection),
                    bottom = paddingValues.calculateBottomPadding()
                    // top is implicitly 0.dp here
                )
        ){
            FadingBackgroundImage(
                imageQuality = displaySetting.imageQuality,
                items = feedArtworkList,
                pagerState = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
            )
            //image viewer page immersion
            ImageCarousel(
                modifier = Modifier.align(Alignment.Center),
                artworkList = feedArtworkList,
                pagerState = pagerState,
                imageQuality = displaySetting.imageQuality,
                artworkAction = {
                    artworkAction(it)
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            artworkAction(ArtworkPageActions.UpdateFocusedIndex(pagerState.currentPage))
        }
    }
}


//artwork card
@Composable
private fun ImageCarousel(
    modifier: Modifier,
    artworkList: List<ArtworkInfo>,
    pagerState:  PagerState,
    imageQuality: Int,
    artworkAction: (ArtworkPageActions)-> Unit,
){
    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 35.dp),
        pageSpacing = 20.dp,
    ) { page->
        val currentItem = artworkList[page]

        if (currentItem.data.totalPage <= 1){
            ArtworkCard(
                artworkId = currentItem.data.id,
                isBookmarked = currentItem.data.isBookmarked,
                artworkUrls = currentItem.pages[0].quality,
                imageTitle = currentItem.data.title,
                width = currentItem.data.width,
                height = currentItem.data.height,
                modifier = Modifier.fillMaxWidth(),
                imageQuality = imageQuality,
                userAction = artworkAction
            )
        }
        else{
            //get the index of the subpage or consider it one
            val subIndex = 0
            //create a vertical pager
            val verticalPagerState = rememberPagerState(
                initialPage = subIndex,
                pageCount = {currentItem.data.totalPage}
            )

            LaunchedEffect(verticalPagerState.currentPage) {
                artworkAction(ArtworkPageActions.UpdateIndexes(
                    artworkId = currentItem.data.id,
                    pageIndex = verticalPagerState.currentPage
                ))
            }

            VerticalPager(
                state = verticalPagerState,
                contentPadding = PaddingValues(vertical = 150.dp),
                pageSpacing = 16.dp
            ) { innerPage ->
                val pageOffset =
                    (verticalPagerState.currentPage - innerPage) + verticalPagerState.currentPageOffsetFraction
                val scale = lerp(0.9f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))

                ArtworkCard(
                    artworkId = currentItem.data.id,
                    isBookmarked = currentItem.data.isBookmarked,
                    artworkUrls = currentItem.pages[innerPage].quality,
                    width = currentItem.data.width,
                    height = currentItem.data.height,
                    modifier = Modifier
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .fillMaxHeight(),
                    imageTitle = currentItem.data.title,
                    imageQuality = imageQuality,
                    userAction = artworkAction
                )
            }
        }
    }
}

@Composable
private fun ArtworkCard(
    artworkId: Long,
    isBookmarked: Boolean,
    artworkUrls: Quality,
    height: Int,
    width: Int,
    imageTitle: String,
    imageQuality: Int,
    modifier: Modifier,
    userAction:(ArtworkPageActions) -> Unit
){
    val aspectRatio = if (height > 0) width.toFloat() / height.toFloat() else 0.7f

    val context = LocalPlatformContext.current

    val imageLoader = SingletonImageLoader.get(context)

    // --- LOCAL BORDER STATE (independent per card) ---
    var localStatus by remember { mutableStateOf<BookmarkStatus>(BookmarkStatus.Idle) }

    // Auto-clear the border after success or error
    LaunchedEffect(localStatus) {
        if (localStatus is BookmarkStatus.Success || localStatus is BookmarkStatus.Error) {
            delay(1500)
            localStatus = BookmarkStatus.Idle
        }
    }

    //can't use the id as changing the page doesn't change the id
    val actualUrl = remember(imageLoader, artworkUrls.original){
        if(imageLoader.memoryCache?.get(MemoryCache.Key(artworkUrls.original)) != null) artworkUrls.original
        else if(imageLoader.memoryCache?.get(MemoryCache.Key(artworkUrls.large)) != null) artworkUrls.large
        //no better version is present use the user provided quality
        else{
            when(imageQuality){
                -1 -> artworkUrls.original
                0 -> artworkUrls.large
                else -> artworkUrls.medium
            }
        }
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        // Use LOCAL status so only this card shows the border
        border = when(localStatus){
            is BookmarkStatus.Processing -> BorderStroke(width = 3.dp, color = Color.Yellow)
            is BookmarkStatus.Idle -> null
            is BookmarkStatus.Error -> BorderStroke(width = 3.dp, color = Color.Red)
            is BookmarkStatus.Success -> BorderStroke(width = 3.dp, color = if((localStatus as BookmarkStatus.Success).isBookmarked) Color.Magenta else Color.Red)
        },

        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(10.dp))
            .semantics{
                contentDescription = "Artwork ${imageTitle}. Double tap to bookmark."
                role = Role.Image
            }
            .combinedClickable(
                onClick = {
                    /*OPEN THE ARTWORK*/
                },
                onDoubleClick = {
                    // Debounce: ignore if already processing
                    if (localStatus is BookmarkStatus.Processing) return@combinedClickable

                    Log.d("ImmersiveArtwork","Bookmarkactive")
                    userAction(
                        ArtworkPageActions.UpdateBookMark(
                            artworkId = artworkId,
                            bookmarkStatus = !isBookmarked,
                            type = BookmarkTypes.PUBLIC,
                            onResult = { resultStatus ->
                                // Only this card receives the callback
                                localStatus = resultStatus
                            }
                        )
                    )
                }
            )
    ) {
        AsyncImage(
            model = actualUrl,
            contentDescription = "Artwork ${imageTitle}. Double tap to bookmark.",
            modifier = Modifier.fillMaxSize()
        )
    }
}
