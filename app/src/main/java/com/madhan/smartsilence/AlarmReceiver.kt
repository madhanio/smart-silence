package com.madhan.smartsilence

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
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
                    automationPrefs.edit()
                        .putBoolean("skipped_today", false)
                        .putBoolean("responded_today", true)
                        .apply()
                    nm.cancel(2001)
                    nm.cancel(2002)
                    Toast.makeText(context, "Automation active for today! 👍", Toast.LENGTH_SHORT).show()
                    notifyMainActivity(context)
                    return
                }
                "ACTION_SKIP_COLLEGE" -> {
                    automationPrefs.edit()
                        .putBoolean("skipped_today", true)
                        .putBoolean("responded_today", true)
                        .apply()
                    
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
            automationPrefs.edit()
                .putBoolean("skipped_today", false)
                .putBoolean("responded_today", false)
                .apply()
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

            if (mode == "SILENT" && !hasResponded && !isSkipped) {
                automationPrefs.edit()
                    .putBoolean("skipped_today", true)
                    .apply()

                Toast.makeText(
                    context,
                    "No response. Automation skipped for today.",
                    Toast.LENGTH_LONG
                ).show()

                notifyMainActivity(context)
                return
            }

            if ((isWeekday || isManualTest) && !isSkipped) {
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
        val channelId = "morning_check_custom_sound" // Unique channel ID for custom sound
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + R.raw.notification_sound)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
                
            val channel = NotificationChannel(channelId, "Morning Check", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(soundUri, audioAttributes)
            }
            nm.createNotificationChannel(channel)
        }

        val options = if (isFollowUp) {
            listOf(
                Pair("Em aipoyav? Reply ivvaledu inka.", Pair("Veltunna le ra 🚶‍♂️", "Vellatle 🛑")),
                Pair("Time aipothundi, thondaraga cheppu. ⏳", Pair("Set chesey ✅", "Vadiley ❌")),
                Pair("Edho okati select chey, veltunnava ledha? 🤷‍♂️", Pair("Ha ha veltunna 🏃‍♂️", "Ledhu ra 💤")),
                Pair("Silent mode ON cheyala vodha? Clear ga cheppu. 📱", Pair("Chey ✅", "Vodhu ❌"))
            )
        } else {
            listOf(
                Pair("Eroju college ki velthunnava leka intlona? 🤔", Pair("Velthunna 🎒", "Intlone 🛌")),
                Pair("Time avthundi, inka ready avaledha? ⏰", Pair("Ready ayya 🏃‍♂️", "Eroju bunk ❌")),
                Pair("Automation start cheyana leka eroju lite aa? ⚙️", Pair("Start chey 👍", "Odhu le ✋")),
                Pair("Inka levaleda? College velle idea undha sir? 🌅", Pair("Undi 🚶‍♂️", "Ledhu 💤")),
                Pair("Attendance chusko.. velthunnava ledha eeroju? 📊", Pair("Yes ✅", "No ❌"))
            )
        }

        val selected = options.random()
        val title = selected.first
        val buttons = selected.second

        val yesIntent = Intent(context, AlarmReceiver::class.java).apply { action = "ACTION_GO_COLLEGE" }
        val yesPendingIntent = PendingIntent.getBroadcast(context, 1001, yesIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val noIntent = Intent(context, AlarmReceiver::class.java).apply { action = "ACTION_SKIP_COLLEGE" }
        val noPendingIntent = PendingIntent.getBroadcast(context, 1002, noIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.bell_only)
            .setContentTitle(title)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .addAction(0, buttons.first, yesPendingIntent)
            .addAction(0, buttons.second, noPendingIntent)
            .build()

        nm.notify(if (isFollowUp) 2002 else 2001, notification)
    }
}