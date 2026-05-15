package com.mahavtaar.jarvis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.jarvis.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val modelPath = settingsRepository.modelPath.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "/sdcard/jarvis/models/gemma4-2b-it-int4.bin"
    )
    val ttsPitch = settingsRepository.ttsPitch.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0.85f
    )
    val contextWindowSize = settingsRepository.contextWindowSize.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 4096
    )

    fun updateModelPath(path: String) {
        viewModelScope.launch {
            settingsRepository.updateModelPath(path)
        }
    }

    fun updateTtsPitch(pitch: Float) {
        viewModelScope.launch {
            settingsRepository.updateTtsPitch(pitch)
        }
    }

    fun updateContextWindowSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.updateContextWindowSize(size)
        }
    }
}
