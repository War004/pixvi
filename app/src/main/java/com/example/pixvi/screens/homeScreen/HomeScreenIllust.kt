package com.example.pixvi.screens.homeScreen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.request.ImageRequest
import kotlinx.coroutines.flow.distinctUntilChanged
import android.os.Build
import android.util.Log
import android.util.LruCache
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.filterNotNull
import coil.compose.AsyncImage
import com.example.pixvi.network.response.Home.Illust.Illust
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import okhttp3.Headers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.time.format.TextStyle as JavaDateTimeTextStyle
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil.imageLoader
import coil.request.ErrorResult
import coil.request.SuccessResult
import com.example.pixvi.FullImageScreen
import com.example.pixvi.network.response.Home.ImageUtils
import com.example.pixvi.network.response.Home.SaveAllFormat
import com.example.pixvi.network.response.Home.SaveDestination
import com.example.pixvi.viewModels.NotificationViewModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.example.pixvi.screens.MaterialBottomSheetOptionsMenu
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.carousel.CarouselItemScope
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextOverflow
import coil.request.CachePolicy
import com.example.pixvi.network.BookmarkRestrict
import com.example.pixvi.screens.FloatingImageInfoToolbar
import com.example.pixvi.utils.NormalImageRequest
import com.example.pixvi.utils.PixivAsyncImage
import com.example.pixvi.viewModels.HomeIllustViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce

private const val NO_PROFILE_IMAGE_URL = "https://s.pximg.net/common/images/no_profile.png"


