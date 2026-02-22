package com.cryptic.pixvi.artwork.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.cryptic.pixvi.artwork.data.ArtworkInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarouselItemScope.CarouselIllustItem(
    artwork: ArtworkInfo,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .maskClip(MaterialTheme.shapes.extraLarge)
            .clickable {
                onItemClick()
            }
    ) {
        // The image serves as the background of the Box
        AsyncImage(
            model = artwork.pages[0].quality.medium, //coded value is fine as it is a carousel view,
            contentDescription = artwork.data.title,
            contentScale = ContentScale.Crop
        )

        // The Text is aligned to the bottom-start of the parent Box, over the scrim
        Text(
            text = "@ ${artwork.author.authorName}",
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
