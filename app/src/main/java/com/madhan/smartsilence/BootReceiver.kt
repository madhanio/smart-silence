package com.madhan.smartsilence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val appSettings = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            val isEnabled = appSettings.getBoolean("automation_enabled", false)
            
            if (isEnabled) {
                val scheduler = AlarmScheduler(context)
                scheduler.scheduleAutomationAlarms()
            }
        }
    }
}
