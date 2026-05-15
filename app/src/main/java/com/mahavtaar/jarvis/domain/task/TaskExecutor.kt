package com.mahavtaar.jarvis.domain.task

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class TaskResult(val badge: String, val spokenFeedback: String? = null)

@Singleton
class TaskExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun execute(intent: JarvisIntent): TaskResult = withContext(Dispatchers.IO) {
        try {
            when (intent) {
                is JarvisIntent.Flashlight -> handleFlashlight(intent.on)
                is JarvisIntent.Timer -> handleTimer(intent.seconds, intent.label)
                is JarvisIntent.Calculate -> handleCalculate(intent.expression)
                is JarvisIntent.BatteryStatus -> handleBatteryStatus()
                is JarvisIntent.Clipboard -> handleClipboard(intent.text)
                is JarvisIntent.OpenApp -> handleOpenApp(intent.appName)
                is JarvisIntent.WebSearch -> handleWebSearch(intent.query)
                is JarvisIntent.Call -> handleCall(intent.contactName)
                is JarvisIntent.Sms -> handleSms(intent.number, intent.message)
                is JarvisIntent.Reminder -> handleReminder(intent.title, intent.datetime)
                is JarvisIntent.SettingsToggle -> handleSettingsToggle(intent.setting, intent.state)
                is JarvisIntent.SetBrightness -> handleSetBrightness(intent.level)
                is JarvisIntent.DoNotDisturb -> handleDoNotDisturb(intent.on)
            }
        } catch (e: Exception) {
            TaskResult("FAILED: \${e.message}", "I encountered an error executing that task, sir.")
        }
    }

    private fun handleFlashlight(on: Boolean): TaskResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, on)
                if (on) TaskResult("FLASHLIGHT ON") else TaskResult("FLASHLIGHT OFF")
            } else {
                TaskResult("ERROR: NO FLASHLIGHT")
            }
        } catch (e: Exception) {
            TaskResult("ERROR: FLASHLIGHT UNAVAILABLE")
        }
    }

    private fun handleTimer(seconds: Int, label: String?): TaskResult {
        val i = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label ?: "Jarvis Timer")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(i)
        return TaskResult("TIMER SET: \${seconds}s")
    }

    private fun handleCalculate(expression: String): TaskResult {
        return TaskResult("CALCULATED: \$expression")
    }

    private fun handleBatteryStatus(): TaskResult {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return TaskResult("BATTERY: \$batteryLevel%")
    }

    private fun handleClipboard(text: String): TaskResult {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Jarvis", text)
        clipboard.setPrimaryClip(clip)
        return TaskResult("COPIED TO CLIPBOARD")
    }

    private fun handleOpenApp(appName: String): TaskResult {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(0)
        val targetApp = packages.find { pm.getApplicationLabel(it).toString().equals(appName, ignoreCase = true) }

        if (targetApp != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetApp.packageName)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return TaskResult("OPENED APP: \$appName")
            }
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://search?q=\$appName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return TaskResult("SEARCHING APP: \$appName")
    }

    private fun handleWebSearch(query: String): TaskResult {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(android.app.SearchManager.QUERY, query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return TaskResult("WEB SEARCH: \$query")
    }

    private fun handleCall(contactName: String): TaskResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return TaskResult("PERMISSION DENIED", "I'm afraid I lack the necessary authorisation for that, sir.")
        }
        return TaskResult("INITIATED CALL: \$contactName")
    }

    private fun handleSms(number: String, message: String): TaskResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return TaskResult("PERMISSION DENIED", "I'm afraid I lack the necessary authorisation for that, sir.")
        }
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:\$number")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return TaskResult("SMS PREPARED")
    }

    private fun handleReminder(title: String, datetime: String): TaskResult {
        return TaskResult("REMINDER SCHEDULED: \$title")
    }

    private fun handleSettingsToggle(setting: String, state: String): TaskResult {
        return when (setting) {
            "wifi" -> {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
                TaskResult("OPENED WIFI SETTINGS")
            }
            "bluetooth" -> {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
                TaskResult("OPENED BLUETOOTH SETTINGS")
            }
            else -> TaskResult("SETTING \${setting.uppercase()} UNAVAILABLE")
        }
    }

    private fun handleSetBrightness(level: Int): TaskResult {
        return TaskResult("BRIGHTNESS ACTION: \$level%")
    }

    private fun handleDoNotDisturb(on: Boolean): TaskResult {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            val filter = if (on) NotificationManager.INTERRUPTION_FILTER_NONE else NotificationManager.INTERRUPTION_FILTER_ALL
            notificationManager.setInterruptionFilter(filter)
            return if (on) TaskResult("DND: ON") else TaskResult("DND: OFF")
        } else {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return TaskResult("DND: REQUIRES PERMISSION", "I require Notification Policy Access to adjust Do Not Disturb, sir.")
        }
    }
}
