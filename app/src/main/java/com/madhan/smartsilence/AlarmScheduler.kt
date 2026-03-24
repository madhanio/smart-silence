package com.madhan.smartsilence

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.util.*

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val appPrefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

    fun scheduleAutomationAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return
            }
        }

        val startH = appPrefs.getInt("start_hour", 9)
        val startM = appPrefs.getInt("start_minute", 0)

        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startH)
            set(Calendar.MINUTE, startM)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val notifyCal = startCal.clone() as Calendar
        notifyCal.add(Calendar.MINUTE, -30)

        scheduleAlarm(
            notifyCal.get(Calendar.HOUR_OF_DAY),
            notifyCal.get(Calendar.MINUTE),
            "MORNING_CHECK",
            105
        )

        val followCal = startCal.clone() as Calendar
        followCal.add(Calendar.MINUTE, -15)

        scheduleAlarm(
            followCal.get(Calendar.HOUR_OF_DAY),
            followCal.get(Calendar.MINUTE),
            "MISSED_CHECK",
            107
        )

        scheduleAlarm(startH, startM, "MORNING_SILENT", 101)

        val lStartH = appPrefs.getInt("lunch_start_hour", 12)
        val lStartM = appPrefs.getInt("lunch_start_minute", 15)
        scheduleAlarm(lStartH, lStartM, "NORMAL", 102)

        val lEndH = appPrefs.getInt("lunch_end_hour", 13)
        val lEndM = appPrefs.getInt("lunch_end_minute", 0)
        scheduleAlarm(lEndH, lEndM, "SILENT", 103)

        val endH = appPrefs.getInt("end_hour", 16)
        val endM = appPrefs.getInt("end_minute", 0)
        scheduleAlarm(endH, endM, "NORMAL", 104)

        scheduleAlarm(0, 0, "RESET", 106)
    }

    fun scheduleAlarm(hour: Int, minute: Int, mode: String, requestCode: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("MODE", mode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelAlarm(requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    fun cancelAllAlarms() {
        val ids = intArrayOf(101, 102, 103, 104, 105, 106, 107, 108)
        for (code in ids) {
            cancelAlarm(code)
        }
    }
}