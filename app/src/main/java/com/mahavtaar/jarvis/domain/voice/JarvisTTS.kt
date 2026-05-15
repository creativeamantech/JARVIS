package com.mahavtaar.jarvis.domain.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import com.mahavtaar.jarvis.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

class JarvisTTS @Inject constructor(
    context: Context,
    private val settingsRepository: SettingsRepository
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentPitch = 0.85f

    init {
        tts = TextToSpeech(context, this)

        CoroutineScope(Dispatchers.Main).launch {
            settingsRepository.ttsPitch.collect { pitch ->
                currentPitch = pitch
                if (isInitialized) {
                    tts?.setPitch(currentPitch)
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let {
                val result = it.setLanguage(Locale.UK) // British male voice (Jarvis)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to default
                    it.setLanguage(Locale.getDefault())
                }
                it.setPitch(currentPitch)
                it.setSpeechRate(0.95f)
                isInitialized = true

                it.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            }
        }
    }

    fun speak(text: String, utteranceId: String = "JARVIS_RESPONSE") {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
