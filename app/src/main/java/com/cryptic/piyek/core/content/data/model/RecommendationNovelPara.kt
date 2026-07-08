package com.cryptic.piyek.core.content.data.model

data class RecommendationNovelPara(
    val includeRanking: Boolean,
    val includePrivacyPolicy: Boolean,
    val viewNovelIds: List<Long>,
    val viewNovelDatatimes: List<String>
): RecommendationPara
