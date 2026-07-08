package com.cryptic.piyek.feature.onboarding.data.repo

import com.cryptic.piyek.core.CResponse
import com.cryptic.piyek.core.content.data.remote.toDomain
import com.cryptic.piyek.core.network.NetworkResult
import com.cryptic.piyek.feature.onboarding.data.remote.PixivWalkThroughApiService
import com.cryptic.piyek.core.content.data.model.Artwork
import com.cryptic.piyek.feature.onboarding.domain.repo.WalkthroughRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WalkthroughRepoImpl(
    private val onBoardingApi: PixivWalkThroughApiService
): WalkthroughRepo{
    override suspend fun getWalkthroughList(): CResponse<List<Artwork>> {
        return withContext(Dispatchers.IO){
            when(val response = onBoardingApi.getWalkthroughIllusts()){
                is NetworkResult.Success -> {
                    val list = response.data.illusts.map { it.toDomain() }
                    CResponse.Success(data = list)
                }
                is NetworkResult.Error -> {
                    CResponse.Failed(Exception("HTTP ${response.code}: ${response.message}"))
                }
                is NetworkResult.Exception -> {
                    CResponse.Failed(response.e)
                }
            }
        }
    }
}