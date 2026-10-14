package com.cryptic.piyek.history.novel.domain.repository

import com.cryptic.piyek.core.domain.error.EmptyResult
import com.cryptic.piyek.core.domain.error.Result
import com.cryptic.piyek.history.novel.domain.model.NovelBrowserRequestBody
import com.cryptic.piyek.history.novel.domain.model.NovelBrowsing
import com.cryptic.piyek.history.novel.domain.model.NovelFinished
import com.cryptic.piyek.history.novel.domain.model.NovelFinishedRequestBody
import com.cryptic.piyek.history.novel.domain.model.NovelHistoryError

interface NovelHistoryRepository {

    /**
     * Retrieves the latest browsing entries up to a hardcoded limit of 50,
     * mapped into [NovelBrowserRequestBody] with ISO-8601 datetimes for network payloads.
     */
    suspend fun getLatestBrowsingRequestBody(): Result<List<NovelBrowserRequestBody>, NovelHistoryError>

    /**
     * Retrieves the latest finished entries up to a hardcoded limit of 50,
     * mapped into [NovelFinishedRequestBody] with ISO-8601 datetimes for network payloads.
     */
    suspend fun getLatestFinishedRequestBody(): Result<List<NovelFinishedRequestBody>, NovelHistoryError>

    /**
     * Retrieves browsing history as clean domain models [NovelBrowsing].
     */
    suspend fun getBrowsingHistory(limit: Int = 50): Result<List<NovelBrowsing>, NovelHistoryError>

    /**
     * Retrieves finished history as clean domain models [NovelFinished].
     */
    suspend fun getFinishedHistory(limit: Int = 50): Result<List<NovelFinished>, NovelHistoryError>

    /**
     * Records a novel browsing event and maintains the history within [maxLimit].
     */
    suspend fun recordBrowsing(
        novelId: Long,
        dateTime: Long = System.currentTimeMillis(),
        maxLimit: Int = 50
    ): EmptyResult<NovelHistoryError>

    /**
     * Records a novel finished event and maintains the history within [maxLimit].
     */
    suspend fun recordFinished(
        novelId: Long,
        dateTime: Long = System.currentTimeMillis(),
        maxLimit: Int = 50
    ): EmptyResult<NovelHistoryError>

    /**
     * Clears all recorded novel browsing entries.
     */
    suspend fun clearBrowsingHistory(): EmptyResult<NovelHistoryError>

    /**
     * Clears all recorded novel finished entries.
     */
    suspend fun clearFinishedHistory(): EmptyResult<NovelHistoryError>

    /**
     * Clears both browsing and finished novel history entries.
     */
    suspend fun clearAllHistory(): EmptyResult<NovelHistoryError>
}
