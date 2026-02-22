package com.cryptic.pixvi.database.notification

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("""
    UPDATE notification 
    SET isPdfRenderingPending = :isPdfRenderingPending, 
        pdfPrintStartIndex = :pdfPrintStartIndex, 
        pdfPrintEndIndex = :pdfPrintEndIndex,
        directFileUri = :directFileUri,
        savedFolder = :savedFolder 
    WHERE id = :id
""")
    suspend fun updatePdfStatus(
        id: Int,
        isPdfRenderingPending: Boolean?,
        pdfPrintStartIndex: Int?,
        pdfPrintEndIndex: Int?,
        directFileUri: String,
        savedFolder: String
    ): Int // This returns the number of rows affected

    @Delete
    suspend fun deleteNotification(notification: NotificationEntity)

    @Query("DELETE FROM notification WHERE id = :id")
    suspend fun deleteNotificationById(id: Int)

    /**
     * Get all download notifications (type = 0) ordered by time descending
     */
    @Query("SELECT * FROM notification WHERE notificationType = :type ORDER BY time DESC")
    fun getNotificationsByType(type: NotifType): Flow<List<NotificationEntity>>

    /**
     * Convenience method for download notifications specifically
     */
    @Query("SELECT * FROM notification WHERE notificationType = 0 ORDER BY time DESC")
    fun getAllDownloadNotifications(): Flow<List<NotificationEntity>>
}