package com.mahavtaar.jarvis.domain.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject

class GemmaEngine @Inject constructor(
    private val context: Context
) {
    private var llmInference: LlmInference? = null
    private val modelPath = "/sdcard/jarvis/models/gemma4-2b-it-int4.bin"

    fun isModelAvailable(): Boolean {
        return File(modelPath).exists()
    }

    suspend fun initialize() {
        if (!isModelAvailable()) {
            throw Exception("Model file not found at \$modelPath")
        }

        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                // In a production app with callback flows, we'd use result listener.
                // For MVP phase 1, we rely on generateResponse synchronously or use a mock.
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
        } catch (e: Exception) {
            throw Exception("Failed to initialize Gemma model: \${e.message}")
        }
    }

    fun generateMockResponseStream(prompt: String): Flow<String> = flow {
        val words = "Sir, I have received your input: \"\$prompt\". This is a simulated response demonstrating the STT to TTS pipeline without the full Gemma model loaded in memory. All systems appear nominal.".split(" ")
        for (word in words) {
            emit("\$word ")
            kotlinx.coroutines.delay(100)
        }
    }.flowOn(Dispatchers.IO)

    fun close() {
        llmInference?.close()
        llmInference = null
    }
}
