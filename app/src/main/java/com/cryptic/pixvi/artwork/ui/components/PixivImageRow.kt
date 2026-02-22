package com.cryptic.pixvi.artwork.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.memory.MemoryCache
import com.cryptic.pixvi.appShell.UserActions
import com.cryptic.pixvi.artwork.data.ArtworkInfo
import com.cryptic.pixvi.artwork.data.ArtworkPage
import com.cryptic.pixvi.artwork.data.Page
import com.cryptic.pixvi.artwork.viewmodel.ArtworkPageActions
import com.cryptic.pixvi.artwork.viewmodel.ViewerType
import com.cryptic.pixvi.core.network.model.artwork.Artwork

@Composable
fun PixivImageRow(
    artwork: ArtworkInfo,
    imageQuality: Int,
    itemIndex: Int,
    isFocused: Boolean,
    onClick:(ArtworkPageActions) -> Unit,
    onLongPress:(String, Int) -> Unit
){
    val isMultiPage = artwork.data.totalPage > 1
    val aspectRatio = if (artwork.data.width > 0 && artwork.data.height > 0) {
        artwork.data.width.toFloat() / artwork.data.height.toFloat()
    } else { 2f / 3f }

    val context = LocalPlatformContext.current

    val imageLoader = SingletonImageLoader.get(context)

    //var pageIndex by remember { mutableIntStateOf(0) }
    //add any other function, for now just showing a simple image
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        val pagerState = if (isMultiPage) {
            rememberPagerState(pageCount = { artwork.data.totalPage })
        } else {
            null
        }

        fun getUrlForPage(index: Int): String {
            val page = artwork.pages[index]
            val originalKey = MemoryCache.Key(page.quality.original)
            val largeKey = MemoryCache.Key(page.quality.large)

            return when {
                imageLoader.memoryCache?.get(originalKey) != null -> page.quality.original
                imageLoader.memoryCache?.get(largeKey) != null -> page.quality.large
                else -> when (imageQuality) {
                    -1 -> page.quality.original
                    0 -> page.quality.large
                    else -> page.quality.medium
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio, matchHeightConstraintsFirst = false)
                .combinedClickable(
                    onClick = {
                        onClick(ArtworkPageActions.ManageUiMode(ViewerType.IMMERSIVE, itemIndex))
                    },
                    onLongClick = {
                        val currentPage = pagerState?.currentPage ?: 0
                        val currentUrl = getUrlForPage(currentPage)
                        onLongPress(currentUrl, currentPage)
                    }
                )
                .border(
                    width = if (isFocused) 3.dp else 0.dp,
                    color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                )
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant), //.combinedClickable.......
            contentAlignment = Alignment.Center
        ){
            if(isMultiPage){
                Box(
                    modifier = Modifier.fillMaxSize()
                ){
                    HorizontalPager(
                        state = pagerState!!, //only exits when the pagerState is multipage
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        val actualUrl = remember(imageLoader, pageIndex) {
                            getUrlForPage(pageIndex)
                        }
                        //update the page index in the viewmodel,?
                        AsyncImage(
                            model = actualUrl,
                            contentDescription = artwork.data.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    CircularPageProgressIndicator(
                        currentPage = pagerState.currentPage,
                        pageCount = pagerState.pageCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    )
                }
            }
            else{

                //flashing
                val actualUrl = remember(imageLoader) { getUrlForPage(0) }

                AsyncImage(
                    model = actualUrl,
                    contentDescription = artwork.data.title,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Pure function to determine the best URL.
 * Easily testable without Compose or Android dependencies.
 */
fun getOptimalImageUrl(
    page: Page,
    imageQuality: Int,
    memoryCache: MemoryCache?
): String {
    return when {
        memoryCache?.get(MemoryCache.Key(page.quality.original)) != null -> page.quality.original
        memoryCache?.get(MemoryCache.Key(page.quality.large)) != null -> page.quality.large
        imageQuality == -1 -> page.quality.original
        imageQuality == 0 -> page.quality.large
        else -> page.quality.medium
    }
}