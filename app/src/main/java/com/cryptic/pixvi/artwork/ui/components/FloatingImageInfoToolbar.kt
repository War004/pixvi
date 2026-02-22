package com.cryptic.pixvi.artwork.ui.components

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptic.pixvi.artwork.data.ArtworkInfo
import com.cryptic.pixvi.core.network.util.convertLongToTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingImageInfoToolbar(
    artwork: ArtworkInfo,
    modifier: Modifier = Modifier,
    onFavoriteClicked:() -> Unit,
    onLongFavorite:() -> Unit
    ){

    var showInfo by remember { mutableStateOf(false) }
    val clipboard: Clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier.padding(10.dp),
        shape = RoundedCornerShape(20.dp),
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f)
        ),
        contentPadding = PaddingValues(start =  12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
    ) {
        if(showInfo){
            //show the info
            Row(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                    .horizontalScroll(state = rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ){
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.RemoveRedEye,
                        contentDescription = "Views",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = artwork.data.totalViews.toString(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = "Likes",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${artwork.data.totalBookmarks}",
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
                        //need more testing for creation date
                        text = convertLongToTime(artwork.data.creationDate),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }else{
            //
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = artwork.data.title,
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
                                    ClipData.newPlainText("Post_title", artwork.data.title)
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
                    text = artwork.author.authorName, // Use the 'info' object
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
                                    ClipData.newPlainText("Post_author_name", artwork.author.authorName) // Use the 'info' object
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
                    onClick =  onFavoriteClicked,
                    onLongClick = onLongFavorite
                )
        ) {
            Icon(
                imageVector = if (artwork.data.isBookmarked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorite",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp) // The icon's visual size remains the same.
            )
        }
    }
}