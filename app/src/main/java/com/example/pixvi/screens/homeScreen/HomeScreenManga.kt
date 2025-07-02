package com.example.pixvi.screens.homeScreen

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.pixvi.FullImageScreen
import com.example.pixvi.network.response.Home.Manga.Illust
import com.example.pixvi.network.response.Home.ImageUtils
import com.example.pixvi.network.response.Home.SaveAllFormat
import com.example.pixvi.network.response.Home.SaveDestination
import com.example.pixvi.viewModels.MangaViewModel
import com.google.gson.Gson
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import okhttp3.Headers
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.pixvi.screens.MaterialBottomSheetOptionsMenu
import com.example.pixvi.viewModels.NotificationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.time.format.TextStyle as JavaDateTimeTextStyle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.carousel.CarouselItemScope
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import com.example.pixvi.network.BookmarkRestrict
import com.example.pixvi.network.response.Home.Manga.RankingIllust
import com.example.pixvi.screens.FloatingImageInfoToolbar
import kotlinx.coroutines.flow.MutableSharedFlow


@OptIn(FlowPreview::class)
@Composable
fun MangaScreen(
    navController: NavController,
    mangaViewModel: MangaViewModel
) {
    val uiState by mangaViewModel.uiState.collectAsState()
    var focusedIndex by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()

    val scope = rememberCoroutineScope()

    val favoriteRequestFlow = remember {
        MutableSharedFlow<Pair<Illust, BookmarkRestrict>>(extraBufferCapacity = 1)
    }

    LaunchedEffect(Unit) {
        mangaViewModel.loadInitialMangaRecommendations()
    }

    LaunchedEffect(Unit) {
        favoriteRequestFlow
            .debounce(400L) // Debounce for 400 milliseconds.
            .collect { (illust, visibility) ->
                // This code runs only after 400ms of inactivity
                mangaViewModel.toggleBookmark(
                    illustId = illust.id.toLong(),
                    isCurrentlyBookmarked = illust.is_bookmarked,
                    visibility = visibility
                )
            }
    }

    // This effect determines the focused item with smart debouncing.
    LaunchedEffect(listState, uiState.rankingIllusts.isNotEmpty()) {
        val recommendationItemOffset = if (uiState.rankingIllusts.isNotEmpty()) 1 else 0

        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) {
                return@snapshotFlow null
            }

            val focusedLazyColumnIndex = layoutInfo.visibleItemsInfo
                .minByOrNull { item ->
                    if (item.index < recommendationItemOffset) return@minByOrNull Float.MAX_VALUE

                    val viewportHeight = layoutInfo.viewportSize.height
                    val biasedCenterY = viewportHeight * 0.5f
                    val itemCenterY = item.offset + (item.size / 2f)
                    abs(itemCenterY - biasedCenterY)
                }?.index

            if (focusedLazyColumnIndex != null && focusedLazyColumnIndex >= recommendationItemOffset) {
                focusedLazyColumnIndex - recommendationItemOffset
            } else {
                null
            }
        }
            .debounce(250L)
            .distinctUntilChanged()
            .collect { recommendationIndex ->
                focusedIndex = recommendationIndex
            }
    }

    // Pagination
    LaunchedEffect(listState, uiState.nextUrl, uiState.isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { lastIndex ->
                val buffer = 5
                val totalItems = listState.layoutInfo.totalItemsCount
                if (uiState.recommendations.isNotEmpty() && totalItems > 0) {
                    val triggerIndex = (totalItems - 1 - buffer).coerceAtLeast(0)
                    if (lastIndex >= triggerIndex &&
                        !uiState.isLoadingMore &&
                        uiState.nextUrl != null
                    ) {
                        mangaViewModel.loadMoreMangaRecommendations()
                    }
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading && uiState.recommendations.isEmpty() -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null && uiState.recommendations.isEmpty() -> {
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
                        uiState.errorMessage ?: "An error occurred loading manga",
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { mangaViewModel.loadInitialMangaRecommendations() }) {
                        Text("Retry")
                    }
                }
            }
            else -> {
                // The LazyColumn is one child of the Box
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {

                    if (uiState.rankingIllusts.isNotEmpty()) {
                        item("manga-ranking-carousel") {
                            MangaRankingCarousel(
                                illusts = uiState.rankingIllusts,
                                navController = navController
                            )
                        }
                    }


                    itemsIndexed(
                        items = uiState.recommendations,
                        key = { _, mangaIllust -> mangaIllust.id }
                    ) { index, mangaIllust ->
                        MangaItemRow(
                            mangaIllust = mangaIllust,
                            navController = navController,
                            isFocused = index == focusedIndex
                        )
                    }
                    item {
                        if (uiState.isLoadingMore) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        }
                    }
                    item {
                        if (uiState.errorMessage != null && !uiState.isLoadingMore && uiState.recommendations.isNotEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Error loading more manga: ${uiState.errorMessage}",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } // --- The LazyColumn scope ends here ---

                // The Floating Toolbar is the second child of the Box
                val focusedIllust = focusedIndex?.let { uiState.recommendations.getOrNull(it) }

                if (focusedIllust != null) {
                    FloatingImageInfoToolbar(
                        illust = focusedIllust,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 30.dp)
                            .fillMaxWidth(),
                        onFavoriteClicked = {
                            scope.launch {
                                favoriteRequestFlow.emit(focusedIllust to BookmarkRestrict.PUBLIC)
                            }
                        },
                        onLongFavorite = {
                            scope.launch {
                                favoriteRequestFlow.emit(focusedIllust to BookmarkRestrict.PRIVATE)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MangaRankingCarousel(
    illusts: List<RankingIllust>,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = "Rankings",
                tint = Color(0xFFFFC107) // Gold color
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Rankings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { /* TODO: Implement navigation to manga rankings */ }) {
                Text("See more")
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null
                )
            }
        }

        val carouselState = rememberCarouselState { illusts.size }

        HorizontalUncontainedCarousel(
            state = carouselState,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            itemWidth = 186.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) { i ->
            val illust = illusts[i]
            CarouselMangaItem(illust = illust, navController = navController)
        }
    }
}

// --- NEW COMPOSABLE: CarouselMangaItem ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarouselItemScope.CarouselMangaItem(
    illust: RankingIllust, // Takes RankingIllust type
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appVersion = "7.14.0"
    val userAgent = "PixivAndroidApp/$appVersion (Android ${Build.VERSION.RELEASE}; ${Build.MODEL})"
    val commonHeaders = remember {
        Headers.Builder()
            .add("Referer", "https://app-api.pixiv.net/")
            .add("User-Agent", userAgent)
            .build()
    }

    val imageUrl = illust.image_urls.medium

    val imageRequest = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .headers(commonHeaders)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .maskClip(MaterialTheme.shapes.extraLarge)
            .clickable {
                val allOriginalUrls = if (illust.page_count > 1 && illust.meta_pages.isNotEmpty()) {
                    illust.meta_pages.map { it.image_urls.original }
                } else {
                    listOfNotNull(illust.meta_single_page.original_image_url)
                }

                if (allOriginalUrls.isNotEmpty()) {
                    navController.navigate(
                        FullImageScreen(
                            illustId = illust.id,
                            initialPageIndex = 0,
                            originalImageUrls = allOriginalUrls,
                            userAgent = userAgent
                        )
                    )
                } else {
                    Toast
                        .makeText(context, "Could not open image (no original URL)", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = illust.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Text(
            text = "@ ${illust.user.name}",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            ),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 15.dp, bottom = 10.dp)
        )
    }
}

@Composable
fun MangaItemRow(
    mangaIllust: Illust,
    navController: NavController,
    isFocused: Boolean
) {
    val context = LocalContext.current
    val title = mangaIllust.title
    val isMultiPage = mangaIllust.page_count > 1 && mangaIllust.meta_pages.isNotEmpty()
    val pageCount = if (isMultiPage) mangaIllust.meta_pages.size else mangaIllust.page_count

    val appVersion = "7.14.0"
    val userAgent = "PixivAndroidApp/$appVersion (Android ${Build.VERSION.RELEASE}; ${Build.MODEL})"
    val commonHeaders = remember {
        Headers.Builder()
            .add("Referer", "https://app-api.pixiv.net/")
            .add("User-Agent", userAgent)
            .build()
    }

    val aspectRatio = if (mangaIllust.width > 0 && mangaIllust.height > 0) {
        mangaIllust.width.toFloat() / mangaIllust.height.toFloat()
    } else { 2f / 3f }

    val pagerState = rememberPagerState(pageCount = { pageCount.coerceAtLeast(1) })
    var showMenuSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Use LruCache for bitmap caching with a reasonable size limit
    val bitmapCache = remember {
        object {
            private val cache = LruCache<Int, Bitmap>(100) // Cache last 100 pages (manga can have more pages)

            fun put(pageIndex: Int, bitmap: Bitmap) {
                cache.put(pageIndex, bitmap)
            }

            fun get(pageIndex: Int): Bitmap? = cache.get(pageIndex)
        }
    }

    val notificationViewModel: NotificationViewModel = viewModel()
    val gson = remember { Gson() }

    // Create a function that returns a page-specific listener
    val createImageRequestListener = remember {
        { pageIndex: Int ->
            object : ImageRequest.Listener {
                override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable) {
                        bitmapCache.put(pageIndex, drawable.bitmap)
                        Log.d("MangaItemRow", "Bitmap cached for page $pageIndex for URL: ${request.data}")
                    } else {
                        Log.w("MangaItemRow", "Drawable is not a BitmapDrawable for URL: ${request.data}")
                    }
                }

                override fun onError(request: ImageRequest, result: ErrorResult) {
                    Log.e("MangaItemRow", "Error loading image for page $pageIndex: ${result.throwable.message}")
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio, matchHeightConstraintsFirst = false)
                .border(
                    width = if (isFocused) 3.dp else 0.dp,
                    color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                )
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(
                    onClick = {
                        val currentPageIndex = if (isMultiPage) pagerState.currentPage else 0
                        val allOriginalUrls: List<String> = if (isMultiPage) {
                            mangaIllust.meta_pages.mapNotNull { it.image_urls.original }
                        } else {
                            listOfNotNull(mangaIllust.meta_single_page.original_image_url)
                        }

                        if (allOriginalUrls.isNotEmpty()) {
                            navController.navigate(
                                FullImageScreen(
                                    illustId = mangaIllust.id,
                                    initialPageIndex = currentPageIndex,
                                    originalImageUrls = allOriginalUrls,
                                    userAgent = userAgent
                                )
                            )
                        } else {
                            Toast.makeText(context, "Original image URL not found.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onLongClick = {
                        val currentPageIndex = if (isMultiPage) pagerState.currentPage else 0
                        Log.d("MangaItemRow", "Long press on page $currentPageIndex")

                        if (bitmapCache.get(currentPageIndex) != null) {
                            showMenuSheet = true
                        } else {
                            Log.d("MangaItemRow", "Long press ignored: Image not loaded yet for page $currentPageIndex")
                            Toast.makeText(context, "Image is loading...", Toast.LENGTH_SHORT).show()
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isMultiPage) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { pageIndex ->
                    val page = mangaIllust.meta_pages.getOrNull(pageIndex)
                    val imageUrl = page?.image_urls?.large
                        ?: page?.image_urls?.medium
                        ?: mangaIllust.image_urls.medium

                    // Create a page-specific listener
                    val pageSpecificListener = remember(pageIndex) {
                        createImageRequestListener(pageIndex)
                    }

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .headers(commonHeaders)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .listener(pageSpecificListener)
                            .build(),
                        contentDescription = "$title (Page ${pageIndex + 1})",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else { // Single Page
                val imageUrl = mangaIllust.image_urls.large ?: mangaIllust.image_urls.medium

                // For single page, use page index 0
                val singlePageListener = remember { createImageRequestListener(0) }

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .headers(commonHeaders)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .listener(singlePageListener)
                        .build(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // --- "Series" Badge ---
            if (mangaIllust.series == null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clickable {
                            //Show the information about the series
                        },
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(
                        text = "Series",
                        color = MaterialTheme.colorScheme.onSecondary,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            ConstraintLayout(modifier = Modifier.fillMaxSize()) {
                val (pageIndicatorRef) = createRefs()

                if (isMultiPage && pageCount > 1) {
                    CircularPageProgressIndicator(
                        pagerState = pagerState,
                        modifier = Modifier
                            .constrainAs(pageIndicatorRef) {
                                top.linkTo(parent.top, margin = 12.dp)
                                end.linkTo(parent.end, margin = 12.dp)
                            }
                    )
                }
            }
        }

        // Optimized Bottom Sheet Menu for image copying
        MaterialBottomSheetOptionsMenu(
            showSheet = showMenuSheet,
            onDismiss = { showMenuSheet = false },
            isMultiPage = isMultiPage,
            onSave = { destination ->
                val currentPageIndex = pagerState.currentPage
                Log.d("MangaItemRow", "Save/copy requested for page $currentPageIndex")

                scope.launch {
                    when (destination) {
                        SaveDestination.Device -> {
                            val originalImageUrl: String? = if (isMultiPage) {
                                mangaIllust.meta_pages.getOrNull(currentPageIndex)?.image_urls?.original
                            } else {
                                mangaIllust.meta_single_page.original_image_url
                            }

                            if (originalImageUrl == null) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Original URL not found for saving", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }

                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Saving original to device...", Toast.LENGTH_SHORT).show()
                            }

                            try {
                                val bitmap = ImageUtils.loadBitmapFromUrl(context, originalImageUrl, commonHeaders)
                                if (bitmap != null) {
                                    launch(Dispatchers.IO) {
                                        ImageUtils.saveBitmapToMediaStore(
                                            context = context,
                                            bitmap = bitmap,
                                            illustId = mangaIllust.id,
                                            pageIndex = currentPageIndex,
                                            displayName = mangaIllust.title
                                        )
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Failed to load image for saving", Toast.LENGTH_SHORT).show()
                                    }
                                    Log.e("MangaItemRow", "Failed to load original bitmap for saving device.")
                                }
                            } catch (e: Exception) {
                                Log.e("MangaItemRow", "Error saving image: ${e.message}", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        SaveDestination.Clipboard -> {
                            // Try to use cached bitmap for the current page
                            val currentBitmap = bitmapCache.get(currentPageIndex)
                            if (currentBitmap != null) {
                                try {
                                    ImageUtils.copyBitmapToClipboardViaCache(
                                        context = context,
                                        bitmap = currentBitmap,
                                        illustId = mangaIllust.id,
                                        pageIndex = currentPageIndex
                                    )
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Copied displayed image to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e("MangaItemRow", "Error copying to clipboard: ${e.message}", e)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Error copying: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                // Fallback to loading from original URL
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Loading original to copy...", Toast.LENGTH_SHORT).show()
                                }

                                val originalImageUrl: String? = if (isMultiPage) {
                                    mangaIllust.meta_pages.getOrNull(currentPageIndex)?.image_urls?.original
                                } else {
                                    mangaIllust.meta_single_page.original_image_url
                                }

                                if (originalImageUrl == null) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Original URL not found for copying", Toast.LENGTH_SHORT).show()
                                    }
                                    return@launch
                                }

                                try {
                                    val bitmapToCopy = ImageUtils.loadBitmapFromUrl(context, originalImageUrl, commonHeaders)
                                    if (bitmapToCopy != null) {
                                        ImageUtils.copyBitmapToClipboardViaCache(
                                            context = context,
                                            bitmap = bitmapToCopy,
                                            illustId = mangaIllust.id,
                                            pageIndex = currentPageIndex
                                        )
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Copied original image to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Failed to load image for copying", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("MangaItemRow", "Error loading original for clipboard: ${e.message}", e)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Error copying: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                }
                showMenuSheet = false
            },
            onSaveAll = { format ->
                scope.launch {
                    if (format == SaveAllFormat.Pdf && isMultiPage) {
                        val originalImageUrlsList = mangaIllust.meta_pages.mapNotNull { it.image_urls.original }
                        if (originalImageUrlsList.size == mangaIllust.page_count) {
                            val headersMap = mapOf(
                                "Referer" to "https://app-api.pixiv.net/",
                                "User-Agent" to userAgent
                            )
                            notificationViewModel.initiateAndTrackPdfExport(
                                illustId = mangaIllust.id,
                                illustTitle = mangaIllust.title,
                                totalPages = mangaIllust.page_count,
                                originalImageUrls = originalImageUrlsList,
                                originalHeadersJson = gson.toJson(headersMap)
                            )
                            Toast.makeText(context, "PDF creation started for manga.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Could not get all original URLs for PDF.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Save All as ${format.displayName} (Manga) not fully implemented.", Toast.LENGTH_SHORT).show()
                    }
                }
                showMenuSheet = false
            }
        )
    }
}

// You'll need these helper functions (formatCount, formatDateString) and composables
// (CircularPageProgressIndicator, SmallInfoIconText, MaterialBottomSheetOptionsMenu)
// available in this scope, either by defining them here or importing them if they are in a shared file.
// For brevity, I'm assuming they are accessible.





// ... imports
@Composable
fun NovelScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text("Novel Screen - Content Here")
    }
}

@Composable
fun NewestScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text("Newest Screen - Content Here")
    }
}

@Composable
fun RankingScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text("Ranking Screen - Content Here")
    }
}