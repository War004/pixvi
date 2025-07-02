package com.example.pixvi.network.api

import HeaderInterceptor
import com.example.pixvi.login.AuthManager
import com.example.pixvi.login.TokenProvider
import com.example.pixvi.network.TokenRefreshInterceptor

import com.example.pixvi.network.api.PixivApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://app-api.pixiv.net/"
    private var tokenProviderInstance: TokenProvider? = null

    fun initialize(tokenProvider: TokenProvider) {
        if (tokenProviderInstance == null) {
            tokenProviderInstance = tokenProvider
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // Add HeaderInterceptor first to ensure headers are present for requests
        builder.addInterceptor(HeaderInterceptor()) // Your existing interceptor

        // Add TokenRefreshInterceptor
        tokenProviderInstance?.let { tp ->
            builder.addInterceptor(TokenRefreshInterceptor(tp, AuthManager))
        } ?: throw IllegalStateException("RetrofitClient not initialized with TokenProvider. Call RetrofitClient.initialize() first.")

        builder.build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: PixivApiService by lazy {
        retrofit.create(PixivApiService::class.java)
    }
}