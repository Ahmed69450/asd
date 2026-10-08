package com.ovos.arabicassistant.domain.usecase

import com.ovos.arabicassistant.domain.model.*
import com.ovos.arabicassistant.domain.repository.CarControlRepository
import com.ovos.arabicassistant.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

/**
 * نتيجة معالجة الإدخال الصوتي
 */
data class ProcessedVoiceResult(
    val spokenResponse: String,
    val intent: String,
    val confidence: Float,
    val isHardwareAction: Boolean,
    val isLlmGenerated: Boolean
)

/**
 * المنسق الرئيسي بين المحرك A (الموجه الدلالي السريع) والمحرك B (النموذج اللغوي الاحتياطي)
 */
class ProcessVoiceInputUseCase(
    private val router: (String) -> RouteResult,
    private val carRepo: CarControlRepository,
    private val memoryRepo: MemoryRepository,
    private val llmGenerator: (prompt: String, context: String) -> Flow<String>
) {

    suspend fun execute(
        utterance: String,
        dynamicContext: DynamicContext? = null
    ): ProcessedVoiceResult {
        val trimmed = utterance.trim()
        if (trimmed.isEmpty()) {
            return ProcessedVoiceResult(
                spokenResponse = "عذراً، لم أسمع أي أمر.",
                intent = "empty",
                confidence = 0f,
                isHardwareAction = false,
                isLlmGenerated = false
            )
        }

        // 1. فحص التوجيه الدلالي من المحرك A (ONNX Semantic Router)
        val routeResult = router(trimmed)

        // 2. إذا كانت الثقة >= 70% وهو أمر عتاد للسيارة: تنفيذ مباشر وفوري
        if (routeResult.isConfidentHardwareCommand) {
            val action = mapIntentToCarAction(routeResult.intent, routeResult.parameters)
            carRepo.executeAction(action)
            val spokenAck = generateHardwareAcknowledgment(routeResult.intent)
            return ProcessedVoiceResult(
                spokenResponse = spokenAck,
                intent = routeResult.intent,
                confidence = routeResult.confidence,
                isHardwareAction = true,
                isLlmGenerated = false
            )
        }

        // 3. التراجع للمحرك B (LLM Fallback) مع تزويده بالذاكرة والسياق الديناميكي
        val facts = memoryRepo.getRelevantFacts(trimmed, limit = 2)
        val contextWithFacts = (dynamicContext ?: DynamicContext(
            time = "الآن",
            date = "اليوم"
        )).copy(memoryFacts = facts)

        val promptContext = contextWithFacts.toPromptBlock()
        val tokens = llmGenerator(trimmed, promptContext).toList()
        val responseText = tokens.joinToString("").trim()

        return ProcessedVoiceResult(
            spokenResponse = if (responseText.isNotEmpty()) responseText else "لم أتمكن من الحصول على إجابة.",
            intent = routeResult.intent,
            confidence = routeResult.confidence,
            isHardwareAction = false,
            isLlmGenerated = true
        )
    }

    private fun mapIntentToCarAction(intent: String, params: Map<String, String>): CarAction {
        return when (intent) {
            "ac_temp_down" -> CarAction(CarActionType.AC_TEMP, "-1", params)
            "ac_temp_up" -> CarAction(CarActionType.AC_TEMP, "+1", params)
            "ac_fan_speed" -> CarAction(CarActionType.AC_FAN, params["speed"] ?: "2", params)
            "window_open" -> CarAction(CarActionType.WINDOW_OPEN, params["target"] ?: "all", params)
            "window_close" -> CarAction(CarActionType.WINDOW_CLOSE, params["target"] ?: "all", params)
            "sunroof_open" -> CarAction(CarActionType.SUNROOF_OPEN, "", params)
            "sunroof_close" -> CarAction(CarActionType.SUNROOF_CLOSE, "", params)
            "volume_up" -> CarAction(CarActionType.VOLUME_UP, "+2", params)
            "volume_down" -> CarAction(CarActionType.VOLUME_DOWN, "-2", params)
            "volume_mute" -> CarAction(CarActionType.VOLUME_MUTE, "", params)
            "media_next" -> CarAction(CarActionType.MEDIA_NEXT, "", params)
            "media_prev" -> CarAction(CarActionType.MEDIA_PREVIOUS, "", params)
            "media_play_pause" -> CarAction(CarActionType.MEDIA_PLAY_PAUSE, "", params)
            else -> CarAction(CarActionType.AC_TEMP, "0", params)
        }
    }

    private fun generateHardwareAcknowledgment(intent: String): String {
        return when (intent) {
            "ac_temp_down" -> "تم خفض درجة حرارة المكيف بنجاح."
            "ac_temp_up" -> "تم رفع درجة حرارة المكيف بنجاح."
            "ac_fan_speed" -> "تم تعديل سرعة مروحة التكييف."
            "window_open" -> "جاري فتح النوافذ."
            "window_close" -> "جاري إغلاق النوافذ."
            "sunroof_open" -> "جاري فتح فتحة السقف."
            "sunroof_close" -> "جاري إغلاق فتحة السقف."
            "volume_up" -> "تم رفع مستوى الصوت."
            "volume_down" -> "تم خفض مستوى الصوت."
            "volume_mute" -> "تم كتم الصوت."
            "media_next" -> "تشغيل المقطع التالي."
            "media_prev" -> "تشغيل المقطع السابق."
            "media_play_pause" -> "تم تبديل حالة التشغيل."
            else -> "تم تنفيذ أمر السيارة بنجاح."
        }
    }
}
