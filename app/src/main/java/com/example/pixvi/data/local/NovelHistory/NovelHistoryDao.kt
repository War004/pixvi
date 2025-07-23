package com.example.pixvi.data.local.NovelHistory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelHistoryDao {

    /**
     * This is the new "UPSERT" method.
     * If a NovelHistoryEntry with the same composite primary key (novelId, interactionType)
     * already exists, it will be REPLACED with the new entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: NovelHistoryEntry)

    /**
     * This is the new logic for marking a novel as read. It's a two-step process
     * wrapped in a transaction to ensure both steps succeed or fail together.
     * 1. Inserts or updates the 'READ' entry with a new timestamp.
     * 2. Deletes the 'VIEW' entry for that novel, if one exists.
     */
    @Transaction
    suspend fun markAsReadAndDeleteView(novelId: Int) {
        // Step 1: Upsert the READ entry.
        val readEntry = NovelHistoryEntry(novelId = novelId, interactionType = InteractionType.READ)
        upsert(readEntry)

        // Step 2: Delete the corresponding VIEW entry.
        delete(novelId, InteractionType.VIEW)
    }

    /**
     * A simple query to delete a specific history entry.
     */
    @Query("DELETE FROM novel_history WHERE novel_id = :novelId AND interaction_type = :type")
    suspend fun delete(novelId: Int, type: InteractionType)

    // Your getHistoryByTypeSnapshot query remains the same and will work perfectly.
    @Query("SELECT * FROM novel_history WHERE interaction_type = :type ORDER BY timestamp DESC")
    suspend fun getHistoryByTypeSnapshot(type: InteractionType): List<NovelHistoryEntry>
}