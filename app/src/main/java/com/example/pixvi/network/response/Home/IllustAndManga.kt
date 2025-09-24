package com.example.pixvi.network.response.Home

import kotlinx.serialization.Serializable

data class basePost(
    val contest_exists: Boolean?,
    val illusts: List<Illust>,
    val next_url: String,
    val privacy_policy: PrivacyPolicy,
    val ranking_illusts: List<Illust>
)

@Serializable
data class CollaborateStatus(
    val collaborate_anonymous_flag: Boolean,
    val collaborate_user_samples: List<String>,
    val collaborating: Boolean
)

@Serializable
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
    val request: Request?,  //Any?
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

@Serializable
data class Series(
    val id: Int,
    val title: String
)

@Serializable
data class ImageUrls(
    val large: String,
    val medium: String,
    val square_medium: String
)

@Serializable
data class ImageUrlsX(
    val large: String,
    val medium: String,
    val original: String,
    val square_medium: String
)

@Serializable
data class MetaPage(
    val image_urls: ImageUrlsX
)

//waifu
@Serializable
data class MetaSinglePage(
    val original_image_url: String?
)

@Serializable
data class PrivacyPolicy(
    val message: String,
    val version: String
)

@Serializable
data class ProfileImageUrls(
    val medium: String
)

@Serializable
data class Request(
    val request_info: RequestInfo,
    val request_users: List<RequestUser>
)

@Serializable
data class RequestInfo(
    val collaborate_status: CollaborateStatus,
    val fan_user_id: Int,
    val role: String
)


@Serializable
data class RequestUser(
    val account: String,
    val id: Int,
    val is_accept_request: Boolean,
    val is_access_blocking_user: Boolean,
    val is_followed: Boolean,
    val name: String,
    val profile_image_urls: ProfileImageUrls
)

@Serializable
data class Tag(
    val name: String,
    val translated_name: String?
)

@Serializable
data class User(
    val account: String,
    val id: Int,
    val is_accept_request: Boolean,
    val is_followed: Boolean,
    val name: String,
    val profile_image_urls: ProfileImageUrls
)