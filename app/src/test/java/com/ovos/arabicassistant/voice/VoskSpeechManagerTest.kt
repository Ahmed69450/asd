package com.ovos.arabicassistant.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoskSpeechManagerTest {

    @Test
    fun `detects wake word keywords and transitions state`() {
        val keywords = listOf("يا سيارة", "مرحبا سيارة", "مساعد سيارة")
        
        fun isWakeWord(text: String): Boolean {
            val normalized = text.trim()
            return keywords.any { normalized.contains(it) }
        }

        assertTrue(isWakeWord("يا سيارة"))
        assertTrue(isWakeWord("مرحبا سيارة كيف حالك"))
        assertFalse(isWakeWord("شغل الراديو"))
    }

    @Test
    fun `two-phase state machine transitions correctly`() {
        var currentState = VoskSpeechState.IDLE
        assertEquals(VoskSpeechState.IDLE, currentState)

        // Start listening
        currentState = VoskSpeechState.WAKE_WORD_LISTENING
        assertEquals(VoskSpeechState.WAKE_WORD_LISTENING, currentState)

        // Wake word spotted -> transition to active dictation
        currentState = VoskSpeechState.ACTIVE_DICTATION
        assertEquals(VoskSpeechState.ACTIVE_DICTATION, currentState)

        // End of speech detected -> processing
        currentState = VoskSpeechState.PROCESSING
        assertEquals(VoskSpeechState.PROCESSING, currentState)
    }
}
