package com.cryptic.pixvi.notification.model



sealed class DownloadNotification{
    data class CompletedDownloadNotification(
        val id: Long,
        val mediaType: Int,
        val fileName: String,
        val time: Long,
        val formattedTime: String,
        val savedFolder: String,
        val directFileUri: String?,
    ): DownloadNotification()

    data class PdfRenderingNotification(
        val id: Long,
        val baseFolder: String,
        val baseFileName: String,
        val startIndex: Int,
        val endIndex: Int,
    ): DownloadNotification()

    data class OngoingDownloadNotification(
        val fileName: String,
        val time: Long,
        val formattedTime: String,
        val totalItems: Int,
        val totalSuccess: Int,
        val totalFailed: Int,
        val hasForcedToStopped: Boolean,
        val errorCode: Int?,
        val errorMessage: String?
    )
}

data class CompletedDownloadNotification(
    val databaseId: Int,
    val notificationType: Int,
    val mediaType: Int,
    val needsPdfRendering: Boolean? = null,
    val pdfSessionId: String? = null,
    val startIndex: Int? = null,
    val endIndex: Int? = null,
    val fileName: String,
    val time: Long,
    val formattedTime: String,
    val savedFolder: String,
    val directFileUri: String?,
)


/*


data class DownloadNotification(
    val fileName: String,
    val time: Long,
    val formattedTime: String,
    val totalItems: Int,
    val totalSuccess: Int,
    val totalFailed: Int,
    val isCompleted: Boolean,
    val isProcessing: Boolean,
    val hasError: Boolean,

    val errorCode: Int?,
    val errorMessage: String?,

    val savedFolder: String?,
    val directFileUri: String?,

    val workId: String?
)

*/

data class OngoingDownloadNotification(
    val fileName: String,
    val time: Long,
    val formattedTime: String,
    val totalItems: Int,
    val totalSuccess: Int,
    val totalFailed: Int,
    val hasForcedToStopped: Boolean,
    val errorCode: Int?,
    val errorMessage: String?
)

data class SavedDownloadNotification(
    val isPdfProcessingReaming: Boolean?,
    val fileName: String,
    val time: Long,
    val formattedTime: String,
    val savedFolder: String,
    val directFileUri: String?, //null for mutiple image
)