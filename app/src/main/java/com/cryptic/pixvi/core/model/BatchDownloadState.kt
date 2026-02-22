package com.cryptic.pixvi.core.model

import android.net.Uri

sealed class BatchDownloadState {
    data class Progress(
        val current: Int,
        val total: Int,
        val recentUri: Uri? = null
    ) : BatchDownloadState()

    data class Completed(
        val results: List<Uri>,
        val successCount: Int,
        val failCount: Int
    ) : BatchDownloadState()
}