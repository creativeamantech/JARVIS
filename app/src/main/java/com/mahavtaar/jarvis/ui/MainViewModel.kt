package com.mahavtaar.jarvis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.jarvis.domain.llm.GemmaEngine
import com.mahavtaar.jarvis.domain.voice.JarvisTTS
import com.mahavtaar.jarvis.domain.voice.VoiceRecognizer
import com.mahavtaar.jarvis.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AssistantState {
    object IDLE : AssistantState()
    object LISTENING : AssistantState()
    object PROCESSING_STT : AssistantState()
    object THINKING : AssistantState()
    object SPEAKING : AssistantState()
    data class ERROR(val message: String) : AssistantState()
}

data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val voiceRecognizer: VoiceRecognizer,
    private val jarvisTTS: JarvisTTS,
    private val gemmaEngine: GemmaEngine,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _assistantState = MutableStateFlow<AssistantState>(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _currentStreamingText = MutableStateFlow("")
    val currentStreamingText: StateFlow<String> = _currentStreamingText.asStateFlow()

    val rmsAmplitude: StateFlow<Float> = voiceRecognizer.rmsAmplitude

    val isModelAvailable: Boolean
        get() = gemmaEngine.isModelAvailable()

    init {
        viewModelScope.launch {
            voiceRecognizer.recognizedText.collect { text ->
                if (text.isNotEmpty() && _assistantState.value == AssistantState.LISTENING) {
                    _assistantState.value = AssistantState.PROCESSING_STT
                    processUserInput(text)
                }
            }
        }

        viewModelScope.launch {
            voiceRecognizer.error.collect { error ->
                error?.let {
                    if (it == "SOFT_ERROR_NO_MATCH") {
                        _assistantState.value = AssistantState.SPEAKING
                        jarvisTTS.speak("I didn't catch that, sir.")
                    } else {
                        _assistantState.value = AssistantState.ERROR(it)
                        recoverToIdle()
                    }
                }
            }
        }

        viewModelScope.launch {
            jarvisTTS.isSpeaking.collect { isSpeaking ->
                if (!isSpeaking && _assistantState.value == AssistantState.SPEAKING) {
                    _assistantState.value = AssistantState.IDLE
                } else if (isSpeaking && _assistantState.value != AssistantState.SPEAKING) {
                    _assistantState.value = AssistantState.SPEAKING
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.modelPath.collect { path ->
                gemmaEngine.updateModelPath(path)
            }
        }
    }

    fun startListening() {
        if (_assistantState.value == AssistantState.IDLE || _assistantState.value is AssistantState.ERROR) {
            jarvisTTS.stop()
            _assistantState.value = AssistantState.LISTENING
            voiceRecognizer.startListening()
        }
    }

    fun stopListening() {
        if (_assistantState.value == AssistantState.LISTENING) {
            voiceRecognizer.stopListening()
            _assistantState.value = AssistantState.IDLE
        }
    }

    private fun processUserInput(text: String) {
        addMessage(Message(text = text, isUser = true))
        _assistantState.value = AssistantState.THINKING

        viewModelScope.launch {
            _currentStreamingText.value = ""
            var fullResponse = ""

            val responseFlow = if (isModelAvailable) {
                gemmaEngine.generateResponseStream(text)
            } else {
                gemmaEngine.generateResponseStream(text)
            }

            responseFlow.collect { chunk ->
                fullResponse += chunk
                _currentStreamingText.value = fullResponse
            }

            addMessage(Message(text = fullResponse, isUser = false))
            _currentStreamingText.value = ""

            _assistantState.value = AssistantState.SPEAKING
            jarvisTTS.speak(fullResponse)
        }
    }

    private fun addMessage(message: Message) {
        val currentList = _messages.value.toMutableList()
        currentList.add(message)
        _messages.value = currentList
    }

    private fun recoverToIdle() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            if (_assistantState.value is AssistantState.ERROR) {
                _assistantState.value = AssistantState.IDLE
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecognizer.destroy()
        jarvisTTS.destroy()
        gemmaEngine.close()
    }
}
