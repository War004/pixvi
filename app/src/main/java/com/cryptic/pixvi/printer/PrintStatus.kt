package com.cryptic.pixvi.printer

import android.net.Uri

sealed class PrintStatus{
    data class Success(val fileUri: Uri, val safeFolder: String): PrintStatus()
    data class Error(val errorCode: Int? = null, val errorMessage: String? = null, val solution: String?= null): PrintStatus()
}