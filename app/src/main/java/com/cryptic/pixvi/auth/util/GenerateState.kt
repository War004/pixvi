package com.cryptic.pixvi.auth.util

import android.util.Base64
import java.security.SecureRandom

fun generateState(): String {
    val bytes = ByteArray(24)
    SecureRandom().nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}