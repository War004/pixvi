package com.cryptic.piyek.core.network.interceptor

import android.util.Log
import com.cryptic.piyek.AppConfig
import com.cryptic.piyek.core.auth.data.local.token.TokenStorage
import com.cryptic.piyek.core.auth.domain.repository.OAuthUserRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.Authenticator

class AuthInterceptor(
    private val tokenStorage: TokenStorage,
    private val appConfig: AppConfig,
    private val oAuthUserRepository: OAuthUserRepository,
) : Interceptor,Authenticator  {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenStorage.getAccessToken()
        val xClient = appConfig.clientHashGenerator()

        val requestBuilder = originalRequest.newBuilder()
            .header("App-OS",appConfig.getOsName())
            .header("App-OS-Version",appConfig.getOsVersion())
            .header("App-Version",appConfig.getAppVersion())
            .header("User-Agent", appConfig.getUserAgent())
            .header("Accept-Language", appConfig.getAcceptLan())
            .header("App-Accept-Language", appConfig.getAppAcceptLan())
            .header("Referer", appConfig.getRefererUrl())
            .header("X-Client-Time", xClient.timeStamp)
            .header("X-Client-Hash", xClient.clientSecret)

        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.responseCount >= 3) {
            return null
        }

        return runBlocking {
            tokenStorage.writeMutex.withLock {
                val currentInMemoryToken = tokenStorage.getAccessToken()

                if (currentInMemoryToken.isNullOrBlank()) {
                    return@runBlocking null
                }

                val requestToken = response.request.header("Authorization")
                    ?.removePrefix("Bearer ")

                if (currentInMemoryToken != requestToken) {
                    return@runBlocking response.request.newBuilder()
                        .header("Authorization", "Bearer $currentInMemoryToken")
                        .build()
                }

                val refreshTokenResult = oAuthUserRepository.getRefreshTokenForActiveAcc()

                if (refreshTokenResult.isSuccess) {
                    val actualRefreshToken = refreshTokenResult.getOrThrow()

                    val accessToken = oAuthUserRepository.exchangeCodeForToken(
                        grantType = "refresh_token",
                        clientId = appConfig.getClientId(),
                        clientSecret = appConfig.getClientSecret(),
                        includePolicy = false,
                        refreshToken = actualRefreshToken
                    )

                    if (accessToken.isSuccess) {
                        val newToken = accessToken.getOrThrow()

                        return@runBlocking response.request.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                    } else {
                        return@runBlocking null
                    }
                } else {
                    return@runBlocking null
                }
            }
        }
    }
}

private val Response.responseCount: Int
    get() {
        var count = 1
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }