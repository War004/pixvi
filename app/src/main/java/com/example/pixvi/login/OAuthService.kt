package com.example.pixvi.login


import android.os.Build
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Service class responsible for handling OAuth 2.0 related API calls to Pixiv.
 * This includes fetching IDP URLs and exchanging authorization codes for tokens.
 */
class OAuthService {

    private val authApi = AuthRetrofitClient.authApiService

    private fun buildUserAgent(): String {
        return "PixivAndroidApp/6.144.0 (Android ${Build.VERSION.RELEASE}; ${Build.MODEL})" //Version number
    }

    /**
     * Retrieves IDP (Identity Provider) URLs from Pixiv's API.
     * These URLs are required to initiate the OAuth 2.0 flow, specifically to get the authorization token URL.
     *
     * @param clientTime A formatted timestamp representing the current time, required by Pixiv API headers.
     * @param clientHash A hash generated based on the clientTime and a secret salt, required by Pixiv API headers for security.
     * @return [IdpUrlsResponse] object containing the IDP URLs parsed from the JSON response.
     * @throws IOException if the network request fails or the response is not successful.
     */
    //the comment is not updated
    suspend fun getIdpUrls(clientTime: String, clientHash: String): IdpUrlsResponse {
        val response = authApi.getIdpUrls(
            appOsVersion = Build.VERSION.RELEASE,
            userAgent = buildUserAgent(),
            clientTime = clientTime,
            clientHash = clientHash
        )
        if (!response.isSuccessful) {
            throw IOException("Unexpected code ${response.code()} ${response.message()} - ${response.errorBody()?.string()}")
        }
        return response.body() ?: throw IOException("Response body is null")
    }

    /**
     * Exchanges an authorization code for access and refresh tokens from Pixiv's token endpoint.
     * This is the final step in the OAuth 2.0 authorization code flow with PKCE, where the app obtains tokens after the user has authorized access.
     *
     * @param authTokenUrl The URL of the token endpoint obtained from the IdpUrlsResponse.
     * @param authCode The authorization code received from Pixiv after successful user authentication.
     * @param codeVerifier The code verifier generated at the start of the OAuth 2.0 flow, used for PKCE.
     * @param clientTime A formatted timestamp representing the current time, required by Pixiv API headers.
     * @param clientHash A hash generated based on the clientTime and a secret salt, required by Pixiv API headers for security.
     * @return [TokenResponse] object containing the access token, refresh token, and user information parsed from the JSON response.
     * @throws IOException if the network request fails or the response is not successful.
     */

    //the comment is not updated

    suspend fun exchangeToken(
        authTokenUrl: String,
        authCode: String,
        codeVerifier: String,
        clientTime: String,
        clientHash: String
    ): TokenResponse {
        val response = authApi.exchangeToken(
            authTokenUrl = authTokenUrl,
            authCode = authCode,
            codeVerifier = codeVerifier,
            appOsVersion = Build.VERSION.RELEASE,
            userAgent = buildUserAgent(),
            clientTime = clientTime,
            clientHash = clientHash
        )
        if (!response.isSuccessful) {
            throw IOException("Unexpected code ${response.code()} ${response.message()} - ${response.errorBody()?.string()}")
        }
        return response.body() ?: throw IOException("Response body is null")
    }

    /**
     * Refreshes the access token using a refresh token.
     * This is used when an access token has expired but the refresh token is still valid.
     *
     * @param authTokenUrl The URL of the token endpoint obtained from the IdpUrlsResponse.
     * @param refreshToken The refresh token used to obtain a new access token.
     * @param clientTime A formatted timestamp representing the current time, required by Pixiv API headers.
     * @param clientHash A hash generated based on the clientTime and a secret salt, required by Pixiv API headers for security.
     * @return [TokenResponse] object containing the new access token, refresh token, and user information.
     * @throws IOException if the network request fails or the response is not successful.
     */

    //the comment is not updated
    suspend fun refreshToken(
        authTokenUrl: String,
        refreshToken: String,
        clientTime: String,
        clientHash: String
    ): TokenResponse {
        val response = authApi.refreshToken(
            authTokenUrl = authTokenUrl,
            refreshToken = refreshToken,
            appOsVersion = Build.VERSION.RELEASE,
            userAgent = buildUserAgent(),
            clientTime = clientTime,
            clientHash = clientHash
        )
        if (!response.isSuccessful) {
            throw IOException("Unexpected code during token refresh: ${response.code()} ${response.message()} - ${response.errorBody()?.string()}")
        }
        return response.body() ?: throw IOException("Response body is null")
    }
}