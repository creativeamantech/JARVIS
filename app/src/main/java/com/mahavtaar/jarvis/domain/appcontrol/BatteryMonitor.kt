package com.mahavtaar.jarvis.domain.appcontrol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class BatteryMonitor(private val context: Context) {

    private val _batteryWarning = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val batteryWarning = _batteryWarning.asSharedFlow()

    private var hasWarnedThisSession = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val isCharging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING

                if (level != -1 && scale != -1) {
                    val batteryPct = (level * 100) / scale.toFloat()

                    if (batteryPct <= 15f && !isCharging && !hasWarnedThisSession) {
                        hasWarnedThisSession = true
                        _batteryWarning.tryEmit(batteryPct.toInt())
                    } else if (isCharging) {
                        // Reset if plugged in so it can warn again later if disconnected and drains again
                        hasWarnedThisSession = false
                    }
                }
            }
        }
    }

    fun startMonitoring() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
    }

    fun stopMonitoring() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) { }
    }
}
