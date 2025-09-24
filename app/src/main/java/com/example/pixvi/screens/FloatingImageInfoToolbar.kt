package com.example.pixvi.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pixvi.network.response.Home.Illust
import kotlinx.coroutines.launch
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource

//Common property from the illust.illust and manga.illust
private data class FloatingToolbarInfo(
    val title: String,
    val userName: String,
    val create_date: String,
    val total_view: Int,
    val total_bookmarks: Int,
    val is_bookmarked: Boolean
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingImageInfoToolbar(
    illust: Illust,
    modifier: Modifier = Modifier,
    onFavoriteClicked: () -> Unit,
    onLongFavorite: () -> Unit
) {

    val info = remember(illust) {
        FloatingToolbarInfo(
            title = illust.title,
            userName = illust.user.name,
            create_date = illust.create_date,
            total_view = illust.total_view,
            total_bookmarks = illust.total_bookmarks,
            is_bookmarked = illust.is_bookmarked
        )
    }

    var showInfo by remember { mutableStateOf(false) }
    val clipboard: Clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        expandedShadowElevation = 8.dp,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        contentPadding = PaddingValues(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
    ) {
        if (showInfo) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Views
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.RemoveRedEye,
                        contentDescription = "Views",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${info.total_view}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Likes
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = "Likes",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${info.total_bookmarks}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(Modifier.width(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = "Date",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = info.create_date.take(10),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = info.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = {
                            scope.launch {
                                val clipData: ClipData =
                                    ClipData.newPlainText("Post_title", info.title)
                                clipboard.setClipEntry(ClipEntry(clipData))
                                Toast.makeText(
                                    context,
                                    "Title copied to clipboard",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                )
                Text(
                    text = info.userName, // Use the 'info' object
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.combinedClickable(
                        onClick = {}, // Does nothing on simple click
                        onLongClick = {
                            scope.launch {
                                val clipData: ClipData =
                                    ClipData.newPlainText("Post_author_name", info.userName) // Use the 'info' object
                                clipboard.setClipEntry(ClipEntry(clipData))
                                Toast.makeText(
                                    context,
                                    "Author copied to clipboard",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp) // Increased touch target size for better accessibility.
                .align(Alignment.CenterVertically)
                .clip(CircleShape) // Ensures the ripple effect is circular.
                .clickable { showInfo = !showInfo }
        ) {
            Icon(
                imageVector = if (showInfo) Icons.Filled.Info else Icons.Outlined.Info,
                contentDescription = "Info Icon",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp) // The icon's visual size remains the same.
            )
        }

        // Add a small spacer for a bit more visual separation between the buttons.
        Spacer(modifier = Modifier.width(4.dp))

        // Favorite Icon with a larger, circular touch target.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp) // Increased touch target size.
                .align(Alignment.CenterVertically)
                .clip(CircleShape) // Ensures the ripple effect is circular.
                .combinedClickable(
                    onClick = { onFavoriteClicked() },
                    onLongClick = { onLongFavorite() }
                )
        ) {
            Icon(
                imageVector = if (info.is_bookmarked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorite",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp) // The icon's visual size remains the same.
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true)
@Composable
fun Preview() {
    // 1. The new list of icons for the toolbar
    val toolbarIcons = listOf(
        Icons.Default.FormatColorText,
        Icons.Default.FormatPaint,
        Icons.Default.PlayArrow,
        Icons.Default.FormatLineSpacing,
        Icons.AutoMirrored.Filled.Undo,
    )

    val purpleColor = Color(0xFFE0D6FF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF131316))
            .padding(16.dp)
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            toolbarIcons.forEach { item->
                if(item != Icons.Default.PlayArrow){
                    IconButton(
                        onClick = {},
                    ){
                        Icon(
                            imageVector = item,
                            contentDescription = "Play button",
                        )
                    }
                }
                else{
                    Button(
                        onClick = {},
                        contentPadding = PaddingValues(0.dp),
                        //colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Icon(
                            imageVector = item,
                            contentDescription = "Play",
                            //tint = Color.Red
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun Previewtwo(){
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF131316))
            .padding(16.dp)
    ) {
        FloatingActionButton(
            onClick = {},
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.dp)
        ){
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Edit button"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun MusicPlayer(){
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF131316))
            .padding(16.dp)
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                //.fillMaxWidth()
        ) {
            Column(

                modifier = Modifier
                    .wrapContentSize()
                    .padding(16.dp)
            ){
                Row(
                    modifier = Modifier.wrapContentSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* TODO: Handle Translate */ }) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Translate"
                        )
                    }
                    IconButton(onClick = { /* TODO: Handle Speed */ }) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Playback Speed"
                        )
                    }
                    Button(
                        onClick = { /* TODO: Handle Play */ },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play"
                        )
                    }
                    IconButton(onClick = { /* TODO: Handle Equalizer */ }) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = "Equalizer"
                        )
                    }
                    IconButton(onClick = { /* TODO: Handle Undo */ }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo"
                        )
                    }
                }

                LinearWavyProgressIndicator()
            }
        }
    }
}

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


