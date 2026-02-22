package com.cryptic.pixvi.core.network.model.artwork

import com.cryptic.pixvi.artwork.data.ArtworkData
import com.cryptic.pixvi.artwork.data.ArtworkInfo
import com.cryptic.pixvi.artwork.data.ArtworkPage
import com.cryptic.pixvi.artwork.data.Author
import com.cryptic.pixvi.artwork.data.ExtraInfo
import com.cryptic.pixvi.artwork.data.Page
import com.cryptic.pixvi.artwork.data.Quality
import com.cryptic.pixvi.artwork.data.Series
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Serializable
data class ArtworkPageResponse(
    @SerialName("illusts")
    val artwork: List<Artwork>,
    @SerialName("contest_exists")
    val contentExists: Boolean,
    @SerialName("next_url")
    val nextUrl: String, //do null
    @SerialName("privacy_policy")
    val privacyPolicy: PrivacyPolicy? = null,
    @SerialName("ranking_illusts")
    val rankingArtwork: List<Artwork> = emptyList()
)

@Serializable
data class Artwork(
    val id: Long,
    val title: String,
    val type: String,
    @SerialName("image_urls")
    val imageUrls: ImageUrls,
    val caption:String,
    val restrict: Int,
    val user: User,
    val tags: List<Tag> = emptyList(),
    val tools: List<String> = emptyList(),
    @SerialName("create_date")
    val createDate: String,
    @SerialName("page_count")
    val pageCount: Int,
    val width: Int,
    val height: Int,
    @SerialName("sanity_level")
    val sanityLevel: Int,
    @SerialName("x_restrict")
    val xRestrict: Int,
    val series: com.cryptic.pixvi.core.network.model.artwork.Series? = null,
    @SerialName("meta_single_page")
    val metaSinglePage: MetaSinglePage,
    @SerialName("meta_pages")
    val metaPages: List<MetaPage> = emptyList(),
    @SerialName("total_view")
    val totalViews: Int,
    @SerialName("total_bookmarks")
    val totalBookmarks: Int,
    @SerialName("is_bookmarked")
    val isBookmarked: Boolean,
    val visible: Boolean,
    @SerialName("is_muted")
    val isMuted: Boolean,
    @SerialName("seasonal_effect_animation_urls")
    val animationEffectUrls: String? = null,
    @SerialName("event_banners")
    val eventBanners: String? =null,
    @SerialName("illust_ai_type")
    val illustAiType: Int,
    @SerialName("illust_book_style")
    val illustBookStyle: Int,
    val request: Request? = null,
    @SerialName("restriction_attributes")
    val restrictionAttributes: List<String>? = null,
)

@Serializable
data class ImageUrls(
    val large: String,
    val medium: String,
    @SerialName("square_medium")
    val squareMedium: String
)

@Serializable
data class User(
    val id: Long,
    val name: String,
    //Usage of account is unknown
    val account: String,
    @SerialName("profile_image_urls")
    val profileImageUrls: ProfileImageUrls,
    @SerialName("is_followed")
    val isFollowed: Boolean,
    @SerialName("is_accept_request")
    val isAcceptRequest: Boolean,
    @SerialName("restriction_attributes")
    val restrictionAttributes: List<String>? = null
)
@Serializable
data class ProfileImageUrls(
    val medium: String
)

@Serializable
data class Tag(
    val name: String,
    @SerialName("translated_name")
    val translatedName: String? = null
)

@Serializable
data class Series(
    val id: Int,
    val title: String
)


@Serializable
data class MetaSinglePage(
    @SerialName("original_image_url")
    val originalImageUrl: String?=null
)

@Serializable
data class MetaPage(
    @SerialName("image_urls")
    val imageUrls: ImageUrlsX
)
@Serializable
data class ImageUrlsX(
    val large: String,
    val medium: String,
    val original: String,
    @SerialName("square_medium")
    val squareMedium: String
)

@Serializable
data class PrivacyPolicy(
    val message: String? = null,
    val version: String? = null,
    val url: String? = null,
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
data class CollaborateStatus(
    @SerialName("collaborate_anonymous_flag")
    val collaborateAnonymousFlag: Boolean,
    @SerialName("collaborate_user_samples")
    val collaborateUserSamples: List<String>,
    val collaborating: Boolean
)

private fun Artwork.toArtworkInfo(): ArtworkInfo {
    val parsedDate: Long = try {
        ZonedDateTime.parse(this.createDate).toEpochSecond() * 1000
    } catch (e: Exception) {
        0L
    }

    val mappedPages = if (this.pageCount == 1) {
        listOf(
            Page(
                quality = Quality(
                    squareMedium = this.imageUrls.squareMedium,
                    medium = this.imageUrls.medium,
                    large = this.imageUrls.large,
                    // metaSinglePage contains the high-res original for single posts
                    original = this.metaSinglePage.originalImageUrl!! //for only one page this will exist
                )
            )
        )
    } else {
        this.metaPages.map { metaPage ->
            Page(
                quality = Quality(
                    squareMedium = metaPage.imageUrls.squareMedium,
                    medium = metaPage.imageUrls.medium,
                    large = metaPage.imageUrls.large,
                    original = metaPage.imageUrls.original
                )
            )
        }
    }
    return ArtworkInfo(
        data = ArtworkData(
            id = this.id,
            title = this.title,
            type = this.type,
            caption = this.caption,
            restrict = this.restrict,
            creationDate = parsedDate,
            totalPage = this.pageCount,
            width = this.width,
            height = this.height,
            totalViews = this.totalViews,
            totalBookmarks = this.totalBookmarks,
            isBookmarked = this.isBookmarked
        ),
        author = Author(
            authorId = this.user.id,
            authorName = this.user.name,
            accountName = this.user.account,
            profileImageUrl = this.user.profileImageUrls.medium,
            isFollowedByUser = this.user.isFollowed,
            isAcceptRequest = this.user.isAcceptRequest
        ),
        extraData = ExtraInfo(
            sanityLevel = this.sanityLevel,
            xRestrict = this.xRestrict,
            visible = this.visible,
            isMuted = this.isMuted
        ),
        pages = mappedPages,
        tags = this.tags.map { tag ->
            com.cryptic.pixvi.artwork.data.Tag(
                name = tag.name,
                translatedName = tag.translatedName
            )
        },
        series = this.series?.let { series ->
            Series(
                id = series.id,
                title = series.title
            )
        }
    )
}

// Keep 'suspend' here to allow context switching
suspend fun ArtworkPageResponse.toArtworkPage(): ArtworkPage = withContext(Dispatchers.Default) {

    val mappedArtwork = this@toArtworkPage.artwork.map { it.toArtworkInfo() }
    val mappedRanking = this@toArtworkPage.rankingArtwork.map { it.toArtworkInfo() }

    ArtworkPage(
        rankingArt = mappedRanking,
        recommendedArt = mappedArtwork,
        nextUrl = this@toArtworkPage.nextUrl
    )
}