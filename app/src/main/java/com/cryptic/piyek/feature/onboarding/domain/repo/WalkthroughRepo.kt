package com.cryptic.piyek.feature.onboarding.domain.repo

import com.cryptic.piyek.core.CResponse
import com.cryptic.piyek.core.content.data.model.Artwork

interface WalkthroughRepo{
    suspend fun getWalkthroughList(): CResponse<List<Artwork>>
}