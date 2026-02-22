package com.cryptic.pixvi.core.network

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.cryptic.pixvi.core.network.api.PixivApiService
import com.cryptic.pixvi.core.network.interceptor.AuthInterceptor
import retrofit2.Retrofit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory


class NetworkClient(
    injector: AuthInterceptor,
    context: Context
){
    private val BASE_URL= "https://app-api.pixiv.net/"

    private val client = OkHttpClient.Builder()
        .addInterceptor(injector)
        .build()

    private val retrofit : Retrofit by lazy {
        Retrofit.Builder()
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(Json.asConverterFactory("application/json; charset=utf-8".toMediaType()))
            .addCallAdapterFactory(NetworkResultCallAdapterFactory.create())
            .client(client)
            .baseUrl(BASE_URL)
            .build()
    }

    val imageLoader by lazy { ImageLoader.Builder(context)
        .components {
            add(
                OkHttpNetworkFetcherFactory(
                    callFactory = {
                        client
                    }
                )
            )
        }.build() }

    val apiService: PixivApiService by lazy {
        retrofit.create(PixivApiService::class.java)
    }
}