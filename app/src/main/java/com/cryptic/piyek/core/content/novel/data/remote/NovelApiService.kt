package com.cryptic.piyek.core.content.novel.data.remote

import com.cryptic.piyek.core.content.novel.data.model.NovelHomeResponse
import com.cryptic.piyek.core.network.NetworkResult
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface NovelApiService {
    @FormUrlEncoded
    @POST("v1/novel/recommended")
    suspend fun getRecommendation(
        @Field("include_ranking_novels") wantsRanking: Boolean = true,
        @Field("include_privacy_policy") includePrivacyPolicy: Boolean = true,
        @Field("read_novel_ids[]") readNovelIds: List<String> = emptyList(),
        @Field("read_novel_datetimes[]") readNovelDateTimes: List<String> = emptyList(),
        @Field("view_novel_ids[]") viewNovelIds: List<String> = emptyList(),
        @Field("view_novel_datetimes") viewNovelDateTimes: List<String> = emptyList()
    ): NetworkResult<NovelHomeResponse>

    @GET
    suspend fun getMoreRecommendation(
        @Url url: String
    ): NetworkResult<NovelHomeResponse>

    //experimental
    @GET("v1/novel/recommended")
    suspend fun getMoreRecommendation(
        @Query("include_ranking_novels") includeRankingNovels: Boolean = false,
        @Query("include_privacy_policy") includePrivacyPolicy: Boolean = false,
        @Query("offset") offset: Int,
        @Query("already_recommended") alreadyRecommended: String,
        @Query("max_bookmark_id_for_recommend") maxBookmarkId: String?
    ): NetworkResult<NovelHomeResponse>

    @FormUrlEncoded
    @POST("v2/user/Browse-history/novel/add")
    suspend fun syncNovelHistory(
        @Field("novel_ids[]") novelIds: List<String>
    ): NetworkResult<Unit>

    @FormUrlEncoded
    @POST("v1/novel/related")
    suspend fun getRelatedNovels(
        @Field("novel_id") novelId: String,
        @Field("read_novel_ids[]") readNovelIds: List<String>
    ): NetworkResult<Unit>
}