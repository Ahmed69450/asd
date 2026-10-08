package com.ovos.arabicassistant.accessibility

import java.util.concurrent.atomic.AtomicReference

/**
 * حافظ سياق الشاشة اللحظي المستخرج من CarAccessibilityService
 * يخزن التطبيق النشط والنصوص المرئية المعروضة على شاشة السيارة،
 * ويجهز ملخصاً مكثفاً ومصفى لتغذية أدوات MCP والمحرك B (LLM).
 */
object ScreenContextHolder {

    data class ScreenState(
        val packageName: String = "",
        val visibleTexts: List<String> = emptyList(),
        val timestamp: Long = System.currentTimeMillis()
    )

    private val currentState = AtomicReference(ScreenState())

    fun update(packageName: String, visibleTexts: List<String>) {
        val cleanTexts = visibleTexts
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 1 }
            .distinct()
            .take(20)

        currentState.set(
            ScreenState(
                packageName = packageName,
                visibleTexts = cleanTexts,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun clear() {
        currentState.set(ScreenState())
    }

    /**
     * إرجاع ملخص نصي مكثف للشاشة الحالية
     */
    fun getCurrentSummary(): String {
        val state = currentState.get()
        if (state.packageName.isBlank() && state.visibleTexts.isEmpty()) {
            return "لا توجد بيانات شاشة حالية."
        }

        val sb = StringBuilder()
        if (state.packageName.isNotBlank()) {
            sb.append("التطبيق المفتوح: ").append(state.packageName).append(". ")
        }
        if (state.visibleTexts.isNotEmpty()) {
            sb.append("العناصر والنصوص الظاهرة: ")
            sb.append(state.visibleTexts.joinToString("، "))
        }

        return sb.toString().take(600)
    }
}
