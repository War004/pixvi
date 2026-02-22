package com.cryptic.pixvi.artwork.data

data class PageIndices(
    val recommendationsIndex: Int?,
    val rankingIndex: Int?,
    val postIndex: Map<Long,Int>
)