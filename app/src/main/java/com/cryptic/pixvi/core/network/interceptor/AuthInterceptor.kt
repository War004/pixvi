package com.cryptic.pixvi.core.network.interceptor

import android.os.Build
import android.util.Log
import com.cryptic.pixvi.auth.data.AuthTokenManager
import com.cryptic.pixvi.core.network.util.clientHashGenerator
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale

class AuthInterceptor(
    private val authTokenManager: AuthTokenManager,
    private val onRefreshToken: suspend () -> Boolean,
) : Interceptor {

    private val refreshMutex = Mutex()
    private val appVersion = "6.168.0" //as of 26th jan 2026
    private val osVersion = Build.VERSION.RELEASE
    private val deviceModel = Build.MODEL
    private val osName = "Android"
    private val userAgent = "PixivAndroidApp/$appVersion ($osName $osVersion; $deviceModel)"
    private val locale = Locale.getDefault()
    private val acceptLanguage = "${locale.language}_${locale.country}"
    private val appAcceptLanguage = locale.language
    private val referer = "https://app-api.pixiv.net/"

    override fun intercept(chain: Interceptor.Chain): Response {

        val secretAndTime = clientHashGenerator()
        var accessToken = authTokenManager.getToken()

        // CASE 1: No access token in memory
        if (accessToken.isNullOrBlank()) {
            if (authTokenManager.hasStoredSession()) {
                val refreshed = refreshWithLock()
                if (refreshed) {
                    accessToken = authTokenManager.getToken()
                } else {
                    Log.e("Interceptor","AccessToken is null and refresh failed")
                }
            } else {
                Log.e("Interceptor","No session found")
            }
        }

        //add a filter for the domains later
        val request = chain.request().newBuilder().apply {
            if (!accessToken.isNullOrBlank()) {
                header("Authorization", "Bearer $accessToken")
            }
            header("App-OS",osName)
            header("App-OS-Version",osVersion)
            header("App-Version",appVersion)
            header("User-Agent", userAgent)
            header("Accept-Language", acceptLanguage)
            header("App-Accept-Language", appAcceptLanguage)
            header("Referer", referer)
            header("X-Client-Time", secretAndTime.timeStamp)
            header("X-Client-Hash", secretAndTime.clientSecret)
        }.build()

        val response = chain.proceed(request)

        // CASE 2: Server rejected the token (401/403)
        if (response.code == 401 || response.code == 403) {

            // If the server says 401, we should trust it and try to refresh,
            // even if our local clock says the token is still valid.
            val refreshed = refreshWithLock()

            if (refreshed) {
                // Close the old failed response ONLY after success is confirmed
                response.close()

                val newToken = authTokenManager.getToken()
                val retryRequest = chain.request().newBuilder().apply {
                    header("Authorization", "Bearer $newToken")
                    header("App-OS",osName)
                    header("App-OS-Version",osVersion)
                    header("App-Version",appVersion)
                    header("User-Agent", userAgent)
                    header("Accept-Language", acceptLanguage)
                    header("App-Accept-Language", appAcceptLanguage)
                    header("Referer", referer)
                    header("X-Client-Time", secretAndTime.timeStamp)
                    header("X-Client-Hash", secretAndTime.clientSecret)
                }.build()

                return chain.proceed(retryRequest)
            } else {
                Log.e("Interceptor","Refresh failed during 401. Returning original error.")
                // Return the ORIGINAL 401 response so UI handles logout.
                // Do NOT call chain.proceed(request) again.
                return response
            }
        }
        return response
    }

    private fun refreshWithLock(): Boolean {
        return runBlocking {
            refreshMutex.withLock {
                // Double check: If another thread just refreshed it, use that token.
                // We check isTokenExpired here just to be safe, but the main driver is the 401.
                if (!authTokenManager.getToken().isNullOrBlank() && !authTokenManager.isTokenExpired()) {
                    return@withLock true
                }
                onRefreshToken()
            }
        }
    }
}