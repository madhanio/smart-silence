package com.madhan.smartsilence

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var soundManager: SoundManager
    private lateinit var scheduler: AlarmScheduler
    private lateinit var appSettings: SharedPreferences
    private lateinit var automationPrefs: SharedPreferences
    private var isProgrammaticChange = false

    private lateinit var tvStartTime: TextView
    private lateinit var tvLunchStartTime: TextView
    private lateinit var tvLunchEndTime: TextView
    private lateinit var tvEndTime: TextView
    private lateinit var tvCurrentStatus: TextView
    private lateinit var tvNextAction: TextView
    private lateinit var statusDot: View
    private lateinit var automationSwitch: MaterialSwitch
    private lateinit var expandableSchedule: LinearLayout
    private lateinit var ivScheduleChevron: ImageView

    private lateinit var tvFocusMode: TextView
    private lateinit var ivFocusIcon: ImageView
    private lateinit var ivFocusChevron: ImageView

    private var focusEndTime: String? = null

    private val statusUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            runOnUiThread { refreshUIState() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    decorView.systemUiVisibility =
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
                statusBarColor = Color.TRANSPARENT
            }
        }

        setContentView(R.layout.activity_main)
        handleFirstLaunchPermissions()

        soundManager = SoundManager(this)
        scheduler = AlarmScheduler(this)
        appSettings = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        automationPrefs = getSharedPreferences("AutomationPrefs", Context.MODE_PRIVATE)

        automationSwitch = findViewById(R.id.automationSwitch)
        val layoutAutomationSwitch = findViewById<View>(R.id.layoutAutomationSwitch)
        val layoutSkipToday = findViewById<LinearLayout>(R.id.layoutSkipToday)
        val layoutFocusMode = findViewById<LinearLayout>(R.id.layoutFocusMode)
        val layoutToggleSchedule = findViewById<LinearLayout>(R.id.layoutToggleSchedule)
        expandableSchedule = findViewById(R.id.expandableSchedule)
        ivScheduleChevron = findViewById(R.id.ivScheduleChevron)

        tvFocusMode = findViewById(R.id.tvFocusMode)
        ivFocusIcon = findViewById(R.id.ivFocusIcon)
        ivFocusChevron = findViewById(R.id.ivFocusChevron)

        val layoutStartTime = findViewById<LinearLayout>(R.id.layoutStartTime)
        val layoutLunchBreak = findViewById<LinearLayout>(R.id.layoutLunchBreak)
        val layoutEndTime = findViewById<LinearLayout>(R.id.layoutEndTime)

        tvStartTime = findViewById(R.id.tvStartTime)
        tvLunchStartTime = findViewById(R.id.tvLunchStartTime)
        tvLunchEndTime = findViewById(R.id.tvLunchEndTime)
        tvEndTime = findViewById(R.id.tvEndTime)

        tvCurrentStatus = findViewById(R.id.tvCurrentStatus)
        tvNextAction = findViewById(R.id.tvNextAction)
        statusDot = findViewById(R.id.statusIndicator)

        checkPermissions()
        loadAndDisplaySettings()

        layoutAutomationSwitch.setOnClickListener { automationSwitch.toggle() }

        automationSwitch.setOnCheckedChangeListener { view, isChecked ->
            if (isProgrammaticChange) return@setOnCheckedChangeListener
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            updateAutomationState(isChecked)
            updateStatusDisplay()
        }

        layoutStartTime.setOnClickListener { showTimePicker("start") }
        layoutLunchBreak.setOnClickListener {
            showTimePicker("lunch_start") { showTimePicker("lunch_end") }
        }
        layoutEndTime.setOnClickListener { showTimePicker("end") }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(
            statusUpdateReceiver,
            IntentFilter("com.madhan.clgautomation.UPDATE_STATUS")
        )
        refreshUIState()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(statusUpdateReceiver)
    }

    private fun refreshUIState() {
        isProgrammaticChange = true
        automationSwitch.isChecked = appSettings.getBoolean("automation_enabled", false)
        isProgrammaticChange = false
        loadAndDisplaySettings()
    }

    private fun loadAndDisplaySettings() {
        updateUITime(appSettings.getInt("start_hour", 9), appSettings.getInt("start_minute", 0), "start")
        updateUITime(appSettings.getInt("lunch_start_hour", 12), appSettings.getInt("lunch_start_minute", 15), "lunch_start")
        updateUITime(appSettings.getInt("lunch_end_hour", 13), appSettings.getInt("lunch_end_minute", 0), "lunch_end")
        updateUITime(appSettings.getInt("end_hour", 16), appSettings.getInt("end_minute", 0), "end")
        updateStatusDisplay()
    }

    private fun showTimePicker(type: String, onComplete: (() -> Unit)? = null) {
        val currentHour = appSettings.getInt("${type}_hour", 9)
        val currentMinute = appSettings.getInt("${type}_minute", 0)
        TimePickerDialog(this, { _, hour, minute ->
            appSettings.edit().putInt("${type}_hour", hour).putInt("${type}_minute", minute).apply()
            updateUITime(hour, minute, type)
            if (automationSwitch.isChecked) scheduler.scheduleAutomationAlarms()
            updateStatusDisplay()
            onComplete?.invoke()
        }, currentHour, currentMinute, false).show()
    }

    private fun updateUITime(hour: Int, minute: Int, type: String) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val formattedTime = SimpleDateFormat("hh:mm a", Locale.US).format(calendar.time)
        when (type) {
            "start" -> tvStartTime.text = formattedTime
            "lunch_start" -> tvLunchStartTime.text = formattedTime
            "lunch_end" -> tvLunchEndTime.text = formattedTime
            "end" -> tvEndTime.text = formattedTime
        }
    }

    private fun updateAutomationState(isEnabled: Boolean) {
        appSettings.edit().putBoolean("automation_enabled", isEnabled).apply()
        if (isEnabled) {
            scheduler.scheduleAutomationAlarms()
            Toast.makeText(this, "Automation Enabled", Toast.LENGTH_SHORT).show()
        } else {
            scheduler.cancelAllAlarms()
            soundManager.setNormalMode()
            Toast.makeText(this, "Automation Disabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatusDisplay() {
        val isEnabled = appSettings.getBoolean("automation_enabled", false)
        if (!isEnabled) {
            tvCurrentStatus.text = "AUTOMATION DISABLED"
            tvNextAction.text = "Turn on to start scheduling"
            return
        }
        tvCurrentStatus.text = "SYSTEM ACTIVE"
        tvNextAction.text = "Automation running"
    }

    private fun checkPermissions() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!notificationManager.isNotificationPolicyAccessGranted) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
    }

    private fun handleFirstLaunchPermissions() {
        val prefs = getSharedPreferences("FirstLaunch", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        if (!isFirstLaunch) return
        prefs.edit().putBoolean("is_first_launch", false).apply()
        requestAllRequiredPermissions()
    }

    private fun requestAllRequiredPermissions() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!notificationManager.isNotificationPolicyAccessGranted) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
    }
}