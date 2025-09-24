package com.example.pixvi.bookmark

import com.example.pixvi.network.BookmarkRestrict
import com.example.pixvi.utils.Result

interface BookmarkRepository {
    suspend fun toggleBookmark(
        illustId: Long,
        isCurrentlyBookmarked: Boolean,
        visibility: BookmarkRestrict
    ): Result<Boolean>
}