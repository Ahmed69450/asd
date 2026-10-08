package com.ovos.arabicassistant.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * واجهة الوصول لبيانات الذاكرة (Memory DAO)
 */
@Dao
interface MemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFact(entity: MemoryEntity): Long

    @Query("SELECT * FROM memory_facts WHERE keywords LIKE '%' || :query || '%' OR fact_text LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun searchFacts(query: String, limit: Int = 3): List<MemoryEntity>

    @Query("SELECT * FROM memory_facts ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentFacts(limit: Int = 5): List<MemoryEntity>

    @Query("DELETE FROM memory_facts WHERE id = :id")
    suspend fun deleteFact(id: Long): Int

    @Query("DELETE FROM memory_facts")
    suspend fun clearAll()
}
