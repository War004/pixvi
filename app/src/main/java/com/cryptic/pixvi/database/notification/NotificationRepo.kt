package com.cryptic.pixvi.database.notification

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NotificationRepo(
    private val notificationDao: NotificationDao
) {
    /**
     * Insert a download notification
     * @return Result with inserted row ID on success
     */
    suspend fun insertDownloadNotification(notification: NotificationEntity): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val id = notificationDao.insertNotification(notification)
                Result.success(id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    suspend fun switchPdfNotification(directFileUri: String, savedFolder: String, id: Int): Boolean {
        val rowsUpdated = notificationDao.updatePdfStatus(
            id = id,
            isPdfRenderingPending = false,
            pdfPrintStartIndex = null,
            pdfPrintEndIndex = null,
            directFileUri= directFileUri,
            savedFolder = savedFolder
        )
        // If it updated 1 or more rows, it was successful.
        // If it returns 0, no matching ID was found.
        return rowsUpdated > 0
    }
    /**
     * Get all download notifications as a Flow (type = DOWNLOAD)
     * Ordered by time descending
     */
    fun getAllDownloadNotifications(): Flow<List<NotificationEntity>> {
        return notificationDao.getAllDownloadNotifications()
    }

    /**
     * Delete a download notification by entity
     * @return Result.success(Unit) on success, Result.failure on error
     */
    suspend fun deleteDownloadNotification(notification: NotificationEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                notificationDao.deleteNotification(notification)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Delete a download notification by ID
     * @return Result.success(Unit) on success, Result.failure on error
     */
    suspend fun deleteDownloadNotificationById(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                notificationDao.deleteNotificationById(id)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}