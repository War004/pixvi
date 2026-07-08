package com.cryptic.piyek.core.content.data.remote

import com.cryptic.piyek.core.content.data.model.ArtworkQuality
import com.cryptic.piyek.feature.onboarding.domain.model.ArtworkType
import com.cryptic.piyek.core.content.data.model.ImageQuality
import com.cryptic.piyek.core.content.data.model.User
import com.cryptic.piyek.core.content.data.model.Artwork
import com.cryptic.piyek.core.content.data.model.Series
import com.cryptic.piyek.core.content.data.model.Tag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ArtworkResponse(
    @SerialName("id") val id: Long, // Use Long for database/network IDs to prevent integer overflow
    @SerialName("title") val title: String,
    @SerialName("type") val type: String,
    @SerialName("image_urls") val imageUrls: ImageUrlsDto,
    @SerialName("caption") val caption: String,
    @SerialName("restrict") val restrict: Int,
    @SerialName("user") val user: UserDto,
    @SerialName("tags") val tags: List<TagDto>,
    @SerialName("tools") val tools: List<String>,
    @SerialName("create_date") val createDate: String,
    @SerialName("page_count") val pageCount: Int,
    @SerialName("width") val width: Int,
    @SerialName("height") val height: Int,
    @SerialName("sanity_level") val sanityLevel: Int,
    @SerialName("x_restrict") val xRestrict: Int,
    @SerialName("series") val series: SeriesDto? = null,
    @SerialName("meta_single_page") val metaSinglePage: MetaSinglePageDto,
    @SerialName("meta_pages") val metaPages: List<MetaPageDto> = emptyList(),
    //the server will return empty value for meta_single_page if the number of pages are more than one,
    //if the page are just one, then the list is empty for the meta_pages
    @SerialName("total_view") val totalView: Int,
    @SerialName("total_bookmarks") val totalBookmarks: Int,
    @SerialName("is_bookmarked") val isBookmarked: Boolean,
    @SerialName("visible") val visible: Boolean,
    @SerialName("is_muted") val isMuted: Boolean,
    @SerialName("seasonal_effect_animation_urls") val seasonalEffectAnimationUrls: String? = null,
    @SerialName("event_banners") val eventBanners: JsonElement? = null,
    @SerialName("illust_ai_type") val illustAiType: Int,
    @SerialName("request") val request: Request? = null,
    @SerialName("illust_book_style") val illustBookStyle: Int,
    @SerialName("restriction_attributes") val restrictionAttributes: List<String> = emptyList()
)

@Serializable
data class Request(
    @SerialName("request_info")
    val requestInfo: RequestInfo,
    @SerialName("request_users")
    val requestUsers: List<RequestUser>
)

@Serializable
data class RequestInfo(
    @SerialName("collaborate_status")
    val collaborateStatus: CollaborateStatus,
    @SerialName("fan_user_id")
    val fanUserId: Long?,
    val role: String
)
@Serializable
data class CollaborateStatus(
    @SerialName("collaborate_anonymous_flag")
    val collaborateAnonymousFlag: Boolean,
    @SerialName("collaborate_user_samples")
    val collaborateUserSamples: List<String>,
    val collaborating: Boolean
)
@Serializable
data class RequestUser(
    val account: String,
    val id: Int,
    @SerialName("is_accept_request")
    val isAcceptRequest: Boolean,
    @SerialName("is_access_blocking_user")
    val isAccessBlockingUser: Boolean,
    @SerialName("is_followed")
    val isFollowed: Boolean,
    val name: String,
    @SerialName("profile_image_urls")
    val profileImageUrls: ProfileImageUrls
)

@Serializable
data class ProfileImageUrls(
    val medium: String
)

@Serializable
data class ImageUrlsDto(
    @SerialName("square_medium") val squareMedium: String,
    @SerialName("medium") val medium: String,
    @SerialName("large") val large: String
)

@Serializable
data class MetaPageDto(
    @SerialName("image_urls") val imageUrl: MetaPageImageUrls
)
@Serializable
data class MetaPageImageUrls(
    @SerialName("square_medium") val squareMedium: String,
    @SerialName("medium") val medium: String,
    @SerialName("large") val large: String,
    @SerialName("original") val original: String
)
@Serializable
data class UserDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("account") val account: String,
    @SerialName("profile_image_urls") val profileImageUrls: ProfileImageUrlsDto,
    @SerialName("is_followed") val isFollowed: Boolean,
    @SerialName("is_accept_request") val isAcceptRequest: Boolean
)

@Serializable
data class ProfileImageUrlsDto(
    @SerialName("medium") val medium: String
)

@Serializable
data class TagDto(
    @SerialName("name") val name: String,
    @SerialName("translated_name") val translatedName: String? = null
)

@Serializable
data class SeriesDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String
)

@Serializable
data class MetaSinglePageDto(
    @SerialName("original_image_url") val originalImageUrl: String? = null
)


fun ArtworkResponse.toDomain(): Artwork {
    // 1. Map the User
    val domainUser = User(
        userId = this.user.id.toInt(),
        userProfile = this.user.name,
        userProfileImageUrlHighest = this.user.profileImageUrls.medium
    )

    // 2. Reusable single-page quality (also used as fallback)
    val singlePageQuality = ImageQuality(
        original = this.metaSinglePage.originalImageUrl ?: "",
        high = this.imageUrls.large,
        medium = this.imageUrls.medium,
        square = this.imageUrls.squareMedium
    )

    // 3. Map Image Qualities based on page count
    val imageQualities = if (this.pageCount == 1) {
        listOf(singlePageQuality)
    } else {
        this.metaPages.map { metaPage ->
            ImageQuality(
                original = metaPage.imageUrl.original,
                high = metaPage.imageUrl.large,
                medium = metaPage.imageUrl.medium,
                square = metaPage.imageUrl.squareMedium
            )
        }.ifEmpty { listOf(singlePageQuality) } // Fallback for malformed API response
    }

    // 4. Map the Type
    val mappedType = when (this.type) {
        "illust" -> ArtworkType.ILLUST
        "manga" -> ArtworkType.MANGA
        else -> ArtworkType.UNKNOWN
    }

    val series = this.series?.let {
        Series(id = it.id, title = it.title)
    }

    val tags = this.tags.map {
        Tag(name = it.name, translatedName = it.translatedName)
    }

    // 5. Construct the final Domain Model
    return Artwork(
        id = this.id,
        title = this.title,
        caption = this.caption,
        user = domainUser,
        type = mappedType,
        quality = ArtworkQuality(
            firstPage = imageQualities.first(),
            all = imageQualities
        ),
        createDate = this.createDate,
        seasonalEffectAnimationUrls = this.seasonalEffectAnimationUrls,
        illustAiType = this.illustAiType,
        illustBookStyle = this.illustBookStyle,
        isBookmarked = this.isBookmarked,
        isMuted = this.isMuted,
        pageCount = this.pageCount,
        restrict = this.restrict,
        restrictionAttributes = this.restrictionAttributes,
        sanityLevel = this.sanityLevel,
        series = series,
        tags = tags,
        tools = this.tools,
        totalBookmarks = this.totalBookmarks,
        totalViews = this.totalBookmarks,
        visible = this.visible,
        xRestrict = this.xRestrict,
    )
}