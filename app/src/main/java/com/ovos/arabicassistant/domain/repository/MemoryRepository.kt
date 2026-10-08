package com.ovos.arabicassistant.domain.repository

import com.ovos.arabicassistant.domain.model.MemoryFact

interface MemoryRepository {
    suspend fun getRelevantFacts(query: String, limit: Int = 3): List<MemoryFact>
    suspend fun saveFact(fact: MemoryFact): Long
}