@OptIn(FlowPreview::class)
@Composable
fun IllustrationsScreen(
    modifier: Modifier,
    navController: NavController,
    homeIllustViewModel: HomeIllustViewModel
) {
    val uiState by homeIllustViewModel.uiState.collectAsState()

    var focusedIndex by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()

    val scope = rememberCoroutineScope()

    val favoriteRequestFlow = remember {
        MutableSharedFlow<Pair<Illust, BookmarkRestrict>>(extraBufferCapacity = 1)
    }

    LaunchedEffect(Unit) {
        favoriteRequestFlow
            .debounce(400L) // Debounce for 400 milliseconds.
            .collect { (illust, visibility) ->
                // This code runs only after 400ms of inactivity
                homeIllustViewModel.toggleBookmark(
                    illustId = illust.id.toLong(),
                    isCurrentlyBookmarked = illust.is_bookmarked,
                    visibility = visibility
                )
            }
    }


    LaunchedEffect(Unit) {
        homeIllustViewModel.loadInitialRecommendations()
    }

    // This effect determines the focused item with smart debouncing.
    LaunchedEffect(listState, uiState.rankingIllusts.isNotEmpty()) {
        // This calculates how many non-recommendation items are at the top of the list.
        val recommendationItemOffset = if (uiState.rankingIllusts.isNotEmpty()) 1 else 0

        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) {
                return@snapshotFlow null // No items visible, no focus.
            }

            // Determine the LazyColumn index of the item nearest the biased center.
            val focusedLazyColumnIndex = layoutInfo.visibleItemsInfo
                .minByOrNull { item ->
                    // Crucially, ignore the carousel when calculating which item to focus.
                    if (item.index < recommendationItemOffset) return@minByOrNull Float.MAX_VALUE

                    val viewportHeight = layoutInfo.viewportSize.height
                    val biasedCenterY = viewportHeight * 0.5f // A 50% bias feels more natural.
                    val itemCenterY = item.offset + (item.size / 2f)
                    abs(itemCenterY - biasedCenterY)
                }?.index

            // Convert the absolute LazyColumn index to a relative index for the recommendations list.
            if (focusedLazyColumnIndex != null && focusedLazyColumnIndex >= recommendationItemOffset) {
                focusedLazyColumnIndex - recommendationItemOffset
            } else {
                null // If the carousel is the most prominent item, nothing is focused.
            }
        }
            .debounce(250L) // A slightly shorter debounce feels more responsive.
            .distinctUntilChanged()
            .collect { recommendationIndex ->
                // This collected index is now correctly relative to the recommendations list.
                focusedIndex = recommendationIndex
            }
    }
    // --- END NEW ---

    // Pagination (This existing effect remains unchanged)
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
                        homeIllustViewModel.loadMoreRecommendations()
                    }
                }
            }
    }

    Box(
        modifier = modifier
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
                        uiState.errorMessage ?: "An error occurred",
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { homeIllustViewModel.loadInitialRecommendations() }) {
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
                        item("ranking-carousel") {
                            RankingCarousel(
                                illusts = uiState.rankingIllusts,
                                navController = navController
                            )
                        }
                    }

                    itemsIndexed(
                        items = uiState.recommendations,
                        key = { _, illust -> illust.id }
                    ) { index, illust ->
                        PixivImageRow(
                            illust = illust,
                            navController = navController,
                            isFocused = index == focusedIndex
                        )
                    }

                    // Loading/Error items at the bottom of the list
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
                                    "Error loading more: ${uiState.errorMessage}",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } // --- The LazyColumn scope ends here ---

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
private fun RankingCarousel(
    illusts: List<Illust>,
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
                imageVector = Icons.Default.WorkspacePremium, // Icon similar to a ranking crown/badge
                contentDescription = "Rankings",
                tint = Color(0xFFFFC107) // A gold color
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Rankings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f)) // Pushes "See more" to the end
            TextButton(onClick = { /* TODO: Implement navigation to a rankings screen */ }) {
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
                .height(220.dp), // Height to contain the items + padding
            itemWidth = 186.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) { i ->
            val illust = illusts[i]
            CarouselIllustItem(illust = illust, navController = navController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarouselItemScope.CarouselIllustItem(
    illust: Illust,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val imageUrl = illust.image_urls.medium
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
                            initialPageIndex = 0, // Carousel items always start at the first page
                            originalImageUrls = allOriginalUrls,
                        )
                    )
                } else {
                    Toast
                        .makeText(context, "Could not open image (no original URL)", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    ) {
        // The image serves as the background of the Box
        PixivAsyncImage(
            imageUrl = imageUrl,
            contentDescription = illust.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // The Text is aligned to the bottom-start of the parent Box, over the scrim
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
fun PixivImageRow(
    illust: Illust,
    navController: NavController,
    isFocused: Boolean // New parameter to control the highlight
) {
    val context = LocalContext.current
    val title = illust.title
    val isMultiPage = illust.page_count > 1 && illust.meta_pages.isNotEmpty()
    val pageCount = if (isMultiPage) illust.meta_pages.size else illust.page_count

    val imageLoadStates = remember { mutableStateMapOf<Int, Boolean>() } //image states for showing the bottom sheet

    val aspectRatio = if (illust.width > 0 && illust.height > 0) {
        illust.width.toFloat() / illust.height.toFloat()
    } else { 2f / 3f }

    val pagerState = rememberPagerState(pageCount = { pageCount })

    var showMenuSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val notificationViewModel: NotificationViewModel = viewModel()
    val getImageUrl = remember {
        { pageIndex: Int ->
            if (isMultiPage) {
                val page = illust.meta_pages.getOrNull(pageIndex)
                page?.image_urls?.large ?: page?.image_urls?.medium
            } else {
                illust.image_urls.large ?: illust.image_urls.medium
            }
        }
    }
    val getOriginalUrl = remember {
        { pageIndex: Int ->
            if (isMultiPage) {
                illust.meta_pages.getOrNull(pageIndex)?.image_urls?.original
            } else {
                illust.meta_single_page.original_image_url
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
                            illust.meta_pages.map { it.image_urls.original }
                        } else {
                            listOfNotNull(illust.meta_single_page.original_image_url)
                        }

                        if (allOriginalUrls.isNotEmpty()) {
                            navController.navigate(
                                FullImageScreen(
                                    illustId = illust.id,
                                    initialPageIndex = currentPageIndex,
                                    originalImageUrls = allOriginalUrls,
                                )
                            )
                        } else {
                            Toast.makeText(context, "Could not open image (no original URL)", Toast.LENGTH_SHORT).show()
                            Log.e("PixivImageRow", "Could not find original image URL(s) for illust ID: ${illust.id}")
                        }
                    },
                    onLongClick = {
                        val currentPageIndex = if (isMultiPage) pagerState.currentPage else 0
                        Log.d("PixivImageRow", "Long press on page $currentPageIndex")

                        if (imageLoadStates[currentPageIndex] == true) {
                            showMenuSheet = true
                        } else {
                            Log.d("PixivImageRow", "Long press ignored: Image not loaded yet for page $currentPageIndex")
                            Toast.makeText(context, "Image is loading...", Toast.LENGTH_SHORT).show()
                        }
                    },
                ),
            contentAlignment = Alignment.Center
        ) {
            // The rest of the Box content (HorizontalPager, ImageLoaderItem, overlays) remains the same
            if (isMultiPage) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { pageIndex ->
                    ImageLoaderItem(
                        pageIndex = pageIndex,
                        getImageUrl = getImageUrl,
                        contentDescription = "$title (Page ${pageIndex + 1})",
                        context = context,
                        imageLoadStates = imageLoadStates
                    )
                }
            } else { // Single page image
                ImageLoaderItem(
                    pageIndex = 0,
                    getImageUrl = getImageUrl,
                    contentDescription = title,
                    context = context,
                    imageLoadStates = imageLoadStates
                )
            }

            //removed the info on the image and now using floating toolbar
            if (isMultiPage && pageCount > 1) {
                ConstraintLayout(modifier = Modifier.fillMaxSize()) {
                    val (pageIndicatorRef) = createRefs()
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

        // The BottomSheet remains unchanged
        MaterialBottomSheetOptionsMenu(
            showSheet = showMenuSheet,
            onDismiss = { showMenuSheet = false },
            isMultiPage = isMultiPage,
            onSave = { destination ->
                val currentPageIndex = pagerState.currentPage
                Log.d("PixivImageRow", "Save requested for page $currentPageIndex")

                scope.launch {
                    when (destination) {
                        SaveDestination.Device -> {
                            val originalImageUrl = getOriginalUrl(currentPageIndex)

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
                                val bitmap = ImageUtils.loadBitmapFromUrl(context, originalImageUrl)
                                if (bitmap != null) {
                                    launch(Dispatchers.IO) {
                                        ImageUtils.saveBitmapToMediaStore(
                                            context = context,
                                            bitmap = bitmap,
                                            illustId = illust.id,
                                            pageIndex = currentPageIndex,
                                            displayName = illust.title
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

                        SaveDestination.Clipboard -> {
                            val currentPageIndex = pagerState.currentPage
                            val displayedImageUrl = getImageUrl(currentPageIndex)

                            var copiedFromCache = false
                            if (displayedImageUrl != null) {
                                val cacheKey = coil.memory.MemoryCache.Key(displayedImageUrl)

                                // 3. Check Coil's memory cache for the bitmap
                                context.imageLoader.memoryCache?.let { cache ->
                                    val cachedBitmap = cache[cacheKey]?.bitmap
                                    if (cachedBitmap != null) {
                                        Log.d("FastCopy", "Found bitmap in Coil's memory cache.")
                                        try {
                                            ImageUtils.copyBitmapToClipboardViaCache(
                                                context,
                                                cachedBitmap,
                                                illust.id,
                                                currentPageIndex
                                            )
                                            Toast.makeText(context, "Copied displayed image", Toast.LENGTH_SHORT).show()
                                            copiedFromCache = true
                                        } catch (e: Exception) {
                                            Log.e("FastCopy", "Error copying from cache", e)
                                        }
                                    }
                                }
                            }

                            // 4. FALLBACK PATH: If not found in cache, download the original.
                            if (!copiedFromCache) {
                                Log.d("FastCopy", "Bitmap not in cache. Downloading original.")
                                val originalImageUrl = getOriginalUrl(currentPageIndex)

                                if (originalImageUrl != null) {
                                    // ... your existing logic to download and copy the original image ...
                                    val bitmapToCopy = ImageUtils.loadBitmapFromUrl(context, originalImageUrl)
                                    if (bitmapToCopy != null) {
                                        ImageUtils.copyBitmapToClipboardViaCache(
                                            context,
                                            bitmapToCopy,
                                            illust.id,
                                            currentPageIndex
                                        )
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Copied original image", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Failed to load image for copying", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Original URL not found", Toast.LENGTH_SHORT).show()
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
                    if (format == SaveAllFormat.Pdf) {
                        try {
                            val originalImageUrlsList: List<String> = if (isMultiPage) {
                                illust.meta_pages.map { it.image_urls.original }
                            } else {
                                listOfNotNull(illust.meta_single_page.original_image_url)
                            }

                            if (originalImageUrlsList.isEmpty() || (isMultiPage && originalImageUrlsList.size != illust.page_count)) {
                                Log.e("PixivImageRow", "Mismatch or missing original URLs for PDF export. Found: ${originalImageUrlsList.size}, Expected: ${illust.page_count}")
                                Toast.makeText(context, "Error: Could not get all original image URLs for PDF.", Toast.LENGTH_LONG).show()
                                return@launch
                            }


                            val notificationJobId = withContext(Dispatchers.IO) {
                                notificationViewModel.initiateAndTrackPdfExport(
                                    illustId = illust.id,
                                    illustTitle = illust.title,
                                    totalPages = illust.page_count,
                                    originalImageUrls = originalImageUrlsList,
                                )
                            }

                            if (notificationJobId > 0) {
                                Toast.makeText(context, "PDF creation started. Check notifications.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to start PDF creation job.", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Log.e("PixivImageRow", "Failed to initiate PDF export job", e)
                            Toast.makeText(context, "Error starting PDF creation: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Save All as ${format.displayName} not implemented yet", Toast.LENGTH_SHORT).show()
                    }
                }
                showMenuSheet = false
            }
        )
    }
}

@Composable
private fun ImageLoaderItem(
    pageIndex: Int,
    getImageUrl: (Int) -> String?,
    contentDescription: String,
    context: Context,
    imageLoadStates: MutableMap<Int, Boolean>
) {
    val imageUrl = getImageUrl(pageIndex) ?: return

    DisposableEffect(pageIndex) {
        onDispose {
            imageLoadStates.remove(pageIndex)
        }
    }

    // The request no longer needs a listener attached here.
    val imageRequest = remember(imageUrl) {
        NormalImageRequest.normalImageRequest(context, imageUrl ?: "")
    }

    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        // This is the only part you need to update the state.
        onState = { state ->
            imageLoadStates[pageIndex] = state is coil.compose.AsyncImagePainter.State.Success
        }
    )
}


// Define states for imperative loading
private sealed class ImageLoadState {
    data object Loading : ImageLoadState()
    data class Success(val imageBitmap: ImageBitmap) : ImageLoadState()
    data class Error(val throwable: Throwable) : ImageLoadState()
}

const val DOUBLE_TAP_SCALE_FACTOR = 1.5f  //How much to zoom

@Composable
fun FullScreenImage(
    illustId: Int,
    initialPageIndex: Int,
    originalImageUrls: List<String>,
) {
    val context = LocalContext.current
    var imageLoadState by remember { mutableStateOf<ImageLoadState>(ImageLoadState.Loading) }

    val initialImageUrl = originalImageUrls.getOrElse(initialPageIndex) {
        Log.e("FullScreenImage", "Initial page index $initialPageIndex out of bounds. Falling back.")
        originalImageUrls.firstOrNull() ?: ""
    }

    val appVersion = "7.14.0"
    val userAgent = "PixivAndroidApp/$appVersion (Android ${Build.VERSION.RELEASE}; ${Build.MODEL})"

    var showMenuSheet by remember { mutableStateOf(false) }
    val isMultiPage = originalImageUrls.size > 1
    val scope = rememberCoroutineScope()

    // --- State for Transformations ---
    var scaleState by remember { mutableFloatStateOf(1f) }
    var offsetState by remember { mutableStateOf(Offset.Zero) }

    // --- Animated values for smoother transitions ---
    val animatedScale by animateFloatAsState(targetValue = scaleState, label = "scaleAnimation")
    val animatedOffset by animateOffsetAsState(targetValue = offsetState, label = "offsetAnimation")

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var fittedImageSize by remember { mutableStateOf(Size.Zero) } // Size of image when ContentScale.Fit is applied
    var bitmapSize by remember { mutableStateOf(IntSize.Zero) } // Actual bitmap dimensions

    LaunchedEffect(bitmapSize, containerSize) {
        if (bitmapSize.width > 0 && bitmapSize.height > 0 && containerSize.width > 0 && containerSize.height > 0) {
            val bitmapRatio = bitmapSize.width.toFloat() / bitmapSize.height.toFloat()
            val containerRatio = containerSize.width.toFloat() / containerSize.height.toFloat()

            fittedImageSize = if (bitmapRatio > containerRatio) {
                Size(width = containerSize.width.toFloat(), height = containerSize.width.toFloat() / bitmapRatio)
            } else {
                Size(width = containerSize.height.toFloat() * bitmapRatio, height = containerSize.height.toFloat())
            }
            Log.d("FullScreenImage", "Calculated fittedSize: $fittedImageSize for bitmap $bitmapSize in container $containerSize")
        } else {
            fittedImageSize = Size.Zero
        }
    }

    val transformableState = rememberTransformableState { zoomChange, panChange, rotationChange ->
        val newScale = (scaleState * zoomChange).coerceIn(1f, 5f)

        val scaledFittedWidth = fittedImageSize.width * newScale
        val scaledFittedHeight = fittedImageSize.height * newScale

        val maxOffsetX = if (scaledFittedWidth > containerSize.width) (scaledFittedWidth - containerSize.width) / 2f else 0f
        val maxOffsetY = if (scaledFittedHeight > containerSize.height) (scaledFittedHeight - containerSize.height) / 2f else 0f

        // Adjust panChange by the current scale to make panning feel natural
        // However, transformableState already provides a scaled panChange.
        // If pan feels too slow or too fast, you might adjust it, but usually, the raw panChange is correct.
        val newOffset = offsetState + panChange
        val clampedOffset = Offset(
            x = newOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
            y = newOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
        )

        scaleState = newScale
        offsetState = clampedOffset
    }

    val commonHeaders = remember(userAgent) {
        Headers.Builder()
            .add("Referer", "https://app-api.pixiv.net/")
            .add("User-Agent", userAgent)
            .build()
    }

    LaunchedEffect(initialImageUrl, commonHeaders) {
        Log.d("FullScreenImage", "Loading image: $initialImageUrl")
        scaleState = 1f
        offsetState = Offset.Zero
        bitmapSize = IntSize.Zero
        imageLoadState = ImageLoadState.Loading

        if (initialImageUrl.isBlank()) {
            imageLoadState = ImageLoadState.Error(IllegalArgumentException("Image URL is blank"))
            return@LaunchedEffect
        }

        val request = ImageRequest.Builder(context)
            .data(initialImageUrl)
            .headers(commonHeaders)
            .allowHardware(false) // Important for getting Bitmap directly for transformations
            .listener(
                onStart = { Log.d("FullScreenImage", "Request Started: $initialImageUrl") },
                onSuccess = { _, result ->
                    val drawable: Drawable = result.drawable
                    val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        bitmapSize = IntSize(bitmap.width, bitmap.height)
                        imageLoadState = ImageLoadState.Success(bitmap.asImageBitmap())
                        Log.d("FullScreenImage", "Success - Bitmap size: $bitmapSize")
                    } else {
                        val errorMsg = "Could not get Bitmap from Drawable: ${drawable::class.java.name}"
                        Log.e("FullScreenImage", errorMsg)
                        imageLoadState = ImageLoadState.Error(IllegalStateException(errorMsg))
                    }
                },
                onError = { _, result ->
                    Log.e("FullScreenImage", "Request Error", result.throwable)
                    imageLoadState = ImageLoadState.Error(result.throwable)
                }
            ).build()
        context.imageLoader.execute(request)
    }

    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 48.dp.toPx() }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .clipToBounds()
                .onSizeChanged { newSize ->
                    if (newSize != containerSize) {
                        Log.d("FullScreenImage", "Container size changed: $newSize")
                        containerSize = newSize
                        // Optional: Reset zoom/pan on resize.
                        // scaleState = 1f
                        // offsetState = Offset.Zero
                    }
                }
                // --- GESTURE MODIFIERS ORDERED FOR PRECEDENCE ---
                // 1. PointerInput for Taps, Long Press, Double Tap
                .pointerInput(imageLoadState, scaleState, fittedImageSize, containerSize) { // Add all relevant keys
                    detectTapGestures(
                        onTap = {
                            Log.d("FullScreenImage", "onTap triggered")
                            // TODO: Implement original onClick functionality (e.g., toggle UI elements like system bars)
                        },
                        onLongPress = { offset -> // offset is the position of the long press
                            Log.d("FullScreenImage", "onLongPress triggered. Current scale: $scaleState")
                            if (imageLoadState is ImageLoadState.Success) {
                                showMenuSheet = true
                                Log.d("FullScreenImage", "showMenuSheet set to true via onLongPress")
                            } else {
                                Log.d("FullScreenImage", "onLongPress: Image not loaded.")
                            }
                        },
                        onDoubleTap = { tapOffset ->
                            Log.d("FullScreenImage", "onDoubleTap triggered.")
                            if (imageLoadState is ImageLoadState.Success) {
                                if (scaleState > 1.01f) { // If zoomed in (use epsilon for float comparison)
                                    scaleState = 1f
                                    offsetState = Offset.Zero
                                } else { // If zoomed out, zoom in to the tap point
                                    scaleState = DOUBLE_TAP_SCALE_FACTOR

                                    // Calculate offset to center the zoom on the tap point
                                    val newOffsetX = (tapOffset.x - containerSize.width / 2f) * (1 - DOUBLE_TAP_SCALE_FACTOR) - offsetState.x // adjust by current offset
                                    val newOffsetY = (tapOffset.y - containerSize.height / 2f) * (1 - DOUBLE_TAP_SCALE_FACTOR) - offsetState.y // adjust by current offset

                                    val scaledFittedWidth = fittedImageSize.width * DOUBLE_TAP_SCALE_FACTOR
                                    val scaledFittedHeight = fittedImageSize.height * DOUBLE_TAP_SCALE_FACTOR
                                    val maxOffsetX = if (scaledFittedWidth > containerSize.width) (scaledFittedWidth - containerSize.width) / 2f else 0f
                                    val maxOffsetY = if (scaledFittedHeight > containerSize.height) (scaledFittedHeight - containerSize.height) / 2f else 0f

                                    offsetState = Offset(
                                        x = newOffsetX.coerceIn(-maxOffsetX, maxOffsetX),
                                        y = newOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                    )
                                }
                                Log.d("FullScreenImage", "Double tap: new scale $scaleState, new offset $offsetState")
                            }
                        }
                    )
                }
                // 2. PointerInput for Swipe (conditional)
                .pointerInput(scaleState, swipeThresholdPx) { // Keyed on scaleState to re-evaluate if swipe is enabled
                    if (scaleState <= 1.01f) { // Only detect swipes if fully zoomed out (add small epsilon for float comparison)
                        var dragAmountHorizontal = 0f
                        detectDragGestures(
                            onDragStart = { dragAmountHorizontal = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume() // Consume drag events to prevent transformable from panning
                                dragAmountHorizontal += dragAmount.x
                            },
                            onDragEnd = {
                                if (abs(dragAmountHorizontal) > swipeThresholdPx) {
                                    if (dragAmountHorizontal > 0) {
                                        Log.i("FullScreenImage", "Swiped Right (when zoomed out)")
                                        // TODO: Implement action for swipe right (e.g., navigate to previous image)
                                        // onSwipeRight()
                                    } else {
                                        Log.i("FullScreenImage", "Swiped Left (when zoomed out)")
                                        // TODO: Implement action for swipe left (e.g., navigate to next image)
                                        // onSwipeLeft()
                                    }
                                }
                            }
                        )
                    }
                }
                // 3. Transformable for Pinch-to-Zoom and Pan
                .transformable(
                    state = transformableState,
                    lockRotationOnZoomPan = true
                ),
            contentAlignment = Alignment.Center
        ) {
            when (val state = imageLoadState) {
                is ImageLoadState.Loading -> CircularProgressIndicator()
                is ImageLoadState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Warning, "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Failed to load image.", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(4.dp))
                        Text(state.throwable.localizedMessage ?: "Unknown error", fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
                is ImageLoadState.Success -> {
                    Image(
                        bitmap = state.imageBitmap,
                        contentDescription = "Full Screen Image",
                        modifier = Modifier
                            .fillMaxSize() // Let the Box initially constrain the image
                            .graphicsLayer(
                                scaleX = animatedScale,
                                scaleY = animatedScale,
                                translationX = animatedOffset.x,
                                translationY = animatedOffset.y
                            ),
                        contentScale = ContentScale.Fit // Image will be 'Fit' within its bounds before graphicsLayer transformations
                    )
                }
            }
        }

        MaterialBottomSheetOptionsMenu(
            showSheet = showMenuSheet,
            onDismiss = { showMenuSheet = false },
            isMultiPage = isMultiPage,
            onSave = { destination ->
                scope.launch { // Launch a coroutine for UI feedback
                    var success = false
                    (imageLoadState as? ImageLoadState.Success)?.imageBitmap?.asAndroidBitmap()?.let { bmp ->
                        withContext(Dispatchers.IO) { // Perform disk/clipboard operations on IO thread
                            try {
                                when (destination) {
                                    SaveDestination.Device -> ImageUtils.saveBitmapToMediaStore(context, bmp, illustId, initialPageIndex, "fullscreen_image")
                                    SaveDestination.Clipboard -> ImageUtils.copyBitmapToClipboardViaCache(context, bmp, illustId, initialPageIndex)
                                }
                                success = true
                            } catch (e: Exception) {
                                Log.e("FullScreenImage", "Error during save/copy", e)
                            }
                        }
                    }
                    if (success) {
                        Toast.makeText(context, "${destination.name} action initiated.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to perform action. Image not loaded or error occurred.", Toast.LENGTH_LONG).show()
                    }
                    showMenuSheet = false // Dismiss sheet after action
                }
            },
            onSaveAll = { format ->
                Log.d("FullScreenImage", "Save All as $format (Illust ID: $illustId)")
                // TODO: Implement Save All logic
                scope.launch {
                    Toast.makeText(context, "Save All as $format (not yet implemented)", Toast.LENGTH_SHORT).show()
                    showMenuSheet = false // Dismiss sheet
                }
            }
        )
    }
}


@Composable
fun SmallInfoIconText(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 6.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Content description could be improved for accessibility
            modifier = Modifier.size(14.dp),
            tint = LocalContentColor.current.copy(alpha = 0.8f) // Use LocalContentColor
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = LocalContentColor.current.copy(alpha = 0.9f), // Use LocalContentColor
            maxLines = 1
        )
    }
}

// --- Helper Function to Format Counts (e.g., 1234 -> 1.2k) ---
fun formatCount(count: Int): String {
    if (count < 1000) return count.toString()
    val exp = (ln(count.toDouble()) / ln(1000.0)).toInt()
    val unit = "kMGTPE"[exp - 1]
    return String.format(Locale.US, "%.1f%c", count / 1000.0.pow(exp.toDouble()), unit)
        .replace(".0", "") // Avoid showing .0 for whole thousands (e.g., 1.0k -> 1k)
}


// --- Helper Function to Format Date String (e.g., "2023-05-03T..." -> "May 3") ---
fun formatDateString(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "--"
    return try {
        val odt = OffsetDateTime.parse(dateString)
        val month = odt.month.getDisplayName(JavaDateTimeTextStyle .SHORT, Locale.getDefault())
        val day = odt.dayOfMonth
        "$month $day"
    } catch (e: DateTimeParseException) {
        Log.w("DateParse", "Failed to parse date: $dateString", e)
        // Fallback: Try extracting YYYY-MM-DD if ISO parse fails
        try {
            val datePart = dateString.substringBefore("T")
            val localDate = LocalDate.parse(datePart)
            val month = localDate.month.getDisplayName(JavaDateTimeTextStyle .SHORT, Locale.getDefault())
            val day = localDate.dayOfMonth
            "$month $day"
        } catch (e2: Exception) {
            Log.w("DateParse", "Fallback date parse failed for: $dateString", e2)
            "--" // Return placeholder on failure
        }
    }
}

/**
 * A circular page indicator that shows progress as a filling boundary
 * and the current page number in the center.
 * Includes a semi-transparent background for better visibility.
 */
@Composable
fun CircularPageProgressIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    strokeWidth: Dp = 2.8.dp,
    currentPageProgressColor: Color = Color.White, // Renamed for clarity
    remainingPagesProgressColor: Color = Color.Magenta, // New color for remaining pages (e.g., Pink)
    trackColor: Color = Color.White.copy(alpha = 0.45f),
    textColor: Color = Color.White,
    indicatorBackgroundColor: Color = Color.Black.copy(alpha = 0.4f),
    textStyle: TextStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
) {
    val currentPage = pagerState.currentPage
    val pageCount = pagerState.pageCount

    if (pageCount <= 1) return

    var showCurrentPageNumber by remember { mutableStateOf(true) }

    val textMeasurer = rememberTextMeasurer()

    val textToDraw = if (showCurrentPageNumber) {
        "${currentPage + 1}"
    } else {
        val remaining = pageCount - (currentPage + 1)
        "$remaining"
    }

    val textLayoutResult = remember(textToDraw, textStyle) {
        textMeasurer.measure(textToDraw, style = textStyle)
    }

    val interactionSource = remember { MutableInteractionSource() }

    Canvas(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { showCurrentPageNumber = !showCurrentPageNumber }
            )
    ) {
        val _strokeWidthPx = strokeWidth.toPx()
        val canvasCenter = this.center

        // 1. Draw the background circle
        drawCircle(
            color = indicatorBackgroundColor,
            radius = size.toPx() / 2f,
            center = canvasCenter
        )

        val arcDiameter = this.size.minDimension - _strokeWidthPx
        val arcTopLeft = Offset(
            canvasCenter.x - arcDiameter / 2f,
            canvasCenter.y - arcDiameter / 2f
        )
        val arcSize = Size(arcDiameter, arcDiameter)

        // 2. Draw the background track arc
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = _strokeWidthPx)
        )

        // 3. Draw the progress arc
        // Determine the color based on the display state
        val currentArcColor = if (showCurrentPageNumber) {
            currentPageProgressColor
        } else {
            remainingPagesProgressColor
        }

        // The progress sweep angle still represents the current page's progress,
        // or you might want it to represent remaining pages.
        // For this example, we'll keep it reflecting current page progress,
        // as changing the arc length based on "remaining" might be confusing
        // if the text shows "5 remaining" but the arc shows 1/5th progress.
        // If you want the arc to show remaining progress, you'd adjust `progressSweepAngle` too.
        val progressSweepAngle = 360f * (currentPage + 1).toFloat() / pageCount.toFloat()

        drawArc(
            color = currentArcColor, // Use the dynamically selected color
            startAngle = -90f,
            sweepAngle = progressSweepAngle,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = _strokeWidthPx, cap = StrokeCap.Round)
        )

        // 4. Draw the page number text
        val textX = canvasCenter.x - (textLayoutResult.size.width / 2f)
        val textY = canvasCenter.y - (textLayoutResult.size.height / 2f)
        drawText(
            textLayoutResult = textLayoutResult,
            color = textColor,
            topLeft = Offset(textX, textY)
        )
    }
}