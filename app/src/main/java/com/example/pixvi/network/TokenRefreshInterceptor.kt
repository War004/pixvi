package com.example.pixvi.network

import android.util.Log
import com.example.pixvi.login.AuthManager
import com.example.pixvi.login.ErrorResponsePayload
import com.example.pixvi.login.TokenProvider
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class TokenRefreshInterceptor(
    private val tokenProvider: TokenProvider,
    private val authManager: AuthManager
) : Interceptor {

    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var response = chain.proceed(originalRequest)

        // Check for the specific error: HTTP 400 and "invalid_grant" in the message
        if (response.code == 400) {
            // Peek the body to read it without consuming it, so it can be read again if needed
            // or if this isn't the error we're handling.
            val responseBodyString = response.peekBody(Long.MAX_VALUE).string()
            try {
                val errorResponse = gson.fromJson(responseBodyString, ErrorResponsePayload::class.java)
                if (errorResponse.error?.message?.contains("invalid_grant") == true) {
                    Log.d("TokenRefreshInterceptor", "Detected 'invalid_grant' error. Attempting token refresh.")

                    // Close the current response before trying to refresh token and retry
                    response.close()

                    val newAccessToken: String? = runBlocking { // Bridge suspend world to sync interceptor
                        tokenProvider.getRefreshedAccessToken()
                    }

                    return if (newAccessToken != null) {
                        Log.i("TokenRefreshInterceptor", "Token refreshed successfully. Retrying original request.")
                        val newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                        chain.proceed(newRequest) // Retry with new token
                    } else {
                        Log.e("TokenRefreshInterceptor", "Token refresh failed. Returning original error response.")
                        // If refresh failed, we need to construct a new response with the original body,
                        // We will just proceed with making a new response from the peeked body.
                        val originalErrorBody = response.peekBody(Long.MAX_VALUE) // Get it again if needed
                        Response.Builder()
                            .request(originalRequest)
                            .protocol(response.protocol)
                            .code(response.code)
                            .message(response.message)
                            .body(originalErrorBody) // Use the peeked body
                            .headers(response.headers)
                            .networkResponse(response.networkResponse)
                            .priorResponse(response.priorResponse)
                            .cacheResponse(response.cacheResponse)
                            .sentRequestAtMillis(response.sentRequestAtMillis)
                            .receivedResponseAtMillis(response.receivedResponseAtMillis)
                            .build()
                    }
                }
            } catch (e: Exception) {
                Log.e("TokenRefreshInterceptor", "Error parsing 400 error response body: ${e.message}")
            }
        }
        return response
    }
}