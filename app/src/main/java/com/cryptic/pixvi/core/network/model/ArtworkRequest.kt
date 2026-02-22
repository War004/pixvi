package com.cryptic.pixvi.core.network.model

sealed class ArtworkRequest {
    data class FreshLoad(
        val filter: String = "for_android",
        val includeRanking: Boolean = true,
        val includePolicy: Boolean = false
    ) : ArtworkRequest()

    data class NextPage(
        val url: String
    ) : ArtworkRequest()
}