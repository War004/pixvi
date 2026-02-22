package com.cryptic.pixvi.core.network

import com.cryptic.pixvi.core.network.api.AuthApiService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AuthNetworkBuilder {

    fun createAuthService(): AuthApiService {
        return Retrofit.Builder()
            .baseUrl("https://oauth.secure.pixiv.net/")
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApiService::class.java)
    }
}