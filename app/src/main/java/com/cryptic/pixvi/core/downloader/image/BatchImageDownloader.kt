package com.cryptic.pixvi.core.downloader.image

import com.cryptic.pixvi.core.network.repo.PixivApiRepo
import android.content.Context
import android.net.Uri
import android.util.Log
import com.cryptic.pixvi.core.model.BatchDownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn


fun batchImageDownloader(
    context: Context,
    imageUrls: List<String>,
    mainFileName: String,
    repo: PixivApiRepo
): Flow<BatchDownloadState> = flow {
    val total = imageUrls.size
    val results = mutableListOf<Uri>()

    var successCount = 0
    var failCount = 0

    val appContext = context.applicationContext

    emit(BatchDownloadState.Progress(0, total))

    //loop through the function,
    imageUrls.forEachIndexed { index, url ->
        val fileName = mainFileName + index
        val response = saveImages(
            context = appContext,
            imageUrl = url,
            fileName = fileName,
            repo = repo
        )

        response.onSuccess { uri ->
            results.add(uri)
            successCount++
        }
        response.onFailure{ exception ->
            failCount++
            Log.e("Downloader",exception.localizedMessage?:exception.message?:"No message for the error.")
        }

        emit(BatchDownloadState.Progress(index +1, total, response.getOrNull()))
        //check for the uri
    }
    //complete
    emit(BatchDownloadState.Completed(results, successCount, failCount))
}.flowOn(Dispatchers.IO)