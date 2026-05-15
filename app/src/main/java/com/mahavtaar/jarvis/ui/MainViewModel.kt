package com.mahavtaar.jarvis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.jarvis.data.SettingsRepository
import com.mahavtaar.jarvis.domain.appcontrol.BatteryMonitor
import com.mahavtaar.jarvis.domain.llm.GemmaEngine
import com.mahavtaar.jarvis.domain.task.IntentParser
import com.mahavtaar.jarvis.domain.task.TaskExecutor
import com.mahavtaar.jarvis.domain.voice.JarvisTTS
import com.mahavtaar.jarvis.domain.voice.VoiceRecognizer
import com.mahavtaar.jarvis.domain.voice.WakeWordService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    val isUser: Boolean,
    val actions: List<String> = emptyList()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val voiceRecognizer: VoiceRecognizer,
    private val jarvisTTS: JarvisTTS,
    private val gemmaEngine: GemmaEngine,
    private val settingsRepository: SettingsRepository,
    private val intentParser: IntentParser,
    private val taskExecutor: TaskExecutor,
    private val batteryMonitor: BatteryMonitor
) : ViewModel() {

    private val _assistantState = MutableStateFlow<AssistantState>(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _currentStreamingText = MutableStateFlow("")
    val currentStreamingText: StateFlow<String> = _currentStreamingText.asStateFlow()

    private val _isStandbyMode = MutableStateFlow(false)
    val isStandbyMode: StateFlow<Boolean> = _isStandbyMode.asStateFlow()

    val rmsAmplitude: StateFlow<Float> = voiceRecognizer.rmsAmplitude

    val isModelAvailable: Boolean
        get() = gemmaEngine.isModelAvailable()

    private var inferenceJob: Job? = null
    private var standbyJob: Job? = null
    private var contextWindowSize: Int = 4096

    private var isFirstLoad = true

    init {
        batteryMonitor.startMonitoring()
        resetStandbyTimer()

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
                    resetStandbyTimer()
                } else if (isSpeaking && _assistantState.value != AssistantState.SPEAKING) {
                    _assistantState.value = AssistantState.SPEAKING
                    cancelStandbyTimer()
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.contextWindowSize.collect { size ->
                contextWindowSize = size
            }
        }

        viewModelScope.launch {
            settingsRepository.modelPath.collect { path ->
                gemmaEngine.updateModelPath(path)

                if (!isFirstLoad) {
                    inferenceJob?.cancel()
                    gemmaEngine.close()
                    _assistantState.value = AssistantState.SPEAKING
                    jarvisTTS.speak("Reloading core systems, sir.")

                    try {
                        gemmaEngine.initialize()
                    } catch (e: Exception) {
                    }
                }
                isFirstLoad = false
            }
        }

        viewModelScope.launch {
            batteryMonitor.batteryWarning.collect { pct ->
                if (_assistantState.value == AssistantState.IDLE) {
                    _assistantState.value = AssistantState.SPEAKING
                    jarvisTTS.speak("Power reserves are at \$pct percent, sir. I recommend connecting to a power source.")
                }
            }
        }

        viewModelScope.launch {
            WakeWordService.wakeWordDetected.collect {
                startListening()
            }
        }
    }

    private fun resetStandbyTimer() {
        standbyJob?.cancel()
        _isStandbyMode.value = false
        standbyJob = viewModelScope.launch {
            kotlinx.coroutines.delay(5 * 60 * 1000) // 5 minutes
            if (_assistantState.value == AssistantState.IDLE) {
                _isStandbyMode.value = true
                _assistantState.value = AssistantState.SPEAKING
                jarvisTTS.speak("Standing by, sir.")
            }
        }
    }

    private fun cancelStandbyTimer() {
        standbyJob?.cancel()
        _isStandbyMode.value = false
    }

    fun startListening() {
        cancelStandbyTimer()
        if (_assistantState.value == AssistantState.IDLE || _assistantState.value is AssistantState.ERROR || _assistantState.value == AssistantState.SPEAKING || _assistantState.value == AssistantState.THINKING) {
            inferenceJob?.cancel()
            gemmaEngine.close()

            jarvisTTS.stop()
            _assistantState.value = AssistantState.LISTENING
            voiceRecognizer.startListening()
        }
    }

    fun stopListening() {
        if (_assistantState.value == AssistantState.LISTENING) {
            voiceRecognizer.stopListening()
            viewModelScope.launch {
                kotlinx.coroutines.delay(500)
                if (_assistantState.value == AssistantState.LISTENING) {
                    _assistantState.value = AssistantState.IDLE
                    resetStandbyTimer()
                }
            }
        }
    }

    private fun buildContextPrompt(): String {
        val maxChars = contextWindowSize * 4
        var currentLength = 0
        val contextMessages = mutableListOf<Message>()

        for (message in _messages.value.reversed()) {
            if (currentLength + message.text.length > maxChars) {
                break
            }
            contextMessages.add(0, message)
            currentLength += message.text.length
        }

        return contextMessages.joinToString("\n") {
            if (it.isUser) "User: \${it.text}" else "J.A.R.V.I.S: \${it.text}"
        }
    }

    private fun processUserInput(text: String) {
        addMessage(Message(text = text, isUser = true))
        _assistantState.value = AssistantState.THINKING

        inferenceJob = viewModelScope.launch {
            _currentStreamingText.value = ""
            var rawResponse = ""

            val contextPrompt = buildContextPrompt()

            val responseFlow = if (isModelAvailable) {
                gemmaEngine.generateResponseStream(contextPrompt)
            } else {
                gemmaEngine.generateMockResponseStream(text)
            }

            responseFlow.collect { chunk ->
                rawResponse += chunk
                _currentStreamingText.value = intentParser.cleanStreamingText(rawResponse)
            }

            _currentStreamingText.value = ""

            val parsedResult = intentParser.parse(rawResponse)

            val taskResults = parsedResult.intents.map { intent ->
                async { taskExecutor.execute(intent) }
            }.awaitAll()

            val actionBadges = taskResults.map { it.badge }
            val spokenOverrides = taskResults.mapNotNull { it.spokenFeedback }

            addMessage(Message(text = parsedResult.spokenText, isUser = false, actions = actionBadges))

            var finalSpokenText = parsedResult.spokenText
            if (spokenOverrides.isNotEmpty()) {
                finalSpokenText += " " + spokenOverrides.joinToString(" ")
            }
            finalSpokenText = finalSpokenText.trim()

            _assistantState.value = AssistantState.SPEAKING
            if (finalSpokenText.isNotBlank()) {
                jarvisTTS.speak(finalSpokenText)
            } else {
                 _assistantState.value = AssistantState.IDLE
                 resetStandbyTimer()
            }
        }
    }

    private fun addMessage(message: Message) {
        val currentList = _messages.value.toMutableList()
        currentList.add(message)
        _messages.value = currentList
    }

    private fun recoverToIdle() {
        viewModelScope.launch {
            jarvisTTS.speak("I've encountered a difficulty, sir. Systems restored.")
            kotlinx.coroutines.delay(3000)
            if (_assistantState.value is AssistantState.ERROR) {
                _assistantState.value = AssistantState.IDLE
                resetStandbyTimer()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecognizer.destroy()
        jarvisTTS.destroy()
        gemmaEngine.close()
        batteryMonitor.stopMonitoring()
    }
}
