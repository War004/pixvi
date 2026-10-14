package com.cryptic.piyek.history.novel.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface NovelHistoryDao {
    @Upsert
    suspend fun upsertBrowsing(item: NovelBrowsingEntity)

    // 1. Pixiv andorid apk limits the database size to 50 entries
    @Query("SELECT * FROM novel_browsing_table ORDER BY datetime DESC LIMIT :limit")
    suspend fun getLatestBrowsing(limit: Int): List<NovelBrowsingEntity>

    @Query("SELECT * FROM novel_browsing_table ORDER BY datetime DESC LIMIT 1 OFFSET :limit")
    suspend fun getCutoffBrowsing(limit: Int): NovelBrowsingEntity?

    @Query("DELETE FROM novel_browsing_table WHERE datetime <= :cutoff")
    suspend fun pruneBrowsing(cutoff: Long)

    @Transaction
    suspend fun recordBrowsing(novelId: Long, dateTime: Long, maxHistoryLimit: Int) {
        upsertBrowsing(NovelBrowsingEntity(novelId, dateTime))
        getCutoffBrowsing(maxHistoryLimit)?.let { pruneBrowsing(it.dateTime) }
    }

    @Query("DELETE FROM novel_browsing_table")
    suspend fun clearBrowsing()


    @Upsert
    suspend fun upsertFinished(item: NovelFinishedEntity)

    @Query("SELECT * FROM novel_finished_table ORDER BY datetime DESC LIMIT :limit")
    suspend fun getLatestFinished(limit: Int): List<NovelFinishedEntity>

    @Query("SELECT * FROM novel_finished_table ORDER BY datetime DESC LIMIT 1 OFFSET :limit")
    suspend fun getCutoffFinished(limit: Int): NovelFinishedEntity?

    @Query("DELETE FROM novel_finished_table WHERE datetime <= :cutoff")
    suspend fun pruneFinished(cutoff: Long)

    @Transaction
    suspend fun recordFinished(novelId: Long, dateTime: Long, maxHistoryLimit: Int) {
        upsertFinished(NovelFinishedEntity(novelId, dateTime))
        getCutoffFinished(maxHistoryLimit)?.let { pruneFinished(it.dateTime) }
    }

    @Query("DELETE FROM novel_finished_table")
    suspend fun clearFinished()

    @Transaction
    suspend fun clearAll() {
        clearBrowsing()
        clearFinished()
    }
}