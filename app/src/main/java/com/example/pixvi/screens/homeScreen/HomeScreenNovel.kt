package com.example.pixvi.screens.homeScreen

import android.content.ClipData
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.style.TextOverflow
import com.example.pixvi.network.BookmarkRestrict
import com.example.pixvi.network.response.Home.Novels.Novel
import com.example.pixvi.network.response.Home.Novels.NovelForDisplay
import com.example.pixvi.network.response.Home.Novels.RankingNovel
import com.example.pixvi.viewModels.HomeNovelViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.withStyle
import androidx.core.text.HtmlCompat
import coil3.compose.rememberAsyncImagePainter
import com.example.pixvi.NovelDetailScreen
import com.example.pixvi.utils.PixivAsyncImage
import coil3.request.transformations
import coil3.size.Precision
import com.commit451.coiltransformations.BlurTransformation
import com.example.pixvi.utils.NormalImageRequest

@OptIn(FlowPreview::class)
@Composable
fun NovelHomeScreen(
    modifier: Modifier,
    navController: NavController,
    homeINovelViewModel: HomeNovelViewModel
) {
    val uiState by homeINovelViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
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
                            CardNovel(
                                novel = novel,
                                onClick = { navController.navigate(NovelDetailScreen(novelId = novel.id)) },
                                onLongClick = {/*nothing*/},
                                onNovelView = {homeINovelViewModel.onNovelViewed(novel.id)}
                            )
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
fun CardNovel(novel: NovelForDisplay, onClick: () -> Unit, onLongClick: () -> Unit, onNovelView: ()-> Unit) {
    var showDetails by remember { mutableStateOf(false) }
    val clipboard: androidx.compose.ui.platform.Clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val scrollState1 = rememberScrollState()

    val coverImageRequest = remember(novel.image_urls.medium) {
        NormalImageRequest.normalImageRequest(
            context = context,
            imageUrl = novel.image_urls.medium
        )
    }
    val coverPainter = rememberAsyncImagePainter(model = coverImageRequest)

    // --- REQUEST 2: The Optimized Background Image ---
    // Use your helper again, but this time, provide the configuration block.
    val backgroundImageRequest = remember(novel.image_urls.medium) {
        NormalImageRequest.normalImageRequest(
            context = context,
            imageUrl = novel.image_urls.medium
        ) { builder ->
            // Inside this block, we add our specific optimizations.
            // The builder already has your common headers, crossfade, etc.
            builder
                .size(width = 200, height = 300)
                .precision(Precision.INEXACT)
                .transformations(
                    BlurTransformation(
                        context = context,
                        radius = 25f,
                        sampling = 2f
                    )
                )
        } // The block ends here
    }
    val backgroundPainter = rememberAsyncImagePainter(model = backgroundImageRequest)


    Card(
        modifier = Modifier
            .height(150.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    onNovelView()
                    onClick()
                          },
                onLongClick = {
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ){
            Image(
                painter = coverPainter,
                contentDescription = "Cover for ${novel.title}",
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )

            //Right-side-box
            Box(
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxHeight()
            ) {
                Image(
                    painter = backgroundPainter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.tint(
                        color = Color.Black.copy(alpha = 0.5f),
                        blendMode = BlendMode.SrcOver
                    )
                )

                //Layer 2- for text info
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!showDetails) {
                        // STATE 1: Default view with main info
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            // Top section: Title, Author, and Stats are grouped together
                            Column {
                                Text(
                                    text = novel.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
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
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    // Views
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Filled.RemoveRedEye, "Views", modifier = Modifier.size(17.dp), tint = Color.White)
                                        Text(text = novel.total_view.toString(), color = Color.White, fontSize = 12.sp)
                                    }
                                    // Bookmarks
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Filled.Favorite, "Bookmarks", modifier = Modifier.size(17.dp), tint = Color.White)
                                        Text(text = novel.total_bookmarks.toString(), color = Color.White, fontSize = 12.sp)
                                    }
                                    // Comments
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Comment, "Comments", modifier = Modifier.size(17.dp), tint = Color.White)
                                        Text(text = novel.total_comments.toString(), color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            if (novel.tags.isNotEmpty()) {
                                // 1. Remember the scroll state for this specific text field
                                val tagScrollState = rememberScrollState()

                                val annotatedTagsString = remember(novel.tags) {
                                    // ... (The buildAnnotatedString block is identical to the previous answer)
                                    buildAnnotatedString {
                                        novel.tags.forEachIndexed { index, tag ->
                                            pushStringAnnotation(tag = "tag_click", annotation = tag.name)
                                            withStyle(style = SpanStyle(color = Color(0xFF81D4FA))) {
                                                append(tag.name)
                                            }
                                            pop()
                                            if (index < novel.tags.size - 1) {
                                                append("  •  ")
                                            }
                                        }
                                    }
                                }

                                ClickableText(
                                    text = annotatedTagsString,
                                    style = TextStyle(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    ),
                                    // 2. Critical: Tell the Text composable NOT to wrap lines
                                    softWrap = false,
                                    // 3. Overflow should be Clip, not Ellipsis, for scrolling
                                    overflow = TextOverflow.Clip,
                                    // The maxLines is still a good safeguard
                                    maxLines = 1,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 48.dp)
                                        // 4. Apply the horizontalScroll modifier
                                        .horizontalScroll(tagScrollState),
                                    onClick = { offset ->
                                        annotatedTagsString.getStringAnnotations(tag = "tag_click", start = offset, end = offset)
                                            .firstOrNull()?.let { annotation ->
                                                val clickedTagName = annotation.item
                                                Toast.makeText(context, "Clicked Tag: $clickedTagName", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                )
                            }
                        }
                    } else {

                        val parsedCaption = remember(novel.caption) {
                            AnnotatedString.fromHtml(novel.caption)
                        }
                        val plainTextCaption = remember(novel.caption) {
                            HtmlCompat.fromHtml(novel.caption, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                        }

                        // STATE 2: Flipped view with description and secondary info
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val captionScrollState = rememberScrollState()
                            // The caption/description
                            Text(
                                modifier = Modifier
                                    .weight(1f) // Takes up most of the space
                                    .verticalScroll(captionScrollState)
                                    .pointerInput(Unit){
                                        detectVerticalDragGestures{ change, dragAmount ->
                                            // Consume the pointer input event to stop it from propagating
                                            change.consume()
                                            // Manually scroll the text's state in a coroutine
                                            scope.launch {
                                                captionScrollState.scrollBy(-dragAmount)
                                            }
                                        }
                                    }
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = {
                                            scope.launch{
                                                val clipData: ClipData =
                                                    ClipData.newHtmlText("Post_caption", plainTextCaption ,novel.caption)
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
                                text = parsedCaption
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
                        onClick = {
                            onNovelView()
                            showDetails = !showDetails },
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
}

@Composable
fun LiteTagChip(
    text: String,
    modifier: Modifier = Modifier
) {
    // A Box is a very cheap layout composable.
    Box(
        // The modifier chain is applied once here.
        modifier = modifier
            .height(24.dp) // Set a fixed height
            .background(
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp) // Use a shape for the background
            )
            .padding(horizontal = 8.dp), // Padding for the text
        contentAlignment = Alignment.Center
    ) {
        // Just a simple Text composable inside.
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1 // Ensure it doesn't wrap
        )
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
            PixivAsyncImage(
                imageUrl = novel.image_urls.medium,
                contentDescription = "Background for ${novel.title}",
                modifier = Modifier.fillMaxSize(),
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
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
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