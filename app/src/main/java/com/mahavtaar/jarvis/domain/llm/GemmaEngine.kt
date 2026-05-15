package com.mahavtaar.jarvis.domain.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject

class GemmaEngine @Inject constructor(
    private val context: Context
) {
    private var llmInference: LlmInference? = null
    private var modelPath = "/sdcard/jarvis/models/gemma4-2b-it-int4.bin"

    fun updateModelPath(path: String) {
        modelPath = path
    }

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
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
        } catch (e: Exception) {
            throw Exception("Failed to initialize Gemma model: \${e.message}")
        }
    }

    fun generateResponseStream(prompt: String): Flow<String> = callbackFlow {
        if (llmInference == null) {
            try {
                initialize()
            } catch (e: Exception) {
                trySend("Error: Gemma engine not initialized. Please ensure the model file exists at \$modelPath.")
                close()
                return@callbackFlow
            }
        }

        val systemPrompt = "You are J.A.R.V.I.S., a highly advanced AI assistant created by Mahavtaar Enterprises. You speak in a crisp, authoritative British tone. You are concise, highly intelligent, and occasionally dry in your humor. Always address the user politely as 'Sir' or 'Madam'. Answer the following query:\\n\\n"
        val fullPrompt = systemPrompt + prompt

        try {
            // MediaPipe 0.10.22 uses generateResponseAsync that returns a Flow or expects a listener
            // Given the signature error with `setResultListener`, we'll simulate streaming by using the blocking call
            // and chunking the result to emulate streaming for this dependency version.
            // In a newer MediaPipe, `generateResponseAsync` natively supports a Flow return type.
            val fullResponse = llmInference?.generateResponse(fullPrompt) ?: "Model inference failed."

            val tokens = fullResponse.split(" ")
            for ((index, token) in tokens.withIndex()) {
                val suffix = if (index == tokens.size - 1) "" else " "
                trySend(token + suffix)
                kotlinx.coroutines.delay(50) // Simulate fast streaming
            }
            close()
        } catch (e: Exception) {
            trySend("Error generating response: \${e.message}")
            close()
        }

        awaitClose { }
    }.flowOn(Dispatchers.IO)

    fun generateMockResponseStream(prompt: String): Flow<String> = callbackFlow {
        val response = "Sir, I have received your input: \"\$prompt\". This is a simulated response demonstrating the STT to TTS pipeline without the full Gemma model loaded in memory. All systems appear nominal."
        val tokens = response.split(" ")
        for ((index, token) in tokens.withIndex()) {
            val suffix = if (index == tokens.size - 1) "" else " "
            trySend(token + suffix)
            kotlinx.coroutines.delay(100)
        }
        close()
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    fun close() {
        llmInference?.close()
        llmInference = null
    }
}
