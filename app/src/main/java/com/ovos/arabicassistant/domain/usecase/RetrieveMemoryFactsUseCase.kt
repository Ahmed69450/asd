package com.ovos.arabicassistant.domain.usecase

import com.ovos.arabicassistant.domain.model.MemoryFact
import com.ovos.arabicassistant.domain.repository.MemoryRepository

/**
 * حالة الاستخدام لاسترجاع حقائق الذاكرة المفتاحية الملائمة للجولة الحالية (RAG الخفيف)
 */
class RetrieveMemoryFactsUseCase(
    private val memoryRepository: MemoryRepository
) {
    suspend operator fun invoke(query: String, limit: Int = 3): List<MemoryFact> {
        return memoryRepository.getRelevantFacts(query, limit)
    }
}
