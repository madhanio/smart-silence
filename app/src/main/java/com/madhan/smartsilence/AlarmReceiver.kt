package com.madhan.smartsilence

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.util.*

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val mode = intent.getStringExtra("MODE")
        val action = intent.action
        val isManualTest = intent.getBooleanExtra("IS_MANUAL_TEST", false)

        val soundManager = SoundManager(context)
        val automationPrefs = context.getSharedPreferences("AutomationPrefs", Context.MODE_PRIVATE)
        val appSettings = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val isAutomationEnabled = appSettings.getBoolean("automation_enabled", false)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (action != null) {
            when (action) {
                "ACTION_GO_COLLEGE" -> {
                    automationPrefs.edit().putBoolean("skipped_today", false).putBoolean("responded_today", true).apply()
                    nm.cancel(2001)
                    nm.cancel(2002)
                    Toast.makeText(context, "Automation active for today! 👍", Toast.LENGTH_SHORT).show()
                    notifyMainActivity(context)
                    return
                }
                "ACTION_SKIP_COLLEGE" -> {
                    automationPrefs.edit().putBoolean("skipped_today", true).putBoolean("responded_today", true).apply()
                    appSettings.edit().putBoolean("automation_enabled", false).apply()
                    val scheduler = AlarmScheduler(context)
                    scheduler.cancelAllAlarms()
                    nm.cancel(2001)
                    nm.cancel(2002)
                    soundManager.setNormalMode()
                    Toast.makeText(context, "Enjoy your day off! 🌴", Toast.LENGTH_SHORT).show()
                    notifyMainActivity(context)
                    return
                }
            }
        }

        if (mode == null) return

        if (mode == "RESET") {
            automationPrefs.edit().putBoolean("skipped_today", false).putBoolean("responded_today", false).apply()
            notifyMainActivity(context)
            return
        }

        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val isWeekday = dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY

        if (mode == "MORNING_CHECK") {
            if (isWeekday || isManualTest) {
                automationPrefs.edit().putBoolean("responded_today", false).apply()
                showMorningNotification(context, isFollowUp = false)
            }
            return
        }

        if (mode == "MISSED_CHECK") {
            val hasResponded = automationPrefs.getBoolean("responded_today", false)
            if (!hasResponded && (isWeekday || isManualTest)) {
                nm.cancel(2001)
                showMorningNotification(context, isFollowUp = true)
            }
            return
        }

        if (isAutomationEnabled) {

            val isSkipped = automationPrefs.getBoolean("skipped_today", false)
            val hasResponded = automationPrefs.getBoolean("responded_today", false)

            // ⭐ skip ONLY at FIRST silent trigger (start time)
            if (mode == "SILENT" && !hasResponded && !isSkipped) {

                automationPrefs.edit()
                    .putBoolean("skipped_today", true)
                    .apply()

                val scheduler = AlarmScheduler(context)
                scheduler.cancelAllAlarms()

                Toast.makeText(
                    context,
                    "No response. Automation skipped for today.",
                    Toast.LENGTH_LONG
                ).show()

                notifyMainActivity(context)
                return
            }

            if (isWeekday && !isSkipped) {

                when (mode) {

                    "SILENT" -> {
                        soundManager.setSilentMode()
                        Toast.makeText(
                            context,
                            "Automation: Phone Silenced 🤫",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    "NORMAL" -> {
                        soundManager.setNormalMode()
                        Toast.makeText(
                            context,
                            "Automation: Sound Restored 🔊",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                notifyMainActivity(context)
            }
        }
    }

    private fun notifyMainActivity(context: Context) {
        val intent = Intent("com.madhan.clgautomation.UPDATE_STATUS")
        context.sendBroadcast(intent)
    }

    private fun showMorningNotification(context: Context, isFollowUp: Boolean) {
        val channelId = "morning_check"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Morning Check", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(channel)
        }

        val messages = if (isFollowUp) {
            listOf(Triple("Orey! Reply ivvadam marchipoyava? 🤦‍♂️", "Mowa, please choose an option bro.", Pair("Veltunna ✅", "Vellatle ❌")))
        } else {
            listOf(Triple("College ki velthunnava? 🎒", "Today automation start cheyamantava?", Pair("Avunu ✅", "Vellatle ❌")))
        }

        val selected = messages.random()
        val yesIntent = Intent(context, AlarmReceiver::class.java).apply { action = "ACTION_GO_COLLEGE" }
        val yesPendingIntent = PendingIntent.getBroadcast(context, 1, yesIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val noIntent = Intent(context, AlarmReceiver::class.java).apply { action = "ACTION_SKIP_COLLEGE" }
        val noPendingIntent = PendingIntent.getBroadcast(context, 2, noIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.bell_only)
            .setContentTitle(selected.first)
            .setContentText(selected.second)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .addAction(0, selected.third.first, yesPendingIntent)
            .addAction(0, selected.third.second, noPendingIntent)
            .build()

        nm.notify(if (isFollowUp) 2002 else 2001, notification)
    }
}