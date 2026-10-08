package com.ovos.arabicassistant.data.location

import com.ovos.arabicassistant.domain.model.DynamicContext
import com.ovos.arabicassistant.domain.model.MemoryFact
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationTimeProviderTest {

    @Test
    fun `formats system context block with time date and location`() {
        val context = DynamicContext(
            time = "14:30",
            date = "2026-10-08",
            latitude = 24.7136,
            longitude = 46.6753,
            city = "الرياض",
            activeScreenSummary = "تطبيق الملاحة",
            memoryFacts = listOf(
                MemoryFact(1L, "pref", "حرارة", "الحرارة المفضلة 22", 100L)
            )
        )

        val formatted = context.toPromptBlock()
        assertTrue(formatted.contains("14:30"))
        assertTrue(formatted.contains("2026-10-08"))
        assertTrue(formatted.contains("الرياض"))
        assertTrue(formatted.contains("24.7136"))
        assertTrue(formatted.contains("تطبيق الملاحة"))
        assertTrue(formatted.contains("الحرارة المفضلة 22"))
    }

    @Test
    fun `handles missing location gracefully without null pointer`() {
        val context = DynamicContext(
            time = "09:15",
            date = "2026-10-08",
            latitude = null,
            longitude = null,
            city = null
        )

        val formatted = context.toPromptBlock()
        assertTrue(formatted.contains("09:15"))
        assertTrue(formatted.contains("2026-10-08"))
        assertFalse(formatted.contains("المدينة الحالية"))
    }
}
