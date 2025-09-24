package com.example.pixvi.screens.detail

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.SingletonImageLoader
import com.example.pixvi.repo.BatterySaverThemeRepository
import com.example.pixvi.repo.SystemInfoRepository
import com.example.pixvi.settings.SettingsRepository
import com.example.pixvi.utils.NetworkPerformanceMonitor
import com.example.pixvi.utils.NormalImageRequest
import com.example.pixvi.utils.PixivAsyncImage
import com.example.pixvi.utils.isPowerSaveModeActive
import com.example.pixvi.viewModels.*
import kotlin.math.absoluteValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FullImageScreen(
    contentType: ContentType,
    navController: NavController,
    homeIllustViewModel: HomeIllustViewModel,
    mangaViewModel: MangaViewModel,
    isBatterySaverTheme: BatterySaverThemeRepository
) {
    val initialState by remember(contentType) {
        derivedStateOf {
            when (contentType) {
                ContentType.ILLUST -> {
                    val state = homeIllustViewModel.uiState.value
                    val isFromRanking = state.indices.rankingCurrentIndex != null
                    val list =
                        if (isFromRanking) state.rankingIllusts.map { it.toDisplayableItem() }
                        else state.recommendations.map { it.toDisplayableItem() }
                    val index =
                        if (isFromRanking) state.indices.rankingCurrentIndex
                        else state.indices.recommendationsCurrentIndex
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
                    val list =
                        if (isFromRanking) state.rankingIllusts.map { it.toDisplayableItem() }
                        else state.recommendations.map { it.toDisplayableItem() }
                    val index =
                        if (isFromRanking) state.indices.rankingCurrentIndex
                        else state.indices.recommendationsCurrentIndex
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
            homeIllustViewModel,
            mangaViewModel,
            contentType,
            initialState,
            isBatterySaverTheme
        )
    )

    val uiState by detailViewModel.uiState.collectAsState()
    val imageQuality by detailViewModel.imageQuality.collectAsState()

    val isPowerSaverTheme by detailViewModel.isPowerSaverTheme.collectAsState()

    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(true) {
        detailViewModel.uiEvent.collect { event ->
            // We only care about failure events for our global message
            if (event is BookmarkEffect.Failure) {
                // This is the CRITICAL part for preventing spam:
                // Cancel the previous snackbar job before starting a new one.
                snackbarJob?.cancel()

                // Launch a new coroutine to show the snackbar.
                snackbarJob = scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Error: ${event.errorMessage}",
                        withDismissAction = true // Adds a dismiss button
                    )
                }
            }
        }
    }


    if (uiState.items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
        currentSubPageIndex = if (pagerState.currentPage == uiState.initialIndex)
            uiState.initialSubPageIndex else 0
    }

    LaunchedEffect(pagerState.currentPage, uiState.isLoadingMore, uiState.nextUrl) {
        val buffer = 5
        val triggerIndex = (uiState.items.size - 1 - buffer).coerceAtLeast(0)
        if (pagerState.currentPage >= triggerIndex &&
            !uiState.isLoadingMore &&
            uiState.nextUrl != null
        ) {
            detailViewModel.loadMore()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val lastViewedIndex = pagerState.currentPage
            when (contentType) {
                ContentType.ILLUST -> homeIllustViewModel.updateLastViewedIndex(lastViewedIndex)
                ContentType.MANGA -> mangaViewModel.updateLastViewedIndex(lastViewedIndex)
            }
        }
    }

    if (pagerState.currentPage >= uiState.items.size) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading content...")
        }
        return
    }

    val currentItem = uiState.items[pagerState.currentPage]
    val loadedImageUrls = remember { mutableStateMapOf<Int, String?>() }

    Box(Modifier.fillMaxSize()) {
        if (isPowerSaverTheme) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {}
        } else {
            FadingBackgroundImage(
                pagerState = pagerState,
                loadedImageUrls = loadedImageUrls,
                items = uiState.items,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
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
            uiEventFlow = detailViewModel.uiEvent,
            onBookmark = { illustId, token ->
                detailViewModel.toggleBookmark(illustId, token)
            },
            onSubPageChange = { currentSubPageIndex = it }
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
                    Text(
                        currentItem.userName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        formatDateStringSafe(currentItem.createDate),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
            },
            actions = {
                IconButton(onClick = { detailViewModel.cycleImageQuality() }) {
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
                IconButton(onClick = {
                    val pageToSave =
                        if (currentItem.pageCount > 1) currentSubPageIndex else 0
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
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        "More Options",
                        tint = Color.White
                    )
                }
            }
        )

        if (uiState.isLoadingMore) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        )
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
    uiEventFlow: SharedFlow<BookmarkEffect>,
    onBookmark: (illustId: Long, token: String) -> Unit,
    onSubPageChange: (Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 35.dp),
        pageSpacing = 20.dp
    ) { page ->
        val item = items[page]

        if (item.pageCount <= 1) {
            ArtworkCard(
                item = item,
                pageIndex = 0,
                imageQuality = imageQuality,
                uiEventFlow = uiEventFlow,
                onBookmark = { token -> onBookmark(item.id, token) },
                onImageLoaded = { url -> onImageLoadedForPage(page, url) },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            val subInitial =
                if (page == initialHorizontalIndex) initialSubPageIndex else 0
            val verticalPageState = rememberPagerState(
                initialPage = subInitial,
                pageCount = { item.pageCount }
            )

            LaunchedEffect(verticalPageState.currentPage) {
                onSubPageChange(verticalPageState.currentPage)
            }

            VerticalPager(
                state = verticalPageState,
                contentPadding = PaddingValues(vertical = 150.dp),
                pageSpacing = 16.dp
            ) { innerPage ->
                val pageOffset =
                    (verticalPageState.currentPage - innerPage) + verticalPageState.currentPageOffsetFraction
                val scale = lerp(0.9f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))
                val aspectRatio =
                    if (item.height > 0) item.width.toFloat() / item.height.toFloat() else 0.71f

                ArtworkCard(
                    item = item,
                    pageIndex = innerPage,
                    imageQuality = imageQuality,
                    uiEventFlow = uiEventFlow,
                    onBookmark = { token -> onBookmark(item.id, token) },
                    onImageLoaded = { url ->
                        if (innerPage == 0) {
                            onImageLoadedForPage(page, url)
                        }
                    },
                    modifier = Modifier
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .fillMaxHeight()
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtworkCard(
    item: DisplayableItem,
    pageIndex: Int,
    imageQuality: ImageQuality,
    onBookmark: (token: String) -> Unit,
    uiEventFlow: SharedFlow<BookmarkEffect>,
    onImageLoaded: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val aspectRatio = if (item.height > 0) item.width.toFloat() / item.height.toFloat() else 0.71f

    var lastRequestToken by remember { mutableStateOf<String?>(null) }
    var animationInfo by remember { mutableStateOf<AnimationInfo?>(null) }

    LaunchedEffect(uiEventFlow, lastRequestToken) {
        if (lastRequestToken == null) return@LaunchedEffect
        uiEventFlow.collect { event ->
            if (event.token == lastRequestToken) {
                animationInfo = when (event) {
                    is BookmarkEffect.Processing -> AnimationInfo.Processing
                    is BookmarkEffect.Success -> AnimationInfo.Success(event.newBookmarkState)
                    is BookmarkEffect.Failure -> AnimationInfo.Failure
                }
            }
        }
    }

    LaunchedEffect(animationInfo) {
        if (animationInfo is AnimationInfo.Success || animationInfo is AnimationInfo.Failure) {
            delay(1500L)
            animationInfo = null
        }
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .aspectRatio(aspectRatio)
            .semantics {
                contentDescription = "Artwork ${item.title}. Double tap to bookmark."
                role = Role.Image
            }
            .combinedClickable(
                onClick = { },
                onDoubleClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val token = UUID.randomUUID().toString()
                    lastRequestToken = token
                    onBookmark(token)
                }
            )
    ) {
        SmartPixivImage(
            item = item,
            pageIndex = pageIndex,
            contentDescription = "Artwork: ${item.title}",
            modifier = Modifier.fillMaxSize(),
            imageQuality = imageQuality,
            onImageLoaded = onImageLoaded,
            animationInfo  = animationInfo
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
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = pageOffset.absoluteValue }
                )
            }
        }
    }
}

