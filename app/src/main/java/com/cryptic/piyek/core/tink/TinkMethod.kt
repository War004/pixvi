package com.cryptic.piyek.core.tink

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TinkMethod(private val tinkManager: TinkManager) {

    private val emptyAad = ByteArray(0)

    suspend fun saveToken(token: String): String {
        return withContext(Dispatchers.IO) {
            val ciphertextBytes = tinkManager.aead.encrypt(token.toByteArray(Charsets.UTF_8), emptyAad)
            Base64.encodeToString(ciphertextBytes, Base64.DEFAULT)
        }
    }

    suspend fun getToken(encryptedToken: String): String {
        return withContext(Dispatchers.IO) {
            val encryptedBytes = Base64.decode(encryptedToken, Base64.DEFAULT)
            val decryptedBytes = tinkManager.aead.decrypt(encryptedBytes, emptyAad)
            String(decryptedBytes, Charsets.UTF_8)
        }
    }
}