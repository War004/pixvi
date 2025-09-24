package com.example.pixvi.bookmark

import android.util.Log
import com.example.pixvi.network.BookmarkRestrict
import com.example.pixvi.network.api.PixivApiService
import com.example.pixvi.utils.Result

class BookmarkRepositoryImpl(
    private val pixivApiService: PixivApiService //recived via constructor
) : BookmarkRepository {

    override suspend fun toggleBookmark(
        illustId: Long,
        isCurrentlyBookmarked: Boolean,
        visibility: BookmarkRestrict
    ): Result<Boolean> {
        return try {
            val response: retrofit2.Response<Unit>
            //parse the error response in future
            if (!isCurrentlyBookmarked) {
                val restrict = if (visibility == BookmarkRestrict.PUBLIC) "public" else "private"
                response = pixivApiService.addBookmarkIllust(illustId = illustId, restrict = restrict)
                if (response.isSuccessful) Result.Success(true)
                else Result.Error(response.code(),response.message())
            } else {
                response = pixivApiService.deleteBookmarkIllust(illustId = illustId)
                if (response.isSuccessful) Result.Success(false)
                else Result.Error(response.code(),response.message())
            }
        } catch (e: Exception) {
            Log.e("BookmarkRepo", "Network error while toggling bookmark: $e")
            Result.Error(-1,"No Internet available. Please check your settings")
        }
    }
}