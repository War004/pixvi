package com.cryptic.pixvi.core.network.api

import com.cryptic.pixvi.core.network.NetworkResult
import com.cryptic.pixvi.core.network.model.artwork.ArtworkPageResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface PixivApiService{

    @Streaming
    @GET
    fun downloadFile(@Url url: String): Call<ResponseBody>

    //For getting illustData
    // https://app-api.pixiv.net/v1/illust/recommended?filter=for_android&include_ranking_illusts=true&include_privacy_policy=true
    @GET("v1/illust/recommended")
    suspend fun getArtworkRecommendation(
        @Query("filter") filter: String,
        @Query("include_ranking_illusts") wantRanking: Boolean,
        @Query("include_privacy_policy") includePolicy: Boolean,
    ): NetworkResult<ArtworkPageResponse>

    @GET
    suspend fun nextArtworkRecommendation(
        @Url url:String
    ): NetworkResult<ArtworkPageResponse>

    @FormUrlEncoded
    @POST(value= "v2/illust/bookmark/add")
    suspend fun addBookmarkArtwork(
        @Field("illust_id") artworkId: Long,
        @Field("restrict") restrict: String
    ): NetworkResult<Unit>

    @FormUrlEncoded
    @POST(value = "v1/illust/bookmark/delete")
    suspend fun removeBookmarkArtwork(
        @Field("illust_id") artworkId: Long
    ): NetworkResult<Unit>
}