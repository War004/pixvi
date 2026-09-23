package com.cryptic.piyek.core.content.novel.domain.model

data class NovelHome(
    val novels: List<NovelDomain>,
    val nextUrl: String,
    val rankingNovels: List<NovelDomain>,
    val privacyPolicy: PrivacyPolicyDomain?
)

data class NovelDomain(
    val id: Long,
    val title: String,
    val caption: String,
    val restrict: Int,
    val xRestrict: Int,
    val isOriginal: Boolean,
    val imageQuality: NovelImageQuality,
    val createDate: String,
    val tags: List<NovelTagDomain>,
    val pageCount: Int,
    val textLength: Int,
    val user: NovelUserDomain?,
    val series: NovelSeriesDomain?,
    val isBookmarked: Boolean,
    val totalBookmarks: Int,
    val totalViews: Int,
    val totalComments: Int,
    val visible: Boolean,
    val isMypixivOnly: Boolean,
    val isMuted: Boolean
)

data class NovelImageQuality(
    val square: String,
    val medium: String,
    val large: String
)

data class NovelUserDomain(
    val userId: Long,
    val userName: String,
    val userAccount: String,
    val profileImageUrl: String,
    val isFollowed: Boolean
)

data class NovelSeriesDomain(
    val id: Long,
    val title: String
)

data class NovelTagDomain(
    val name: String,
    val translatedName: String?,
    val addedByUploadedUser: Boolean
)

data class PrivacyPolicyDomain(
    val version: String,
    val message: String,
    val url: String
)