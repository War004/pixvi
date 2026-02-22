package com.cryptic.pixvi.experimental

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.os.Debug
import android.os.Environment
import android.os.Process
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import kotlin.system.measureTimeMillis

/**
 * Benchmark test to compare different blur implementations.
 * 
 * Run with: ./gradlew connectedAndroidTest --tests "*.BlurBenchmarkTest"
 * 
 * Results will appear in Logcat with tag "BlurBench"
 */
@RunWith(AndroidJUnit4::class)
class BlurBenchmarkTest {

    private lateinit var context: Context
    private lateinit var testBitmap: Bitmap
    private lateinit var mutableBitmap: Bitmap

    companion object {
        private const val TAG = "BlurBench"
        private const val WARMUP_ITERATIONS = 3
        private const val BENCHMARK_ITERATIONS = 10
        private const val BLUR_RADIUS = 25f // For RenderScript (max 25)
        
        // Set this to true to run the visual verification test
        // Default is false to keep CI/Benchmarks fast
        private const val RUN_VISUAL_VERIFICATION = true
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Try to load test image from androidTest assets, or create a synthetic one
        val testContext = InstrumentationRegistry.getInstrumentation().context
        testBitmap = try {
            // Load from androidTest/assets (use test context, not target context)
            testContext.assets.open("test_blur_image.jpg").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: throw Exception("Decode returned null")
        } catch (e: Exception) {
            // Create a synthetic 1200x800 test bitmap with gradients
            Log.w(TAG, "Test image not found (${e.message}), creating synthetic bitmap")
            createSyntheticTestBitmap(1200, 800)
        }
        
        // Create mutable copy for operations that need it
        mutableBitmap = testBitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "Test Image: ${testBitmap.width}x${testBitmap.height}")
        Log.d(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        Log.d(TAG, "Android API: ${Build.VERSION.SDK_INT}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
    
    /**
     * Creates a synthetic test bitmap with gradients and patterns.
     * This provides consistent, reproducible benchmark results.
     */
    private fun createSyntheticTestBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Create a gradient with some variation
                val r = (x * 255 / width)
                val g = (y * 255 / height)
                val b = ((x + y) * 127 / (width + height))
                pixels[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    @Test
    fun benchmarkAllBlurMethods() {
        runBlocking {
        val results = mutableListOf<BenchmarkResult>()

        // ─────────────────────────────────────────────────────────────────
        // Test all methods at different scale factors
        // ─────────────────────────────────────────────────────────────────
        val scaleFactors = listOf(0.10f, 0.25f, 0.50f)
        val slidingBlur = SlidingWindowBoxBlur()

        for (scale in scaleFactors) {
            val scalePercent = (scale * 100).toInt()
            
            // 1. SlidingWindowBoxBlur at this scale
            Log.d(TAG, "Testing: SlidingWindowBoxBlur ($scalePercent% scale)...")
            val slidingResult = benchmark("SlidingWindow ($scalePercent%)") {
                // Copy bitmap since blur() now recycles input
                val inputCopy = testBitmap.copy(Bitmap.Config.ARGB_8888, false)
                slidingBlur.blur(inputCopy, SlidingWindowBoxBlur.BlurConfig(
                    scaleFactor = scale,
                    kernelSize = 9,
                    passes = 2
                ))
            }
            results.add(slidingResult)
            
            // 2. RenderScript with same downscale approach for fair comparison
            Log.d(TAG, "Testing: RenderScript ($scalePercent% scale)...")
            @Suppress("DEPRECATION")
            val rsResult = benchmark("RenderScript ($scalePercent%)") {
                renderScriptBlurWithScale(testBitmap, BLUR_RADIUS, scale)
            }
            results.add(rsResult)
            
            // 3. RenderEffect at this scale (API 31+ GPU blur)
            if (Build.VERSION.SDK_INT >= 31) {
                Log.d(TAG, "Testing: RenderEffect ($scalePercent% scale)...")
                val reResult = benchmark("RenderEffect ($scalePercent%)") {
                    renderEffectBlur(testBitmap, BLUR_RADIUS, scale)
                }
                results.add(reResult)
            }
        }
        
        // RenderScript at full resolution (native approach)
        Log.d(TAG, "Testing: RenderScript (100% - native)...")
        @Suppress("DEPRECATION")
        val rsFullResult = benchmark("RenderScript (100%)") {
            renderScriptBlur(mutableBitmap.copy(Bitmap.Config.ARGB_8888, true), BLUR_RADIUS)
        }
        results.add(rsFullResult)
        
        // RenderEffect at full resolution (API 31+)
        if (Build.VERSION.SDK_INT >= 31) {
            Log.d(TAG, "Testing: RenderEffect (100% - native)...")
            val reFullResult = benchmark("RenderEffect (100%)") {
                renderEffectBlur(testBitmap, BLUR_RADIUS, 1.0f)
            }
            results.add(reFullResult)
        }

        // ─────────────────────────────────────────────────────────────────
        // Print Summary
        // ─────────────────────────────────────────────────────────────────
        Log.d(TAG, "")
        Log.d(TAG, "═══════════════════════════════════════════════════════════════════════════════════════════════")
        Log.d(TAG, "                                      BENCHMARK RESULTS                                         ")
        Log.d(TAG, "═══════════════════════════════════════════════════════════════════════════════════════════════")
        Log.d(TAG, String.format("%-24s %7s %6s %6s %9s %9s %9s %7s", 
            "Method", "Avg", "Min", "Max", "Heap(KB)", "Native(KB)", "Total(KB)", "CPU(ms)"))
        Log.d(TAG, "───────────────────────────────────────────────────────────────────────────────────────────────")
        
        val fastest = results.minByOrNull { it.averageMs }
        val lowestMem = results.minByOrNull { it.totalMemoryKB }
        val lowestCpu = results.minByOrNull { it.cpuTimeMs }
        
        results.sortedBy { it.averageMs }.forEach { result ->
            val timeMarker = if (result == fastest) "🏆" else "  "
            val memMarker = if (result == lowestMem) "💾" else "  "
            val cpuMarker = if (result == lowestCpu) "⚡" else "  "
            
            Log.d(TAG, String.format("%-24s %5.1fms %4dms %4dms %7dKB %9dKB %8dKB %5dms %s%s%s",
                result.name,
                result.averageMs,
                result.minMs,
                result.maxMs,
                result.peakHeapKB,
                result.peakNativeKB,
                result.totalMemoryKB,
                result.cpuTimeMs,
                timeMarker, memMarker, cpuMarker
            ))
        }
        
        Log.d(TAG, "═══════════════════════════════════════════════════════════════════════════════════════════════")
        Log.d(TAG, "Legend: 🏆=Fastest  💾=Lowest Total Memory  ⚡=Lowest CPU")
        Log.d(TAG, "Iterations: $BENCHMARK_ITERATIONS per method")
        Log.d(TAG, "═══════════════════════════════════════════════════════════════════════════════════════════════")
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────
    // Visual Verification: Save blur outputs to device storage
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun saveBlurOutputForVerification() {
        if (!RUN_VISUAL_VERIFICATION) {
            Log.i(TAG, "Skipping visual verification (RUN_VISUAL_VERIFICATION = false)")
            return
        }

        runBlocking {
            
            Log.d(TAG, "═══════════════════════════════════════════════════════════════════════════════════════════════")
            Log.d(TAG, "                           SAVING BLUR OUTPUTS FOR VERIFICATION                                 ")
            Log.d(TAG, "═══════════════════════════════════════════════════════════════════════════════════════════════")
            Log.d(TAG, "Output: Pictures/blur_test folder (accessible via Gallery or File Manager)")
            
            // Save original image
            saveImage(testBitmap, "00_original.png")
            Log.d(TAG, "Saved: 00_original.png")
            
            val slidingBlur = SlidingWindowBoxBlur()
            val scaleFactors = listOf(0.10f, 0.25f, 0.50f)
            
            for (scale in scaleFactors) {
                val scalePercent = (scale * 100).toInt()
                
                // SlidingWindowBoxBlur output
                val inputCopy = testBitmap.copy(Bitmap.Config.ARGB_8888, false)
                val slidingResult = slidingBlur.blur(inputCopy, SlidingWindowBoxBlur.BlurConfig(
                    scaleFactor = scale,
                    kernelSize = 9,
                    passes = 2
                ))
                saveImage(slidingResult, "sliding_${scalePercent}pct.png")
                Log.d(TAG, "Saved: sliding_${scalePercent}pct.png")
                slidingResult.recycle()
                
                // RenderScript output
                @Suppress("DEPRECATION")
                val rsResult = renderScriptBlurWithScale(testBitmap, BLUR_RADIUS, scale)
                saveImage(rsResult, "renderscript_${scalePercent}pct.png")
                Log.d(TAG, "Saved: renderscript_${scalePercent}pct.png")
                rsResult.recycle()
                
                // RenderEffect output (API 31+)
                if (Build.VERSION.SDK_INT >= 31) {
                    val reResult = renderEffectBlur(testBitmap, BLUR_RADIUS, scale)
                    saveImage(reResult, "rendereffect_${scalePercent}pct.png")
                    Log.d(TAG, "Saved: rendereffect_${scalePercent}pct.png")
                    reResult.recycle()
                }
            }
            
            // Full resolution RenderScript
            @Suppress("DEPRECATION")
            val rsFullResult = renderScriptBlur(mutableBitmap.copy(Bitmap.Config.ARGB_8888, true), BLUR_RADIUS)
            saveImage(rsFullResult, "renderscript_100pct.png")
            Log.d(TAG, "Saved: renderscript_100pct.png")
            rsFullResult.recycle()
            
            // Full resolution RenderEffect
            if (Build.VERSION.SDK_INT >= 31) {
                val reFullResult = renderEffectBlur(testBitmap, BLUR_RADIUS, 1.0f)
                saveImage(reFullResult, "rendereffect_100pct.png")
                Log.d(TAG, "Saved: rendereffect_100pct.png")
                reFullResult.recycle()
            }
            
            slidingBlur.clearCache()
            
            Log.d(TAG, "═══════════════════════════════════════════════════════════════════════════════════════════════")
            Log.d(TAG, "All images saved to Pictures/blur_test folder!")
            Log.d(TAG, "View via: Gallery app, Files app, or adb pull /storage/emulated/0/Pictures/blur_test/")
            Log.d(TAG, "═══════════════════════════════════════════════════════════════════════════════════════════════")
        }
    }
    
    private fun saveImage(bitmap: Bitmap, filename: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/blur_test")
        }
        
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Benchmark Helper with Memory & CPU Tracking
    // ─────────────────────────────────────────────────────────────────────
    private suspend fun benchmark(
        name: String,
        block: suspend () -> Bitmap
    ): BenchmarkResult {
        val runtime = Runtime.getRuntime()
        
        // Warm up (let JIT optimize)
        repeat(WARMUP_ITERATIONS) {
            block().recycle()
        }
        
        // Force GC before measurement for accurate baseline
        System.gc()
        Thread.sleep(100)
        
        // Capture baseline metrics - both heap and native
        val startCpuTime = Debug.threadCpuTimeNanos()
        val startHeapMemory = runtime.totalMemory() - runtime.freeMemory()
        val startNativeMemory = Debug.getNativeHeapAllocatedSize()
        var peakHeapMemory = startHeapMemory
        var peakNativeMemory = startNativeMemory
        
        // Actual benchmark
        val times = mutableListOf<Long>()
        repeat(BENCHMARK_ITERATIONS) {
            val time = measureTimeMillis {
                block().recycle()
            }
            times.add(time)
            
            // Track peak memory during iterations (both heap and native)
            val currentHeap = runtime.totalMemory() - runtime.freeMemory()
            val currentNative = Debug.getNativeHeapAllocatedSize()
            if (currentHeap > peakHeapMemory) peakHeapMemory = currentHeap
            if (currentNative > peakNativeMemory) peakNativeMemory = currentNative
        }
        
        // Capture end metrics
        val endCpuTime = Debug.threadCpuTimeNanos()
        val endHeapMemory = runtime.totalMemory() - runtime.freeMemory()
        val endNativeMemory = Debug.getNativeHeapAllocatedSize()
        
        // Calculate metrics
        val heapMemoryKB = ((endHeapMemory - startHeapMemory) / 1024).coerceAtLeast(0)
        val peakHeapKB = ((peakHeapMemory - startHeapMemory) / 1024).coerceAtLeast(0)
        val nativeMemoryKB = ((endNativeMemory - startNativeMemory) / 1024).coerceAtLeast(0)
        val peakNativeKB = ((peakNativeMemory - startNativeMemory) / 1024).coerceAtLeast(0)
        val cpuTimeMs = (endCpuTime - startCpuTime) / 1_000_000

        return BenchmarkResult(
            name = name,
            averageMs = times.average(),
            minMs = times.minOrNull() ?: 0,
            maxMs = times.maxOrNull() ?: 0,
            heapMemoryKB = heapMemoryKB,
            peakHeapKB = peakHeapKB,
            nativeMemoryKB = nativeMemoryKB,
            peakNativeKB = peakNativeKB,
            totalMemoryKB = peakHeapKB + peakNativeKB,
            cpuTimeMs = cpuTimeMs
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // RenderScript Blur Implementation (for comparison)
    // ─────────────────────────────────────────────────────────────────────
    @Suppress("DEPRECATION")
    private fun renderScriptBlur(bitmap: Bitmap, radius: Float): Bitmap {
        val rs = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rs, bitmap)
        val output = Allocation.createTyped(rs, input.type)
        
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        script.setRadius(radius.coerceIn(0.1f, 25f))
        script.setInput(input)
        script.forEach(output)
        
        output.copyTo(bitmap)
        
        script.destroy()
        input.destroy()
        output.destroy()
        rs.destroy()
        
        return bitmap
    }
    
    /**
     * RenderScript blur with downscale/upscale to match SlidingWindowBoxBlur approach.
     * This provides a fair comparison at the same scale factors.
     */
    @Suppress("DEPRECATION")
    private fun renderScriptBlurWithScale(bitmap: Bitmap, radius: Float, scaleFactor: Float): Bitmap {
        val originalW = bitmap.width
        val originalH = bitmap.height
        
        // Downscale
        val smallW = (originalW * scaleFactor).toInt().coerceAtLeast(1)
        val smallH = (originalH * scaleFactor).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, smallW, smallH, true)
        val mutableScaled = scaled.copy(Bitmap.Config.ARGB_8888, true)
        if (scaled != mutableScaled) scaled.recycle()
        
        // Blur
        val rs = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rs, mutableScaled)
        val output = Allocation.createTyped(rs, input.type)
        
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        script.setRadius(radius.coerceIn(0.1f, 25f))
        script.setInput(input)
        script.forEach(output)
        
        output.copyTo(mutableScaled)
        
        script.destroy()
        input.destroy()
        output.destroy()
        rs.destroy()
        
        // Upscale back to original size
        val result = Bitmap.createScaledBitmap(mutableScaled, originalW, originalH, true)
        if (result != mutableScaled) mutableScaled.recycle()
        
        return result
    }
    
    // ─────────────────────────────────────────────────────────────────────
    // RenderEffect Blur Implementation (API 31+ GPU blur)
    // ─────────────────────────────────────────────────────────────────────
    @RequiresApi(31)
    private fun renderEffectBlur(bitmap: Bitmap, radius: Float, scaleFactor: Float): Bitmap {
        val originalW = bitmap.width
        val originalH = bitmap.height
        
        // Downscale if needed
        val targetW = (originalW * scaleFactor).toInt().coerceAtLeast(1)
        val targetH = (originalH * scaleFactor).toInt().coerceAtLeast(1)
        val scaledBitmap = if (scaleFactor < 1.0f) {
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        } else {
            bitmap
        }
        
        val width = scaledBitmap.width
        val height = scaledBitmap.height
        
        // Create ImageReader to receive the rendered output
        val imageReader = ImageReader.newInstance(
            width, height,
            PixelFormat.RGBA_8888,
            1,
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        )
        
        // Create RenderNode with blur effect
        val renderNode = RenderNode("BlurNode").apply {
            setPosition(0, 0, width, height)
            setRenderEffect(
                RenderEffect.createBlurEffect(
                    radius.coerceIn(0.1f, 150f),
                    radius.coerceIn(0.1f, 150f),
                    Shader.TileMode.CLAMP
                )
            )
        }
        
        // Draw the bitmap into the RenderNode
        val canvas = renderNode.beginRecording()
        canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
        renderNode.endRecording()
        
        // Create HardwareRenderer and render
        val renderer = HardwareRenderer().apply {
            setContentRoot(renderNode)
            setSurface(imageReader.surface)
        }
        
        // Request render and wait for completion
        renderer.createRenderRequest()
            .setWaitForPresent(true)
            .syncAndDraw()
        
        // Get the result from ImageReader
        val image = imageReader.acquireLatestImage()
        val hardwareBuffer = image.hardwareBuffer
        val resultBitmap = if (hardwareBuffer != null) {
            Bitmap.wrapHardwareBuffer(hardwareBuffer, null)!!
                .copy(Bitmap.Config.ARGB_8888, false)
        } else {
            // Fallback if hardware buffer unavailable
            scaledBitmap.copy(Bitmap.Config.ARGB_8888, false)
        }
        
        // Cleanup
        hardwareBuffer?.close()
        image.close()
        renderer.destroy()
        renderNode.discardDisplayList()
        imageReader.close()
        if (scaleFactor < 1.0f && scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        
        // Upscale back to original size if needed
        val finalResult = if (scaleFactor < 1.0f) {
            Bitmap.createScaledBitmap(resultBitmap, originalW, originalH, true).also {
                if (it != resultBitmap) resultBitmap.recycle()
            }
        } else {
            resultBitmap
        }
        
        return finalResult
    }

    // ─────────────────────────────────────────────────────────────────────
    // Result Data Class
    // ─────────────────────────────────────────────────────────────────────
    data class BenchmarkResult(
        val name: String,
        val averageMs: Double,
        val minMs: Long,
        val maxMs: Long,
        val heapMemoryKB: Long,       // Java heap memory allocated
        val peakHeapKB: Long,         // Peak Java heap usage
        val nativeMemoryKB: Long,     // Native/GPU memory allocated
        val peakNativeKB: Long,       // Peak native memory usage
        val totalMemoryKB: Long,      // Total = heap + native
        val cpuTimeMs: Long           // CPU time consumed
    )
}
