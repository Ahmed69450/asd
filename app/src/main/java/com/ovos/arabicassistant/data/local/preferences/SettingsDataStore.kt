package com.ovos.arabicassistant.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "car_assistant_settings")

/**
 * مدير إعدادات التطبيق عبر Jetpack DataStore
 */
class SettingsDataStore(private val context: Context) {

    companion object {
        val KEY_MODEL_PATH = stringPreferencesKey("model_path")
        val KEY_CPU_THREADS = intPreferencesKey("cpu_threads")
        val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")

        const val DEFAULT_CPU_THREADS = 2
        const val DEFAULT_SPEECH_RATE = 1.15f
    }

    val modelPathFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_MODEL_PATH] }

    val cpuThreadsFlow: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val stored = preferences[KEY_CPU_THREADS] ?: DEFAULT_CPU_THREADS
            stored.coerceIn(2, 3) // تقييد صارم بين 2 و 3 خيوط معالجة
        }

    val speechRateFlow: Flow<Float> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_SPEECH_RATE] ?: DEFAULT_SPEECH_RATE }

    suspend fun setModelPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MODEL_PATH] = path
        }
    }

    suspend fun setCpuThreads(threads: Int) {
        val clamped = threads.coerceIn(2, 3)
        context.dataStore.edit { preferences ->
            preferences[KEY_CPU_THREADS] = clamped
        }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SPEECH_RATE] = rate
        }
    }
}
