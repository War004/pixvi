package com.cryptic.piyek.core.content.illust.domain.model

/*

 */
data class ILLustMangaRecommendation(
    val title: String,
    val createData: String,
    val height: Int,
    val width: Int,
    val id: Long,
    val aiUsage: Int,
    val bookStyle: Int,
    val pageCount: Int,
    val firstPageUrls: PostImageUrls,
    val allPagesUrls: List<PostImageUrls>,
    val isBookmarked: Boolean,
    val isMuted: Boolean,
    val restrict: Int,
    val tags: List<Tag>,
    val visible: Boolean,
    val xRestrict: Int,
)

data class Series(
    val id: Long,
    val title: String
)

data class Tag(
    val name: String,
    val translatedName: String
)

data class PostImageUrls(
    val original: String,
    val high: String,
    val medium: String,
    val squareMedium: String
)

data class Author(
    val accountName: String,
    val nickName: String,
    val id: Long,
    val isTakingCustomWork: Boolean,
    val isFollowed: Boolean,
    val profilePicture: ProfileImageUrls
)

data class ProfileImageUrls(
    val medium: String
)