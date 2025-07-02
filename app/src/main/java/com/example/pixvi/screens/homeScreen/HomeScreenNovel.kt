package com.example.pixvi.screens.homeScreen

import android.content.ClipData
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.distinctUntilChanged
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.filterNotNull
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.pixvi.network.BookmarkRestrict
import com.example.pixvi.network.response.Home.Novels.Novel
import com.example.pixvi.network.response.Home.Novels.NovelForDisplay
import com.example.pixvi.network.response.Home.Novels.RankingNovel
import com.example.pixvi.viewModels.HomeNovelViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import okhttp3.Headers
import androidx.compose.ui.graphics.painter.Painter


private const val NO_PROFILE_IMAGE_URL = "https://s.pximg.net/common/images/no_profile.png"


@OptIn(FlowPreview::class)
@Composable
fun NovelHomeScreen(
    navController: NavController,
    homeINovelViewModel: HomeNovelViewModel
) {
    val uiState by homeINovelViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val scope = rememberCoroutineScope()

    val favoriteRequestFlow = remember {
        MutableSharedFlow<Pair<Novel, BookmarkRestrict>>(extraBufferCapacity = 1)
    }

    LaunchedEffect(Unit) {
        favoriteRequestFlow
            .debounce(400L) // Debounce for 400 milliseconds.
            .collect { (novel, visibility) ->
                // This code runs only after 400ms of inactivity
                homeINovelViewModel.toggleBookmark(
                    illustId = novel.id.toLong(),
                    isCurrentlyBookmarked = novel.is_bookmarked,
                    visibility = visibility
                )
            }
    }


    LaunchedEffect(Unit) {
        homeINovelViewModel.loadInitialRecommendations()
    }

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
                        homeINovelViewModel.loadMoreRecommendations()
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
                        uiState.errorMessage ?: "An error occurred",
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { homeINovelViewModel.loadInitialRecommendations() }) {
                        Text("Retry")
                    }
                }
            }
            else -> {

                //from here we will define our ui
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {

                    if (uiState.rankingNovel.isNotEmpty()) {
                        item("ranking-novels") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Header Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp), // Aligns with LazyColumn's padding
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = "Rankings",
                                        tint = Color(0xFFFFC107) // Gold color for the star
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Rankings",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(onClick = { /* TODO: Navigate to full rankings page */ }) {
                                        Text("See more")
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Horizontally scrolling list of novels
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = uiState.rankingNovel,
                                        key = { novel -> "ranking_${novel.id}" }
                                    ) { novel ->
                                        // Wrap SimpleNovelCard in a Box with a fixed width, as the card itself uses fillMaxWidth()
                                        Box(modifier = Modifier.width(300.dp)) {
                                            SimpleNovelCard(novel = novel)
                                        }
                                    }
                                }
                            } }
                    }

                    itemsIndexed(
                        items = uiState.recommendations,
                        key = { _, novel -> novel.id }
                    ) { index, novel ->
                        Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                            // Pass the NovelForDisplay object to the optimized CardNovel
                            CardNovel(novel = novel)
                        }
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
                }
            }
        }
    }
}


