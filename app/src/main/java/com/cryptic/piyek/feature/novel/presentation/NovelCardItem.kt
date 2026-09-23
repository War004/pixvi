package com.cryptic.piyek.feature.novel.presentation

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun NovelCardItem(
    id: Long,
    title: String,
    caption: String,
    authorName: String,
    authorId: String,
    authorBestProfilePic: String,
    novelCoverBestImage: String,
    totalLength: String,
    totalPages: Int,
    totalWords: Long,
    totalView: Int,
    totalLikes: Int,
    totalComment: Int,
    tags: List<String>,
    onPress:(Long) -> Unit,
    onLongPress:(Long) -> Unit,
    powerSaver: Boolean
){

    val defaultIconPainter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.MenuBook)
    val errorIconPainter = rememberVectorPainter(image = Icons.Default.BrokenImage)

    var showDetails by remember { mutableStateOf(false) }
    val clipboard: Clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .height(150.dp)
            .fillMaxWidth()
            .combinedClickable(
                role = Role.Button,
                onClick = { onPress(id) },
                onLongClick = { onLongPress(id) }
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        /* Two sections
        Left -> Book cover ~ 0.25
        Right -> info(title,author,views,likes,comments,tags,caption[reveled with info icon]) ~ 0.75
         */
        Row(
            modifier = Modifier.fillMaxSize()
        ){
            //book covert
            AsyncImage(
                model = novelCoverBestImage,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                placeholder = defaultIconPainter,
                error = errorIconPainter,
                fallback = defaultIconPainter,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.25f)
            )

            Box(
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxHeight()
                    .clipToBounds()
            ){
                //the blurred background or the dominant colors based on powersaver
                /*
                AsynImage.....
                 */
                if(powerSaver || novelCoverBestImage.isEmpty()){
                    //use the domient color
                } else {
                    //use the bulr
                }

                //the actual displayable info
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                        )
                ){
                    if(!showDetails){
                        //show the actual details
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            // Top section: Title, Author, and Stats are grouped together
                            Column {
                                Text(
                                    text = title,
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
                                                        ClipData.newPlainText("Novel_title", title)
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
                                    text = "by $authorName",
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
                                                    ClipData.newPlainText("Novel_author", authorName)
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
                                        Text(text = totalView.toString(), color = Color.White, fontSize = 12.sp)
                                    }
                                    // Bookmarks
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Filled.Favorite, "Bookmarks", modifier = Modifier.size(17.dp), tint = Color.White)
                                        Text(text = totalLikes.toString(), color = Color.White, fontSize = 12.sp)
                                    }
                                    // Comments
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Comment, "Comments", modifier = Modifier.size(17.dp), tint = Color.White)
                                        Text(text = totalComment.toString(), color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            if (tags.isNotEmpty()) {
                                // 1. Remember the scroll state for this specific text field
                                val tagScrollState = rememberScrollState()

                                val annotatedTagsString = remember(tags) {
                                    // ... (The buildAnnotatedString block is identical to the previous answer)
                                    buildAnnotatedString {
                                        tags.forEachIndexed { index, tag ->
                                            pushStringAnnotation(tag = "tag_click", annotation = tag)
                                            withStyle(style = SpanStyle(color = Color(0xFF81D4FA))) {
                                                append(tag)
                                            }
                                            pop()
                                            if (index < tags.size - 1) {
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
                        //show the captions
                    }
                }
            }
        }
    }
}


@Preview()
@Composable
fun ViewNovelCard(){
    NovelCardItem(
        id = 213123L,
        title = "My novel",
        caption = "Mouse is yellow and black",
        authorName = "Cat",
        authorId = "w35345",
        authorBestProfilePic = "mice",
        novelCoverBestImage = "not mouse",
        totalLength = "244",
        totalPages = 123,
        totalWords = 1,
        totalView = -4,
        totalLikes = -56,
        totalComment = 13,
        tags = listOf("Balls","Bat","Bar"),
        onPress = {},
        onLongPress = {},
        powerSaver = false
    )
}