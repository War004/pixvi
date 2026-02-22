package com.cryptic.pixvi.auth.data

import android.content.Context
import android.util.Base64
import android.util.Log
import com.cryptic.pixvi.core.tink.TinkManager

class TokenStorage(private val context: Context) {

    private val tinkManager = TinkManager(context)

    fun saveToken(token: String): String {
        /*

         */
        val ciphertextBytes = tinkManager.aead.encrypt(token.toByteArray(Charsets.UTF_8), null)
        Log.d("Tint","$token, ${Base64.encodeToString(ciphertextBytes, Base64.DEFAULT)} ")

        return Base64.encodeToString(ciphertextBytes, Base64.DEFAULT)
    }

    fun getToken(encryptedToken: String): String {
        // Decode from Base64 back to raw bytes
        val encryptedBytes = Base64.decode(encryptedToken, Base64.DEFAULT)

        // Decrypt
        val decryptedBytes = tinkManager.aead.decrypt(encryptedBytes, null)

        return String(decryptedBytes, Charsets.UTF_8)
    }
}