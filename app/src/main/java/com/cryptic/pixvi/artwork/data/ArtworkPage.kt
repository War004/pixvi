package com.cryptic.pixvi.artwork.data


data class ArtworkPage(
    val rankingArt: List<ArtworkInfo>,
    val recommendedArt: List<ArtworkInfo>,
    val nextUrl: String
)

data class ArtworkInfo(
    val data: ArtworkData,
    val author: Author,
    val extraData: ExtraInfo,
    val pages: List<Page>,
    val tags: List<Tag>,
    val series: Series?,
)

data class Series(
    val id: Int,
    val title: String
)

data class ArtworkData(
    val id: Long,
    val title: String,
    val type: String,
    val caption: String?,
    val restrict: Int,
    val creationDate: Long,
    val totalPage: Int,
    val width: Int,
    val height: Int,

    val totalViews: Int,
    val totalBookmarks: Int,
    val isBookmarked: Boolean,
)

data class Page(
    val quality: Quality
)

data class Quality(
    val squareMedium: String,
    val medium: String,
    val large: String,
    val original: String
)

data class ExtraInfo(
    val sanityLevel: Int,
    val xRestrict: Int,
    val visible: Boolean,
    val isMuted: Boolean,
)

data class Author(
    val authorId: Long,
    val authorName: String,
    val accountName: String,
    val profileImageUrl: String,
    val isFollowedByUser: Boolean,
    val isAcceptRequest: Boolean
)

data class Tag(
    val name: String,
    val translatedName: String? = null
)