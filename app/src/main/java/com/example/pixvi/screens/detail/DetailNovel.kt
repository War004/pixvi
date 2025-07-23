package com.example.pixvi.screens.detail

import android.app.Application
import com.example.pixvi.R
import android.net.Uri
import android.util.Log
import androidx.appcompat.widget.Toolbar
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.pixvi.network.api.PixivApiService
import com.example.pixvi.network.response.Detail.AuthorDetails
import com.example.pixvi.network.response.Detail.NovelData
import com.example.pixvi.utils.ContentBlock
import com.example.pixvi.utils.NovelParser
import com.example.pixvi.utils.PixivAsyncImage
import com.example.pixvi.viewModels.NovelDetailViewModel
import com.example.pixvi.viewModels.NovelDetailViewModelFactory
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import coil.request.ImageRequest
import com.example.pixvi.network.response.Detail.Rating
import com.example.pixvi.utils.AnimatedWebp
import com.example.pixvi.utils.ColorPickerBottomSheet
import com.example.pixvi.utils.NormalImageRequest
import com.example.pixvi.utils.PageLineSlider
import com.example.pixvi.utils.pageColorPalette
import com.example.pixvi.utils.textColorPalette
import com.example.pixvi.viewModels.NovelReaderSettings
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout


/**
 * The main entry point composable for the novel detail screen.
 * It initializes the ViewModel using a factory and handles the overall screen structure.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun DetailNovel(
    novelId: Int,
    navController: NavController,
    pixivApiService: PixivApiService
) {

    // 1. Get the application instance from the context
    val application = LocalContext.current.applicationContext as Application

    // 1. Create the factory with the required dependencies.
    val factory = NovelDetailViewModelFactory(application,pixivApiService, novelId)

    // 2. Use the factory to create the ViewModel instance.
    val viewModel: NovelDetailViewModel = viewModel(factory = factory)

    // 3. Collect the state from the ViewModel.
    val uiState by viewModel.uiState.collectAsState()

    //This is our previous composable for the novel
    DetailNovelScreen(
        viewModel = viewModel,
        uiState = uiState,
        onRetry = { viewModel.loadNovel(novelId) }
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun DetailNovelScreen(
    viewModel: NovelDetailViewModel,
    uiState: NovelDetailViewModel.NovelDetailUiState,
    onRetry: () -> Unit
) {
    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.errorMessage != null -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = uiState.errorMessage, modifier = Modifier.padding(16.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
        // When data is successfully loaded
        uiState.novelData != null && uiState.authorDetails != null -> {
            NovelContentScreen(
                uiState = uiState,
                viewModel = viewModel, // <-- PASS the ViewModel here
                novelId = uiState.novelData.id.toInt(),
                onSwipeLeft = { Log.d("NovelNavigation", "Swipe Left Detected") },
                onSwipeRight = { Log.d("NovelNavigation", "Swipe Right Detected") },
                onUpdatePageColor = { newColor -> viewModel.updatePageColor(newColor) },
                onUpdateTextColor = { newColor -> viewModel.updateTextColor(newColor) },
                onUpdateLineSpacing = { newLineHeight -> viewModel.updateLineSpacing(newLineHeight) }
            )
        }
    }
}

private enum class ColorPickerTarget { PAGE, TEXT }
private val baseLineHeight = 18.sp


// This composable renders the entire UI based on the design.
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NovelContentScreen(
    uiState: NovelDetailViewModel.NovelDetailUiState,
    viewModel: NovelDetailViewModel,
    novelId: Int,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onUpdatePageColor: (Color) -> Unit,
    onUpdateTextColor: (Color) -> Unit,
    onUpdateLineSpacing: (TextUnit) -> Unit
) {

    val novelData = uiState.novelData!! // We know this is not null if we are here
    val authorDetails = uiState.authorDetails!!
    val contentBlocks = uiState.contentBlocks
    val settings = uiState.settings

    val listState = rememberLazyListState()
    var isGradientMinimized by remember { mutableStateOf(false) }

    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf<ColorPickerTarget?>(null) }

    var showLineSpacingSlider by remember { mutableStateOf(false) }
    var sliderInteractionTimestamp by remember { mutableLongStateOf(0L) }

    var isEditingToolbarManuallyVisible by remember { mutableStateOf(false) }

    val isThisNovelPlaying = uiState.isPlaying && uiState.currentPlayingNovelId == novelId
    var toolbarState by remember { mutableStateOf(ToolbarState.INITIAL) }

    val isFabVisible = toolbarState == ToolbarState.INITIAL && !listState.isScrollingDown()

    val targetScreenTop = if (isGradientMinimized) 0.1f else 0.3f
    val screenTop by animateFloatAsState(
        targetValue = targetScreenTop,
        animationSpec = tween(durationMillis = 400),
        label = "gradientAnimation"
    )

    // Effect to minimize the gradient when the user scrolls past the header.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisibleIndex ->
                isGradientMinimized = firstVisibleIndex > 0
            }
    }

    // Effect to hide the slider after 3 seconds of inactivity.
    // The timestamp is updated whenever the user interacts with the slider.
    LaunchedEffect(showLineSpacingSlider, sliderInteractionTimestamp) {
        if (showLineSpacingSlider) {
            delay(3000L) // 3 seconds delay
            showLineSpacingSlider = false
        }
    }

    // Combined auto-scroll effect for TTS playback
    LaunchedEffect(uiState.currentPlayingMediaId, uiState.isPlaying, listState) {
        if (!uiState.isPlaying || uiState.currentPlayingMediaId == null) return@LaunchedEffect

        val currentMediaId = uiState.currentPlayingMediaId!!
        val actualIndex = currentMediaId + 1 // +1 to account for header item

        // Scenario 2: When player switches to next paragraph, scroll immediately
        launch {
            delay(500L) // Small delay to ensure smooth transition

            // Check if still playing before scrolling
            if (!uiState.isPlaying) return@launch

            val isVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == actualIndex }

            if (!isVisible && !listState.isScrollInProgress) {
                try {
                    listState.animateScrollToItem(actualIndex)
                    Log.d("AutoScroll", "Scrolled to new paragraph at index: $actualIndex")
                } catch (e: Exception) {
                    Log.e("AutoScroll", "Failed to scroll to new paragraph: ${e.message}")
                }
            }
        }

        // Scenario 1: Monitor for user scrolling and auto-scroll back
        launch {
            snapshotFlow { listState.isScrollInProgress }
                .distinctUntilChanged()
                .collect { isScrolling ->
                    // Only proceed if still playing
                    if (!isScrolling && uiState.isPlaying) {
                        val currentId = uiState.currentPlayingMediaId ?: return@collect
                        val targetIndex = currentId + 1

                        // Check if current playing item is visible
                        val isCurrentItemVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }

                        if (!isCurrentItemVisible) {
                            // User has scrolled away, start 2-second timer
                            launch {
                                delay(2000L)

                                // Re-check conditions after delay - must still be playing
                                if (uiState.isPlaying &&
                                    uiState.currentPlayingMediaId == currentId &&
                                    !listState.isScrollInProgress) {

                                    val stillNotVisible = !listState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }

                                    if (stillNotVisible) {
                                        try {
                                            listState.animateScrollToItem(targetIndex)
                                            Log.d("AutoScroll", "Auto-scrolled back to playing item at index: $targetIndex")
                                        } catch (e: Exception) {
                                            Log.e("AutoScroll", "Failed to auto-scroll back: ${e.message}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    val swipeThreshold = size.width / 4
                    if (dragAmount > swipeThreshold) {
                        onSwipeRight()
                    } else if (dragAmount < -swipeThreshold) {
                        onSwipeLeft()
                    }
                }
            }
    ) {

        //For seasonal effect, the function, AnimatedWebp, implementation in TODO
        /*AnimatedWebp(
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            effectUrl = "https://source.pixiv.net/special/seasonal-effect-tag/pixiv-glow-effect/effect.png"
        )*/
        PixivAsyncImage(
            imageUrl = novelData.coverUrl,
            contentDescription = "Cover Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to settings.pageColor.copy(alpha = 0.6f),
                            screenTop to settings.pageColor.copy(alpha = 1.0f)
                        )
                    )
                )
        )

        // Main content list
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 120.dp), // Padding to not hide content behind toolbar
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(showLineSpacingSlider) {
                    if (showLineSpacingSlider) {
                        // When the slider is visible, await the first pointer event
                        // on the LazyColumn (like a touch down or scroll start).
                        awaitPointerEventScope {
                            awaitPointerEvent()
                            // As soon as any interaction is detected on the content,
                            // dismiss the slider.
                            showLineSpacingSlider = false
                        }
                    }
                }
        ) {
            // Header Item (Title, Author, Tags, Info)
            item {
                NovelHeader(novelData, authorDetails, settings = settings)
            }

            // Novel Body
            //Maybe add a highlighter here
            items(contentBlocks) { block ->
                NovelBlock(block = block, settings = uiState.settings)
            }
        }

        // This Box is the main container for ALL floating controls, allowing independent alignment.
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. The FAB remains aligned to the bottom end.
            AnimatedVisibility(
                visible = isFabVisible,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 12.dp),
                enter = slideInHorizontally(initialOffsetX = { it + 200 }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it + 200 }) + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {  toolbarState = ToolbarState.EDITING  }
                ) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Edit button")
                }
            }

            // 2. The Toolbar is now aligned to the bottom center, independent of the slider.
            AnimatedVisibility(
                visible = (toolbarState == ToolbarState.EDITING || toolbarState == ToolbarState.PLAYING),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                enter = slideInHorizontally(initialOffsetX = { it + 200 }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it + 200 }) + fadeOut()
            ) {
                CombinedToolbar(
                    currentState = toolbarState,
                    viewModel = viewModel,
                    uiState = uiState,
                    onUndo = {
                        if (toolbarState == ToolbarState.PLAYING) {
                            viewModel.stopPlayback()
                            // When stopping playback, go back to the editing toolbar
                            toolbarState = ToolbarState.EDITING
                        } else { // Must be in EDITING state
                            // When undoing from edit, hide the toolbar
                            toolbarState = ToolbarState.INITIAL
                        }
                    },
                    onPlay = {
                        //I don't think it was happening because of the reason below, so you can just ignore the comemnt below
                        // IMPORTANT: The order of these two lines is critical for a smooth UI.
                        // Don't put the call for startPlayback() at the start, as it would cause UI jank.
                        //
                        // Here's why:
                        // - `toolbarState = ...` is a synchronous update to local Composable state. It guarantees
                        //   the UI changes *immediately* on the next frame after the click.
                        // - `viewModel.startPlayback()` updates a StateFlow, which is an asynchronous operation.
                        //   The new `uiState` (with `isPlaying=true`) arrives slightly later.
                        //
                        // If the order were reversed, the toolbar would recompose with the new `PLAYING`
                        // state but the *old* `uiState`, causing the wrong icon to flash briefly.

                        toolbarState = ToolbarState.PLAYING

                        try{
                            viewModel.startPlayback()
                        }catch (e: Exception) {
                            Log.e("onPlayCombinedToolBar.EDITING","${e.message}")
                        }
                    },
                    onPageColorClick = {
                        colorPickerTarget = ColorPickerTarget.PAGE
                        showColorPicker = true
                    },
                    onTextColorClick = {
                        colorPickerTarget = ColorPickerTarget.TEXT
                        showColorPicker = true
                    },
                    onLineSpacingClick = {
                        showLineSpacingSlider = !showLineSpacingSlider
                        sliderInteractionTimestamp = System.currentTimeMillis()
                    },
                    onResetLineSpacing = { onUpdateLineSpacing(28.sp) },

                    modifier = Modifier.animateContentSize(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        )
                    )
                )
            }

            // 3. The Slider is ALSO aligned independently to the bottom end,
            //    but with enough padding to place it ABOVE the toolbar.
            AnimatedVisibility(
                visible = showLineSpacingSlider,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    // This padding is the key: it lifts the slider above the toolbar area.
                    .padding(end = 24.dp, bottom = 120.dp),
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                BoxWithConstraints {
                    val sliderHeight = this.maxHeight * 0.4f
                    PageLineSlider(
                        sliderHeight = sliderHeight,
                        value = uiState.settings.lineSpacing.value / baseLineHeight.value,
                        onValueChange = { newMultiplier ->
                            onUpdateLineSpacing(baseLineHeight * newMultiplier)
                            // Reset the timer on interaction
                            sliderInteractionTimestamp = System.currentTimeMillis()
                        },
                        onValueChangeFinished = {
                            // Reset the timer when interaction finishes
                            sliderInteractionTimestamp = System.currentTimeMillis()
                        },
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // Consume clicks to prevent them from passing to the content
                        )
                    )
                }
            }
        }
        if (showColorPicker && colorPickerTarget != null) {
            val target = colorPickerTarget!!
            ColorPickerBottomSheet(
                title = if (target == ColorPickerTarget.PAGE) "Page Color" else "Text Color",
                colors = if (target == ColorPickerTarget.PAGE) pageColorPalette else textColorPalette,
                selectedColor = if (target == ColorPickerTarget.PAGE) settings.pageColor else settings.textColor,
                onColorSelected = { newColor ->
                    if (target == ColorPickerTarget.PAGE) {
                        onUpdatePageColor(newColor)
                    } else {
                        onUpdateTextColor(newColor)
                    }
                },
                onDismissRequest = { showColorPicker = false }
            )
        }
    }
}

