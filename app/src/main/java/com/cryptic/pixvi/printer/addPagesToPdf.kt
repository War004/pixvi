package com.cryptic.pixvi.printer

import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

//not a service
/*
 */

suspend fun addPagesToPdf(document: PdfDocument, pageIndex: Int, bitmap: Bitmap) =withContext(Dispatchers.Default){
    //make the page with respect to the
    val pageInfo = PdfDocument.PageInfo.Builder(
        bitmap.width,
        bitmap.height,
        pageIndex
    )
    //
    val page = document.startPage(pageInfo.create())

    page.canvas.drawBitmap(bitmap, 0f, 0f, null)

    document.finishPage(page)
}