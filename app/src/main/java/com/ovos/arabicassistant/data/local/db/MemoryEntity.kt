package com.ovos.arabicassistant.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ovos.arabicassistant.domain.model.MemoryFact

/**
 * كيان جدول حقائق الذاكرة في قاعدة بيانات Room (Local RAG Memory)
 */
@Entity(tableName = "memory_facts")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "keywords")
    val keywords: String,

    @ColumnInfo(name = "fact_text")
    val factText: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

fun MemoryEntity.toDomain(): MemoryFact = MemoryFact(
    id = id,
    category = category,
    keywords = keywords,
    factText = factText,
    timestamp = timestamp
)

fun MemoryFact.toEntity(): MemoryEntity = MemoryEntity(
    id = id,
    category = category,
    keywords = keywords,
    factText = factText,
    timestamp = timestamp
)
