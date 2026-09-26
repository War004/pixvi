package com.cryptic.piyek.feature.novel.presentation

import com.cryptic.piyek.core.data.local.BookmarkRestrict

sealed interface HomeNovelScreenActions {
    data object Retry: HomeNovelScreenActions
    data object LoadMore: HomeNovelScreenActions
    data class ToggleBookmark(
        val novelId: Long,
        val isCurrentlyBookmarked: Boolean,
        val restrict: BookmarkRestrict? = null,
    ): HomeNovelScreenActions
    data class PushToDownload(val novelId: Long): HomeNovelScreenActions
    data class PushToSpeechService(val novelId: Long): HomeNovelScreenActions
    data class OpenNovel(val novelId: Long): HomeNovelScreenActions
    data class PressForMoreInfo(val novelId: Long): HomeNovelScreenActions
    data class OpenAuthor(val userId: Long): HomeNovelScreenActions
    data class FilterByTag(val tag: String): HomeNovelScreenActions
    data class CopyFeedback(val message: String): HomeNovelScreenActions
}
