package com.ovos.arabicassistant.domain.model

/**
 * حقيقة مسترجعة من الذاكرة طويلة المدى (Lightweight RAG)
 */
data class MemoryFact(
    val id: Long = 0L,
    val category: String,
    val keywords: String,
    val factText: String,
    val timestamp: Long = System.currentTimeMillis()
)
