package com.cryptic.piyek.feature.onboarding

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

class OnboardingManager{
    fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)

        // URL_SAFE: Replaces + and / with - and _
        // NO_PADDING: Removes the = at the end
        // NO_WRAP: Keeps it as one single line
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)

        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun generateLoginUrl(codeVerifier: String): String {
        val codeChallenge = generateCodeChallenge(codeVerifier)
        return "https://app-api.pixiv.net/web/v1/login?code_challenge=$codeChallenge&code_challenge_method=S256&client=pixiv-android"
    }
}