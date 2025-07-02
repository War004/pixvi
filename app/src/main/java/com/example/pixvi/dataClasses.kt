package com.example.pixvi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Data classes for the request for the homepage.
@Serializable
data class policy(
    val version: String = "0",
    val message: String = "Empty"
)
@Serializable
data class image_url(
    val square_medium: String,
    val medium: String,
    val large: String,
)
@Serializable
data class withFilter(
    val illusts: List<illust>,
    val ranking_illusts: List<illust>,
    val contest_exists: Boolean,
    val privacy_policy: policy?,
    val next_url: String
)

@Serializable
data class userInfo(
    val id: Int,
    val name: String,
    val account: String,
    val profile_image_urls: pff_url,
    val is_followed: Boolean
)

@Serializable
data class pff_url(
    val medium: String
)
@Serializable
data class illust(
    val id: Int,
    val title: String,
    val type: String,
    val image_urls: image_url,
    val caption: String?,
    val restrict: Int,
    val user: userInfo,
    val tags: List<Tag>?, //overide it
    val tools: List<String>?,
    val create_date: String?,
    val page_count: Int,
    val width: Int,
    val height: Int,
    val sanity_level: Int,
    val x_restrict: Int,
    val series: serie? = null,
    val meta_single_page: MetaSinglePage? = null,
    val meta_pages: List<MetaPage>? = null,
    val total_view: Int,
    val total_bookmarks: Int,
    val is_bookmarked: Boolean,
    val visible: Boolean,
    val is_muted: Boolean,
    val illust_ai_type: Int,
    val illust_book_style: Int
)

@Serializable
data class serie(
    /*To be implemented*/
    val no: String = ""
)

@Serializable
data class Tag(

    val name: String,
    val translated_name: String? = null,
    @SerialName("is_emphasized") val isEmphasized: Boolean? = null,
    @SerialName("tagged_count") val taggedCount: Int? = null
)

@Serializable
data class MetaSinglePage(
    @SerialName("original_image_url") val originalImageUrl: String? = null
)

@Serializable
data class MetaPage(
    val image_urls: ImageUrls
)

@Serializable
data class ImageUrls(
    @SerialName("square_medium") val squareMedium: String,
    val medium: String,
    val large: String,
    val original: String? = null
)
//End

/*
For the parameters for making api request
 */
data class PixivParameters(
    val acceptLanguage: String = "en_US",
    val appAcceptLanguage: String = "en",
    val appOS: String = "android",
    val appOSVersion: String = "9",
    val appVersion: String = "6.137.0",
    val authorization: String,
    val userAgent: String = "PixivAndroidApp/6.137.0 (Android 9; SM-S908E)",
    val xClientHash: String = "33cbc7ff0ee3f39c8c35b7d4899d3f60"
)
/**
 * A sealed class representing the different states of a network request to Pixiv API.
 * Using a sealed class for result handling provides type safety and forces handling
 * of all possible states in when expressions.
 */
sealed class PixivResult<out T> {
    /**
     * Represents a successful API response with data.
     * @param data The parsed data returned from the API.
     */
    data class Success<out T>(val data: T) : PixivResult<T>()

    /**
     * Represents an error state with additional context about what went wrong.
     * @param exception The exception that caused the error, if any.
     * @param message A user-friendly error message.
     * @param code The HTTP status code or custom error code, if available.
     */
    data class Error(
        val exception: Throwable? = null,
        val message: String? = null,
        val code: Int? = null
    ) : PixivResult<Nothing>()

    /**
     * Represents a loading state when the request is in progress.
     */
    object Loading : PixivResult<Nothing>()
}