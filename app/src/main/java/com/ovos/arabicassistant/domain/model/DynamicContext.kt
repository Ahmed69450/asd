package com.ovos.arabicassistant.domain.model

/**
 * السياق اللحظي الديناميكي للسيارة (الموقع الجغرافي الدقيق والوقت وتفضيلات السائق)
 */
data class DynamicContext(
    val time: String,
    val date: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val city: String? = null,
    val activeScreenSummary: String? = null,
    val memoryFacts: List<MemoryFact> = emptyMap<String, String>().let { emptyList() }
) {
    fun toPromptBlock(): String {
        val sb = StringBuilder()
        sb.append("[سياق النظام الحقيقي: الوقت ")
        sb.append(time)
        sb.append("، التاريخ ")
        sb.append(date)
        if (!city.isNullOrBlank()) {
            sb.append("، المدينة الحالية: ")
            sb.append(city)
        }
        if (latitude != null && longitude != null) {
            sb.append("، الإحداثيات: ")
            sb.append(String.format(java.util.Locale.US, "%.4f, %.4f", latitude, longitude))
        }
        if (!activeScreenSummary.isNullOrBlank()) {
            sb.append("، سياق الشاشة المعروضة: ")
            sb.append(activeScreenSummary)
        }
        if (memoryFacts.isNotEmpty()) {
            sb.append("، معلومات الذاكرة السابقة: ")
            sb.append(memoryFacts.joinToString("; ") { it.factText })
        }
        sb.append("]")
        return sb.toString()
    }
}
