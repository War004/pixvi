package com.cryptic.pixvi.notification

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.cryptic.pixvi.core.downloader.pdf.DownloadPdfRepo
import com.cryptic.pixvi.core.network.util.convertLongToTime
import com.cryptic.pixvi.database.notification.NotifType
import com.cryptic.pixvi.database.notification.NotificationRepo
import com.cryptic.pixvi.database.notification.toCompleteNotificationUiState
import com.cryptic.pixvi.notification.model.CompletedDownloadNotification
import com.cryptic.pixvi.notification.model.OngoingDownloadNotification
import com.cryptic.pixvi.printer.PrintStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationUiState(
    val notificationView: NotifType = NotifType.DOWNLOAD
)

data class NotificationListState(
    val completedDownloadNotification: List<CompletedDownloadNotification> = emptyList(),
    val ongoingDownloadNotification: List<OngoingDownloadNotification> = emptyList(),
    //not applied only experimental
    val systemNotification: List<Any?> = emptyList(),
    val authorNotification: List<Any?> = emptyList(),
    val pixivNotification: List<Any?> = emptyList(),
)

sealed class NotificationAction {
    data class DismissNotification(val id: Int) : NotificationAction()
    data object DismissAllEligibleNotification : NotificationAction()
    data class RetryDownload(val id: Int) : NotificationAction()
    data class CancelDownload(val id: Int) : NotificationAction()
    data class PauseDownload(val id: Int) : NotificationAction()
    data class OpenFolder(val folderPath: Uri) : NotificationAction()
    data class OpenFile(val filePath: Uri): NotificationAction()
    data class SavePdf(val databaseId: Int, val postSessionId: String, val startIndex: Int, val endIndex: Int, val postTitle: String): NotificationAction()
    data class ChangeView(val choice: NotifType): NotificationAction()
}

sealed interface NotificaitonUiEvent{
    data class ShowSnackbar(val message: String): NotificaitonUiEvent
}

class NotificationViewModel(
    workManager: WorkManager,
    private val notificationRepo: NotificationRepo,
    private val downloadPdfRepo: DownloadPdfRepo
) : ViewModel() {

    companion object {
        private const val TAG_SINGLE = "SINGLE_IMAGE_DOWNLOAD"
        private const val TAG_BATCH = "BATCH_IMAGE_DOWNLOAD"
    }

    // Observe single downloads filtered by tag
    private val singleDownloadsFlow = workManager
        .getWorkInfosByTagFlow(TAG_SINGLE)
        .map { workInfoList ->
            workInfoList
                .filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                .map { it.toOngoingNotification() }
        }

    // Observe batch downloads filtered by tag
    private val batchDownloadsFlow = workManager
        .getWorkInfosByTagFlow(TAG_BATCH)
        .map { workInfoList ->
            workInfoList
                .filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                .map { it.toOngoingNotification() }
        }

    private val pdfDownloadFlow = workManager
        .getWorkInfosByTagFlow("PDF_IMAGE_DOWNLOAD")
        .map { workInfoList->
            workInfoList
                .filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                .map { it.toOngoingNotification() }
        }

    // Observe completed downloads from database
    private val completedDownloadsFlow = notificationRepo
        .getAllDownloadNotifications()
        .map { notificationEntity->
            notificationEntity.map {
                it.toCompleteNotificationUiState()
            }
        }
    private val _uiState = MutableStateFlow(NotificationUiState())
    private val _uiEvent = Channel<NotificaitonUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val uiState = _uiState.asStateFlow()

    val listState = combine(
        singleDownloadsFlow,
        batchDownloadsFlow,
        pdfDownloadFlow,
        completedDownloadsFlow,

    ) { single, batch, pdf, completed ->
        Log.d("NotificationViewModel","Size of the completed notification ${completed.size}")

        NotificationListState(
            ongoingDownloadNotification = single + batch + pdf,
            completedDownloadNotification = completed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationListState()
    )

    fun notificationAction(action: NotificationAction){
        when(action){
            is NotificationAction.ChangeView -> {
                if(uiState.value.notificationView == action.choice) return
                _uiState.update {
                    it.copy(notificationView = action.choice)
                }
            }
            is NotificationAction.SavePdf -> {
                Log.d("NotificaitonPdf","Working")
                viewModelScope.launch {

                    val status = downloadPdfRepo.saveRawPdf(
                        baseFolder = action.postSessionId,
                        startIndex = action.startIndex,
                        endIndex = action.endIndex,
                        postTitle = action.postTitle
                    )

                    when(status){
                        is PrintStatus.Success -> {

                            //on success save modify the notification and delete the temp folder.
                            notificationRepo.switchPdfNotification(
                                directFileUri = status.fileUri.toString(),
                                id = action.databaseId,
                                savedFolder = status.safeFolder
                            )
                            //removed the folder
                            val delStatus = downloadPdfRepo.deleteInternalFolder(folderName = action.postSessionId)
                            if(delStatus){
                                /**/
                                _uiEvent.send(NotificaitonUiEvent.ShowSnackbar("Saved the pdf and removed the images from internal storage."))
                            }
                            else{
                                _uiEvent.send(NotificaitonUiEvent.ShowSnackbar("Pdf saved, but removal of images failed. Clear app data to remove."))
                            }
                        }
                        is PrintStatus.Error -> {
                            _uiEvent.send(NotificaitonUiEvent.ShowSnackbar(status.errorMessage?:"Something unknown happened"))
                            //if file save falied.
                        }
                    }
                }
            }
            else -> {
                Log.d("NotificationViewModel","Something was supposed to happen.")
            }
        }
    }


    private fun WorkInfo.toOngoingNotification(): OngoingDownloadNotification {
        val progress = this.progress
        val input = this.outputData // Input data is also accessible via tags, but we stored it

        // Read schedule time from input data (set when work was enqueued)
        val scheduledTime = input.getLong("KEY_SCHEDULE_TIME", System.currentTimeMillis())

        return OngoingDownloadNotification(
            fileName = progress.getString("BASE_FILE_NAME") ?: input.getString("KEY_FILENAME") ?: "Unknown",
            time = scheduledTime,
            formattedTime = convertLongToTime(scheduledTime),
            totalItems = progress.getInt("PROGRESS_TOTAL", 0),
            totalSuccess = progress.getInt("PROGRESS_CURRENT", 0),
            totalFailed = 0, // Not tracked in progress, only in output
            hasForcedToStopped = this.state == WorkInfo.State.CANCELLED,
            errorCode = null,
            errorMessage = null
        )
    }
}