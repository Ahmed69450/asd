package com.ovos.arabicassistant.data.ai.engine_b

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlamaCppEngineTest {

    private class FakeLlamaBridge : LlamaCppBridge() {
        var loadedPath: String? = null
        var loadedThreads: Int = 0

        override fun loadModel(modelPath: String, threads: Int): Boolean {
            loadedPath = modelPath
            loadedThreads = threads
            return true
        }

        override fun generate(prompt: String, onToken: (String) -> Unit): Boolean {
            onToken("هذا ")
            onToken("رد ")
            onToken("تجريبي.")
            return true
        }

        override fun freeModel() {
            loadedPath = null
        }
    }

    @Test
    fun `clamps cpu threads strictly between 2 and 3 threads`() {
        val fakeBridge = FakeLlamaBridge()
        val engine = LlamaCppEngine(bridge = fakeBridge)

        // Requesting 8 threads
        engine.loadModel("/sdcard/model.gguf", requestedThreads = 8)
        assertEquals(3, engine.activeThreads)
        assertEquals(3, fakeBridge.loadedThreads)

        // Requesting 1 thread
        engine.loadModel("/sdcard/model.gguf", requestedThreads = 1)
        assertEquals(2, engine.activeThreads)
        assertEquals(2, fakeBridge.loadedThreads)
    }

    @Test
    fun `streams tokens when model is loaded`() = runBlocking {
        val fakeBridge = FakeLlamaBridge()
        val engine = LlamaCppEngine(bridge = fakeBridge)
        engine.loadModel("/sdcard/model.gguf", requestedThreads = 2)

        val tokens = engine.generateStream("ما هو الطقس؟", "[سياق: الرياض]").toList()
        assertEquals(listOf("هذا ", "رد ", "تجريبي."), tokens)
    }

    @Test
    fun `returns guidance message when no model is loaded`() = runBlocking {
        val fakeBridge = FakeLlamaBridge()
        val engine = LlamaCppEngine(bridge = fakeBridge) // not loaded

        val tokens = engine.generateStream("من أنت؟", "").toList()
        assertTrue(tokens.joinToString("").contains("يرجى اختيار نموذج"))
    }
}
