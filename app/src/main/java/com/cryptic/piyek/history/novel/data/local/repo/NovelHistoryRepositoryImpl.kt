package com.cryptic.piyek.history.novel.data.local.repo

import android.database.sqlite.SQLiteFullException
import com.cryptic.piyek.core.domain.error.EmptyResult
import com.cryptic.piyek.core.domain.error.Result
import com.cryptic.piyek.history.novel.data.local.db.NovelHistoryDao
import com.cryptic.piyek.history.novel.data.local.mapper.toDomain
import com.cryptic.piyek.history.novel.data.local.mapper.toRequestBody
import com.cryptic.piyek.history.novel.domain.model.NovelBrowserRequestBody
import com.cryptic.piyek.history.novel.domain.model.NovelBrowsing
import com.cryptic.piyek.history.novel.domain.model.NovelFinished
import com.cryptic.piyek.history.novel.domain.model.NovelFinishedRequestBody
import com.cryptic.piyek.history.novel.domain.model.NovelHistoryError
import com.cryptic.piyek.history.novel.domain.repository.NovelHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NovelHistoryRepositoryImpl(
    private val dao: NovelHistoryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NovelHistoryRepository {

    companion object {
        private const val HARDCODED_LIMIT = 50
    }

    override suspend fun getLatestBrowsingRequestBody(): Result<List<NovelBrowserRequestBody>, NovelHistoryError> {
        return safeDbCall {
            dao.getLatestBrowsing(HARDCODED_LIMIT).map { it.toRequestBody() }
        }
    }

    override suspend fun getLatestFinishedRequestBody(): Result<List<NovelFinishedRequestBody>, NovelHistoryError> {
        return safeDbCall {
            dao.getLatestFinished(HARDCODED_LIMIT).map { it.toRequestBody() }
        }
    }

    override suspend fun getBrowsingHistory(limit: Int): Result<List<NovelBrowsing>, NovelHistoryError> {
        return safeDbCall {
            dao.getLatestBrowsing(limit).map { it.toDomain() }
        }
    }

    override suspend fun getFinishedHistory(limit: Int): Result<List<NovelFinished>, NovelHistoryError> {
        return safeDbCall {
            dao.getLatestFinished(limit).map { it.toDomain() }
        }
    }

    override suspend fun recordBrowsing(
        novelId: Long,
        dateTime: Long,
        maxLimit: Int
    ): EmptyResult<NovelHistoryError> {
        return safeDbCall {
            dao.recordBrowsing(novelId = novelId, dateTime = dateTime, maxHistoryLimit = maxLimit)
        }
    }

    override suspend fun recordFinished(
        novelId: Long,
        dateTime: Long,
        maxLimit: Int
    ): EmptyResult<NovelHistoryError> {
        return safeDbCall {
            dao.recordFinished(novelId = novelId, dateTime = dateTime, maxHistoryLimit = maxLimit)
        }
    }

    override suspend fun clearBrowsingHistory(): EmptyResult<NovelHistoryError> {
        return safeDbCall {
            dao.clearBrowsing()
        }
    }

    override suspend fun clearFinishedHistory(): EmptyResult<NovelHistoryError> {
        return safeDbCall {
            dao.clearFinished()
        }
    }

    override suspend fun clearAllHistory(): EmptyResult<NovelHistoryError> {
        return safeDbCall {
            dao.clearAll()
        }
    }

    private suspend fun <T> safeDbCall(block: suspend () -> T): Result<T, NovelHistoryError> {
        return withContext(ioDispatcher) {
            try {
                Result.Success(block())
            } catch (e: SQLiteFullException) {
                Result.Error(NovelHistoryError.DISK_FULL)
            } catch (e: Exception) {
                Result.Error(NovelHistoryError.DATABASE_ERROR)
            }
        }
    }
}
