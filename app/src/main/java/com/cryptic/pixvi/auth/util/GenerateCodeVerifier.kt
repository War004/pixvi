package com.cryptic.pixvi.auth.util

import android.util.Base64
import java.security.SecureRandom

fun generateCodeVerifier(): String {
    val secureRandom = SecureRandom()
    val bytes = ByteArray(32)
    secureRandom.nextBytes(bytes)

    // URL_SAFE: Replaces + and / with - and _
    // NO_PADDING: Removes the = at the end
    // NO_WRAP: Keeps it as one single line
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}