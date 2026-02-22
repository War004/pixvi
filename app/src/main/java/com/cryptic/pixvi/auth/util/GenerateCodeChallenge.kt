package com.cryptic.pixvi.auth.util

import android.util.Base64
import java.security.MessageDigest

fun generateCodeChallenge(verifier: String): String {
    val bytes = verifier.toByteArray(Charsets.US_ASCII)
    val messageDigest = MessageDigest.getInstance("SHA-256")
    val digest = messageDigest.digest(bytes)

    return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}