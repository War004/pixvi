package com.cryptic.piyek.core.content.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ILLustMangaRecommendationResponse(
    @SerialName("illusts")
    val artwork: List<ArtworkResponse>,
    @SerialName("contest_exists")
    val contestExists: Boolean,
    @SerialName("next_url")
    val nextUrl: String, //do null
    @SerialName("privacy_policy")
    val privacyPolicy: PrivacyPolicy? = null,
    @SerialName("ranking_illusts")
    val rankingArtwork: List<ArtworkResponse> = emptyList()
)

@Serializable
data class PrivacyPolicy(
    val message: String? = null,
    val version: String? = null,
    val url: String? = null,
)