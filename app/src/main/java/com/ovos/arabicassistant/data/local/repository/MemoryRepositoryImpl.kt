package com.ovos.arabicassistant.data.local.repository

import com.ovos.arabicassistant.data.local.db.MemoryDao
import com.ovos.arabicassistant.data.local.db.toDomain
import com.ovos.arabicassistant.data.local.db.toEntity
import com.ovos.arabicassistant.domain.model.MemoryFact
import com.ovos.arabicassistant.domain.repository.MemoryRepository

class MemoryRepositoryImpl(
    private val memoryDao: MemoryDao
) : MemoryRepository {

    override suspend fun getRelevantFacts(query: String, limit: Int): List<MemoryFact> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return emptyList()

        // استخراج الكلمات المفتاحية
        val words = cleanQuery.split(Regex("\\s+")).filter { it.length > 2 }
        val results = mutableListOf<MemoryFact>()

        // 1. استعلام بالجملة المباشرة
        val directMatches = memoryDao.searchFacts(cleanQuery, limit)
        results.addAll(directMatches.map { it.toDomain() })

        // 2. إذا كانت النتائج قليلة، نبحث بالكلمات الرئيسية
        if (results.size < limit) {
            for (word in words) {
                if (results.size >= limit) break
                val wordMatches = memoryDao.searchFacts(word, limit - results.size)
                for (match in wordMatches) {
                    val domainFact = match.toDomain()
                    if (results.none { it.id == domainFact.id }) {
                        results.add(domainFact)
                    }
                }
            }
        }

        return results.take(limit)
    }

    override suspend fun saveFact(fact: MemoryFact): Long {
        return memoryDao.insertFact(fact.toEntity())
    }
}
