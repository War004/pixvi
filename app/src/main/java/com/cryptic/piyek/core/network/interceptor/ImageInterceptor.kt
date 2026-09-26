package com.cryptic.piyek.core.network.interceptor

import com.cryptic.piyek.AppConfig
import okhttp3.Interceptor
import okhttp3.Response

class ImageInterceptor(
    private val appConfig: AppConfig
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", appConfig.getUserAgent())
            .header("Referer", appConfig.getRefererUrl())
            .build()

        return chain.proceed(request)
    }
}