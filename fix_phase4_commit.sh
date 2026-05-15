# Redoing Phase 4 files since they were apparently lost in the git state.

mkdir -p app/src/main/res/xml
cat << 'XML_EOF' > app/src/main/res/xml/accessibility_service_config.xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowContentChanged|typeWindowStateChanged|typeViewClicked"
    android:accessibilityFeedbackType="feedbackSpoken|feedbackVisual"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows|flagReportViewIds|flagIncludeNotImportantViews"
    android:canPerformGestures="true"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100" />
XML_EOF

mkdir -p app/src/main/java/com/mahavtaar/jarvis/domain/appcontrol

cat << 'CODE_EOF' > app/src/main/java/com/mahavtaar/jarvis/domain/appcontrol/JarvisAccessibilityService.kt
package com.mahavtaar.jarvis.domain.appcontrol

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        var instance: JarvisAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }

    override fun onInterrupt() { }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    fun typeText(text: String, targetPackage: String? = null): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val editTexts = findEditTexts(rootNode)

        val targetNode = editTexts.firstOrNull {
            targetPackage == null || it.packageName?.toString() == targetPackage
        } ?: editTexts.firstOrNull()

        return if (targetNode != null) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        } else {
            false
        }
    }

    fun clickByContentDescription(desc: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val node = findNodeByContentDescription(rootNode, desc)
        return node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    fun readScreenContent(): String {
        val rootNode = rootInActiveWindow ?: return "Screen content unavailable."
        val textBuilder = StringBuilder()
        extractText(rootNode, textBuilder)
        return textBuilder.toString().trim().takeIf { it.isNotBlank() } ?: "No readable text found on screen."
    }

    fun performGlobalBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun performGlobalHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun performGlobalRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)

    private fun findEditTexts(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val list = mutableListOf<AccessibilityNodeInfo>()
        if (node.isEditable) {
            list.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) list.addAll(findEditTexts(child))
        }
        return list
    }

    private fun findNodeByContentDescription(node: AccessibilityNodeInfo, desc: String): AccessibilityNodeInfo? {
        if (node.contentDescription?.toString()?.equals(desc, ignoreCase = true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val result = findNodeByContentDescription(child, desc)
                if (result != null) return result
            }
        }
        return null
    }

    private fun extractText(node: AccessibilityNodeInfo, builder: StringBuilder) {
        if (node.text != null) {
            builder.append(node.text).append(". ")
        } else if (node.contentDescription != null) {
            builder.append(node.contentDescription).append(". ")
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) extractText(child, builder)
        }
    }
}
CODE_EOF

cat << 'CODE_EOF' > app/src/main/java/com/mahavtaar/jarvis/domain/appcontrol/AppDeepLinkHandler.kt
package com.mahavtaar.jarvis.domain.appcontrol

import android.content.Context
import android.content.Intent
import android.net.Uri

object AppDeepLinkHandler {
    fun whatsappMessage(context: Context, number: String, message: String): Boolean {
        return try {
            val uri = Uri.parse("https://wa.me/\$number?text=\${Uri.encode(message)}")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            true
        } catch (e: Exception) { false }
    }

    fun telegramMessage(context: Context, username: String, message: String): Boolean {
        return try {
            val uri = Uri.parse("tg://resolve?domain=\$username&text=\${Uri.encode(message)}")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            true
        } catch (e: Exception) { false }
    }

    fun shareText(context: Context, packageName: String?, text: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (packageName != null) intent.setPackage(packageName)
            context.startActivity(intent)
            true
        } catch (e: Exception) { false }
    }
}
CODE_EOF

cat << 'CODE_EOF' > app/src/main/java/com/mahavtaar/jarvis/domain/appcontrol/UsageStatsHelper.kt
package com.mahavtaar.jarvis.domain.appcontrol

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import java.util.Calendar

object UsageStatsHelper {
    fun getForegroundApp(context: Context): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000 * 60 * 5

        val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
        val event = android.app.usage.UsageEvents.Event()
        var currentForegroundApp: String? = null

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                currentForegroundApp = event.packageName
            }
        }
        return currentForegroundApp
    }

    fun getDailyScreenTime(context: Context, packageName: String): Long {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, calendar.timeInMillis, System.currentTimeMillis())
        return stats.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
    }

    fun formatScreenTime(timeMs: Long): String {
        val minutes = (timeMs / (1000 * 60)) % 60
        val hours = (timeMs / (1000 * 60 * 60))
        return if (hours > 0) "\${hours}h \${minutes}m" else "\${minutes}m"
    }

    fun getAppNameFromPackage(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) { packageName }
    }
}
CODE_EOF
