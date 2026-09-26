package com.cryptic.piyek.domain.image

import android.graphics.Bitmap

interface BlurredImageRepository {
    fun getCached(url: String): Bitmap?
    suspend fun getOrGenerateBlur(url: String): Bitmap?
}

class GetBlurredImageUseCase(
    private val repository: BlurredImageRepository
) {
    fun getSynchronouslyIfCached(url: String): Bitmap? {
        return repository.getCached(url)
    }

    suspend operator fun invoke(url: String): Bitmap? {
        return repository.getOrGenerateBlur(url)
    }
}