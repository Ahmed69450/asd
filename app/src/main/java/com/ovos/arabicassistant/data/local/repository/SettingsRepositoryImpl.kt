package com.ovos.arabicassistant.data.local.repository

import com.ovos.arabicassistant.data.local.preferences.SettingsDataStore
import com.ovos.arabicassistant.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override val modelPathFlow: Flow<String?> = settingsDataStore.modelPathFlow
    override val cpuThreadsFlow: Flow<Int> = settingsDataStore.cpuThreadsFlow
    override val speechRateFlow: Flow<Float> = settingsDataStore.speechRateFlow

    override suspend fun setModelPath(path: String) {
        settingsDataStore.setModelPath(path)
    }

    override suspend fun setCpuThreads(threads: Int) {
        settingsDataStore.setCpuThreads(threads)
    }

    override suspend fun setSpeechRate(rate: Float) {
        settingsDataStore.setSpeechRate(rate)
    }
}
