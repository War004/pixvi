package com.cryptic.piyek.core.image.blur

import android.graphics.Bitmap
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

/**
 * Request coalescer: Joins concurrent requests for the identical key.
 */
class SingleFlightCoalescer {
    private val activeJobs = ConcurrentHashMap<String, CompletableDeferred<Bitmap?>>()

    suspend fun joinOrExecute(key: String, block: suspend () -> Bitmap?): Bitmap? {
        var isLeader = false
        val deferred = activeJobs.computeIfAbsent(key) {
            isLeader = true
            CompletableDeferred()
        }

        if (!isLeader) {
            return deferred.await()
        }

        return try {
            val result = block()
            deferred.complete(result)
            result
        } catch (t: Throwable) {
            deferred.complete(null)
            null
        } finally {
            activeJobs.remove(key)
        }
    }
}