/**
 * Displays the header section of the novel screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NovelHeader(
    novelData: NovelData,
    authorDetails: AuthorDetails,
    settings: NovelReaderSettings
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // This spacer pushes the content down, making the cover image visible at the top.
        Spacer(modifier = Modifier.height(50.dp))
        //Maybe we don't need the spacer

        // Title
        Text(
            text = novelData.title,
            style = MaterialTheme.typography.headlineLarge,
            color = settings.textColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Author
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixivAsyncImage(
                imageUrl = authorDetails.profileImage.url,
                contentDescription = "Author: ${authorDetails.userName}",
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = authorDetails.userName, style = MaterialTheme.typography.titleMedium, color = settings.textColor)
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Tags
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            novelData.tags.forEach { tag ->
                Box(
                    modifier = Modifier
                        .border(1.dp, Color.Gray, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = tag, color = settings.textColor.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(
            Modifier,
            DividerDefaults.Thickness,
            color = settings.textColor.copy(alpha = 0.3f)
        )

        // Info Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoItem(icon = Icons.Default.CalendarToday, text = novelData.creationDate.substringBefore("T"),settings = settings)
            InfoItem(icon = Icons.Default.Visibility, text = novelData.rating.view.toString(),settings = settings)
            InfoItem(icon = Icons.Default.Favorite, text = novelData.rating.like.toString(),settings = settings)
            InfoItem(icon = Icons.Default.Bookmark, text = novelData.rating.bookmark.toString(),settings = settings)
        }
        HorizontalDivider(
            Modifier,
            DividerDefaults.Thickness,
            color = settings.textColor.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * A small helper composable for items in the info bar.
 */
