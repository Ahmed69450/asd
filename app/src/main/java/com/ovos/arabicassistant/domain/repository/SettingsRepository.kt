package com.ovos.arabicassistant.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val modelPathFlow: Flow<String?>
    val cpuThreadsFlow: Flow<Int>
    val speechRateFlow: Flow<Float>

    suspend fun setModelPath(path: String)
    suspend fun setCpuThreads(threads: Int)
    suspend fun setSpeechRate(rate: Float)
}
