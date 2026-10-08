package com.ovos.arabicassistant.data.local

import com.ovos.arabicassistant.data.local.db.MemoryEntity
import com.ovos.arabicassistant.data.local.db.toDomain
import com.ovos.arabicassistant.data.local.db.toEntity
import com.ovos.arabicassistant.domain.model.MemoryFact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryDaoTest {

    @Test
    fun `entity converts to domain model and back correctly`() {
        val domain = MemoryFact(
            id = 5L,
            category = "preferences",
            keywords = "مكيف,حرارة,تبريد",
            factText = "السائق يفضل درجة 21 في الصيف",
            timestamp = 1700000000L
        )

        val entity = domain.toEntity()
        assertEquals(5L, entity.id)
        assertEquals("preferences", entity.category)
        assertEquals("مكيف,حرارة,تبريد", entity.keywords)
        assertEquals("السائق يفضل درجة 21 في الصيف", entity.factText)

        val convertedDomain = entity.toDomain()
        assertEquals(domain, convertedDomain)
    }

    @Test
    fun `keyword matching identifies relevant facts`() {
        val entities = listOf(
            MemoryEntity(1L, "pref", "حرارة,مكيف", "درجة الحرارة المفضلة 22", 100L),
            MemoryEntity(2L, "music", "أغاني,فيروز", "السائق يستمع إلى فيروز صباحاً", 200L),
            MemoryEntity(3L, "route", "عمل,طريق", "الطريق المفضل للعمل هو الطريق الدائري", 300L)
        )

        // Matching query "مكيف"
        val query = "مكيف"
        val matched = entities.filter {
            it.keywords.contains(query) || it.factText.contains(query)
        }

        assertEquals(1, matched.size)
        assertEquals("درجة الحرارة المفضلة 22", matched.first().factText)
    }
}
