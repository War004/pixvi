package com.example.pixvi.viewModels

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.print.PrintManager
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
// androidx.compose.material.icons.Icons
// androidx.compose.material.icons.filled.AccountCircle
// androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.pixvi.utils.PdfPrintDocumentAdapter
import com.example.pixvi.workers.PdfExportWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// --- Data Class for UI ---
data class NotificationInfoUi(
    val id: Long,
    val author: String,
    val profileIconUrl: String?,
    val message: String,
    val timestamp: String,
    val actionText: String?,       // Primary action
    val secondaryActionText: String?, //For outlined | but it is the deserved action that should happen
    val deepLinkUrl: String? = null,
    val isDismissible: Boolean,
    val taskStatus: String? = null // Useful for UI logic beyond just actionText
)

// --- Room Database Entity ---
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val author: String,
    val profileIconUrl: String? = null,
    val message: String,
    val timestamp: Long,
    val actionText: String?,
    val deepLinkUrl: String? = null,
    val isRead: Boolean = false,
    val type: String = "device",
    val isDismissible: Boolean = true,

    // --- Task management fields ---
    val taskIllustId: Int? = null,
    val taskTitle: String? = null,
    val taskStatus: String? = null,
    val progressCurrent: Int? = null,
    val progressMax: Int? = null,       // Total pages
    val taskPayload: String? = null,     // For downloaded image URIs (JSON) or error messages on failure

    // --- NEW: Fields for Retry Functionality for PDF Export Jobs ---
    val originalImageUrlsJson: String? = null, // JSON string array of original image URLs
)

// --- Enum for Task Statuses ---
object TaskStatus {
    const val PENDING_DOWNLOAD = "PENDING_DOWNLOAD"
    const val DOWNLOADING = "DOWNLOADING"
    const val DOWNLOAD_COMPLETE = "DOWNLOAD_COMPLETE" // Ready for Print or Direct Save
    const val GENERATING_PDF = "GENERATING_PDF"       // For direct save process
    const val PDF_SAVED_DIRECTLY = "PDF_SAVED_DIRECTLY" // Direct save successful
    const val FAILED = "FAILED"
}


// --- Data Access Object (DAO) ---
@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long // Returns the new rowId

    @Query("SELECT * FROM notifications WHERE type = 'device' OR type = 'pdf_export_job' ORDER BY timestamp DESC")
    fun getDeviceAndPdfJobNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE type = 'device' ORDER BY timestamp DESC")
    fun getDeviceNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE type = 'in_app' ORDER BY timestamp DESC")
    fun getInAppNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotificationsInternal(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE id = :notificationId")
    fun getNotificationById(notificationId: Long): Flow<NotificationEntity?> // Flow to observe a single task

    @Query("SELECT * FROM notifications WHERE id = :notificationId")
    suspend fun getNotificationByIdOnce(notificationId: Long): NotificationEntity? // Non-flow version for worker


    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Long)

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotification(notificationId: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()

    @Query("DELETE FROM notifications WHERE type = 'pdf_export_job' AND isDismissible = 1")
    suspend fun clearPdfNotifications()

    @Query("DELETE FROM notifications WHERE type = 'device' AND isDismissible = 1")
    suspend fun clearDeviceNotifications()

    @Query("DELETE FROM notifications WHERE type = 'in_app'")
    suspend fun clearInAppNotifications()

    // --- PDF Task Specific Updates ---
    @Query("UPDATE notifications SET progressCurrent = :current, taskStatus = :status, message = :message, timestamp = :timestamp WHERE id = :notificationId")
    suspend fun updateTaskProgress(notificationId: Long, current: Int, status: String, message: String, timestamp: Long)

    @Query("UPDATE notifications SET taskStatus = :status, taskPayload = :payload, message = :message, actionText = :actionText, isDismissible = :isDismissible, timestamp = :timestamp WHERE id = :notificationId")
    suspend fun updateTaskCompletionOrFailure(
        notificationId: Long,
        status: String,
        payload: String?,
        message: String,
        actionText: String?,
        isDismissible: Boolean,
        timestamp: Long
    )
}


