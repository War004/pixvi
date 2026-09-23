package com.cryptic.piyek.core.content.data.model

data class RecommendationNovelPara(
    val includeRanking: Boolean,
    val includePrivacyPolicy: Boolean,
    val readNovelIds: List<Long>,
    val readNovelDateTimes: List<String>,
    val viewNovelIds: List<Long>,
    val viewNovelDataTimes: List<String>,

    ): RecommendationPara
