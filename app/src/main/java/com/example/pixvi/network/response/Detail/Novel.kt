package com.example.pixvi.network.response.Detail

import android.text.SpannableString
import androidx.compose.ui.text.AnnotatedString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


/**
 * The root object of the JSON payload embedded in the HTML.
 * It contains the main novel data, author details, and other metadata.
 */
@Serializable
data class PixivNovelResponse(
    @SerialName("novel")
    val novel: NovelData,

    @SerialName("authorDetails")
    val authorDetails: AuthorDetails,
)

/**
 * Contains all the core information about the novel itself.
 */
@Serializable
data class NovelData(
    val id: String,
    val title: String,
    val caption: String, // The novel's description, may contain HTML. //will always have html
    val text: String,    // The main body of the novel. Contains Pixiv-specific markup.

    @SerialName("userId")
    val userId: String,

    @SerialName("coverUrl")
    val coverUrl: String,

    @SerialName("cdate")
    val creationDate: String,

    val tags: List<String>,
    val rating: Rating,

    @SerialName("seriesId")
    val seriesId: String? = null,

    @SerialName("seriesTitle")
    val seriesTitle: String? = null,

    val illusts: JsonElement,

    @SerialName("seriesNavigation")
    val seriesNavigation: SeriesNavigation? = null,

    @SerialName("aiType")
    val aiType: Int, // 0 for "No", 1 for "Yes"
)

/**
 * Represents a single tag associated with the novel.
 */
@Serializable
data class Tag(
    val tag: String,
    val userId: String? = null, // Can be null
)

/**
 * Contains view counts and engagement metrics.
 */
@Serializable
data class Rating(
    val like: Int,
    val bookmark: Int,
    val view: Int
)

/**
 * Contains links to the next and previous novels in a series.
 */
@Serializable
data class SeriesNavigation(
    @SerialName("nextNovel")
    val nextNovel: NavigationNovel? = null,

    @SerialName("prevNovel")
    val prevNovel: NavigationNovel? = null,
)

/**
 * A simplified model for a novel in a series navigation link.
 */
@Serializable
data class NavigationNovel(
    val id: Int,
    val title: String,
    @SerialName("coverUrl")
    val coverUrl: String
)

/**
 * Represents a single illustration entry in the `illusts` map.
 */
@Serializable
data class IllustData(
    val visible: Boolean,

    @SerialName("illust")
    val details: IllustDetails,

    @SerialName("user")
    val user: IllustUserData,
)

/**
 * Contains the specific details of the illustration, like its title and image URLs.
 */
@Serializable
data class IllustDetails(
    val title: String,
    val description: String,
    val images: IllustImages,
    val tags: List<Tag>,

    @SerialName("sl")
    val serviceLevel: Int, // Unknown purpose, but good to have.
)

/**
 * URLs for the illustration in different sizes.
 */
@Serializable
data class IllustImages(
    val small: String? = null,
    val medium: String? = null,
    val original: String? = null
)

/**
 * Represents the user who created the embedded illustration.
 */
@Serializable
data class IllustUserData(
    val id: String,
    val name: String,
    val image: String,
)
/**
 * Contains information about the author of the novel.
 */
@Serializable
data class AuthorDetails(
    @SerialName("userId")
    val userId: Long,

    @SerialName("userName")
    val userName: String,

    @SerialName("isFollowed")
    val isFollowed: Boolean,

    @SerialName("profileImg")
    val profileImage: ProfileImage,
)

/**
 * Holds the URL for the author's profile image.
 */
@Serializable
data class ProfileImage(
    val url: String
)