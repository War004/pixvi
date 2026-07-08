package com.cryptic.piyek.core.content.data.model

data class ArtworkContentList(
    val artworkList: List<Artwork>,
    val rankingArtworkList: List<Artwork> = emptyList(),
    val nextUrl: String,
    val focusedIndex: Int,
)
