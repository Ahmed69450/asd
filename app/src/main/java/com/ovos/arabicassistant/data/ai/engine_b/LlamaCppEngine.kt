package com.ovos.arabicassistant.data.ai.engine_b

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * المحرك B: الذكاء الاصطناعي الاحتياطي (Engine B - LLM Fallback)
 * يدير نموذج GGUF الصغير عبر llama.cpp، مقيد بـ 2 إلى 3 أنوية CPU فقط
 * لمنع سخونة معالج شاشة السيارة واستهلاك موارد النظام.
 */
class LlamaCppEngine(
    private val bridge: LlamaCppBridge = LlamaCppBridge()
) {

    companion object {
        private const val TAG = "LlamaCppEngine"
        const val MIN_THREADS = 2
        const val MAX_THREADS = 3

        private const val SYSTEM_PROMPT_PREFIX =
            "أنت مساعد صوتي ذكي ولبق مدمج في سيارة BYD DiLink. أجب باختصار ولغة عربية سليمة وواضحة تناسب السائق أثناء القيادة دون أي تنسيقات ماركداون."
    }

    var isLoaded: Boolean = false
        private set

    var activeThreads: Int = MIN_THREADS
        private set

    var currentModelPath: String? = null
        private set

    /**
     * تحميل نموذج GGUF وضبط عدد الأنوية المقيدة
     */
    fun loadModel(modelPath: String, requestedThreads: Int = MIN_THREADS): Boolean {
        if (modelPath.isBlank()) return false

        // تقييد صارم لعدد أنوية المعالج لحماية نظام السيارة
        activeThreads = requestedThreads.coerceIn(MIN_THREADS, MAX_THREADS)

        val success = bridge.loadModel(modelPath, activeThreads)
        isLoaded = success
        if (success) {
            currentModelPath = modelPath
            Log.i(TAG, "تم تحميل نموذج LLM بنجاح من: $modelPath (الأنوية: $activeThreads)")
        } else {
            Log.e(TAG, "فشل تحميل نموذج LLM من المسار: $modelPath")
        }
        return success
    }

    /**
     * توليد الرد بتدفق لحظي للرموز (Token Streaming) عبر Kotlin Flow
     */
    fun generateStream(userQuery: String, dynamicContext: String): Flow<String> {
        if (!isLoaded) {
            return flow {
                emit("يرجى اختيار نموذج ذكاء اصطناعي بصيغة GGUF من شاشة الإعدادات لتفعيل المحادثات المتقدمة.")
            }
        }

        val fullPrompt = buildPrompt(userQuery, dynamicContext)

        return callbackFlow {
            val success = bridge.generate(fullPrompt) { token ->
                trySend(token)
            }
            if (!success) {
                trySend("عذراً، حدث خطأ أثناء معالجة السؤال.")
            }
            channel.close()
            awaitClose {
                bridge.stop()
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun buildPrompt(query: String, context: String): String {
        val sb = StringBuilder()
        sb.append("<|system|>\n")
        sb.append(SYSTEM_PROMPT_PREFIX).append("\n")
        if (context.isNotBlank()) {
            sb.append(context).append("\n")
        }
        sb.append("<|user|>\n")
        sb.append(query.trim()).append("\n")
        sb.append("<|assistant|>\n")
        return sb.toString()
    }

    fun stop() {
        bridge.stop()
    }

    fun release() {
        bridge.freeModel()
        isLoaded = false
        currentModelPath = null
    }
}
