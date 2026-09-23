package com.cryptic.piyek.core.content.manga.data.remote

import com.cryptic.piyek.core.content.data.remote.ILLustMangaRecommendationResponse
import com.cryptic.piyek.core.network.NetworkResult
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
        @Query("include_ranking_illusts") includeRankingLabel: Boolean = true,
        @Query("include_privacy_policy") includePrivacyPolicy: Boolean = true,
        @Query("max_bookmark_id_for_recommend") maxBookmarkId: Long? = null,
        @Query("min_bookmark_id_for_recent_recommend") minBookmarkId: Long? = null,
        @Query("offset") offset: Int? = null
    ): NetworkResult<ILLustMangaRecommendationResponse>

    @GET
    suspend fun getMoreRecommendation(
        @Url nextUrl: String
    ): NetworkResult<ILLustMangaRecommendationResponse>

    @FormUrlEncoded
    @POST("v2/illust/bookmark/add")
    suspend fun addBookmark(
        @Field("illust_id") iLLust: Long,
        @Field("restrict") restrict: String
    ): NetworkResult<Unit>

    @FormUrlEncoded
    @POST(value = "v1/illust/bookmark/delete")
    suspend fun deleteBookmark(
        @Field("illust_id") iLLust: Long
    ): NetworkResult<Unit>
}