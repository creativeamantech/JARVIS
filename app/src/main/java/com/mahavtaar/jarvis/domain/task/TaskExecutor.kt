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
import com.mahavtaar.jarvis.domain.appcontrol.AppDeepLinkHandler
import com.mahavtaar.jarvis.domain.appcontrol.JarvisAccessibilityService
import com.mahavtaar.jarvis.domain.appcontrol.UsageStatsHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class TaskResult(val badge: String, val spokenFeedback: String? = null)

@Singleton
class TaskExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val privacyBlocklist = listOf(
        "com.google.android.gms",
        "com.android.settings",
        "com.bank",
        "bank",
        "finance",
        "wallet"
    )

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

                // Phase 4
                is JarvisIntent.WhatsAppMsg -> handleWhatsAppMsg(intent.number, intent.message)
                is JarvisIntent.TelegramMsg -> handleTelegramMsg(intent.username, intent.message)
                is JarvisIntent.AppMsg -> handleAppMsg(intent.packageName, intent.message)
                is JarvisIntent.AppClose -> handleAppClose(intent.packageName)
                is JarvisIntent.GlobalBack -> handleGlobalBack()
                is JarvisIntent.GlobalHome -> handleGlobalHome()
                is JarvisIntent.GlobalRecents -> handleGlobalRecents()
                is JarvisIntent.ScreenRead -> handleScreenRead()
                is JarvisIntent.ShareText -> handleShareText(intent.packageName, intent.text)
                is JarvisIntent.AppUsage -> handleAppUsage(intent.packageName)
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

    private suspend fun handleWhatsAppMsg(number: String, message: String): TaskResult {
        val success = AppDeepLinkHandler.whatsappMessage(context, number, message)
        if (success && JarvisAccessibilityService.instance != null) {
            val service = JarvisAccessibilityService.instance!!
            val appReady = service.waitForPackage("com.whatsapp", timeoutMs = 3000)
            if (appReady) {
                // Pre-filled by intent usually, just click send
                service.clickByContentDescription("Send")
                return TaskResult("WHATSAPP MSG SENT")
            }
            return TaskResult("WHATSAPP TIMEOUT")
        }
        return if (success) TaskResult("WHATSAPP OPENED") else TaskResult("FAILED WHATSAPP")
    }

    private suspend fun handleTelegramMsg(username: String, message: String): TaskResult {
        val success = AppDeepLinkHandler.telegramMessage(context, username, message)
        if (success && JarvisAccessibilityService.instance != null) {
            val service = JarvisAccessibilityService.instance!!
            val appReady = service.waitForPackage("org.telegram", timeoutMs = 3000)
            if (appReady) {
                service.clickByContentDescription("Send")
                return TaskResult("TELEGRAM MSG SENT")
            }
            return TaskResult("TELEGRAM TIMEOUT")
        }
        return if (success) TaskResult("TELEGRAM OPENED") else TaskResult("FAILED TELEGRAM")
    }

    private fun handleAppMsg(packageName: String, message: String): TaskResult {
        val service = JarvisAccessibilityService.instance ?: return TaskResult("ACCESSIBILITY REQUIRED", "I require Accessibility permission to perform that action inside apps, sir.")
        val typed = service.typeText(message, packageName)
        return if (typed) TaskResult("TYPED IN APP") else TaskResult("COULD NOT TYPE")
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun handleAppClose(packageName: String): TaskResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.KILL_BACKGROUND_PROCESSES) != PackageManager.PERMISSION_GRANTED) {
            return TaskResult("PERMISSION DENIED", "I am unable to kill background processes without the proper permission, sir.")
        }
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            try { am.killBackgroundProcesses(packageName) } catch(e: SecurityException) { }
            TaskResult("CLOSED APP: \$packageName")
        } catch (e: Exception) {
            TaskResult("FAILED TO CLOSE APP")
        }
    }

    private fun handleGlobalBack(): TaskResult {
        val service = JarvisAccessibilityService.instance ?: return TaskResult("ACCESSIBILITY REQUIRED")
        return if (service.performGlobalBack()) TaskResult("GLOBAL BACK") else TaskResult("FAILED BACK")
    }

    private fun handleGlobalHome(): TaskResult {
        val service = JarvisAccessibilityService.instance ?: return TaskResult("ACCESSIBILITY REQUIRED")
        return if (service.performGlobalHome()) TaskResult("GLOBAL HOME") else TaskResult("FAILED HOME")
    }

    private fun handleGlobalRecents(): TaskResult {
        val service = JarvisAccessibilityService.instance ?: return TaskResult("ACCESSIBILITY REQUIRED")
        return if (service.performGlobalRecents()) TaskResult("GLOBAL RECENTS") else TaskResult("FAILED RECENTS")
    }

    private fun handleScreenRead(): TaskResult {
        val service = JarvisAccessibilityService.instance ?: return TaskResult("ACCESSIBILITY REQUIRED", "I require Accessibility permissions to read the screen, sir.")

        val fgApp = UsageStatsHelper.getForegroundApp(context)
        if (fgApp != null && privacyBlocklist.any { fgApp.contains(it, ignoreCase = true) }) {
            return TaskResult("PRIVACY BLOCK", "I cannot read this screen, sir. It appears to be a sensitive application.")
        }

        val text = service.readScreenContent()
        return TaskResult("SCREEN READ", "Shall I read the current screen, sir? It may contain sensitive information. \n" + text)
    }

    private fun handleShareText(packageName: String?, text: String): TaskResult {
        val success = AppDeepLinkHandler.shareText(context, packageName, text)
        return if (success) TaskResult("SHARED TEXT") else TaskResult("FAILED SHARE")
    }

    private fun handleAppUsage(packageName: String?): TaskResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.PACKAGE_USAGE_STATS) != PackageManager.PERMISSION_GRANTED) {
             return TaskResult("USAGE STATS DENIED", "I require Usage Access to determine screen time, sir.")
        }

        if (packageName != null) {
            val ms = UsageStatsHelper.getDailyScreenTime(context, packageName)
            val formatted = UsageStatsHelper.formatScreenTime(ms)
            val name = UsageStatsHelper.getAppNameFromPackage(context, packageName)
            return TaskResult("USAGE: \$formatted", "You have spent \$formatted on \$name today, sir.")
        } else {
            val fg = UsageStatsHelper.getForegroundApp(context) ?: "Unknown"
            val name = UsageStatsHelper.getAppNameFromPackage(context, fg)
            return TaskResult("FOREGROUND: \$name")
        }
    }
}
