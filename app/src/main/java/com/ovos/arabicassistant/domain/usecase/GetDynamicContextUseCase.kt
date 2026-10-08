package com.ovos.arabicassistant.domain.usecase

import com.ovos.arabicassistant.data.location.LocationTimeProvider
import com.ovos.arabicassistant.domain.model.DynamicContext
import com.ovos.arabicassistant.domain.repository.MemoryRepository

/**
 * حالة الاستخدام لتجهيز السياق الديناميكي المتكامل (الموقع، الوقت، شاشة الوصول، وحقائق الذاكرة)
 */
class GetDynamicContextUseCase(
    private val locationTimeProvider: LocationTimeProvider,
    private val memoryRepository: MemoryRepository
) {

    suspend operator fun invoke(
        userQuery: String,
        activeScreenSummary: String? = null
    ): DynamicContext {
        val baseContext = locationTimeProvider.getCurrentContext(activeScreenSummary)
        val facts = if (userQuery.isNotBlank()) {
            memoryRepository.getRelevantFacts(userQuery, limit = 2)
        } else {
            emptyList()
        }

        return baseContext.copy(memoryFacts = facts)
    }
}
