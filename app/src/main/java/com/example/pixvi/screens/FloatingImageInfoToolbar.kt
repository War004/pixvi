package com.example.pixvi.screens

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pixvi.network.response.Home.Illust.Illust
import kotlinx.coroutines.launch


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
    illust: Any,
    modifier: Modifier = Modifier,
    onFavoriteClicked: () -> Unit,
    onLongFavorite: () -> Unit
) {

    val info = remember(illust) {
        when (illust) {
            is Illust -> FloatingToolbarInfo(
                title = illust.title,
                userName = illust.user.name,
                create_date = illust.create_date,
                total_view = illust.total_view,
                total_bookmarks = illust.total_bookmarks,
                is_bookmarked = illust.is_bookmarked
            )

            is com.example.pixvi.network.response.Home.Manga.Illust-> FloatingToolbarInfo(
                title = illust.title,
                userName = illust.user.name,
                create_date = illust.create_date,
                total_view = illust.total_view,
                total_bookmarks = illust.total_bookmarks,
                is_bookmarked = illust.is_bookmarked
            )
            else -> throw IllegalArgumentException("Unsupported type for FloatingImageInfoToolbar: ${illust::class.java.name}")
        }
    }

    var showInfo by remember { mutableStateOf(false) }
    val clipboard: androidx.compose.ui.platform.Clipboard = LocalClipboard.current
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