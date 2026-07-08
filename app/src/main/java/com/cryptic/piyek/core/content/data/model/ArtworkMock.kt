package com.cryptic.piyek.core.content.data.model

import com.cryptic.piyek.feature.onboarding.domain.model.ArtworkType

fun createMockUser(
    userId: Int = (1000..9999).random(),
    userProfile: String = "Senior digital artist specializing in abstract expressionism and futuristic concept art.",
    userProfileImageUrlHighest: String = ArtworkMockData.PROFILE_IMAGES.random()
): User {
    return User(
        userId = userId,
        userProfile = userProfile,
        userProfileImageUrlHighest = userProfileImageUrlHighest
    )
}

fun createMockImageQuality(
    baseId: String = ArtworkMockData.ARTWORK_IDS.random(),
    original: String = "https://images.unsplash.com/$baseId?q=100&w=2048&auto=format&fit=crop",
    high: String = "https://images.unsplash.com/$baseId?q=80&w=1080&auto=format&fit=crop",
    medium: String = "https://images.unsplash.com/$baseId?q=80&w=720&auto=format&fit=crop",
    square: String = "https://images.unsplash.com/$baseId?q=80&w=400&h=400&auto=format&fit=crop"
): ImageQuality {
    return ImageQuality(
        original = original,
        high = high,
        medium = medium,
        square = square
    )
}

fun createMockArtworkQuality(
    firstPage: ImageQuality = createMockImageQuality(),
    all: List<ImageQuality> = listOf(firstPage)
): ArtworkQuality {
    return ArtworkQuality(
        firstPage = firstPage,
        all = all
    )
}

fun createMockSeries(
    id: Long = (10000L..99999L).random(),
    title: String = "Visions of the Void Vol. ${(1..5).random()}"
): Series {
    return Series(id = id, title = title)
}

fun createMockTag(
    name: String = "Abstract",
    translatedName: String? = "アブストラクト"
): Tag {
    return Tag(name = name, translatedName = translatedName)
}

fun createMockArtwork(
    id: Long = (1000000L..9999999L).random(),
    title: String = ArtworkMockData.TITLES.random(),
    caption: String = ArtworkMockData.CAPTIONS.random(),
    user: User = createMockUser(),
    type: ArtworkType = ArtworkType.values().random(),
    quality: ArtworkQuality = createMockArtworkQuality(),
    createDate: String = "2026-07-08T13:00:00Z",
    seasonalEffectAnimationUrls: String? = null,
    illustAiType: Int = 0,
    illustBookStyle: Int = 0,
    isBookmarked: Boolean = (0..1).random() == 1,
    isMuted: Boolean = false,
    pageCount: Int = (1..12).random(),
    restrict: Int = 0,
    restrictionAttributes: List<String> = emptyList(),
    sanityLevel: Int = 2,
    series: Series? = if ((0..1).random() == 1) createMockSeries() else null,
    tags: List<Tag> = ArtworkMockData.randomTags(),
    tools: List<String> = ArtworkMockData.randomTools(),
    totalBookmarks: Int = (500..50000).random(),
    totalViews: Int = (10000..500000).random(),
    visible: Boolean = true,
    xRestrict: Int = 0,
    currentIndex: Int = 0
): Artwork {
    return Artwork(
        id = id,
        title = title,
        caption = caption,
        user = user,
        type = type,
        quality = quality,
        createDate = createDate,
        seasonalEffectAnimationUrls = seasonalEffectAnimationUrls,
        illustAiType = illustAiType,
        illustBookStyle = illustBookStyle,
        isBookmarked = isBookmarked,
        isMuted = isMuted,
        pageCount = pageCount,
        restrict = restrict,
        restrictionAttributes = restrictionAttributes,
        sanityLevel = sanityLevel,
        series = series,
        tags = tags,
        tools = tools,
        totalBookmarks = totalBookmarks,
        totalViews = totalViews,
        visible = visible,
        xRestrict = xRestrict,
        currentIndex = currentIndex
    )
}

// --- LOCALIZED MOCK DATA SOURCE ---

private object ArtworkMockData {
    val PROFILE_IMAGES = listOf(
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=200&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?q=80&w=200&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?q=80&w=200&auto=format&fit=crop"
    )

    val ARTWORK_IDS = listOf(
        "photo-1618005182384-a83a8bd57fbe", // Purple Liquid Abstract
        "photo-1541701494587-cb58502866ab", // Blue/Yellow Abstract Canvas
        "photo-1550684848-fac1c5b4e853", // Dark Fluid Waves
        "photo-1618005198143-e528346430d9", // Symmetrical Color Block
        "photo-1500485035595-cbe6f645feb1"  // Red Splash
    )

    val TITLES = listOf(
        "Neon Resonance",
        "Chromatic Drift",
        "Ethereal Synthesis",
        "Resonance Protocol",
        "Solar Wind",
        "Starlight Sonata",
        "Nebula Conductor II"
    )

    val CAPTIONS = listOf(
        "A study in fluid dynamics, light path refraction, and cosmic lighting constraints.",
        "Exploring the bounds of micro-typography and digital fluid layering.",
        "Brutalist structural geometry intersecting with traditional organic oil flow.",
        "A piece representing computational balance, tension, and structural gravity."
    )

    val TAG_POOL = listOf(
        Tag("Digital Art", "デジタルアート"),
        Tag("Abstract", "抽象画"),
        Tag("Concept Art", "コンセプトアート"),
        Tag("Sci-Fi", "サイファイ"),
        Tag("Yuri", "百合"),
        Tag("Cyberpunk", "サイバーパンク"),
        Tag("Vaporwave", "ヴェイパーウェア")
    )

    val TOOL_POOL = listOf(
        "Procreate",
        "Clip Studio Paint",
        "Photoshop CC",
        "Blender 3D",
        "Figma"
    )

    fun randomTags(): List<Tag> = TAG_POOL.shuffled().take((1..3).random())
    fun randomTools(): List<String> = TOOL_POOL.shuffled().take((1..2).random())
}