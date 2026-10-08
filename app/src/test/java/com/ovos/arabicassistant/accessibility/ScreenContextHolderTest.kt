package com.ovos.arabicassistant.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenContextHolderTest {

    @Test
    fun `summarizes active screen package and UI text nodes concisely`() {
        ScreenContextHolder.update(
            packageName = "com.spotify.music",
            visibleTexts = listOf("Amr Diab", "Nour El Ain", "Play", "Next", "Play") // has duplicate "Play"
        )

        val summary = ScreenContextHolder.getCurrentSummary()
        assertTrue(summary.contains("com.spotify.music"))
        assertTrue(summary.contains("Amr Diab"))
        assertTrue(summary.contains("Nour El Ain"))

        // Verify deduplication
        val occurrencesOfPlay = summary.split("Play").size - 1
        org.junit.Assert.assertEquals(1, occurrencesOfPlay)
    }

    @Test
    fun `handles empty screen gracefully`() {
        ScreenContextHolder.clear()
        val summary = ScreenContextHolder.getCurrentSummary()
        assertTrue(summary.isBlank() || summary.contains("لا توجد بيانات"))
    }
}
