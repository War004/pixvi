package com.cryptic.piyek.core.content.data.model

import com.cryptic.piyek.feature.onboarding.domain.model.ArtworkType

data class Artwork(
    val id: Long,
    val title: String,
    val caption: String,
    val user: User,
    val type: ArtworkType,
    val quality: ArtworkQuality,
    val createDate: String,
    val seasonalEffectAnimationUrls: String?,
    val illustAiType: Int,
    val illustBookStyle: Int,
    val isBookmarked: Boolean,
    val isMuted: Boolean,
    val pageCount: Int,
    //add request later
    val restrict: Int,
    val restrictionAttributes: List<String>,
    val sanityLevel: Int,
    val series: Series?, //custom
    val tags: List<Tag>, //custom
    val tools: List<String>,
    val totalBookmarks: Int,
    val totalViews: Int,
    val visible: Boolean,
    val xRestrict: Int,
    //custom data to be used internally by the app not provided via the api
    val currentIndex: Int = 0
)

data class Tag(
    val name: String,
    val translatedName: String?
)
data class Series(
    val id: Long,
    val title: String
)

data class User(
    val userId: Int,
    val userProfile: String,
    val userProfileImageUrlHighest: String
)

data class ImageQuality(
    val original: String,
    val high: String,
    val medium: String,
    val square: String
)

data class ArtworkQuality(
    val firstPage: ImageQuality,
    val all: List<ImageQuality>
)