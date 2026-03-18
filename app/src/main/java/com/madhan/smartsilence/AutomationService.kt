package com.madhan.smartsilence

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AutomationService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getStringExtra("MODE")
        
        createNotificationChannel()
        val notification = createNotification()
        startForeground(1, notification)

        val soundManager = SoundManager(this)
        val prefs = getSharedPreferences("AutomationPrefs", Context.MODE_PRIVATE)
        val isAutomationEnabled = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            .getBoolean("automation_enabled", false)

        if (isAutomationEnabled) {
            val isSkipped = prefs.getBoolean("skipped_today", false)
            if (!isSkipped) {
                when (mode) {
                    "SILENT" -> soundManager.setSilentMode()
                    "NORMAL" -> soundManager.setNormalMode()
                }
            }
        }

        stopForeground(true)
        stopSelf()
        
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "automation_service",
                "Automation Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "automation_service")
            .setContentTitle("Smart Silence")
            .setContentText("Updating sound profile...")
            .setSmallIcon(R.drawable.bell_only)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
