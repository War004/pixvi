package com.cryptic.pixvi.database.notification

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cryptic.pixvi.notification.CompletedNotificationCard
import com.cryptic.pixvi.notification.model.CompletedDownloadNotification


/*
Only completed downloads, for ongoing download we would load directly from work manager
 */
/*
    DOWNLOAD(0),
    SYSTEM(1),
    AUTHOR(2),
    PIXIV(id=3)
 */
/*

 */
//use just 0 for the notificatiojn type we are not ready yet with other types of notification

@Entity(
    tableName = "notification",
    indices = [Index(value = ["pdfSessionId"], unique = true)]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)val id: Int = 0,
    val notificationType: Int,
    val mediaType: Int, /*Use till 0 to 3*/
    val isPdfRenderingPending: Boolean? = null,
    val pdfSessionId: String? = null,
    val pdfPrintStartIndex: Int? = null,
    val pdfPrintEndIndex: Int? = null,
    val fileName: String,
    val time: Long,
    val formattedTime: String,
    val savedFolder: String,
    val directFileUri: String?,
)

fun NotificationEntity.toCompleteNotificationUiState(): CompletedDownloadNotification{
    return CompletedDownloadNotification(
        databaseId = this.id,
        notificationType = this.notificationType,
        mediaType = this.mediaType,
        needsPdfRendering = this.isPdfRenderingPending,
        pdfSessionId = this.pdfSessionId,
        startIndex = this.pdfPrintStartIndex,
        endIndex = this.pdfPrintEndIndex,
        fileName = this.fileName,
        time = this.time,
        formattedTime = this.formattedTime,
        savedFolder = this.savedFolder,
        directFileUri = this.directFileUri
    )
}