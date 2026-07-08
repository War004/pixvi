package com.cryptic.piyek.core.content.domain.repo

import com.cryptic.piyek.core.CResponse
import com.cryptic.piyek.core.content.data.model.RecommendationPara
import com.cryptic.piyek.core.data.local.BookmarkRestrict
import kotlinx.coroutines.flow.StateFlow

interface CoreContentApiRepo<T, in P : RecommendationPara, Q> {
    val contentList: StateFlow<Q?>
    suspend fun getRecommendation(parameters: P): CResponse<Unit>
    suspend fun getMoreRecommendation(): CResponse<Unit>
    suspend fun addBookmark(postId: Long, restrict: BookmarkRestrict): CResponse<Unit>
    suspend fun deleteBookmark(postId: Long): CResponse<Unit>
    suspend fun getRelatedOnBookMark(data: T): CResponse<T>
    suspend fun getRelatedForPost(data: T): CResponse<T>
    fun changeFocusedIndex(index: Int): Unit
}