@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalAnimationApi::class
)
@Preview(showBackground = true, name = "Interactive Toolbar Demo")
@Composable
fun InteractiveFloatingToolbar() {
    // Manages which toolbar is currently visible
    var currentState by remember { mutableStateOf(ToolbarState.INITIAL) }
    val listState = rememberLazyListState()
    val isFabVisible = currentState == ToolbarState.INITIAL && !listState.isScrollingDown()

    Scaffold(
        containerColor = Color(0xFF131316),
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(100) { index ->
                    Text(
                        text = "Scrollable Item #$index",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = Color.White
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                AnimatedVisibility(
                    visible = isFabVisible,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    enter = slideInHorizontally(initialOffsetX = { it + 200 }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it + 200 }) + fadeOut()
                ) {
                    FloatingActionButton(
                        onClick = { currentState = ToolbarState.EDITING },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit button"
                        )
                    }
                }
                if (currentState == ToolbarState.EDITING || currentState == ToolbarState.PLAYING) {
                    CombinedToolbar(
                        currentState = currentState,
                        onUndo = {
                            currentState = if (currentState == ToolbarState.PLAYING) {
                                ToolbarState.EDITING
                            } else {
                                ToolbarState.INITIAL
                            }
                        },
                        onPlay = { currentState = ToolbarState.PLAYING },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .animateContentSize(
                                animationSpec = tween(
                                    durationMillis = 500, // Animation will take 500 milliseconds
                                    easing = FastOutSlowInEasing // Customize the acceleration and deceleration
                                )
                            )
                            .padding(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CombinedToolbar(
    currentState: ToolbarState,
    onUndo: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier // Added modifier parameter
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier // Apply the passed modifier
    ) {
        when (currentState) {
            ToolbarState.EDITING -> EditingContent(onUndo = onUndo, onPlay = onPlay)
            ToolbarState.PLAYING -> PlayerContent(onUndo = onUndo)
            else -> Box {} // Should not happen in this context
        }
    }
}

@Composable
private fun EditingContent(onUndo: () -> Unit, onPlay: () -> Unit) {
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
            val onClickAction = when (item) {
                Icons.AutoMirrored.Filled.Undo -> onUndo
                Icons.Default.PlayArrow -> onPlay
                else -> ({})
            }
            val contentDescription = item.name.replaceFirstChar { it.uppercase() }

            if (item != Icons.Default.PlayArrow) {
                IconButton(onClick = onClickAction) {
                    Icon(imageVector = item, contentDescription = contentDescription)
                }
            } else {
                Button(onClick = onClickAction, contentPadding = PaddingValues(0.dp)) {
                    Icon(imageVector = item, contentDescription = contentDescription)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerContent(onUndo: () -> Unit) {
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
            Button(onClick = { /* TODO */ }, contentPadding = PaddingValues(0.dp)) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play")
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
