package com.example.pixvi.screens.detail

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Sd
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.Coil
import com.example.pixvi.utils.NetworkPerformanceMonitor
import com.example.pixvi.utils.NormalImageRequest
import com.example.pixvi.utils.PixivAsyncImage
import com.example.pixvi.utils.isPowerSaveModeActive
import com.example.pixvi.viewModels.HomeIllustViewModel
import com.example.pixvi.viewModels.MangaViewModel
import com.example.pixvi.viewModels.ContentType
import com.example.pixvi.viewModels.DetailScreenState
import com.example.pixvi.viewModels.DetailViewModel
import com.example.pixvi.viewModels.DisplayableItem
import com.example.pixvi.viewModels.formatDateStringSafe
import com.example.pixvi.viewModels.toDisplayableItem
import kotlin.math.absoluteValue
import com.example.pixvi.viewModels.ImageQuality
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FullImageScreen(
    contentType: ContentType,
    navController: NavController,
    homeIllustViewModel: HomeIllustViewModel,
    mangaViewModel: MangaViewModel
) {

    val initialState by remember(contentType) {
        derivedStateOf {
            when (contentType) {
                ContentType.ILLUST -> {
                    val state = homeIllustViewModel.uiState.value
                    val isFromRanking = state.indices.rankingCurrentIndex != null
                    val list = if (isFromRanking) state.rankingIllusts.map { it.toDisplayableItem() } else state.recommendations.map { it.toDisplayableItem() }
                    val index = if (isFromRanking) state.indices.rankingCurrentIndex else state.indices.recommendationsCurrentIndex
                    DetailScreenState(
                        items = list,
                        initialIndex = index ?: 0,
                        initialSubPageIndex = state.indices.subRecommendationsCurrentIndex ?: 0,
                        isLoadingMore = state.isLoadingMore,
                        nextUrl = state.nextUrl
                    )
                }
                ContentType.MANGA -> {
                    val state = mangaViewModel.uiState.value
                    val isFromRanking = state.indices.rankingCurrentIndex != null
                    val list = if (isFromRanking) state.rankingIllusts.map { it.toDisplayableItem() } else state.recommendations.map { it.toDisplayableItem() }
                    val index = if (isFromRanking) state.indices.rankingCurrentIndex else state.indices.recommendationsCurrentIndex
                    DetailScreenState(
                        items = list,
                        initialIndex = index ?: 0,
                        initialSubPageIndex = state.indices.subRecommendationsCurrentIndex ?: 0,
                        isLoadingMore = state.isLoadingMore,
                        nextUrl = state.nextUrl
                    )
                }
            }
        }
    }

    val detailViewModel: DetailViewModel = viewModel(
        factory = DetailViewModel.provideFactory(
            homeIllustViewModel = homeIllustViewModel,
            mangaViewModel = mangaViewModel,
            contentType = contentType,
            initialState = initialState
        )
    )

    val uiState by detailViewModel.uiState.collectAsState()

    val isBatterySaverOn = isPowerSaveModeActive()
    var imageQuality by remember { mutableStateOf(ImageQuality.AUTO) }
    val context = LocalContext.current

    if (uiState.items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = uiState.initialIndex,
        pageCount = { uiState.items.size }
    )


    var currentSubPageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(pagerState.currentPage) {
        // Reset sub-page index when horizontal page changes
        currentSubPageIndex = if (pagerState.currentPage == uiState.initialIndex) {
            uiState.initialSubPageIndex
        } else {
            0
        }
    }


    LaunchedEffect(pagerState.currentPage, uiState.isLoadingMore, uiState.nextUrl) {
        val buffer = 5
        val triggerIndex = (uiState.items.size - 1 - buffer).coerceAtLeast(0)
        if (pagerState.currentPage >= triggerIndex && !uiState.isLoadingMore && uiState.nextUrl != null) {
            detailViewModel.loadMore()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Get the last page the user was on
            val lastViewedIndex = pagerState.currentPage
            // Update the appropriate ViewModel based on the content type
            when (contentType) {
                ContentType.ILLUST -> homeIllustViewModel.updateLastViewedIndex(lastViewedIndex)
                ContentType.MANGA -> mangaViewModel.updateLastViewedIndex(lastViewedIndex)
            }
        }
    }

    if (pagerState.currentPage >= uiState.items.size) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading content...")
        }
        return
    }

    val currentItem = uiState.items[pagerState.currentPage]
    val loadedImageUrls = remember { mutableStateMapOf<Int, String?>() }

    Box(modifier = Modifier.fillMaxSize()) {

        if (isBatterySaverOn) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {}
        } else {
            // Battery Saver is OFF: Draw the expensive blurred background.
            FadingBackgroundImage(
                pagerState = pagerState,
                loadedImageUrls = loadedImageUrls,
                items = uiState.items,
                modifier = Modifier.fillMaxSize().blur(radius = 80.dp)
            )
        }

        ImageCarousel(
            modifier = Modifier.align(Alignment.Center),
            items = uiState.items,
            pagerState = pagerState,
            onImageLoadedForPage = { page, url -> loadedImageUrls[page] = url },
            initialHorizontalIndex = uiState.initialIndex,
            initialSubPageIndex = uiState.initialSubPageIndex,
            imageQuality = imageQuality,
            isBorderEnabled = isBatterySaverOn,
            onSubPageChange = { newSubPageIndex ->
                currentSubPageIndex = newSubPageIndex
            }
        )

        TopAppBar(
            modifier = Modifier.padding(horizontal = 7.dp),
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            title = {
                Column {
                    Text(currentItem.userName, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = formatDateStringSafe(currentItem.createDate),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    imageQuality = when (imageQuality) {
                        ImageQuality.AUTO -> {
                            //Toast.makeText(context, "Forcing large quality", Toast.LENGTH_SHORT).show()
                            ImageQuality.LARGE
                        }
                        ImageQuality.LARGE -> {
                            //Toast.makeText(context, "Forcing medium quality", Toast.LENGTH_SHORT).show()
                            ImageQuality.MEDIUM
                        }
                        ImageQuality.MEDIUM -> {
                            //Toast.makeText(context, "Auto quality enabled", Toast.LENGTH_SHORT).show()
                            ImageQuality.AUTO
                        }
                    }
                }) {
                    Icon(
                        imageVector = when (imageQuality) {
                            ImageQuality.AUTO -> Icons.Default.AutoAwesome
                            ImageQuality.LARGE -> Icons.Default.Hd
                            ImageQuality.MEDIUM -> Icons.Default.Sd
                        },
                        contentDescription = "Toggle Image Quality",
                        tint = Color.White
                    )
                }
                /*
                IconButton(onClick = { detailViewModel.toggleBookmark(currentItem.id) }) {
                    Icon(
                        imageVector =if (currentItem.isBookmarked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (currentItem.isBookmarked) Color(0xFFE8457E) else Color.White
                    )
                }*/
                IconButton(onClick = {
                    val pageToSave = if (currentItem.pageCount > 1) currentSubPageIndex else 0
                    detailViewModel.saveOrginalImageToDevice(
                        context = context,
                        originalImageUrl = currentItem.originalImageUrls[pageToSave],
                        illustId = currentItem.id.toInt(),
                        displayName = currentItem.title,
                        currentPageIndex = pageToSave
                    )
                }) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = "Save to Device",
                        tint = Color.White
                    )
                }
                IconButton(onClick = {}) { Icon(Icons.Outlined.MoreVert, "More Options", tint = Color.White) }
            }
        )

        if (uiState.isLoadingMore) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageCarousel(
    modifier: Modifier,
    items: List<DisplayableItem>,
    pagerState: PagerState,
    onImageLoadedForPage: (Int, String) -> Unit,
    initialHorizontalIndex: Int,
    initialSubPageIndex: Int,
    imageQuality: ImageQuality,
    isBorderEnabled: Boolean,
    onSubPageChange: (Int) -> Unit
) {
    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 35.dp),
        pageSpacing = 20.dp
    ) { page ->
        val item = items[page]
        if (item.pageCount <= 1) {
            CarouselItem(
                item = item,
                onImageLoaded = { url -> onImageLoadedForPage(page, url) },
                imageQuality = imageQuality,
                pageIndex = 0,
                isBorderEnabled = isBorderEnabled
            )
        } else {
            val subPageInitial = if (page == initialHorizontalIndex) initialSubPageIndex else 0
            val verticalPageState = rememberPagerState(initialPage = subPageInitial, pageCount = { item.pageCount })

            LaunchedEffect(verticalPageState.currentPage) {
                onSubPageChange(verticalPageState.currentPage)
            }

            VerticalPager(
                state = verticalPageState,
                contentPadding = PaddingValues(vertical = 150.dp),
                pageSpacing = 16.dp
            ) { innerPage ->
                val pageOffset = (verticalPageState.currentPage - innerPage) + verticalPageState.currentPageOffsetFraction
                val scale = lerp(0.9f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))
                val aspectRatio = if (item.height > 0) item.width.toFloat() / item.height.toFloat() else 0.71f

                val cardModifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .fillMaxHeight()
                    .aspectRatio(aspectRatio)

                Card(
                    shape = RoundedCornerShape(10.dp),
                    modifier = if (isBorderEnabled) {
                        cardModifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    } else {
                        cardModifier
                    }
                ) {
                    SmartPixivImage(
                        item = item,
                        pageIndex = innerPage,
                        contentDescription = "Image ${innerPage + 1} for ${item.title}",
                        modifier = Modifier.fillMaxSize(),
                        imageQuality = imageQuality,
                        onImageLoaded = { url ->
                            if (innerPage == 0) {
                                onImageLoadedForPage(page, url)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CarouselItem(
    item: DisplayableItem,
    onImageLoaded: (String) -> Unit,
    imageQuality: ImageQuality,
    pageIndex: Int,
    isBorderEnabled: Boolean
) {
    val aspectRatio = if (item.height > 0) item.width.toFloat() / item.height.toFloat() else 0.71f
    val cardModifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio)

    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = if (isBorderEnabled) {
            cardModifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), // Use a subtle outline color
                shape = RoundedCornerShape(10.dp)
            )
        } else {
            cardModifier
        }
    ) {
        SmartPixivImage(
            item = item,
            pageIndex = pageIndex,
            contentDescription = "Artwork: ${item.title}",
            modifier = Modifier.fillMaxSize(),
            imageQuality = imageQuality,
            onImageLoaded = onImageLoaded,
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun FadingBackgroundImage(
    items: List<DisplayableItem>,
    pagerState: PagerState,
    loadedImageUrls: Map<Int, String?>,
    modifier: Modifier = Modifier
) {
    val subsamplingFactor = 4
    BoxWithConstraints(modifier) {
        val fromPage = pagerState.currentPage.coerceIn(items.indices)
        val pageOffset = pagerState.currentPageOffsetFraction
        val toPage = when {
            pageOffset > 0 -> (fromPage + 1)
            pageOffset < 0 -> (fromPage - 1)
            else -> fromPage
        }.coerceIn(items.indices)

        val fromUrl = loadedImageUrls[fromPage]
        val toUrl = loadedImageUrls[toPage]

        val targetWidth = constraints.maxWidth / subsamplingFactor
        val targetHeight = constraints.maxHeight / subsamplingFactor

        Box(Modifier.fillMaxSize()) {
            if (fromUrl != null) {
                PixivAsyncImage(
                    imageUrl = fromUrl,
                    contentDescription = "Background From",
                    contentScale = ContentScale.Crop,
                    width = targetWidth,
                    height = targetHeight,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (fromPage != toPage && toUrl != null) {
                PixivAsyncImage(
                    imageUrl = toUrl,
                    contentDescription = "Background To",
                    width = targetWidth,
                    height = targetHeight,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = pageOffset.absoluteValue }
                )
            }
        }
    }
}

/**
 *
 * This composable is the intelligent core for displaying images. It is responsible for:
 * 1.  Correctly scoping its internal state to each unique image to prevent bugs when swiping.
 * 2.  Implementing the user's manual quality choice (LARGE or MEDIUM).
 * 3.  Implementing a hybrid "AUTO" mode that:
 *     - Uses the global NetworkPerformanceMonitor for a smart default quality.
 *     - Applies an immediate timeout to any 'large' image attempt to ensure UI responsiveness.
 *     - Falls back to 'medium' quality if the timeout is exceeded.
 *     - Reports all performance metrics back to the monitor.
 * 4.  Preventing wasteful network reloads when a user toggles to a lower quality
 *     for an image that is already displayed in high quality.
 */
@Composable
fun SmartPixivImage(
    item: DisplayableItem,
    pageIndex: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    imageQuality: ImageQuality,
    onImageLoaded: (String) -> Unit
) {
    val largeUrl = item.largeImageUrls.getOrElse(pageIndex) { "" }
    val mediumUrl = item.mediumImageUrls.getOrElse(pageIndex) { "" }

    val context = LocalContext.current

    var imageUrlToDisplay by remember(largeUrl, mediumUrl) { mutableStateOf<String?>(null) }
    var displayedQuality by remember(largeUrl, mediumUrl) { mutableStateOf<ImageQuality?>(null) }

    val autoQuality by NetworkPerformanceMonitor.currentAutoQuality.collectAsState()


    LaunchedEffect(imageUrlToDisplay) {
        imageUrlToDisplay?.let { onImageLoaded(it) }
    }

    LaunchedEffect(largeUrl, mediumUrl, imageQuality, autoQuality) {
        val imageLoader = Coil.imageLoader(context)

        val finalTargetQuality = if (imageQuality == ImageQuality.AUTO) autoQuality else imageQuality

        if (displayedQuality == ImageQuality.LARGE && finalTargetQuality == ImageQuality.MEDIUM) {
            return@LaunchedEffect
        }
        if (finalTargetQuality == displayedQuality && imageUrlToDisplay != null) {
            return@LaunchedEffect
        }

        imageUrlToDisplay = null

        when (imageQuality) {
            ImageQuality.LARGE, ImageQuality.MEDIUM -> {
                val url = if (imageQuality == ImageQuality.LARGE) largeUrl else mediumUrl
                val request = NormalImageRequest.normalImageRequest(context, url)
                if (imageLoader.execute(request).drawable != null) {
                    imageUrlToDisplay = url
                    displayedQuality = imageQuality
                }
            }
            ImageQuality.AUTO -> {
                val isWifi = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                    .getNetworkCapabilities((context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetwork)
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: true
                val timeoutMs = if (isWifi) 350L else 600L

                coroutineScope {
                    if (autoQuality == ImageQuality.MEDIUM) {
                        val request = NormalImageRequest.normalImageRequest(context, mediumUrl)
                        if (imageLoader.execute(request).drawable != null) {
                            imageUrlToDisplay = mediumUrl
                            displayedQuality = ImageQuality.MEDIUM
                        }
                        return@coroutineScope
                    }

                    val startTime = System.currentTimeMillis()
                    val targetJob = launch {
                        val request = NormalImageRequest.normalImageRequest(context, largeUrl)
                        if (imageLoader.execute(request).drawable != null) {
                            if (coroutineContext.isActive) {
                                imageUrlToDisplay = largeUrl
                                displayedQuality = ImageQuality.LARGE
                                val duration = System.currentTimeMillis() - startTime
                                NetworkPerformanceMonitor.recordLoadTime(duration)
                            }
                        } else {
                            if (coroutineContext.isActive) {
                                imageUrlToDisplay = mediumUrl
                                displayedQuality = ImageQuality.MEDIUM
                                NetworkPerformanceMonitor.recordLoadTime(timeoutMs)
                            }
                        }
                    }

                    delay(timeoutMs)

                    if (targetJob.isActive) {
                        targetJob.cancelAndJoin()
                        NetworkPerformanceMonitor.recordLoadTime(timeoutMs)
                        imageUrlToDisplay = mediumUrl
                        displayedQuality = ImageQuality.MEDIUM
                    }
                }
            }
        }
    }

    PixivAsyncImage(
        imageUrl = imageUrlToDisplay,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
    )
}