@Composable
private fun InfoItem(icon: ImageVector, text: String, settings: NovelReaderSettings) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val secondaryColor = settings.textColor.copy(alpha = 0.7f)
        Icon(imageVector = icon, contentDescription = null, tint = secondaryColor, modifier = Modifier.size(16.dp))
        Text(text = text, color = secondaryColor, fontSize = 12.sp)
    }
}

/**
 * Renders a single block of content from the parsed novel body.
 */
@Composable
private fun NovelBlock(
    block: ContentBlock,
    settings: NovelReaderSettings
) {
    val blockModifier = Modifier.padding(horizontal = 16.dp)
    when (block) {
        is ContentBlock.Text -> Text(
            text = block.annotatedString,
            color = settings.textColor,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = settings.lineSpacing,
            modifier = blockModifier.padding(bottom = 12.dp)
        )
        is ContentBlock.Chapter -> Text(
            text = block.title,
            color = settings.textColor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = blockModifier.padding(vertical = 24.dp)
        )
        is ContentBlock.Image -> {
            PixivAsyncImage(
                imageUrl = block.imageUrl, //using the medium quality
                contentDescription = "Novel Image",
                contentScale = ContentScale.FillWidth,
                modifier = blockModifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
        is ContentBlock.PageBreak -> {
            HorizontalDivider(
                modifier = blockModifier.padding(vertical = 32.dp),
                thickness = 1.dp,
                color = Color.Gray
            )
        }
    }
}

enum class ScrollDirection {
    UP, DOWN
}

//

// Enum to manage the current state of the UI
private enum class ToolbarState {
    INITIAL,
    EDITING,
    PLAYING
}

/**
 * A helper extension function on LazyListState to determine if the user is scrolling down.
 * This is used to hide the initial FloatingActionButton.
 */
@Composable
private fun LazyListState.isScrollingDown(): Boolean {
    var previousIndex by remember(this) { mutableStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                firstVisibleItemIndex > previousIndex
            } else {
                firstVisibleItemScrollOffset > previousScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}


@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CombinedToolbar(
    currentState: ToolbarState,
    viewModel: NovelDetailViewModel,
    uiState: NovelDetailViewModel.NovelDetailUiState,
    onUndo: () -> Unit,
    onPlay: () -> Unit,
    onPageColorClick: () -> Unit,
    onTextColorClick: () -> Unit,
    onLineSpacingClick: () -> Unit,
    onResetLineSpacing: () -> Unit,
    modifier: Modifier = Modifier // Added modifier parameter
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier // Apply the passed modifier
    ) {
        when (currentState) {
            ToolbarState.EDITING -> EditingContent(
                onUndo = onUndo,
                onPlay = onPlay,
                onPageColorClick = onPageColorClick,
                onTextColorClick = onTextColorClick,
                onLineSpacingClick = onLineSpacingClick,
                onResetLineSpacing = onResetLineSpacing
            )
            ToolbarState.PLAYING -> PlayerContent(
                viewModel = viewModel,
                uiState = uiState,
                onUndo = onUndo
            )
            else -> Box {} // Should not happen in this context
        }
    }
}

@Composable
private fun EditingContent(
    onUndo: () -> Unit,
    onPlay: () -> Unit,
    onPageColorClick: () -> Unit,
    onTextColorClick: () -> Unit,
    onLineSpacingClick: () -> Unit,
    onResetLineSpacing: () -> Unit
) {
    val toolbarIcons = listOf(
        Icons.Default.FormatColorText,
        Icons.Default.FormatPaint,
        Icons.Default.PlayArrow,
        Icons.Default.FormatLineSpacing,
        Icons.AutoMirrored.Filled.Undo,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        toolbarIcons.forEach { item ->
            val contentDescription = item.name.replaceFirstChar { it.uppercase() }

            when (item) {
                Icons.AutoMirrored.Filled.Undo -> IconButton(onClick = onUndo) {
                    Icon(imageVector = item, contentDescription = contentDescription)
                }
                Icons.Default.PlayArrow -> Button(onClick = onPlay, contentPadding = PaddingValues(0.dp)) {
                    Icon(imageVector = item, contentDescription = contentDescription)
                }
                Icons.Default.FormatPaint -> IconButton(onClick = onPageColorClick) {
                    Icon(imageVector = item, contentDescription = contentDescription)
                }
                Icons.Default.FormatColorText -> IconButton(onClick = onTextColorClick) {
                    Icon(imageVector = item, contentDescription = contentDescription)
                }
                Icons.Default.FormatLineSpacing -> {
                    // This is the IconButton for the line spacing
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(CircleShape) // Ensures ripple effect is circular
                            .combinedClickable(
                                onClick = onLineSpacingClick,
                                // Pass a lambda for the long click action.
                                // It can also handle onDoubleClick if needed.
                                onLongClick = onResetLineSpacing,
                            )
                            .padding(8.dp) // Replicate IconButton's default padding for a good touch target
                    ) {
                        Icon(imageVector = item, contentDescription = contentDescription)
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerContent(
    viewModel: NovelDetailViewModel,
    uiState: NovelDetailViewModel.NovelDetailUiState,
    onUndo: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            //.padding(horizontal = 16.dp, vertical = 8.dp),
            .wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.wrapContentSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* TODO */ }) {
                Icon(imageVector = Icons.Default.Translate, contentDescription = "Translate")
            }
            IconButton(onClick = { /* TODO */ }) {
                Icon(imageVector = Icons.Default.Speed, contentDescription = "Playback Speed")
            }
            Button(
                onClick = { viewModel.togglePlayPause() }, // Use the new toggle function
                contentPadding = PaddingValues(0.dp),
                enabled = uiState.isPlayerReady // Disable the button until the controller is connected
            ) {
                // The icon changes based on the isPlaying state from the ViewModel
                val icon = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                Icon(imageVector = icon, contentDescription = if (uiState.isPlaying) "Pause" else "Play")
            }
            IconButton(onClick = { /* TODO */ }) {
                Icon(imageVector = Icons.Default.Equalizer, contentDescription = "Equalizer")
            }
            IconButton(onClick = onUndo) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
            }
        }

        LinearWavyProgressIndicator(
            progress = { 0.3f }, // Dummy progress
            modifier = Modifier.padding()
        )
    }
}