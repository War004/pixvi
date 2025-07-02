package com.example.pixvi.login

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * A singleton object responsible for creating and configuring a Retrofit instance
 * specifically for handling authentication-related API calls.
 *
 * This client is separate from the main API client to ensure that authentication
 * requests (like logging in or refreshing a token) do not include an Authorization
 * Bearer token, which wouldn't be available or valid at that stage.
 */
object AuthRetrofitClient {

    /**
     * Creates a lazy-initialized OkHttpClient for authentication requests.
     * 'lazy' means this instance is created only when it's first accessed.
     *
     * This client is configured specifically for auth flows and intentionally
     * EXCLUDES the main HeaderInterceptor that would add a "Bearer" token to requests.
     */
    private val authOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // Set timeouts to prevent the app from hanging if the server is unresponsive.
            .connectTimeout(30, TimeUnit.SECONDS) // Time to establish a connection.
            .readTimeout(30, TimeUnit.SECONDS)    // Time to wait for data after connecting.
            .writeTimeout(30, TimeUnit.SECONDS)   // Time to wait when sending data.
            .build()
    }

    /**
     * Creates a lazy-initialized Retrofit instance for authentication.
     * It's configured with a base URL and uses the custom `authOkHttpClient`.
     */
    private val authRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            // The base URL for all API endpoints defined in AuthApiService.
            .baseUrl("https://app-api.pixiv.net/")
            // Use the specially configured OkHttpClient for auth requests.
            .client(authOkHttpClient)
            // Add Gson support to automatically convert JSON responses to Kotlin objects.
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Creates a public, lazy-initialized implementation of the AuthApiService interface.
     *
     * This is the entry point for the rest of the application to make authentication calls.
     * For example: `AuthRetrofitClient.authApiService.login(...)`
     */
    val authApiService: AuthApiService by lazy {
        // Retrofit generates the necessary networking code for the AuthApiService interface.
        authRetrofit.create(AuthApiService::class.java)
    }
}