@Composable
fun SmartPixivImage(
    item: DisplayableItem,
    pageIndex: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    imageQuality: ImageQuality,
    onImageLoaded: (String) -> Unit,
    animationInfo: AnimationInfo?
) {
    val largeUrl = item.largeImageUrls.getOrElse(pageIndex) { "" }
    val mediumUrl = item.mediumImageUrls.getOrElse(pageIndex) { "" }
    val context = LocalContext.current

    var imageUrlToDisplay by remember(largeUrl, mediumUrl) { mutableStateOf<String?>(null) }
    var displayedQuality by remember(largeUrl, mediumUrl) { mutableStateOf<ImageQuality?>(null) }
    val autoQuality by NetworkPerformanceMonitor.currentAutoQuality.collectAsState()

    LaunchedEffect(imageUrlToDisplay) {
        imageUrlToDisplay?.let(onImageLoaded)
    }

    LaunchedEffect(largeUrl, mediumUrl, imageQuality, autoQuality) {
        val imageLoader = SingletonImageLoader.get(context)

        suspend fun canLoad(url: String): Boolean {
            if (url.isEmpty()) return false
            return withContext(Dispatchers.IO) {
                val request = NormalImageRequest.normalImageRequest(context, url)
                imageLoader.execute(request).image != null
            }
        }

        val finalTargetQuality =
            if (imageQuality == ImageQuality.AUTO) autoQuality else imageQuality

        if (displayedQuality == ImageQuality.LARGE && finalTargetQuality == ImageQuality.MEDIUM) return@LaunchedEffect
        if (finalTargetQuality == displayedQuality && imageUrlToDisplay != null) return@LaunchedEffect

        imageUrlToDisplay = null

        when (imageQuality) {
            ImageQuality.LARGE, ImageQuality.MEDIUM -> {
                val url = if (imageQuality == ImageQuality.LARGE) largeUrl else mediumUrl
                if (canLoad(url)) {
                    imageUrlToDisplay = url
                    displayedQuality = imageQuality
                }
            }
            ImageQuality.AUTO -> {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val isWifi = cm.getNetworkCapabilities(cm.activeNetwork)
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: true
                val timeoutMs = if (isWifi) 350L else 600L

                coroutineScope {
                    if (autoQuality == ImageQuality.MEDIUM) {
                        if (canLoad(mediumUrl)) {
                            imageUrlToDisplay = mediumUrl
                            displayedQuality = ImageQuality.MEDIUM
                        }
                        return@coroutineScope
                    }

                    val startTime = System.currentTimeMillis()
                    val targetJob = launch {
                        if (canLoad(largeUrl)) {
                            if (coroutineContext.isActive) {
                                imageUrlToDisplay = largeUrl
                                displayedQuality = ImageQuality.LARGE
                                NetworkPerformanceMonitor.recordLoadTime(
                                    System.currentTimeMillis() - startTime
                                )
                            }
                        } else if (coroutineContext.isActive) {
                            if (canLoad(mediumUrl)) {
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
                        if (canLoad(mediumUrl)) {
                            imageUrlToDisplay = mediumUrl
                            displayedQuality = ImageQuality.MEDIUM
                        }
                    }
                }
            }
        }
    }

    val borderModifier = when (animationInfo) {
        is AnimationInfo.Processing -> Modifier.border(4.dp, Color.Yellow, RoundedCornerShape(10.dp))
        is AnimationInfo.Success -> {
            val borderColor = when (animationInfo.isBookmarked) {
                true -> Color.Magenta
                false -> Color.Gray
            }
            Modifier.border(4.dp, borderColor, RoundedCornerShape(10.dp))
        }
        is AnimationInfo.Failure -> Modifier.border(4.dp, Color.Red, RoundedCornerShape(10.dp))
        null -> Modifier
    }

    PixivAsyncImage(
        imageUrl = imageUrlToDisplay,
        contentDescription = contentDescription,
        modifier = modifier.then(borderModifier),
        contentScale = ContentScale.Crop
    )
}

sealed interface AnimationInfo {
    data class Success(val isBookmarked: Boolean) : AnimationInfo
    object Processing : AnimationInfo
    object Failure : AnimationInfo
}