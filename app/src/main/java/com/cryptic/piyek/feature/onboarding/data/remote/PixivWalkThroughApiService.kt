package com.cryptic.piyek.feature.onboarding.data.remote

import com.cryptic.piyek.core.network.NetworkResult
import retrofit2.http.GET

interface PixivWalkThroughApiService {
    @GET(value = "v1/walkthrough/illusts")
    suspend fun getWalkthroughIllusts(): NetworkResult<WalkthroughResponse>
}