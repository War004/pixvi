package com.cryptic.piyek.feature.novel.presentation

import androidx.compose.runtime.Immutable
import com.cryptic.piyek.core.content.novel.domain.model.NovelDomain
import java.util.Locale

@Immutable
data class NovelCardUi(
    val id: Long,
    val title: String,
    val caption: String,
    val authorName: String,
    val authorId: Long,
    val authorProfilePicUrl: String,
    val coverImageUrl: String,
    val totalPages: Int,
    val totalWords: Long,
    val formattedViews: String,
    val formattedLikes: String,
    val formattedComments: String,
    val formattedPages: String,
    val formattedWords: String,
    val tags: List<String>,
    val seriesTitle: String? = null,
    val isBookmarked: Boolean = false,
    val isOriginal: Boolean = false
)

fun NovelDomain.toNovelCardUi(): NovelCardUi {
    val nonNegativeWords = textLength.coerceAtLeast(0)
    return NovelCardUi(
        id = id,
        title = title,
        caption = caption,
        authorName = user?.userName.orEmpty(),
        authorId = user?.userId ?: 0L,
        authorProfilePicUrl = user?.profileImageUrl.orEmpty(),
        coverImageUrl = imageQuality.large,
        totalPages = pageCount,
        totalWords = textLength.toLong(),
        formattedViews = formatMetricCount(totalViews),
        formattedLikes = formatMetricCount(totalBookmarks),
        formattedComments = formatMetricCount(totalComments),
        formattedPages = "$pageCount Pages",
        formattedWords = "${formatMetricCount(nonNegativeWords)} Words",
        tags = tags.map { it.name },
        seriesTitle = series?.title,
        isBookmarked = isBookmarked,
        isOriginal = isOriginal
    )
}

private fun formatMetricCount(count: Int): String {
    val nonNegative = count.coerceAtLeast(0)
    return when {
        nonNegative >= 1_000_000 -> String.format(Locale.US, "%.1fM", nonNegative / 1_000_000f)
        nonNegative >= 1_000 -> String.format(Locale.US, "%.1fK", nonNegative / 1_000f)
        else -> nonNegative.toString()
    }
}

@Immutable
data class HomeNovelUiState(
    val status: ScreenState = ScreenState.Loading,
    val novelList: List<NovelCardUi> = emptyList(),
    val rankingList: List<NovelCardUi> = emptyList(),
    val errorMessage: String? = null
)

sealed interface ScreenState {
    data object Idle : ScreenState
    data object Loading : ScreenState
    data object LoadingMore : ScreenState
}
