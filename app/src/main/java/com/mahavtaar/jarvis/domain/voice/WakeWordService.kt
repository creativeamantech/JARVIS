package com.mahavtaar.jarvis.domain.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.File
import java.io.IOException

class WakeWordService : Service(), RecognitionListener {

    companion object {
        private const val CHANNEL_ID = "WakeWordChannel"
        private const val NOTIFICATION_ID = 101

        private val _wakeWordDetected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val wakeWordDetected = _wakeWordDetected.asSharedFlow()
    }

    private var model: Model? = null
    private var speechService: SpeechService? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        initModel()
    }

    private fun initModel() {
        StorageService.unpack(this, "model-en-us", "model", { model ->
            this.model = model
            startListening()
        }, { exception: IOException ->
            // Error unpacking model, typically just retry or log for MVP
        })
    }

    private fun startListening() {
        if (speechService != null) {
            speechService?.startListening(this)
        } else {
            try {
                // Initialize recognizer with sampling rate 16000.0f
                val recognizer = Recognizer(model, 16000.0f)
                speechService = SpeechService(recognizer, 16000.0f)
                speechService?.startListening(this)
            } catch (e: IOException) {
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        speechService?.stop()
        speechService?.shutdown()
        model?.close()
    }

    override fun onPartialResult(hypothesis: String?) {
        checkWakeWord(hypothesis)
    }

    override fun onResult(hypothesis: String?) {
        checkWakeWord(hypothesis)
    }

    override fun onFinalResult(hypothesis: String?) {
        checkWakeWord(hypothesis)
    }

    override fun onError(exception: Exception?) {
        // Automatically recover and start listening again
        startListening()
    }

    override fun onTimeout() {
        startListening()
    }

    private fun checkWakeWord(text: String?) {
        if (text?.contains("hey jarvis", ignoreCase = true) == true) {
            _wakeWordDetected.tryEmit(Unit)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "J.A.R.V.I.S Active Listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Listening for wake word..."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚡ JARVIS is active")
            .setContentText("Listening for 'Hey Jarvis'")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // Built-in mic icon for MVP
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
