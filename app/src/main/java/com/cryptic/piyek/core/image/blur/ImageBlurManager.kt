package com.cryptic.piyek.core.image.blur

/**
 * Manages the lifecycle of low-level CPU blur execution components.
 * Scoped to AppContainer (Application lifetime).
 */
class ImageBlurManager(
    val config: BlurConfig = BlurConfig(radius = 4, passes = 2),
    poolSize: Int = 2,
    maxCacheEntries: Int = 120
) {
    val engine: CpuBoxBlurEngine = CpuBoxBlurEngine(config)
    val workspacePool: BlurWorkspacePool = BlurWorkspacePool(poolSize = poolSize)
    val singleFlight: SingleFlightCoalescer = SingleFlightCoalescer()
    val lruCache: MicroBlurLruCache = MicroBlurLruCache(maxEntries = maxCacheEntries)
}