package com.cryptic.piyek.feature.novel.presentation

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Precision
import com.cryptic.piyek.presentation.components.FastBlurredBackdrop
import kotlinx.coroutines.launch

@Composable
fun NovelCardItem(
    novel: NovelCardUi,
    onAction: (HomeNovelScreenActions) -> Unit,
    modifier: Modifier = Modifier,
    powerSaver: Boolean = false,
) {
    val defaultIconPainter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.MenuBook)
    val errorIconPainter = rememberVectorPainter(image = Icons.Default.BrokenImage)

    var showDetails by remember { mutableStateOf(false) }
    val clipboard: Clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier
            .height(150.dp)
            .fillMaxWidth()
            .combinedClickable(
                role = Role.Button,
                onClick = { onAction(HomeNovelScreenActions.OpenNovel(novel.id)) },
                onLongClick = { onAction(HomeNovelScreenActions.PressForMoreInfo(novel.id)) }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Section: Book Cover Image
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(novel.coverImageUrl)
                    .size(width = 300, height = 450)
                    .precision(Precision.INEXACT)
                    .build(),
                contentDescription = novel.title,
                contentScale = ContentScale.Crop,
                placeholder = defaultIconPainter,
                error = errorIconPainter,
                fallback = defaultIconPainter,
                modifier = Modifier
                    .weight(0.28f)
                    .fillMaxHeight()
            )

            // Right Section: Info & Synopsis Content
            Box(
                modifier = Modifier
                    .weight(0.72f)
                    .fillMaxHeight()
            ) {
                // Background Layer: Frosted Cover Backdrop
                if (!powerSaver && novel.coverImageUrl.isNotEmpty()) {
                    FastBlurredBackdrop(
                        imageUrl = novel.coverImageUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (showDetails) {
                    // Synopsis View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SYNOPSIS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.1.sp
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onAction(HomeNovelScreenActions.PushToDownload(novel.id)) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = "Download novel",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { showDetails = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close synopsis",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Text(
                            text = novel.caption.ifBlank { "No synopsis provided." },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 16.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = novel.formattedPages,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Text(
                                text = novel.formattedWords,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Main Details View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Header Row: Title, Author, and Info Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp)
                            ) {
                                Text(
                                    text = novel.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.combinedClickable(
                                        onClick = { onAction(HomeNovelScreenActions.OpenNovel(novel.id)) },
                                        onLongClick = {
                                            scope.launch {
                                                clipboard.setClipEntry(
                                                    ClipEntry(ClipData.newPlainText("novel_title", novel.title))
                                                )
                                                onAction(HomeNovelScreenActions.CopyFeedback("Novel title copied to clipboard"))
                                            }
                                        }
                                    )
                                )

                                Text(
                                    text = if (novel.authorName.isNotEmpty()) "by ${novel.authorName}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.combinedClickable(
                                        onClick = { onAction(HomeNovelScreenActions.OpenAuthor(novel.authorId)) },
                                        onLongClick = {
                                            scope.launch {
                                                clipboard.setClipEntry(
                                                    ClipEntry(ClipData.newPlainText("author_name", novel.authorName))
                                                )
                                                onAction(HomeNovelScreenActions.CopyFeedback("Novel author copied to clipboard"))
                                            }
                                        }
                                    )
                                )
                            }

                            IconButton(
                                onClick = { showDetails = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "Show novel synopsis",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Stats Row: Clean icon + text without pill backgrounds
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatItem(
                                icon = Icons.Filled.RemoveRedEye,
                                value = novel.formattedViews,
                                contentDescription = "Views"
                            )
                            StatItem(
                                icon = if (novel.isBookmarked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                value = novel.formattedLikes,
                                contentDescription = "Likes",
                                tint = if (novel.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = {
                                    onAction(HomeNovelScreenActions.ToggleBookmark(novel.id, novel.isBookmarked))
                                }
                            )
                            StatItem(
                                icon = Icons.AutoMirrored.Filled.Comment,
                                value = novel.formattedComments,
                                contentDescription = "Comments"
                            )
                        }

                        // Tags Row: Clean text with link styles
                        if (novel.tags.isNotEmpty()) {
                            val tagColor = MaterialTheme.colorScheme.primary
                            val dividerColor = MaterialTheme.colorScheme.outlineVariant

                            val annotatedTagsString = remember(novel.tags, tagColor, dividerColor) {
                                buildAnnotatedString {
                                    novel.tags.forEachIndexed { index, tag ->
                                        withLink(
                                            LinkAnnotation.Clickable(
                                                tag = tag,
                                                styles = TextLinkStyles(
                                                    style = SpanStyle(
                                                        color = tagColor,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                ),
                                                linkInteractionListener = { annotation ->
                                                    (annotation as? LinkAnnotation.Clickable)?.tag?.let {
                                                        onAction(HomeNovelScreenActions.FilterByTag(it))
                                                    }
                                                }
                                            )
                                        ) {
                                            append("#")
                                            append(tag)
                                        }

                                        if (index < novel.tags.size - 1) {
                                            withStyle(SpanStyle(color = dividerColor)) {
                                                append("  •  ")
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                text = annotatedTagsString,
                                style = MaterialTheme.typography.labelSmall,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        modifier.clickable(
            role = Role.Button,
            onClick = onClick
        )
    } else {
        modifier
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(14.dp),
            tint = tint
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NovelCardItemPreview() {
    MaterialTheme {
        NovelCardItem(
            novel = NovelCardUi(
                id = 213123L,
                title = "The Obsidian Chronicles",
                caption = "In an empire fractured by dark sorcery and hidden bloodlines, an outcast scribe discovers a tome that can manipulate royal fates.",
                authorName = "C. K. Thorne",
                authorId = 99L,
                authorProfilePicUrl = "",
                coverImageUrl = "",
                totalPages = 480,
                totalWords = 142000L,
                formattedViews = "24.5K",
                formattedLikes = "3.2K",
                formattedComments = "412",
                formattedPages = "480 Pages",
                formattedWords = "142K Words",
                tags = listOf("HighFantasy", "Mystery", "AntiHero", "MagicSystem"),
                isBookmarked = true
            ),
            onAction = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