// --- Room Database ---
@Database(entities = [NotificationEntity::class], version = 4, exportSchema = false)
abstract class AppNotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppNotificationDatabase? = null

        fun getDatabase(context: Context): AppNotificationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppNotificationDatabase::class.java,
                    "app_notification_database"
                )
                    .fallbackToDestructiveMigration(false) // Handles schema changes by destroying and recreating
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
// --- Notification ViewModel ---
class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val notificationDao = AppNotificationDatabase.getDatabase(application).notificationDao()
    private val gson = Gson()

    val deviceNotifications: Flow<List<NotificationInfoUi>> =
        notificationDao.getDeviceAndPdfJobNotifications().map { entities ->
            entities.map { entity ->
                mapEntityToUi(entity) // mapEntityToUi will handle the specifics
            }
        }

    val inAppNotifications: Flow<List<NotificationInfoUi>> =
        notificationDao.getInAppNotifications().map { entities -> entities.map { entity -> mapEntityToUi(entity, "In-App: ") } }

    // Flow for all notifications, useful for a unified notification screen
    val allNotifications: Flow<List<NotificationInfoUi>> =
        notificationDao.getAllNotificationsInternal().map { entities -> entities.map { entity -> mapEntityToUi(entity) } }


    private fun enqueuePdfExportWorker(
        notificationId: Long,
        illustId: Int,
        illustTitle: String,
        totalPages: Int,
        imageUrls: List<String>, // Pass the actual list of URLs
        isRetry: Boolean = false // Optional flag for logging or different tags
    ) {
        if (imageUrls.isEmpty()) {
            Log.e("NotificationVM_Enqueue", "Cannot enqueue worker for job $notificationId: Image URLs list is empty.")
            // If this happens during retry, the job might be marked as failed again by a calling function.
            return
        }

        val workerInputData = Data.Builder()
            .putLong(PdfExportWorker.KEY_NOTIFICATION_ID, notificationId)
            .putInt(PdfExportWorker.KEY_ILLUST_ID, illustId)
            .putString(PdfExportWorker.KEY_ILLUST_TITLE, illustTitle)
            .putInt(PdfExportWorker.KEY_TOTAL_PAGES, totalPages)
            .putStringArray(PdfExportWorker.KEY_IMAGE_URLS, imageUrls.toTypedArray())
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val tag = if (isRetry) "pdf_export_retry_${illustId}" else "pdf_export_${illustId}"

        val pdfExportWorkRequest = OneTimeWorkRequestBuilder<PdfExportWorker>()
            .setInputData(workerInputData)
            .setConstraints(constraints)
            .addTag(tag)
            .build()

        WorkManager.getInstance(getApplication()).enqueue(pdfExportWorkRequest)
        Log.d("NotificationVM_Enqueue", "Enqueued PdfExportWorker for job ID: $notificationId. Is Retry: $isRetry")
    }



    private fun mapEntityToUi(entity: NotificationEntity, authorPrefix: String = ""): NotificationInfoUi {
        var displayMessage = entity.message
        var displayActionText = entity.actionText // This will be for the "Primary" action
        var currentIsDismissible = entity.isDismissible
        var secondaryActionText: String? = null // For the "Print Options" button

        if (entity.type == "pdf_export_job") {
            val title = entity.taskTitle ?: "Untitled Work"
            val current = entity.progressCurrent ?: 0
            val max = entity.progressMax ?: 0
            currentIsDismissible = false

            when (entity.taskStatus) {
                TaskStatus.PENDING_DOWNLOAD -> { /* ... */ }
                TaskStatus.DOWNLOADING -> { /* ... */ }
                TaskStatus.DOWNLOAD_COMPLETE -> {
                    displayMessage = "Pages for '$title' downloaded ($max/$max). Ready to finalize."
                    displayActionText = "Save" // Primary action: Direct Save
                    secondaryActionText = "Print" // Secondary action: Print Preview
                    currentIsDismissible = false
                }
                TaskStatus.GENERATING_PDF -> {
                    displayMessage = "Generating PDF for '$title'..."
                    displayActionText = "Saving..." // Or null if no button needed during this state
                    secondaryActionText = null
                }
                TaskStatus.PDF_SAVED_DIRECTLY -> {
                    displayMessage = "PDF '$title' saved directly to Downloads."
                    displayActionText = "Open Folder" // Or null, and the notification could auto-dismiss
                    secondaryActionText = null
                    currentIsDismissible = true
                }
                TaskStatus.FAILED -> {
                    displayMessage = "Failed to prepare PDF for '$title': ${entity.taskPayload ?: "Unknown error"}"
                    displayActionText = "Retry" // Ensure this is set
                    currentIsDismissible = true
                }
                else -> { displayMessage = entity.message }
            }
        }
        // ... (else if for other types) ...

        return NotificationInfoUi(
            id = entity.id,
            author = authorPrefix + entity.author,
            profileIconUrl = entity.profileIconUrl,
            message = displayMessage,
            timestamp = formatTimestamp(entity.timestamp),
            actionText = displayActionText, // Primary action
            secondaryActionText = secondaryActionText, // <--- Pass secondary action
            deepLinkUrl = entity.deepLinkUrl,
            isDismissible = currentIsDismissible,
            // type = entity.type // If you added 'type' o NotificationInfoUi
            taskStatus = entity.taskStatus // Pass status for UI logic if needed
        )
    }

    private suspend fun updateJobStatus(notificationId: Long, status: String, message: String, actionText: String?, isDismissible: Boolean) {
        notificationDao.updateTaskCompletionOrFailure(
            notificationId,
            status,
            if (status == TaskStatus.FAILED) message else null, // Only store error message in payload on failure
            message,
            actionText,
            isDismissible,
            System.currentTimeMillis()
        )
    }
    // private suspend fun cleanupTempFiles(imagePaths: List<String>) { ... }


    fun savePdfDirectly(context: Context, notificationId: Long) {
        viewModelScope.launch(Dispatchers.IO) { // Perform file operations on IO dispatcher
            val notification = notificationDao.getNotificationByIdOnce(notificationId)
            if (notification?.type != "pdf_export_job" || notification.taskStatus != TaskStatus.DOWNLOAD_COMPLETE) {
                Log.w("NotificationVM", "Cannot save PDF directly. Notification ID $notificationId not found or not in correct state: ${notification?.taskStatus}")
                return@launch
            }

            val imagePathsJson = notification.taskPayload
            if (imagePathsJson.isNullOrEmpty()) {
                Log.e("NotificationVM", "No image paths for direct save, job $notificationId")
                updateJobStatus(notificationId, TaskStatus.FAILED, "No image data found.", "Retry", true)
                return@launch
            }

            val imagePaths: List<String> = try {
                gson.fromJson(imagePathsJson, object : TypeToken<List<String>>() {}.type)
            } catch (e: Exception) {
                Log.e("NotificationVM", "Failed to parse image paths for direct save: $imagePathsJson", e)
                updateJobStatus(notificationId, TaskStatus.FAILED, "Error parsing image data.", "Retry", true)
                return@launch
            }

            if (imagePaths.isEmpty()) {
                Log.e("NotificationVM", "Image paths list is empty for direct save, job $notificationId")
                updateJobStatus(notificationId, TaskStatus.FAILED, "No images to save.", "Retry", true)
                return@launch
            }

            // Update status to GENERATING_PDF
            updateJobStatus(notificationId, TaskStatus.GENERATING_PDF, "Generating PDF...", null, false)

            val pdfDocument = PdfDocument()
            val illustTitle = notification.taskTitle ?: "Illustration"
            var success = true

            try {
                imagePaths.forEachIndexed { index, imagePath ->
                    val bitmap = BitmapFactory.decodeFile(imagePath)
                    if (bitmap == null) {
                        Log.e("NotificationVM_DirectSave", "Failed to decode bitmap from $imagePath for direct save.")
                        // Optionally skip or add an error page
                        throw IOException("Failed to load image for PDF: $imagePath")
                    }

                    // Page dimensions match image dimensions
                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(page)
                    bitmap.recycle()
                }

                // Save the PdfDocument to a file
                val fileName = "Pixiv_${notification.taskIllustId}_${illustTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_")}.pdf"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PixivExports")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val resolver = context.contentResolver
                var uri: Uri? = null
                try {
                    uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        ?: throw IOException("Failed to create new MediaStore record for PDF.")

                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    } ?: throw IOException("Failed to get output stream for PDF.")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                    Log.i("NotificationVM_DirectSave", "PDF saved directly to: $uri")
                    updateJobStatus(notificationId, TaskStatus.PDF_SAVED_DIRECTLY, "PDF '$illustTitle' saved to Downloads.", "Open Folder", true)

                } catch (e: Exception) {
                    uri?.let { resolver.delete(it, null, null) } // Clean up pending entry on error
                    throw e // Re-throw to be caught by outer try-catch
                }

            } catch (e: Exception) {
                Log.e("NotificationVM_DirectSave", "Error generating or saving PDF directly for job $notificationId", e)
                success = false
                updateJobStatus(notificationId, TaskStatus.FAILED, "Failed to save PDF: ${e.localizedMessage}", "Retry", true)
            } finally {
                pdfDocument.close()
                // Optionally clean up temp image files after direct save, similar to PdfPrintDocumentAdapter
                // cleanupTempFiles(imagePaths)
            }
        }
    }

    fun retryPdfExportJob(notificationId: Long, context: Context) { // context might still be needed for Toasts
        viewModelScope.launch(Dispatchers.IO) {
            val notification = notificationDao.getNotificationByIdOnce(notificationId)

            if (notification?.type == "pdf_export_job" && notification.taskStatus == TaskStatus.FAILED) {
                if (notification.originalImageUrlsJson.isNullOrEmpty() ||
                    notification.taskIllustId == null ||
                    notification.taskTitle == null ||
                    notification.progressMax == null) {
                    Log.e("NotificationVM_Retry", "Cannot retry job $notificationId: Missing original data in DB.")
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Retry failed: Critical data missing.", Toast.LENGTH_LONG).show()}
                    return@launch
                }

                Log.d("NotificationVM_Retry", "Retrying PDF export job ID: $notificationId for illust: ${notification.taskTitle}")

                // 1. Update Notification Status
                updateJobStatus(
                    notificationId,
                    TaskStatus.PENDING_DOWNLOAD,
                    "Retrying PDF for '${notification.taskTitle}'...",
                    "In Progress",
                    false
                )

                // 2. Deserialize stored data
                val originalImageUrlsList: List<String> = try {
                    gson.fromJson(notification.originalImageUrlsJson, object : TypeToken<List<String>>() {}.type)
                } catch (e: Exception) {
                    Log.e("NotificationVM_Retry", "Failed to parse originalImageUrlsJson for retry.", e)
                    updateJobStatus(notificationId, TaskStatus.FAILED, "Retry setup failed (URL parse).", "Retry", true)
                    return@launch
                }

                // 3. Call the helper to re-enqueue the worker
                enqueuePdfExportWorker(
                    notificationId = notificationId,
                    illustId = notification.taskIllustId,
                    illustTitle = notification.taskTitle,
                    totalPages = notification.progressMax,
                    imageUrls = originalImageUrlsList, // Pass deserialized list
                    isRetry = true
                )

            } else {
                Log.w("NotificationVM_Retry", "Cannot retry job $notificationId. Invalid state. Status: ${notification?.taskStatus}")
            }
        }
    }


    suspend fun initiateAndTrackPdfExport(
        illustId: Int,
        illustTitle: String,
        totalPages: Int,
        originalImageUrls: List<String>, // Now takes the List<String> directly
        authorName: String = "PDF Export Service"
    ): Long {
        val initialMessage = "Preparing PDF for '$illustTitle'..."
        val timestamp = System.currentTimeMillis()
        val originalImageUrlsJsonToStore = gson.toJson(originalImageUrls)

        val notification = NotificationEntity(
            // id = 0, // Defaulted by autoGenerate = true
            author = authorName,
            profileIconUrl = null, // Explicitly null if not provided
            message = initialMessage,
            timestamp = timestamp,
            actionText = null, // <--- EXPLICITLY PROVIDE NULL or an initial value
            deepLinkUrl = null,    // Explicitly null
            isRead = false,        // Explicitly set
            type = "pdf_export_job",
            isDismissible = false,
            taskIllustId = illustId,
            taskTitle = illustTitle,
            taskStatus = TaskStatus.PENDING_DOWNLOAD,
            progressCurrent = 0,
            progressMax = totalPages,
            taskPayload = null, // Explicitly null
            originalImageUrlsJson = originalImageUrlsJsonToStore,
        )
        val notificationJobId = notificationDao.insertNotification(notification)
        Log.d("NotificationVM", "Initiated PDF export job ID: $notificationJobId for illust: $illustTitle")

        enqueuePdfExportWorker(
            notificationId = notificationJobId,
            illustId = illustId,
            illustTitle = illustTitle,
            totalPages = totalPages,
            imageUrls = originalImageUrls,
            isRetry = false
        )
        return notificationJobId
    }


    // Called when the "Save as PDF" action on a notification is clicked
    @SuppressLint("ServiceCast")
    fun triggerPrintPreviewForPdfJob(context: Context, notificationId: Long) {
        viewModelScope.launch {
            val notification = notificationDao.getNotificationByIdOnce(notificationId)
            if (notification?.type == "pdf_export_job" && notification.taskStatus == TaskStatus.DOWNLOAD_COMPLETE) {
                val imagePathsJson = notification.taskPayload
                if (imagePathsJson.isNullOrEmpty()) {
                    Log.e("NotificationVM", "No image paths found in notification payload for job $notificationId")
                    // Optionally update notification to an error state
                    return@launch
                }

                val imagePaths: List<String> = try {
                    gson.fromJson(imagePathsJson, object : TypeToken<List<String>>() {}.type)
                } catch (e: Exception) {
                    Log.e("NotificationVM", "Failed to parse image paths from JSON: $imagePathsJson", e)
                    return@launch
                }

                if (imagePaths.isEmpty()) {
                    Log.e("NotificationVM", "Image paths list is empty for job $notificationId")
                    return@launch
                }

                Log.d("NotificationVM", "Starting print preview for job $notificationId with ${imagePaths.size} images.")

                // Update status to indicate PDF generation is in progress (optional)
                // notificationDao.updateTaskStatus(...)

                //something is wrong???? suspicious cast to <ErrorType> for a PRINT_SERVICE: expected PrintManager
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val jobName = "Pixiv_Illust_${notification.taskIllustId}_${notification.taskTitle?.replace(" ", "_")}"

                // Use withContext(Dispatchers.Main) if PrintManager requires it, usually it's fine.
                printManager?.print(jobName, PdfPrintDocumentAdapter(context, imagePaths, notification.taskTitle ?: "Illustration"), null)

            } else {
                Log.w("NotificationVM", "Cannot trigger print preview. Notification ID $notificationId not found or not in correct state: ${notification?.taskStatus}")
            }
        }
    }


    fun addNotification( // General purpose notification
        author: String, message: String, type: String = "device",
        actionText: String? = null, deepLinkUrl: String? = null,
        profileIconUrl: String? = null, isDismissible: Boolean = true
    ) {
        viewModelScope.launch {
            val notification = NotificationEntity(
                author = author, profileIconUrl = profileIconUrl, message = message,
                timestamp = System.currentTimeMillis(), actionText = actionText,
                deepLinkUrl = deepLinkUrl, type = type, isDismissible = isDismissible
            )
            notificationDao.insertNotification(notification)
        }
    }

    fun markNotificationAsRead(notificationId: Long) {
        viewModelScope.launch {
            notificationDao.markAsRead(notificationId)
        }
    }

    fun deleteNotification(notificationId: Long) {
        // This will delete the notification from the DB.
        // The UI should only call this if the notification isDismissible.
        viewModelScope.launch {
            notificationDao.deleteNotification(notificationId)
        }
    }

    fun clearAllPdfNotifications(){
        viewModelScope.launch{
            notificationDao.clearPdfNotifications()
        }
    }

    fun clearAllDeviceNotifications() {
        viewModelScope.launch {
            notificationDao.clearDeviceNotifications()
        }
    }
    fun clearAllInAppNotifications() {
        viewModelScope.launch {
            notificationDao.clearInAppNotifications()
        }
    }


    fun clearAllNotifications() { // Clears everything
        viewModelScope.launch {
            notificationDao.clearAllNotifications()
        }
    }

    fun handleDeepLink(context: Context, deepLinkUrl: String?) {
        if (deepLinkUrl.isNullOrEmpty()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, deepLinkUrl.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Log.e("NotificationViewModel", "No activity found to handle deep link: $deepLinkUrl")
            }
        } catch (e: Exception) {
            Log.e("NotificationViewModel", "Error handling deep link: $deepLinkUrl", e)
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 1 -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
            days == 1L -> "Yesterday"
            hours > 0 -> "$hours hr ago"
            minutes > 0 -> "$minutes min ago"
            else -> "Just now"
        }
    }
}