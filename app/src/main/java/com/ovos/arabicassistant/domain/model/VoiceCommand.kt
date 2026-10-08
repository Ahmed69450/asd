package com.ovos.arabicassistant.domain.model

/**
 * تمثيل الأمر الصوتي الصادر من السائق
 */
data class VoiceCommand(
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis()
)
