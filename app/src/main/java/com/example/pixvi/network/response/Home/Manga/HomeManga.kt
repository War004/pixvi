package com.example.pixvi.network.response.Home.Manga

/**
 * Response that we expect while loading the home page under the Manga filter
 *  url -> https://app-api.pixiv.net/v1/manga/recommended?filter=for_android&include_ranking_illusts=true&include_privacy_policy=true
 *  get response
 */

data class HomeManga(
    val illusts: List<Illust>,
    val next_url: String,
    val privacy_policy: PrivacyPolicy,
    val ranking_illusts: List<RankingIllust>
)

data class Illust(
    val caption: String,
    val create_date: String,
    val height: Int,
    val id: Int,
    val illust_ai_type: Int,
    val illust_book_style: Int,
    val image_urls: ImageUrls,
    val is_bookmarked: Boolean,
    val is_muted: Boolean,
    val meta_pages: List<MetaPage>,
    val meta_single_page: MetaSinglePage,
    val page_count: Int,
    val request: Any?,
    val restrict: Int,
    val restriction_attributes: List<String>?,
    val sanity_level: Int,
    val series: Series?,
    val tags: List<Tag>,
    val title: String,
    val tools: List<String>,
    val total_bookmarks: Int,
    val total_view: Int,
    val type: String,
    val user: User,
    val visible: Boolean,
    val width: Int,
    val x_restrict: Int
)

data class ImageUrls(
    val large: String,
    val medium: String,
    val square_medium: String
)

data class ImageUrlsX(
    val large: String,
    val medium: String,
    val original: String,
    val square_medium: String
)

data class MetaPage(
    val image_urls: ImageUrlsX
)

data class MetaSinglePage(
    val original_image_url: String
)

data class PrivacyPolicy(
    val message: String,
    val version: String
)

data class ProfileImageUrls(
    val medium: String
)

data class RankingIllust(
    val caption: String,
    val create_date: String,
    val height: Int,
    val id: Int,
    val illust_ai_type: Int,
    val illust_book_style: Int,
    val image_urls: ImageUrls,
    val is_bookmarked: Boolean,
    val is_muted: Boolean,
    val meta_pages: List<MetaPage>,
    val meta_single_page: MetaSinglePage,
    val page_count: Int,
    val request: Any,
    val restrict: Int,
    val sanity_level: Int,
    val series: Series?,
    val tags: List<Tag>,
    val title: String,
    val tools: List<String>,
    val total_bookmarks: Int,
    val total_view: Int,
    val type: String,
    val user: User,
    val visible: Boolean,
    val width: Int,
    val x_restrict: Int
)

data class Series(
    val id: Int,
    val title: String
)

data class Tag(
    val name: String,
    val translated_name: String?
)

data class User(
    val account: String,
    val id: Int,
    val is_accept_request: Boolean,
    val is_followed: Boolean,
    val name: String,
    val profile_image_urls: ProfileImageUrls
)