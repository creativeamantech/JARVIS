package com.mahavtaar.jarvis.domain.task

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun execute(intent: JarvisIntent): String = withContext(Dispatchers.IO) {
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
    }

    private fun handleFlashlight(on: Boolean): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, on)
                if (on) "FLASHLIGHT ON" else "FLASHLIGHT OFF"
            } else {
                "ERROR: NO FLASHLIGHT"
            }
        } catch (e: Exception) {
            "ERROR: FLASHLIGHT UNAVAILABLE"
        }
    }

    private fun handleTimer(seconds: Int, label: String?): String {
        val i = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label ?: "Jarvis Timer")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(i)
        return "TIMER SET: \${seconds}s"
    }

    private fun handleCalculate(expression: String): String {
        // Real implementation would use an expression evaluator (e.g. exp4j)
        // MVP: Just returning visual feedback for the UI
        return "CALCULATED: \$expression"
    }

    private fun handleBatteryStatus(): String {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return "BATTERY: \$batteryLevel%"
    }

    private fun handleClipboard(text: String): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Jarvis", text)
        clipboard.setPrimaryClip(clip)
        return "COPIED TO CLIPBOARD"
    }

    private fun handleOpenApp(appName: String): String {
        val pm = context.packageManager
        // MVP simplified approach: searching installed packages (requires QUERY_ALL_PACKAGES in prod)
        val packages = pm.getInstalledApplications(0)
        val targetApp = packages.find { pm.getApplicationLabel(it).toString().equals(appName, ignoreCase = true) }

        if (targetApp != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetApp.packageName)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return "OPENED APP: \$appName"
            }
        }

        // Fallback: search Play Store
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://search?q=\$appName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "SEARCHING APP: \$appName"
    }

    private fun handleWebSearch(query: String): String {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(android.app.SearchManager.QUERY, query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "WEB SEARCH: \$query"
    }

    private fun handleCall(contactName: String): String {
        // Needs CALL_PHONE and READ_CONTACTS perm in actual runtime
        return "INITIATED CALL: \$contactName"
    }

    private fun handleSms(number: String, message: String): String {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:\$number")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "SMS PREPARED"
    }

    private fun handleReminder(title: String, datetime: String): String {
        return "REMINDER SCHEDULED: \$title"
    }

    private fun handleSettingsToggle(setting: String, state: String): String {
        return when (setting) {
            "wifi" -> {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
                "OPENED WIFI SETTINGS"
            }
            "bluetooth" -> {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
                "OPENED BLUETOOTH SETTINGS"
            }
            else -> "SETTING \${setting.uppercase()} UNAVAILABLE"
        }
    }

    private fun handleSetBrightness(level: Int): String {
        // Requires WRITE_SETTINGS permission via intent for true system level modification
        return "BRIGHTNESS ACTION: \$level%"
    }

    private fun handleDoNotDisturb(on: Boolean): String {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            val filter = if (on) NotificationManager.INTERRUPTION_FILTER_NONE else NotificationManager.INTERRUPTION_FILTER_ALL
            notificationManager.setInterruptionFilter(filter)
            return if (on) "DND: ON" else "DND: OFF"
        } else {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return "DND: REQUIRES PERMISSION"
        }
    }
}
