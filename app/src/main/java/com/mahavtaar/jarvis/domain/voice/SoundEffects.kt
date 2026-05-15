package com.mahavtaar.jarvis.domain.voice

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

object SoundEffects {
    suspend fun playStartupSound() = withContext(Dispatchers.IO) {
        val sampleRate = 44100
        val durationMs = 150
        val numSamples = (durationMs * sampleRate) / 1000

        // Frequencies for an ascending sci-fi beep (C5, E5, G5)
        val frequencies = listOf(523.25, 659.25, 783.99)

        val generatedSound = ByteArray(2 * numSamples * frequencies.size)

        var bufferIndex = 0
        for (freq in frequencies) {
            for (i in 0 until numSamples) {
                val sample = sin(2.0 * Math.PI * i / (sampleRate / freq))
                val pcmValue = (sample * 32767).toInt() // Scale to 16-bit

                // Add simple envelope (fade in/out) to avoid clicks
                val envelope = when {
                    i < 200 -> i / 200f
                    i > numSamples - 200 -> (numSamples - i) / 200f
                    else -> 1f
                }

                val finalPcm = (pcmValue * envelope).toInt().toShort()

                generatedSound[bufferIndex++] = (finalPcm.toInt() and 0x00FF).toByte()
                generatedSound[bufferIndex++] = ((finalPcm.toInt() and 0xFF00) ushr 8).toByte()
            }
        }

        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            generatedSound.size,
            AudioTrack.MODE_STATIC
        )

        audioTrack.write(generatedSound, 0, generatedSound.size)
        audioTrack.play()

        // Clean up
        kotlinx.coroutines.delay((durationMs * frequencies.size).toLong() + 100)
        audioTrack.release()
    }
}
