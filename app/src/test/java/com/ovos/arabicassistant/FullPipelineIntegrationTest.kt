package com.ovos.arabicassistant

import com.ovos.arabicassistant.accessibility.ScreenContextHolder
import com.ovos.arabicassistant.data.ai.engine_a.AnchorVector
import com.ovos.arabicassistant.data.ai.engine_a.OnnxSemanticRouter
import com.ovos.arabicassistant.data.ai.engine_b.LlamaCppBridge
import com.ovos.arabicassistant.data.ai.engine_b.LlamaCppEngine
import com.ovos.arabicassistant.data.car.BydCarIntentDispatcher
import com.ovos.arabicassistant.data.car.CarControlManager
import com.ovos.arabicassistant.data.car.CarControlRepositoryImpl
import com.ovos.arabicassistant.data.mcp.McpClient
import com.ovos.arabicassistant.data.mcp.McpToolRegistry
import com.ovos.arabicassistant.data.mcp.tools.ScreenAwarenessTool
import com.ovos.arabicassistant.data.mcp.tools.WebSearchTool
import com.ovos.arabicassistant.data.mcp.transport.InAppMcpTransport
import com.ovos.arabicassistant.domain.model.CarActionType
import com.ovos.arabicassistant.domain.model.DynamicContext
import com.ovos.arabicassistant.domain.model.MemoryFact
import com.ovos.arabicassistant.domain.repository.MemoryRepository
import com.ovos.arabicassistant.domain.usecase.ProcessVoiceInputUseCase
import com.ovos.arabicassistant.voice.VoiceTtsFormatter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullPipelineIntegrationTest {

    private class MockCarDispatcher : BydCarIntentDispatcher(null) {
        var lastAcDelta: Int? = null
        override fun dispatchAcCommand(tempDelta: Int): Boolean {
            lastAcDelta = tempDelta
            return true
        }
    }

    private class MockMemoryRepo : MemoryRepository {
        val facts = mutableListOf<MemoryFact>()
        override suspend fun getRelevantFacts(query: String, limit: Int): List<MemoryFact> {
            return facts.filter { it.keywords.contains(query) || it.factText.contains(query) }.take(limit)
        }
        override suspend fun saveFact(fact: MemoryFact): Long {
            facts.add(fact)
            return facts.size.toLong()
        }
    }

    private class MockLlamaBridge : LlamaCppBridge() {
        override fun loadModel(modelPath: String, threads: Int) = true
        override fun generate(prompt: String, onToken: (String) -> Unit): Boolean {
            onToken("## الجواب: **كوكب المشتري** هو أكبر كواكب المجموعة الشمسية 🪐.")
            return true
        }
    }

    @Test
    fun `full end-to-end flow for car hardware command`() = runBlocking {
        val carDispatcher = MockCarDispatcher()
        val carManager = CarControlManager(carDispatcher)
        val carRepo = CarControlRepositoryImpl(carManager)

        val anchors = listOf(
            AnchorVector("ac_temp_down", "برد المكيف", floatArrayOf(1f, 0f, 0f))
        )
        val onnxRouter = OnnxSemanticRouter(testAnchors = anchors)
        val memoryRepo = MockMemoryRepo()

        val llamaEngine = LlamaCppEngine(MockLlamaBridge())
        llamaEngine.loadModel("/sdcard/model.gguf", 2)

        val pipeline = ProcessVoiceInputUseCase(
            router = { onnxRouter.route(it) },
            carRepo = carRepo,
            memoryRepo = memoryRepo,
            llmGenerator = { prompt, context -> llamaEngine.generateStream(prompt, context) }
        )

        // Command: "برد المكيف"
        val result = pipeline.execute("برد المكيف")

        // 1. Check direct hardware execution
        assertTrue(result.isHardwareAction)
        assertFalse(result.isLlmGenerated)
        assertEquals("ac_temp_down", result.intent)
        assertEquals(-1, carDispatcher.lastAcDelta)

        // 2. Check TTS sanitization
        val spoken = VoiceTtsFormatter.sanitize(result.spokenResponse)
        assertEquals("تم خفض درجة حرارة المكيف بنجاح.", spoken)
    }

    @Test
    fun `full end-to-end flow for general LLM query with context and TTS sanitization`() = runBlocking {
        val carDispatcher = MockCarDispatcher()
        val carManager = CarControlManager(carDispatcher)
        val carRepo = CarControlRepositoryImpl(carManager)

        val onnxRouter = OnnxSemanticRouter(testAnchors = emptyList())
        val memoryRepo = MockMemoryRepo()
        memoryRepo.saveFact(MemoryFact(1L, "science", "كوكب", "السائق مهتم بعلم الفلك", 100L))

        val llamaEngine = LlamaCppEngine(MockLlamaBridge())
        llamaEngine.loadModel("/sdcard/model.gguf", 2)

        val pipeline = ProcessVoiceInputUseCase(
            router = { onnxRouter.route(it) },
            carRepo = carRepo,
            memoryRepo = memoryRepo,
            llmGenerator = { prompt, context -> llamaEngine.generateStream(prompt, context) }
        )

        ScreenContextHolder.update("com.google.android.apps.maps", listOf("طريق الملك فهد", "ملاحة"))

        val dynamicContext = DynamicContext(
            time = "18:45",
            date = "2026-10-08",
            city = "الرياض",
            activeScreenSummary = ScreenContextHolder.getCurrentSummary()
        )

        val result = pipeline.execute("حدثني عن كوكب المشتري", dynamicContext)

        // Verify fallback to LLM
        assertFalse(result.isHardwareAction)
        assertTrue(result.isLlmGenerated)

        // Verify raw response contains markdown and emoji from mock LLM
        assertTrue(result.spokenResponse.contains("**"))
        assertTrue(result.spokenResponse.contains("🪐"))

        // Verify TTS Formatter sanitizes the response completely for Piper speech
        val sanitizedForTts = VoiceTtsFormatter.sanitize(result.spokenResponse)
        assertFalse(sanitizedForTts.contains("**"))
        assertFalse(sanitizedForTts.contains("##"))
        assertFalse(sanitizedForTts.contains("🪐"))
        assertEquals("الجواب: كوكب المشتري هو أكبر كواكب المجموعة الشمسية.", sanitizedForTts)
    }

    @Test
    fun `mcp client tool execution for screen awareness and web search`() = runBlocking {
        ScreenContextHolder.update("com.byd.radio", listOf("راديو إف إم 99.0", "موسيقى هادئة"))

        val registry = McpToolRegistry().apply {
            registerTool(ScreenAwarenessTool { ScreenContextHolder.getCurrentSummary() })
            registerTool(object : WebSearchTool(null) {
                override suspend fun execute(query: String) = "درجة الحرارة في الرياض اليوم 28 درجة مئوية."
            })
        }

        val mcpClient = McpClient(InAppMcpTransport(registry))

        val screenResult = mcpClient.callTool("screen_awareness")
        assertTrue(screenResult.isSuccess)
        assertTrue(screenResult.content.contains("com.byd.radio"))
        assertTrue(screenResult.content.contains("راديو إف إم 99.0"))

        val webResult = mcpClient.callTool("web_search", mapOf("query" to "طقس الرياض"))
        assertTrue(webResult.isSuccess)
        assertTrue(webResult.content.contains("28 درجة مئوية"))
    }
}
