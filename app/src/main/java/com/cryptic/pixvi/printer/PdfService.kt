package com.cryptic.pixvi.printer

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.io.IOException

// Constants defined at the top level are accessible to the whole file (or project if public)
private const val TAG = "PdfUtils"
const val PDF_MIME_TYPE = "application/pdf"

/**
 * For API 29+ (Android 10+).
 * Saves a pre-built PdfDocument directly to the Downloads folder via MediaStore.
 */
suspend fun saveViaMediaStore(
    pdfDocument: PdfDocument,
    pdfInfo: PdfInfo,
    contentResolver: ContentResolver
): PrintStatus = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        Log.e(TAG, "saveViaMediaStore called on API < 29. Use saveToUri instead.")
        return@withContext PrintStatus.Error(
            errorMessage = "saveViaMediaStore called on API < 29. Use saveToUri instead."
        )
    }

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, pdfInfo.title)
        put(MediaStore.MediaColumns.MIME_TYPE, PDF_MIME_TYPE)
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }

    val uri = contentResolver.insert(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        contentValues
    ) ?: run {
        Log.e(TAG, "Failed to create MediaStore entry")
        return@withContext PrintStatus.Error(
            errorMessage = "Failed to create MediaStore entry"
        )
    }

    val success = try {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            pdfDocument.writeTo(outputStream)
            true
        } ?: run {
            Log.e(TAG, "Failed to open output stream for MediaStore Uri")
            false
        }
    } catch (e: IOException) {
        Log.e(TAG, "Error writing PDF via MediaStore", e)
        false
    }

    if (success) {
        contentValues.clear()
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        contentResolver.update(uri, contentValues, null, null)

        return@withContext PrintStatus.Success(
            fileUri = uri,
            safeFolder = Environment.DIRECTORY_DOWNLOADS
        )
    } else {
        contentResolver.delete(uri, null, null)

        return@withContext PrintStatus.Error()
    }
}

/**
 * For below API 29 (or when user picks a specific location).
 * Writes the pre-built PdfDocument to a Uri obtained from SAF.
 */
suspend fun saveToUri(
    pdfDocument: PdfDocument,
    contentResolver: ContentResolver,
    outputUri: Uri
): Boolean = withContext(Dispatchers.IO) {
    try {
        contentResolver.openOutputStream(outputUri)?.use { outputStream ->
            pdfDocument.writeTo(outputStream)
            true
        } ?: run {
            Log.e(TAG, "Failed to open output stream for SAF Uri")
            false
        }
    } catch (e: IOException) {
        Log.e(TAG, "Error writing PDF to SAF Uri", e)
        false
    }
}

//Title: The name of the chapter or volume.
//
//Author: The writer/artist.
//
//Subject: Description or synopsis.
//
//Keywords: Comma-separated tags (e.g., "Shonen, Action, Isekai").
//
//Creator: The software used to create the original document (e.g., "Clip Studio Paint").
//
//Producer: The software that converted it to PDF (e.g., "Adobe Acrobat Pro").
//
//CreationDate: Date the file was created.
//
//ModDate: Date the file was last modified.
//
//Trapped: (Boolean) Pre-press printing information.

data class PdfInfo(
    val title: String,
    val author: String,
    val subject: String? = null,
    val keywords: List<String> = emptyList(),
    val creator: List<String> = emptyList(),
    val produce: String = "Pixvi",
    val creationDate: Long = System.currentTimeMillis(),
    val modDate: Long = System.currentTimeMillis(),
)