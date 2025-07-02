package com.example.pixvi.network.api

import com.example.pixvi.network.response.Home.Illust.HomeIllust
import com.example.pixvi.network.response.AppLoading.UserStateResponse
import com.example.pixvi.network.response.Home.Manga.HomeManga
import com.example.pixvi.network.response.Home.Novels.HomeNovel
import com.example.pixvi.network.response.Home.Novels.RelatedNovel
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface PixivApiService {
    // Base URL: https://app-api.pixiv.net/

    @GET("v1/illust/recommended")
    suspend fun getRecommendedIllusts(
        @Query("filter") filter: String = "for_android",
        @Query("include_ranking_illusts") includeRankingIllusts: Boolean = true,
        @Query("include_privacy_policy") includePrivacyPolicy: Boolean = true
    ): Response<HomeIllust>

    @GET // No path needed when using @Url //Warning
    suspend fun getNextIllusts(@Url nextUrl: String): Response<HomeIllust>

    /**
     * Fetches the current user's state and profile information.
     * This endpoint provides details about the logged-in user.
     */
    @GET("v1/user/me/state")
    suspend fun getUserState(): Response<UserStateResponse>

    //https://app-api.pixiv.net/v1/manga/recommended?filter=for_android&include_ranking_illusts=true&include_privacy_policy=true
    @GET("v1/manga/recommended")
    suspend fun getRecommendedManga(
        @Query("filter") filter: String = "for_android",
        @Query("include_ranking_illusts") includeRankingIllusts: Boolean = true,
        @Query("include_privacy_policy") includePrivacyPolicy: Boolean = true
    ): Response<HomeManga>

    @GET // URL will be provided dynamically by next_url
    suspend fun getNextManga(
        @Url url: String,
    ): Response<HomeManga>

    /**
     * Fetches recommended novels. This function can be used to get generic recommendations
     * or be personalized by providing the user's recent viewing and reading history.
     *
     * By default, if no history lists are provided, this will fetch a generic list.
     *
     * @param wantRanking Request to include novels from the rankings. Defaults to true.
     * @param includePrivacyPolicy A required flag for the API call. Defaults to true.
     * @param readNovelIds A list of recently "read" novel IDs to use for personalization. Defaults to an empty list.
     * @param readNovelDatetimes Corresponding ISO 8601 timestamps for the read novels.
     * @param viewNovelIds A list of recently "viewed" novel IDs to use for personalization. Defaults to an empty list.
     * @param viewNovelDatetimes Corresponding ISO 8601 timestamps for the viewed novels.
     * @return A Response containing the recommended novel data.
     */
    @FormUrlEncoded
    @POST("v1/novel/recommended")
    suspend fun getRecommendedNovel(
        @Field("include_ranking_novels") wantRanking: Boolean = true,
        @Field("include_privacy_policy") includePrivacyPolicy: Boolean = true,
        @Field("read_novel_ids[]") readNovelIds: List<String> = emptyList(),
        @Field("read_novel_datetimes[]") readNovelDatetimes: List<String> = emptyList(),
        @Field("view_novel_ids[]") viewNovelIds: List<String> = emptyList(),
        @Field("view_novel_datetimes[]") viewNovelDatetimes: List<String> = emptyList()
    ): Response<HomeNovel>


    /**
     * NEW: A custom function for fetching subsequent pages of recommended novels.
     * Based on the observed API behavior, this is a GET request where we manually
     * construct all the parameters for pagination and content blending.
     *
     * @param offset The number of items already loaded. Used for pagination.
     * @param alreadyRecommended A comma-separated string of novel IDs that have already been shown to the user.
     * @param maxBookmarkId The session-specific ID that points to a pool of personalized recommendations.
     *                      This is omitted for "general" recommendation calls.
     * @return A Response containing the next batch of novel data.
     */
    @GET("v1/novel/recommended")
    suspend fun getMoreRecommendedNovels(
        @Query("include_ranking_novels") includeRankingNovels: Boolean = false,
        @Query("include_privacy_policy") includePrivacyPolicy: Boolean = false,
        @Query("offset") offset: Int,
        @Query("already_recommended") alreadyRecommended: String,
        @Query("max_bookmark_id_for_recommend") maxBookmarkId: String? // Nullable because it's conditional
    ): Response<HomeNovel>

    @GET //URL automatically created in the previous response
    suspend fun getNextNovel(
        @Url url: String
    ): Response<HomeNovel>

    /**
     * Syncs a user's viewing history to their Pixiv account.
     * This should be called when a user first opens a novel.
     *
     * @param novelIds A list of novel IDs to add to the user's Browse history.
     * @return A Response object, likely with no body on success (HTTP 204).
     */
    @FormUrlEncoded
    @POST("v2/user/Browse-history/novel/add")
    suspend fun syncBrowseHistory(
        @Field("novel_ids[]") novelIds: List<String>
    ): Response<Unit>


    /**
     * ## 💡 Database Schema Suggestion for Local History
     *
     * To gather the parameters needed for the `getRelatedNovels` function below,
     * you can use a simple local database table (like SQLite Room) with this structure:
     *
     * **Table: `NovelHistory`**
     * - `id`: INTEGER (Primary Key, Auto-increment)
     * - `novel_id`: TEXT (The ID of the novel)
     * - `interaction_type`: TEXT ("VIEW" or "READ")
     * - `timestamp`: TEXT (The ISO 8601 formatted timestamp string)
     *
     * **Usage:**
     * 1. When a user opens a novel, insert a new row: `(novel_id="123", interaction_type="VIEW", timestamp="...")`.
     * 2. When the app determines a novel is "read", update that row's `interaction_type` to "READ".
     * 3. Before calling `getRelatedNovels`, query this table to get two separate lists
     * for "VIEW" and "READ" events, then extract the IDs and timestamps.

     * Fetches novels related to a specific novel, personalized with the user's recent history.
     * This should be called when a user finishes reading a novel (e.g., reaches the bottom).
     *
     * @param novelId The ID of the novel the user just finished reading.
     * @param readNovelIds List of recently read novel IDs from local history.
     * @param readNovelDatetimes Corresponding timestamps for the read novels.
     * @param viewNovelIds List of recently viewed novel IDs from local history.
     * @param viewNovelDatetimes Corresponding timestamps for the viewed novels.
     * @return A Response containing a list of related novels.
     */
    @FormUrlEncoded
    @POST("v1/novel/related")
    suspend fun getRelatedNovels(
        @Field("novel_id") novelId: String,
        @Field("read_novel_ids[]") readNovelIds: List<String>,
        @Field("read_novel_datetimes[]") readNovelDatetimes: List<String>,
        @Field("view_novel_ids[]") viewNovelIds: List<String>,
        @Field("view_novel_datetimes[]") viewNovelDatetimes: List<String>
    ): Response<RelatedNovel>

    /**
     * Adds a  public or private bookmark for an illustration or a manga
     */
    @FormUrlEncoded
    @POST("v2/illust/bookmark/add")
    suspend fun addBookmarkIllust(
        @Field("illust_id") illustId: Long,
        @Field("restrict") restrict: String
    ): Response<Unit>

    /**
     * Deletes the bookmark from a illustration or a manga
     */
    @FormUrlEncoded
    @POST(value = "v1/illust/bookmark/delete")
    suspend fun deleteBookmarkIllust(
        @Field("illust_id") illustId: Long
    ): Response<Unit>

    /**
     * Adds a public or private bookmark for an novel
     */
    @FormUrlEncoded
    @POST("v2/novel/bookmark/add")
    suspend fun addBookmarkNovel(
        @Field("illust_id") illustId: Long,
        @Field("restrict") restrict: String
    ): Response<Unit>

    /**
     * Deletes the bookmark from a novel
     */
    @FormUrlEncoded
    @POST(value = "v1/novel/bookmark/delete")
    suspend fun deleteBookmarkNovel(
        @Field("illust_id") illustId: Long
    ): Response<Unit>

}