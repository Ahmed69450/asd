package com.ovos.arabicassistant.domain.usecase

import com.ovos.arabicassistant.domain.model.RouteResult

/**
 * حالة الاستخدام لتوجيه العبارة دلالياً وتطبيق عتبة الـ 70% الصارمة
 */
class RouteSemanticIntentUseCase(
    private val routerFunction: (String) -> RouteResult
) {
    operator fun invoke(utterance: String): RouteResult {
        return routerFunction(utterance)
    }
}
