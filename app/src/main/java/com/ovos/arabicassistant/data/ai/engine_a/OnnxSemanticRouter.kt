package com.ovos.arabicassistant.data.ai.engine_a

import android.content.Context
import android.util.Log
import com.ovos.arabicassistant.domain.model.RouteResult
import org.json.JSONObject
import java.io.InputStream

/**
 * موجه المعاني الدلالي السريع (Engine A - ONNX Semantic Router)
 * يعتمد على متجهات التضمين الدلالي (Dense Embeddings) وحساب Cosine Similarity،
 * ويطبق عتبة الثقة الصارمة (≥ 70%) لتنفيذ أوامر عتاد السيارة فورياً في أقل من 20ms
 * وتوجيه ما دون ذلك إلى المحرك B (LLM Fallback).
 */
class OnnxSemanticRouter(
    private val context: Context? = null,
    testAnchors: List<AnchorVector>? = null
) {

    companion object {
        private const val TAG = "OnnxSemanticRouter"
        const val CONFIDENCE_THRESHOLD = 0.70f

        // قائمة النوايا العتادية المادية الصريحة للسيارة
        private val HARDWARE_INTENTS = setOf(
            "ac_temp_down", "ac_temp_up", "ac_fan_speed",
            "window_open", "window_close", "sunroof_open", "sunroof_close",
            "volume_up", "volume_down", "volume_mute",
            "media_next", "media_prev", "media_play_pause"
        )

        // الأنماط اللفظية المباشرة للاستجابة الفورية الصفرية (Zero Latency Fast-Path)
        private val FAST_KEYWORD_PATTERNS = mapOf(
            "ac_temp_down" to listOf("برد المكيف", "نزل الحرارة", "خفض الحرارة", "خفض المكيف", "برد السيارة"),
            "ac_temp_up" to listOf("ارفع الحرارة", "سخن المكيف", "سخن السيارة", "دفي المكيف", "زيد الحرارة"),
            "ac_fan_speed" to listOf("قوي المروحة", "خفف المروحة", "سرعة المروحة"),
            "window_open" to listOf("افتح النافذة", "افتح الشباك", "نزل القزاز", "فتح النوافذ", "افتح النوافذ"),
            "window_close" to listOf("سكر النافذة", "اغلق النافذة", "اغلق الشباك", "ارفع القزاز", "سكر النوافذ"),
            "sunroof_open" to listOf("افتح فتحة السقف", "افتح السقف", "فتح السقف"),
            "sunroof_close" to listOf("اغلق فتحة السقف", "سكر السقف", "اغلق السقف"),
            "volume_up" to listOf("ارفع الصوت", "علي الصوت", "زيد الصوت"),
            "volume_down" to listOf("وطي الصوت", "اخفض الصوت", "قصر الصوت"),
            "volume_mute" to listOf("اكتم الصوت", "صامت", "اسكت"),
            "media_next" to listOf("التالي", "المقطع التالي", "الأغنية التالية", "غير الأغنية"),
            "media_prev" to listOf("السابق", "المقطع السابق", "الأغنية السابقة"),
            "media_play_pause" to listOf("شغل الموسيقى", "وقف الموسيقى", "ايقاف مؤقت", "متابعة التشغيل")
        )
    }

    private val intentAnchors = mutableListOf<AnchorVector>()

    init {
        if (testAnchors != null) {
            intentAnchors.addAll(testAnchors)
        } else if (context != null) {
            loadIntentEmbeddings()
        }
    }

    private fun loadIntentEmbeddings() {
        try {
            val jsonStream: InputStream = context!!.assets.open("intent_embeddings.json")
            val jsonString = jsonStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(jsonString)
            val intentsObj = root.optJSONObject("intents") ?: return

            val keys = intentsObj.keys()
            while (keys.hasNext()) {
                val intentName = keys.next()
                val anchorsArray = intentsObj.getJSONArray(intentName)
                for (i in 0 until anchorsArray.length()) {
                    val item = anchorsArray.getJSONObject(i)
                    val text = item.getString("text")
                    val vecArray = item.getJSONArray("vector")
                    val floats = FloatArray(vecArray.length()) { idx ->
                        vecArray.getDouble(idx).toFloat()
                    }
                    intentAnchors.add(AnchorVector(intentName, text, floats))
                }
            }
            Log.d(TAG, "تم تحميل مصفوفة المتجهات الدلالية بنجاح (${intentAnchors.size} نمط).")
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء تحميل intent_embeddings.json: ${e.message}")
        }
    }

    /**
     * الفحص اللفظي السريع المباشر (Fast-Path Matching)
     */
    fun fastKeywordRoute(utterance: String): RouteResult {
        val trimmed = utterance.trim()
        for ((intent, patterns) in FAST_KEYWORD_PATTERNS) {
            if (patterns.any { trimmed.contains(it) }) {
                return RouteResult(
                    intent = intent,
                    confidence = 0.99f,
                    isHardware = intent in HARDWARE_INTENTS,
                    matchedAnchor = trimmed
                )
            }
        }
        return RouteResult(
            intent = "unknown",
            confidence = 0f,
            isHardware = false
        )
    }

    /**
     * التوجيه الدلالي العام مع حساب Cosine Similarity
     */
    fun routeWithVector(utterance: String, vector: FloatArray): RouteResult {
        val fastResult = fastKeywordRoute(utterance)
        if (fastResult.isConfidentHardwareCommand) {
            return fastResult
        }

        if (intentAnchors.isEmpty()) {
            return RouteResult(intent = "fallback", confidence = 0f, isHardware = false)
        }

        var bestIntent = "fallback"
        var bestConfidence = 0f
        var bestAnchor = ""

        for (anchor in intentAnchors) {
            val sim = CosineSimilarity.compute(vector, anchor.vector)
            if (sim > bestConfidence) {
                bestConfidence = sim
                bestIntent = anchor.intent
                bestAnchor = anchor.text
            }
        }

        val isHardware = bestIntent in HARDWARE_INTENTS
        val isConfident = bestConfidence >= CONFIDENCE_THRESHOLD

        return RouteResult(
            intent = if (isConfident) bestIntent else "fallback",
            confidence = bestConfidence,
            isHardware = isHardware && isConfident,
            matchedAnchor = bestAnchor
        )
    }

    /**
     * الواجهة الموحدة للمحرك A
     */
    fun route(utterance: String, embeddingVector: FloatArray? = null): RouteResult {
        val fastResult = fastKeywordRoute(utterance)
        if (fastResult.isConfidentHardwareCommand) {
            return fastResult
        }

        return if (embeddingVector != null) {
            routeWithVector(utterance, embeddingVector)
        } else {
            RouteResult(intent = "fallback", confidence = 0f, isHardware = false)
        }
    }
}
