package com.cryptic.piyek.core.content.novel.data.model

import com.cryptic.piyek.core.content.novel.domain.model.NovelDomain
import com.cryptic.piyek.core.content.novel.domain.model.NovelHome
import com.cryptic.piyek.core.content.novel.domain.model.NovelImageQuality
import com.cryptic.piyek.core.content.novel.domain.model.NovelSeriesDomain
import com.cryptic.piyek.core.content.novel.domain.model.NovelTagDomain
import com.cryptic.piyek.core.content.novel.domain.model.NovelUserDomain
import com.cryptic.piyek.core.content.novel.domain.model.PrivacyPolicyDomain
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NovelHomeResponse(
    @SerialName("novels")
    val novels: List<Novel> = emptyList(),
    @SerialName("next_url")
    val nextUrl: String? = null,
    @SerialName("ranking_novels")
    val rankingNovels: List<Novel> = emptyList(),
    @SerialName("privacy_policy")
    val privacyPolicy: PrivacyPolicy? = null
)

@Serializable
data class Novel(
    @SerialName("id")
    val id: Long,
    @SerialName("title")
    val title: String,
    @SerialName("caption")
    val caption: String,
    @SerialName("restrict")
    val restrict: Int,
    @SerialName("x_restrict")
    val xRestrict: Int,
    @SerialName("is_original")
    val isOriginal: Boolean,
    @SerialName("image_urls")
    val imageUrls: ImageUrls,
    @SerialName("create_date")
    val createDate: String,
    @SerialName("tags")
    val tags: List<NovelTag> = emptyList(),
    @SerialName("page_count")
    val pageCount:Int,
    @SerialName("text_length")
    val textLength: Int,
    @SerialName("user")
    val user: NovelUser,
    @SerialName("series")
    val series: NovelSeries? = null,
    @SerialName("is_bookmarked")
    val isBookmarked: Boolean,
    @SerialName("total_bookmarks")
    val totalBookmarks: Int,
    @SerialName("total_view")
    val totalView: Int,
    @SerialName("total_comments")
    val totalComments: Int,
    @SerialName("visible")
    val visible: Boolean,
    @SerialName("is_mypixiv_only")
    val isMyPixivOnly: Boolean,
    @SerialName("is_muted")
    val isMuted: Boolean
)

@Serializable
data class NovelUser(
    @SerialName("id")
    val id: Long,
    @SerialName("name")
    val name: String,
    @SerialName("account")
    val account: String,
    @SerialName("profile_image_urls")
    val profileImageUrls: ProfileImageUrls,
    @SerialName("is_followed")
    val isFollowed: Boolean
)

@Serializable
data class NovelSeries(
    @SerialName("id")
    val id: Long,
    @SerialName("title")
    val title: String
)

@Serializable
data class NovelTag(
    @SerialName("name")
    val name: String,
    @SerialName("translated_name")
    val translatedName: String? = null,
    @SerialName("added_by_uploaded_user")
    val addedByUploadedUser: Boolean
)

@Serializable
data class ImageUrls(
    @SerialName("square_medium")
    val squareMedium: String,
    @SerialName("medium")
    val medium: String,
    @SerialName("large")
    val large: String
)

@Serializable
data class ProfileImageUrls(
    @SerialName("medium")
    val medium: String
)

@Serializable
data class PrivacyPolicy(
    @SerialName("version")
    val version: String? = null,
    @SerialName("message")
    val message: String? = null,
    @SerialName("url")
    val url: String? = null
)


fun NovelHomeResponse.toDomain(): NovelHome {
    return NovelHome(
        novels = this.novels.map { it.toDomain() },
        nextUrl = this.nextUrl?:"", //warning
        rankingNovels = this.rankingNovels.map { it.toDomain() },
        privacyPolicy = this.privacyPolicy?.toDomain()
    )
}

fun Novel.toDomain(): NovelDomain {
    // 1. Map the User
    val domainUser = this.user.toDomain()

    // 2. Map Image Qualities
    val domainImageQuality = this.imageUrls.toDomain()

    // 3. Map Series
    val domainSeries = this.series?.toDomain()

    // 4. Map Tags
    val domainTags: List<NovelTagDomain> = this.tags.map { it.toDomain() }

    // 5. Construct Final Domain Model
    return NovelDomain(
        id = this.id,
        title = this.title,
        caption = this.caption,
        restrict = this.restrict,
        xRestrict = this.xRestrict,
        isOriginal = this.isOriginal,
        imageQuality = domainImageQuality,
        createDate = this.createDate,
        tags = domainTags,
        pageCount = this.pageCount,
        textLength = this.textLength,
        user = domainUser,
        series = domainSeries,
        isBookmarked = this.isBookmarked,
        totalBookmarks = this.totalBookmarks,
        totalViews = this.totalView,
        totalComments = this.totalComments,
        visible = this.visible,
        isMypixivOnly = this.isMyPixivOnly,
        isMuted = this.isMuted
    )
}

fun NovelUser.toDomain(): NovelUserDomain {
    return NovelUserDomain(
        userId = this.id,
        userName = this.name,
        userAccount = this.account,
        profileImageUrl = this.profileImageUrls?.medium.orEmpty(),
        isFollowed = this.isFollowed
    )
}

fun ImageUrls?.toDomain(): NovelImageQuality {
    return NovelImageQuality(
        square = this?.squareMedium.orEmpty(),
        medium = this?.medium.orEmpty(),
        large = this?.large.orEmpty()
    )
}

fun NovelSeries.toDomain(): NovelSeriesDomain {
    return NovelSeriesDomain(
        id = this.id,
        title = this.title
    )
}

fun NovelTag.toDomain(): NovelTagDomain {
    return NovelTagDomain(
        name = this.name,
        translatedName = this.translatedName,
        addedByUploadedUser = this.addedByUploadedUser
    )
}

fun PrivacyPolicy.toDomain(): PrivacyPolicyDomain {
    return PrivacyPolicyDomain(
        version = this.version.orEmpty(),
        message = this.message.orEmpty(),
        url = this.url.orEmpty()
    )
}