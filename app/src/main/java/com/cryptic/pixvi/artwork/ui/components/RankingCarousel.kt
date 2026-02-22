package com.cryptic.pixvi.artwork.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.cryptic.pixvi.artwork.data.ArtworkInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingCarousel(
    artworkList: List<ArtworkInfo>,
    modifier: Modifier = Modifier,
    onItemClick: (rankingIndex: Int) -> Unit
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

        val carouselState = rememberCarouselState { artworkList.size }

        HorizontalUncontainedCarousel(
            state = carouselState,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp), // Height to contain the items + padding
            itemWidth = 186.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) { i ->
            val artwork = artworkList[i]
            CarouselIllustItem(
                artwork = artwork,
                onItemClick = {
                    /*TODO*/
                }
            )
        }
    }
}