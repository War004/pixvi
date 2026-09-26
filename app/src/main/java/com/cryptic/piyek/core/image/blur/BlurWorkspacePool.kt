package com.cryptic.piyek.core.image.blur

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Bounded pool containing strictly 2 workspaces (16 KB total scratch RAM).
 */
class BlurWorkspacePool(poolSize: Int = 2) {
    private val semaphore = Semaphore(poolSize)
    private val pool = Array(poolSize) { BlurWorkspace() }
    private val available = ArrayDeque<BlurWorkspace>(poolSize).apply {
        pool.forEach { add(it) }
    }

    suspend fun <T> useWorkspace(block: (BlurWorkspace) -> T): T {
        semaphore.withPermit {
            val workspace = synchronized(available) { available.removeFirst() }
            try {
                return block(workspace)
            } finally {
                synchronized(available) { available.addLast(workspace) }
            }
        }
    }
}