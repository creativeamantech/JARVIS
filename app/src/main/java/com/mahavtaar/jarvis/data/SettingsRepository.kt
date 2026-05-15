package com.mahavtaar.jarvis.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "jarvis_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val MODEL_PATH = stringPreferencesKey("model_path")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val CONTEXT_WINDOW_SIZE = intPreferencesKey("context_window_size")
    }

    val modelPath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[MODEL_PATH] ?: "/sdcard/jarvis/models/gemma4-2b-it-int4.bin"
    }

    val ttsPitch: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[TTS_PITCH] ?: 0.85f
    }

    val contextWindowSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[CONTEXT_WINDOW_SIZE] ?: 4096
    }

    suspend fun updateModelPath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[MODEL_PATH] = path
        }
    }

    suspend fun updateTtsPitch(pitch: Float) {
        context.dataStore.edit { prefs ->
            prefs[TTS_PITCH] = pitch
        }
    }

    suspend fun updateContextWindowSize(size: Int) {
        context.dataStore.edit { prefs ->
            prefs[CONTEXT_WINDOW_SIZE] = size
        }
    }
}
