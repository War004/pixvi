package com.cryptic.piyek.core.content.data.model

data class RecommendationNonNovelPara(
    val filter: String = "for_android",
    val includeRanking: Boolean,
    val includePrivacyPolicy: Boolean
): RecommendationPara
