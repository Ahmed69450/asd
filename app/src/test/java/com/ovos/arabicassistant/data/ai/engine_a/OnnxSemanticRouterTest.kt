package com.ovos.arabicassistant.data.ai.engine_a

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnnxSemanticRouterTest {

    @Test
    fun `cosine similarity calculation is exact for identical and orthogonal vectors`() {
        val vec1 = floatArrayOf(1f, 0f, 0f)
        val vec2 = floatArrayOf(1f, 0f, 0f)
        val vec3 = floatArrayOf(0f, 1f, 0f)
        assertEquals(1.0f, CosineSimilarity.compute(vec1, vec2), 0.001f)
        assertEquals(0.0f, CosineSimilarity.compute(vec1, vec3), 0.001f)
    }

    @Test
    fun `routes to hardware when similarity is 70 percent or higher`() {
        val anchors = listOf(
            AnchorVector("ac_temp_down", "برد المكيف", floatArrayOf(1.0f, 0.0f, 0.0f)),
            AnchorVector("volume_up", "ارفع الصوت", floatArrayOf(0.0f, 1.0f, 0.0f))
        )
        val router = OnnxSemanticRouter(testAnchors = anchors)

        // Query vector close to ac_temp_down (0.95 similarity)
        val queryVector = floatArrayOf(0.95f, 0.31f, 0.0f)
        val result = router.routeWithVector("نزل الحرارة", queryVector)

        assertTrue(result.confidence >= 0.70f)
        assertEquals("ac_temp_down", result.intent)
        assertTrue(result.isHardware)
    }

    @Test
    fun `falls back when similarity is below 70 percent`() {
        val anchors = listOf(
            AnchorVector("ac_temp_down", "برد المكيف", floatArrayOf(1.0f, 0.0f, 0.0f))
        )
        val router = OnnxSemanticRouter(testAnchors = anchors)

        // Query vector with low similarity (~0.40)
        val queryVector = floatArrayOf(0.40f, 0.90f, 0.0f)
        val result = router.routeWithVector("حدثني عن تاريخ الصين", queryVector)

        assertFalse(result.isConfidentHardwareCommand)
        assertFalse(result.isHardware)
    }

    @Test
    fun `fast pattern matching detects vehicle phrases instantly`() {
        val router = OnnxSemanticRouter(testAnchors = emptyList())
        val result = router.fastKeywordRoute("افتح النافذة")

        assertTrue(result.isConfidentHardwareCommand)
        assertEquals("window_open", result.intent)
        assertEquals(0.99f, result.confidence, 0.01f)
    }
}
