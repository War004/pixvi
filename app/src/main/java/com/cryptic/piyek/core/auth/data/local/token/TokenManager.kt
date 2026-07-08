package com.cryptic.piyek.core.auth.data.local.token

import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicReference

class TokenManager : TokenStorage {

    private val accessTokenReference = AtomicReference<String?>(null)

    override val writeMutex = Mutex()

    override fun getAccessToken(): String? {
        return accessTokenReference.get()
    }

    override fun setAccessToken(token: String?) {
        accessTokenReference.set(token)
    }

    override fun clearToken() {
        accessTokenReference.set(null)
    }
}