package com.cryptic.pixvi.core.downloader.pdf

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.cryptic.pixvi.printer.PdfInfo
import com.cryptic.pixvi.printer.PrintStatus
import com.cryptic.pixvi.printer.saveViaMediaStore
import com.cryptic.pixvi.worker.PdfImageDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository for PDF download/save operations.
 *
 * Flow:
 * 1. Download all images from Pixiv URLs to internal app storage
 *    (filesDir/pdf_temp/{pixivId}_{startPage}-{endPage}/)
 * 2. Collect the local file URIs
 * 3. Pass URIs to PdfService for PDF generation
 * 4. Clean up temp files after PDF is written
 */
class DownloadPdfRepo(
    private val context: Context,
    private val workManager: WorkManager
) {

    val contentResolver: ContentResolver
        get() = context.contentResolver

    companion object {
        private const val TAG = "DownloadPdfRepo"
        private const val TEMP_DIR = "pdf_temp"
    }
    /*
     */
    //parameter would be
    /*
    Session id,
    start index and end index,
    post id: Long
     */
    //then we would check for expected image, and then try to run a loop to extract it
    //user press the safe pdf button
    //start the function to make the pdf
    //at the finish, we will safe the info and then launch an intent,
    //when it is retunr, we will chain up to link it up. To the download repo,

    fun downloadPdf(
        postId: Long,
        postTitle: String,
        startIndex: Int,
        endIndex: Int,
        singleImageUrl: String
    ){

        val baseFolder = "${postId}_${startIndex}_${endIndex}"

        val input = workDataOf(
            "KEY_BASE_FOLDER" to baseFolder,
            "KEY_POST_TITLE" to postTitle,
            "KEY_START_INDEX" to startIndex,
            "KEY_END_INDEX" to endIndex,
            "KEY_IMAGE_URL" to singleImageUrl
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<PdfImageDownloader>()
            .setInputData(input)
            .setConstraints(constraints)
            .addTag("PDF_IMAGE_DOWNLOAD")
            .build()

        workManager.enqueueUniqueWork(
            "${baseFolder}_${System.currentTimeMillis()}",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    suspend fun deleteInternalFolder(folderName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val folder = File(context.filesDir, folderName)

            if (folder.exists() && folder.isDirectory) {
                folder.deleteRecursively()
            } else {
                false
            }
        }
    }

    suspend fun saveRawPdf(
        baseFolder: String,
        startIndex: Int,
        endIndex: Int,
        postTitle: String,
    ): PrintStatus = withContext(Dispatchers.IO){

        val folderToCheck = File(context.filesDir, baseFolder)

        Log.d("PdfRepo","Folder path: $folderToCheck")
        if (!folderToCheck.exists()) {
            Log.e("SafePdf", "Cannot find the internal pdf folder: ${folderToCheck.absolutePath}")
            return@withContext PrintStatus.Error(
                errorCode = 404,
                errorMessage = "Internal folder not found.",
                solution = "Try downloading the images again."
            )
        }

        //make the document
        val document = PdfDocument()
        var currentBitmap: Bitmap? = null
        var pagePdf:  PdfDocument.Page? = null

        for(page in startIndex until endIndex){
            //make the new file,
            val pictureName = "${postTitle}_p${page}"

            val targetImage = folderToCheck.listFiles()?.find { file ->
                file.nameWithoutExtension == pictureName
            }

            if(targetImage==null){
                Log.d("Pdf Maker","Can't find the image, check the internal folder.")
            }

            currentBitmap = BitmapFactory.decodeFile(targetImage?.absolutePath)

            if(currentBitmap ==null){
                //early exit
                Log.d("SafePdf","Images in the internal folder are not bitmap")
                return@withContext PrintStatus.Error(
                    errorCode = 100,
                    errorMessage = "The bitrate for the selected pdf was found to be empty.",
                    solution = "Clear the app data."
                )
            }
            pagePdf = document.startPage(PdfDocument.PageInfo.Builder(currentBitmap.width, currentBitmap.height, page).create())

            pagePdf.canvas.drawBitmap(currentBitmap, 0f, 0f, null)

            document.finishPage(pagePdf)

            currentBitmap.recycle()
        }

        //calculate the document lenght.
        if(document.pages.size != endIndex-startIndex){
            Log.d("PdfSafer","Final pdf pages doesn't match the total index count in the internal image")
            return@withContext PrintStatus.Error(
                errorCode = 100,
                errorMessage = "The final pdf pages were not completed",
                solution = "Retry the pdf making process"
            )
        }
        //to save the pdf we have two methods one is silent and other uses an intent
        //use different function based on the
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.Q){
            //call the saveViaMediaStore
            val results = saveViaMediaStore(
                pdfDocument = document,
                pdfInfo = PdfInfo(
                    title = postTitle,
                    author = "unknown",
                ),
                contentResolver = contentResolver
            )

            document.close()

            when(results){
                is PrintStatus.Success -> {
                    Log.d("Pdf Maker","Saved the pdf in the device")
                    return@withContext results
                }
                is PrintStatus.Error -> {
                    return@withContext PrintStatus.Error(
                        errorCode = 500,
                        errorMessage = "Failed to save the PDF via MediaStore.",
                        solution = "Check device storage or permissions."
                    )
                }
            }
        }
        else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O){

            val tempFile = File(context.cacheDir, "pending_report.pdf")
            tempFile.outputStream().use {
                document.writeTo(it)
            }
            document.close()
            Log.d("Pdf Maker","Android version below then 10, not implemented now.")
            //launch an intent
            //signal to launch an intent, plus

            // Added Success Return
            return@withContext PrintStatus.Success(fileUri = Uri.fromFile(tempFile), "unknown")
        }

        // Added Catch-all Return to fix the "expected Unit" mismatch error
        return@withContext PrintStatus.Error(
            errorCode = 501,
            errorMessage = "Unhandled Android Version (API 26-28).",
            solution = "Update the app logic to support this Android version."
        )
    }
    /**
     * Recursively deletes the temp directory and its contents.
     */
    private fun cleanupTempDir(dir: File) {
        try {
            dir.deleteRecursively()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clean up temp dir: ${dir.absolutePath}", e)
        }
    }
}
