package com.cryptic.piyek.core.content.manga.data.remote

import com.cryptic.piyek.core.CResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface MangaApiService {
    @GET("v1/manga/recommended")
    suspend fun getRecommendation(
        @Query("filter") filter: String = "for_android",
        @Query("include_ranking_illusts") includeRanking: Boolean = true,
        @Query("include_privacy_policy") includePrivacyPolicy: Boolean = true
    ): CResponse<String>

    @GET
    suspend fun getMoreRecommendation(
        @Url nextUrl: String
    ): CResponse<String>

    @FormUrlEncoded
    @POST("v2/illust/bookmark/add")
    suspend fun addBookmark(
        @Field("illust_id") iLLust: Long,
        @Field("restrict") restrict: String
    ): CResponse<Unit>

    @FormUrlEncoded
    @POST(value = "v1/illust/bookmark/delete")
    suspend fun deleteBookmark(
        @Field("illust_id") iLLust: Long
    ): CResponse<Unit>
}