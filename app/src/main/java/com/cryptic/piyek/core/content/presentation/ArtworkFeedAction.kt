package com.cryptic.piyek.core.content.presentation

import com.cryptic.piyek.core.content.data.model.Artwork
import com.cryptic.piyek.core.data.local.BookmarkRestrict

sealed interface ArtworkFeedAction {
    /** Initial data load / retry after error. */
    data object Retry : ArtworkFeedAction

    /** Triggered when the user scrolls near the end of the feed. */
    data object LoadMore : ArtworkFeedAction

    /** Scroll-driven: the artwork closest to the viewport center changed. */
    data class UpdateFocusedIndex(val index: Int) : ArtworkFeedAction

    /** Toggle bookmark state for a given artwork. */
    data class ToggleBookmark(
        val postId: Long,
        val isCurrentlyBookmarked: Boolean,
        val restrict: BookmarkRestrict? = null,
    ) : ArtworkFeedAction

    /** Navigate to the full artwork detail view. */
    data class NavigateToArtwork(val artworkId: Long) : ArtworkFeedAction

    /** Navigate to the user's profile. */
    data class NavigateToUser(val userId: Int) : ArtworkFeedAction

    /** Navigate to the full ranking list screen. */
    data object NavigateToRankingList : ArtworkFeedAction

    /** Long-press on an artwork (e.g. to show a context menu). */
    data class LongPressArtwork(
        val artwork: Artwork,
        val pageIndex: Int,
    ) : ArtworkFeedAction
}
