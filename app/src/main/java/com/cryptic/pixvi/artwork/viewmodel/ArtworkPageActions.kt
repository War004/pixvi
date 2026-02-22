package com.cryptic.pixvi.artwork.viewmodel

import android.content.Context
import coil3.ImageLoader
import com.cryptic.pixvi.ArtworkScreen
import com.cryptic.pixvi.core.network.model.BookmarkTypes

sealed class ArtworkPageActions{
    data class UpdateBookMark(val artworkId: Long, val bookmarkStatus: Boolean, val type: BookmarkTypes, val onResult: ((BookmarkStatus) -> Unit)? = null): ArtworkPageActions()
    data class UpdateBookmarkInteractive(val artworkId: Long, val bookmarkStatus: Boolean, val type: BookmarkTypes): ArtworkPageActions()
    data class UpdateIndexes(val artworkId: Long, val pageIndex: Int): ArtworkPageActions()
    data class UpdateFocusedIndex(val index: Int): ArtworkPageActions()
    data object ForceGetMoreContent: ArtworkPageActions()
    data class OpenArtworkDetail(val artWorkId: Long): ArtworkPageActions()
    data class OpenAuthor(val userId: Long): ArtworkPageActions()
    data class DownloadImage(val imageUrl: List<String>, val fileName: String, val fileId: Long): ArtworkPageActions()
    data class CopyImageToClipboard(val targetImageUrl: String, val application: Context, val imageLoader: ImageLoader, val illustId: Long, val pageIndex: Int): ArtworkPageActions()
    data class DownloadAllImage(val imageUrl: String, val fileName: String, val fileId: Long, val startIndex: Int, val pageCount: Int): ArtworkPageActions()
    data class DownloadPdf(val postId: Long, val postTitle: String, val startIndex: Int, val endIndex: Int, val singleImageUrl: String): ArtworkPageActions()
    data class ManageUiMode(val mode: ViewerType, val currentItemIndex: Int): ArtworkPageActions()
}