//Indivual elements
@Composable
fun CardNovel(novel: NovelForDisplay) {
    var showDetails by remember { mutableStateOf(false) }
    val clipboard: androidx.compose.ui.platform.Clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val plainTextContent = remember(novel.caption) {
        HtmlCompat.fromHtml(novel.caption, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    }

    val imagePainter = rememberPixivImagePainter(novel.image_urls.medium)

    val scrollState1 = rememberScrollState()

    Card(
        modifier = Modifier
            .height(150.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = {/*Opens the novel*/},
                onLongClick = {
                    /*Opens a bottom sheet providing additional info */
                }
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Box 1: Having the cover page
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.25f)
            ) {
                Image(
                    painter = imagePainter,
                    contentDescription = "Cover for ${novel.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // Container for the switchable content with a background
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.75f)
            ) {

                Image(
                    painter = imagePainter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.6f), blendMode = BlendMode.SrcOver) //replacing the box
                )


                if (!showDetails) {
                    // STATE 1: Default view with main info
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween // This pushes content to top and bottom
                    ) {
                        // Top section: Title, Author, and Stats are grouped together
                        Column {
                            Text(
                                text = novel.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = Color.White,
                                modifier = Modifier
                                    .combinedClickable(
                                    onClick = {/*Nothing*/},
                                    onLongClick = {
                                        scope.launch{
                                            val clipData: ClipData =
                                                ClipData.newPlainText("Novel_title", novel.title)
                                            clipboard.setClipEntry(ClipEntry(clipData))
                                            Toast.makeText(
                                                context,
                                                "Novel title copied to clipboard",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                                    .horizontalScroll(scrollState1)
                            )
                            Text(
                                text = "by ${novel.user.name}",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier.combinedClickable(
                                    onClick = {/*Open the author page*/},
                                    onLongClick = {
                                        scope.launch{
                                            val clipData: ClipData =
                                                ClipData.newPlainText("Novel_author", novel.user.name)
                                            clipboard.setClipEntry(ClipEntry(clipData))
                                            Toast.makeText(
                                                context,
                                                "Novel author copied to clipboard",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            // Stats Row - This layout is preserved exactly as in your original code
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Views
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Filled.RemoveRedEye, "Views", modifier = Modifier.size(17.dp), tint = Color.White)
                                    Text(text = novel.total_view.toString(), color = Color.White, fontSize = 12.sp)
                                }
                                // Bookmarks
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Filled.Favorite, "Bookmarks", modifier = Modifier.size(17.dp), tint = Color.White)
                                    Text(text = novel.total_bookmarks.toString(), color = Color.White, fontSize = 12.sp)
                                }
                                // Comments
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Comment, "Comments", modifier = Modifier.size(17.dp), tint = Color.White)
                                    Text(text = novel.total_comments.toString(), color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 48.dp), // Padding to avoid the icon
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = novel.tags,
                                key = { tag -> tag.name } // Provide a stable key
                            ) { tag ->
                                val displayName = tag.translated_name.takeIf { !it.isNullOrBlank() } ?: tag.name
                                SuggestionChip(
                                    onClick = { /* TODO: Handle tag click */ },
                                    label = { Text(text = displayName, fontSize = 12.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = Color.White.copy(alpha = 0.25f),
                                        labelColor = Color.White
                                    ),
                                    border = null,
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }
                } else {
                    // STATE 2: Flipped view with description and secondary info
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        // The caption/description
                        Text(
                            modifier = Modifier
                                .weight(1f) // Takes up most of the space
                                .verticalScroll(rememberScrollState())
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        scope.launch{
                                            val clipData: ClipData =
                                                ClipData.newHtmlText("Post_caption", plainTextContent,novel.caption)
                                            clipboard.setClipEntry(ClipEntry(clipData))
                                            Toast.makeText(
                                                context,
                                                "Caption copied to clipboard",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            text = novel.parsedCaption
                        )
                        // Bottom row: Page/Word count
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                // KEY CHANGE: Add padding to the end to avoid the icon
                                .padding(end = 48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            //Handle edge case for large number of pages and words
                            Text(text = "Pages: ${novel.page_count}", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "Words: ${novel.text_length}", style = MaterialTheme.typography.labelMedium, color = Color.White,maxLines = 1)

                        }
                    }
                }

                // KEY CHANGE: The IconButton is now placed in the parent Box, aligned to the corner.
                // It's outside the if/else block, so its position is always the same.
                IconButton(
                    onClick = { showDetails = !showDetails },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(
                        imageVector = if (showDetails) Icons.Filled.Info else Icons.Outlined.Info,
                        contentDescription = "Toggle Details",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleNovelCard(novel: RankingNovel) {

    val scrollStateSimple = rememberScrollState()
    Card(
        modifier = Modifier
            .height(180.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 1. Background Image
            Image(
                painter = rememberPixivImagePainter(novel.image_urls.medium),
                contentDescription = "Background for ${novel.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.5f), blendMode = BlendMode.SrcOver)
            )

            // 2. No Black box
            // 3. Content (Title and Author)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), // Padding ensures it's not stuck to the absolute corner
                verticalArrangement = Arrangement.Top // Aligned to the top-left
            ) {
                Text(
                    text = novel.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .horizontalScroll(scrollStateSimple)
                )
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    text = "by ${novel.user.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/**
 * A reusable composable that creates and remembers a Painter to load network images
 * with Pixiv-specific headers and caching.
 *
 * @param url The URL of the image to load. Can be null.
 * @return A [Painter] that can be used in an `Image` composable or a `paint` modifier.
 */
@Composable
fun rememberPixivImagePainter(
    url: String?
): Painter {
    val context = LocalContext.current

    // This logic is taken directly from PixivImageRow
    // It's good practice to define these as constants elsewhere in your app
    val appVersion = "6.143.0"
    val userAgent = "PixivAndroidApp/$appVersion (Android ${Build.VERSION.RELEASE}; ${Build.MODEL})"
    val commonHeaders = remember {
        Headers.Builder()
            .add("Referer", "https://app-api.pixiv.net/")
            .add("User-Agent", userAgent)
            .build()
    }

    // We only want to rebuild the request if the URL changes
    val imageRequest = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .headers(commonHeaders)
            .crossfade(true) // For a smooth fade-in animation
            .diskCachePolicy(CachePolicy.ENABLED) // Use Coil's disk cache
            .memoryCachePolicy(CachePolicy.ENABLED) // Use Coil's memory cache
            .build()
    }

    return rememberAsyncImagePainter(model = imageRequest)
}