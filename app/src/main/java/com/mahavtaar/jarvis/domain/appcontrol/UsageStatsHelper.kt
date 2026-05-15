package com.mahavtaar.jarvis.domain.appcontrol

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import java.util.Calendar

object UsageStatsHelper {
    fun getForegroundApp(context: Context): String? {
        return runCatching {
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
            currentForegroundApp
        }.getOrNull()
    }

    fun getDailyScreenTime(context: Context, packageName: String): Long {
        return runCatching {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)

            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, calendar.timeInMillis, System.currentTimeMillis())
            stats?.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
        }.getOrDefault(0L)
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
