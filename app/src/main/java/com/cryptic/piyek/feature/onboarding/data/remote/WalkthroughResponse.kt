package com.cryptic.piyek.feature.onboarding.data.remote

import com.cryptic.piyek.core.content.data.remote.ArtworkResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WalkthroughResponse(
    @SerialName("illusts") val illusts: List<ArtworkResponse>,
    @SerialName("next_url") val nextUrl: String? = null
)