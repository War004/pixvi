package com.example.pixvi.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PdfPrintDocumentAdapter(
    private val context: Context,
    private val imageFilePaths: List<String>,
    private val documentTitle: String
) : PrintDocumentAdapter() {

    private val TAG = "PdfPrintAdapter"
    // A scope for the adapter's lifecycle, can be used for cleanup if needed,
    // but onFinish is better for print-job specific cleanup.
    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)


    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        Log.d(TAG, "onLayout called. Old: $oldAttributes, New: $newAttributes")

        if (cancellationSignal?.isCanceled == true) {
            Log.d(TAG, "onLayout: Cancelled by signal.")
            callback.onLayoutCancelled()
            return
        }

        // Create new PrintDocumentInfo each time. This is important.
        val pdi = PrintDocumentInfo.Builder("${documentTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_")}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(imageFilePaths.size) // Each image path represents one page
            .build()

        val layoutChanged = newAttributes != oldAttributes
        Log.d(TAG, "onLayout: Finished. Page count: ${imageFilePaths.size}. Layout changed: $layoutChanged")
        callback.onLayoutFinished(pdi, layoutChanged)
    }

    override fun onWrite(
        pages: Array<out PageRange>?, // The specific pages the system wants us to write
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        Log.d(TAG, "onWrite called for pages: ${pages?.joinToString { "[${it.start}-${it.end}]" }}")

        if (cancellationSignal?.isCanceled == true) {
            Log.d(TAG, "onWrite: Cancelled by signal.")
            callback.onWriteCancelled()
            // It's good practice to try and close the destination if we're cancelling early,
            // though the system should also handle it.
            try { destination.close() } catch (e: IOException) { Log.w(TAG, "onWrite: Error closing destination on cancellation.", e)}
            return
        }

        val pdfDocument = PdfDocument()
        var success = false

        try {
            // Iterate through all image paths, but only write pages requested by the 'pages' array.
            imageFilePaths.forEachIndexed { index, imagePath ->
                // Check if the current page (index) is requested by the system.
                // Page numbers in PageRange are 0-indexed.
                val pageNumberInDocument = index // 0-indexed for our list
                var shouldWriteThisPage = false
                pages?.forEach { range ->
                    if (pageNumberInDocument >= range.start && pageNumberInDocument <= range.end) {
                        shouldWriteThisPage = true
                        return@forEach // Found in a range, no need to check further ranges for this page
                    }
                }
                // If pages array is null, or contains PageRange.ALL_PAGES, or if explicitly requested.
                if (pages == null || pages.isEmpty() || pages.any { it == PageRange.ALL_PAGES } || shouldWriteThisPage) {
                    // Proceed to write this page
                } else {
                    Log.d(TAG, "onWrite: Skipping page ${index + 1} (index $index) as it's not in requested ranges.")
                    return@forEachIndexed // Continue to the next image path
                }


                if (cancellationSignal?.isCanceled == true) { // Check before processing each page
                    Log.d(TAG, "onWrite: Cancelled during page ${index + 1} processing.")
                    throw IOException("Print job cancelled during page write.")
                }

                Log.d(TAG, "onWrite: Writing page ${index + 1} (index $index) from path: $imagePath")
                val imageFile = File(imagePath)
                if (!imageFile.exists()) {
                    Log.e(TAG, "onWrite: Image file NOT FOUND for page ${index + 1}: $imagePath")
                    drawErrorPageOnCanvas(pdfDocument, index + 1, "Error: Image file missing.")
                    return@forEachIndexed // Skip this page, continue to next, or throw to fail job
                }

                val bitmap = BitmapFactory.decodeFile(imagePath)
                if (bitmap == null) {
                    Log.e(TAG, "onWrite: Failed to decode bitmap from $imagePath for page ${index + 1}")
                    drawErrorPageOnCanvas(pdfDocument, index + 1, "Error loading image") // Draw error on PDF
                    return@forEachIndexed // Skip this page, continue to next
                }

                // Page dimensions match image dimensions
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create() // PdfDocument page numbers are 1-based
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                bitmap.recycle()
                Log.d(TAG, "onWrite: Finished writing page ${index + 1}")
            }

            // Write the complete PDF document to the destination file descriptor
            FileOutputStream(destination.fileDescriptor).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            success = true
            // Tell the system that we've written the N pages it asked for.
            callback.onWriteFinished(pages ?: arrayOf(PageRange.ALL_PAGES))
            Log.d(TAG, "onWrite: Finished successfully for requested pages.")

        } catch (e: Exception) { // Catching generic Exception is safer here
            callback.onWriteFailed(e.toString())
            Log.e(TAG, "onWrite: Error during write operation", e)
        } finally {
            pdfDocument.close() // Always close the PdfDocument
            // The 'destination' ParcelFileDescriptor is managed by the system and should not be closed here
            // if FileOutputStream was used with it (especially with .use {} which closes the underlying stream).
            // If we manually opened 'destination' without 'use', we'd close it.
            Log.d(TAG, "onWrite: PdfDocument closed.")
            // *** CRITICAL: DO NOT CLEANUP TEMP FILES HERE ***
        }
    }

    private fun drawErrorPageOnCanvas(pdfDocument: PdfDocument, pageNumberForPdfDoc: Int, message: String) {
        Log.w(TAG, "drawErrorPageOnCanvas: Drawing error on page $pageNumberForPdfDoc: $message")
        // Use a standard page size like A4 for error pages if image dimensions aren't known/valid
        val errorPageWidth = (PrintAttributes.MediaSize.ISO_A4.widthMils / 1000f * 72f).toInt()
        val errorPageHeight = (PrintAttributes.MediaSize.ISO_A4.heightMils / 1000f * 72f).toInt()

        val pageInfo = PdfDocument.PageInfo.Builder(errorPageWidth, errorPageHeight, pageNumberForPdfDoc).create()
        val errorPage = pdfDocument.startPage(pageInfo)
        val canvas = errorPage.canvas
        val paint = Paint().apply {
            color = Color.RED
            textSize = 30f // Adjusted for potentially smaller A4 page
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(message, canvas.width / 2f, canvas.height / 2f, paint)
        pdfDocument.finishPage(errorPage)
    }

    override fun onFinish() {
        super.onFinish()
        Log.d(TAG, "onFinish: Print job ended (completed, cancelled, or failed). Cleaning up temp files.")
        // Clean up temporary image files now.
        cleanupTempFiles()
        adapterScope.cancel() // Cancel the adapter's coroutine scope if it was used for anything else
    }

    private fun cleanupTempFiles() {
        // Launching on a background thread for file I/O.
        // Using the adapterScope which uses Dispatchers.IO if defined that way, or a simple new thread.
        adapterScope.launch(Dispatchers.IO) { // Or Thread { ... }.start()
            Log.d(TAG, "cleanupTempFiles: Starting cleanup of ${imageFilePaths.size} files.")
            var deletedCount = 0
            imageFilePaths.forEach { path ->
                try {
                    val file = File(path)
                    if (file.exists()) {
                        if (file.delete()) {
                            deletedCount++
                            Log.d(TAG, "cleanupTempFiles: Deleted: $path")
                        } else {
                            Log.w(TAG, "cleanupTempFiles: Failed to delete: $path")
                        }
                    } else {
                        Log.w(TAG, "cleanupTempFiles: File not found for deletion: $path")
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "cleanupTempFiles: SecurityException deleting file: $path", e)
                } catch (e: Exception) {
                    Log.e(TAG, "cleanupTempFiles: Error deleting file: $path", e)
                }
            }
            Log.d(TAG, "cleanupTempFiles: Finished. Deleted $deletedCount of ${imageFilePaths.size} files.")
        }
    }
}