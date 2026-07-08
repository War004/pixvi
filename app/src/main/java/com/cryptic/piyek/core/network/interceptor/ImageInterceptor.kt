package com.cryptic.piyek.core.network.interceptor

import com.cryptic.piyek.AppConfig
import okhttp3.Interceptor
import okhttp3.Response

class ImageInterceptor(
    private val appConfig: AppConfig
):Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val requestBuilder = originalRequest.newBuilder()
            .header("Accept-Encoding","gzip")
            .header("Connection","Keep-Alive")
            .header("Host","i.pximg.net")
            .header("User-Agent", appConfig.getUserAgent())
            .header("Referer", appConfig.getRefererUrl())

        return chain.proceed(requestBuilder.build())
    }
}