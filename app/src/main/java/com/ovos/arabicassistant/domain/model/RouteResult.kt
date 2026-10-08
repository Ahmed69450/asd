package com.ovos.arabicassistant.domain.model

/**
 * نتيجة التوجيه الدلالي من المحرك A (ONNX Semantic Router)
 */
data class RouteResult(
    val intent: String,
    val confidence: Float,
    val isHardware: Boolean = false,
    val matchedAnchor: String = "",
    val parameters: Map<String, String> = emptyMap()
) {
    val isConfidentHardwareCommand: Boolean
        get() = isHardware && confidence >= 0.70f
}
