package com.madhan.smartsilence

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TextSwitcher
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var soundManager: SoundManager
    private lateinit var scheduler: AlarmScheduler
    private lateinit var appSettings: SharedPreferences
    private lateinit var automationPrefs: SharedPreferences
    private var isProgrammaticChange = false
    private var lastStatusColor = Color.parseColor("#34C759")
    private var colorAnimator: ValueAnimator? = null

    private lateinit var tvStartTime: TextView
    private lateinit var tvLunchStartTime: TextView
    private lateinit var tvLunchEndTime: TextView
    private lateinit var tvEndTime: TextView
    private lateinit var tsCurrentStatus: TextSwitcher
    private lateinit var tsNextAction: TextSwitcher
    private lateinit var statusCard: View
    private lateinit var statusDot: View
    private lateinit var automationSwitch: MaterialSwitch
    private lateinit var expandableSchedule: LinearLayout
    private lateinit var ivScheduleChevron: ImageView

    private lateinit var tvFocusMode: TextView
    private lateinit var ivFocusIcon: ImageView
    private lateinit var ivFocusChevron: ImageView
    private lateinit var tvSkipToday: TextView

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
                    val isNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
                    var flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    if (!isNightMode) {
                        flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    }
                    decorView.systemUiVisibility = flags
                }
                statusBarColor = Color.TRANSPARENT
            }
        }

        setContentView(R.layout.activity_main)

        soundManager = SoundManager(this)
        scheduler = AlarmScheduler(this)
        appSettings = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        automationPrefs = getSharedPreferences("AutomationPrefs", Context.MODE_PRIVATE)

        // Initialize default values if not present
        if (!appSettings.contains("start_hour")) {
            appSettings.edit()
                .putInt("start_hour", 9).putInt("start_minute", 0)
                .putInt("lunch_start_hour", 12).putInt("lunch_start_minute", 15)
                .putInt("lunch_end_hour", 13).putInt("lunch_end_minute", 0)
                .putInt("end_hour", 16).putInt("end_minute", 0)
                .apply()
        }

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
        tvSkipToday = findViewById(R.id.tvSkipToday)

        val layoutStartTime = findViewById<LinearLayout>(R.id.layoutStartTime)
        val layoutLunchBreak = findViewById<LinearLayout>(R.id.layoutLunchBreak)
        val layoutEndTime = findViewById<LinearLayout>(R.id.layoutEndTime)

        tvStartTime = findViewById(R.id.tvStartTime)
        tvLunchStartTime = findViewById(R.id.tvLunchStartTime)
        tvLunchEndTime = findViewById(R.id.tvLunchEndTime)
        tvEndTime = findViewById(R.id.tvEndTime)

        tsCurrentStatus = findViewById(R.id.tsCurrentStatus)
        tsNextAction = findViewById(R.id.tsNextAction)
        statusCard = findViewById(R.id.statusCard)
        statusDot = findViewById(R.id.statusIndicator)

        loadAndDisplaySettings()
        checkFirstLaunchPermissions()
        checkBatteryOptimizations()

        layoutAutomationSwitch.setOnClickListener { automationSwitch.toggle() }

        automationSwitch.setOnCheckedChangeListener { view, isChecked ->
            if (isProgrammaticChange) return@setOnCheckedChangeListener
            
            if (isChecked && !hasDNDPermission()) {
                showPermissionDialog()
                
                isProgrammaticChange = true
                automationSwitch.isChecked = false
                isProgrammaticChange = false
                return@setOnCheckedChangeListener
            }
            
            updateAutomationState(isChecked)
            updateStatusDisplay()
        }

        layoutSkipToday.setOnClickListener {
            if (!automationSwitch.isChecked) {
                Toast.makeText(this, "Enable automation first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentTime = Calendar.getInstance()
            val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
            val currentMinute = currentTime.get(Calendar.MINUTE)
            val endHour = appSettings.getInt("end_hour", 16)
            val endMinute = appSettings.getInt("end_minute", 0)

            val now = currentHour * 60 + currentMinute
            val end = endHour * 60 + endMinute

            if (now >= end) {
                Toast.makeText(this, "Today's schedule is already finished", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isSkipped = automationPrefs.getBoolean("skipped_today", false)
            if (isSkipped) {
                automationPrefs.edit().putBoolean("skipped_today", false).apply()
                Toast.makeText(this, "Automation restored for today", Toast.LENGTH_SHORT).show()
            } else {
                automationPrefs.edit().putBoolean("skipped_today", true).apply()
                Toast.makeText(this, "Automation skipped for today", Toast.LENGTH_SHORT).show()
            }
            loadAndDisplaySettings()
        }

        layoutFocusMode.setOnClickListener {
            val isCurrentlyFocus = automationPrefs.getBoolean("manual_focus_active", false)
            if (isCurrentlyFocus) {
                soundManager.setNormalMode()
                automationPrefs.edit().putBoolean("manual_focus_active", false).apply()
                scheduler.cancelAlarm(108) // Cancel focus end alarm
                Toast.makeText(this, "Focus Mode Ended", Toast.LENGTH_SHORT).show()
                loadAndDisplaySettings()
            } else {
                if (hasDNDPermission()) {
                    showFocusDurationPicker()
                } else {
                    requestDNDPermission()
                }
            }
        }

        val mainContentContainer = findViewById<LinearLayout>(R.id.mainContentContainer)
        mainContentContainer?.layoutTransition?.enableTransitionType(android.animation.LayoutTransition.CHANGING)
        
        val scheduleCardInner = findViewById<LinearLayout>(R.id.layoutScheduleCardInner)
        scheduleCardInner?.layoutTransition?.enableTransitionType(android.animation.LayoutTransition.CHANGING)

        layoutToggleSchedule.setOnClickListener {
            if (expandableSchedule.visibility == View.VISIBLE) {
                expandableSchedule.visibility = View.GONE
                ivScheduleChevron.animate().rotation(0f).setDuration(200).start()
            } else {
                expandableSchedule.visibility = View.VISIBLE
                ivScheduleChevron.animate().rotation(90f).setDuration(200).start()
            }
        }

        layoutStartTime.setOnClickListener { showTimePicker("start") }
        layoutLunchBreak.setOnClickListener {
            showLunchTimePicker()
        }
        layoutEndTime.setOnClickListener { showTimePicker("end") }

        // Debug tools
        val sectionDebug = findViewById<LinearLayout>(R.id.sectionDebug)
        findViewById<TextView>(R.id.tvFooterNotice).setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            sectionDebug.visibility = if (sectionDebug.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            Toast.makeText(this, "Debug tools toggled", Toast.LENGTH_SHORT).show()
            true
        }

        findViewById<View>(R.id.layoutTestSilent).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (hasDNDPermission()) {
                soundManager.setSilentMode()
                Toast.makeText(this, "Force Silent Triggered", Toast.LENGTH_SHORT).show()
            } else requestDNDPermission()
        }
        
        findViewById<View>(R.id.layoutTestNormal).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            soundManager.setNormalMode()
            Toast.makeText(this, "Normal Mode Restored", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.layoutTestMorningCheck).setOnClickListener {
            val intent = Intent(this, AlarmReceiver::class.java).apply {
                putExtra("MODE", "MORNING_CHECK")
                putExtra("IS_MANUAL_TEST", true)
            }
            sendBroadcast(intent)
        }

        findViewById<View>(R.id.layoutTestFollowUp).setOnClickListener {
            val intent = Intent(this, AlarmReceiver::class.java).apply {
                putExtra("MODE", "MISSED_CHECK")
                putExtra("IS_MANUAL_TEST", true)
            }
            sendBroadcast(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction("com.madhan.clgautomation.UPDATE_STATUS")
            addAction(Intent.ACTION_TIME_TICK)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statusUpdateReceiver, filter)
        }
        refreshUIState()
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(statusUpdateReceiver) } catch (e: Exception) {}
    }

    private fun refreshUIState() {
        val focusActive = automationPrefs.getBoolean("manual_focus_active", false)
        if (focusActive) {
            val focusEndTime = automationPrefs.getLong("focus_end_time", 0L)
            if (focusEndTime > 0 && System.currentTimeMillis() >= focusEndTime) {
                automationPrefs.edit().putBoolean("manual_focus_active", false).apply()
            }
        }

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
        
        val focusActive = automationPrefs.getBoolean("manual_focus_active", false)
        if (focusActive) {
            tvFocusMode.text = "End Focus"
            tvFocusMode.setTextColor(ContextCompat.getColor(this, R.color.ios_red))
            ivFocusIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ios_red))
            ivFocusChevron.visibility = View.GONE
        } else {
            tvFocusMode.text = "Start Focus Mode"
            tvFocusMode.setTextColor(ContextCompat.getColor(this, R.color.ios_blue))
            ivFocusIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ios_blue))
            ivFocusChevron.visibility = View.VISIBLE
        }
        
        val isSkipped = automationPrefs.getBoolean("skipped_today", false)
        if (isSkipped) {
            tvSkipToday.text = "Restore Today"
            tvSkipToday.setTextColor(ContextCompat.getColor(this, R.color.ios_blue))
        } else {
            tvSkipToday.text = "Skip Today"
            tvSkipToday.setTextColor(ContextCompat.getColor(this, R.color.ios_red))
        }
        
        updateStatusDisplay()
    }

    private fun showFocusDurationPicker() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_focus_duration, null)

        view.findViewById<TextView>(R.id.btn30m).setOnClickListener { startFocusMode(30); dialog.dismiss() }
        view.findViewById<TextView>(R.id.btn1h).setOnClickListener { startFocusMode(60); dialog.dismiss() }
        view.findViewById<TextView>(R.id.btn2h).setOnClickListener { startFocusMode(120); dialog.dismiss() }
        view.findViewById<TextView>(R.id.btnCustom).setOnClickListener { showFocusTimePicker(); dialog.dismiss() }
        view.findViewById<TextView>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }

        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun startFocusMode(minutes: Int) {
        val now = Calendar.getInstance()
        now.add(Calendar.MINUTE, minutes)
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)

        soundManager.setSilentMode()
        automationPrefs.edit()
            .putBoolean("manual_focus_active", true)
            .putInt("focus_end_hour", hour)
            .putInt("focus_end_minute", minute)
            .putLong("focus_end_time", now.timeInMillis)
            .apply()
        
        scheduler.scheduleAlarm(hour, minute, "FOCUS_END", 108)
        
        Toast.makeText(this, "Focus Mode Started until ${formatTime(hour, minute)}", Toast.LENGTH_SHORT).show()
        loadAndDisplaySettings()
    }

    private fun showFocusTimePicker() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_time_picker, null)
        
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val timePicker = view.findViewById<android.widget.TimePicker>(R.id.timePicker)
        
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)
        val btnSave = view.findViewById<TextView>(R.id.btnSave)

        tvTitle.text = "Focus Until..."
        val now = Calendar.getInstance()
        timePicker.hour = now.get(Calendar.HOUR_OF_DAY)
        timePicker.minute = now.get(Calendar.MINUTE)

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val hour = timePicker.hour
            val minute = timePicker.minute
            
            val endCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                }
            }
            
            soundManager.setSilentMode()
            automationPrefs.edit()
                .putBoolean("manual_focus_active", true)
                .putInt("focus_end_hour", hour)
                .putInt("focus_end_minute", minute)
                .putLong("focus_end_time", endCalendar.timeInMillis)
                .apply()
            
            scheduler.scheduleAlarm(hour, minute, "FOCUS_END", 108)
            
            Toast.makeText(this, "Focus Mode Started until ${formatTime(hour, minute)}", Toast.LENGTH_SHORT).show()
            loadAndDisplaySettings()
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showTimePicker(type: String) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_time_picker, null)
        
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val timePicker = view.findViewById<android.widget.TimePicker>(R.id.timePicker)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)
        val btnSave = view.findViewById<TextView>(R.id.btnSave)

        tvTitle.text = when(type) {
            "start" -> "Morning Start"
            "end" -> "College End"
            else -> "Set Time"
        }

        val currentHour = appSettings.getInt("${type}_hour", getDefaultHour(type))
        val currentMinute = appSettings.getInt("${type}_minute", getDefaultMinute(type))
        
        timePicker.hour = currentHour
        timePicker.minute = currentMinute

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val hour = timePicker.hour
            val minute = timePicker.minute
            appSettings.edit().putInt("${type}_hour", hour).putInt("${type}_minute", minute).apply()
            updateUITime(hour, minute, type)
            if (automationSwitch.isChecked) scheduler.scheduleAutomationAlarms()
            updateStatusDisplay()
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showLunchTimePicker() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_lunch_picker, null)
        
        val timePickerStart = view.findViewById<android.widget.TimePicker>(R.id.timePickerStart)
        val timePickerEnd = view.findViewById<android.widget.TimePicker>(R.id.timePickerEnd)
        
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)
        val btnSave = view.findViewById<TextView>(R.id.btnSave)

        timePickerStart.hour = appSettings.getInt("lunch_start_hour", 12)
        timePickerStart.minute = appSettings.getInt("lunch_start_minute", 15)
        timePickerEnd.hour = appSettings.getInt("lunch_end_hour", 13)
        timePickerEnd.minute = appSettings.getInt("lunch_end_minute", 0)

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            appSettings.edit()
                .putInt("lunch_start_hour", timePickerStart.hour)
                .putInt("lunch_start_minute", timePickerStart.minute)
                .putInt("lunch_end_hour", timePickerEnd.hour)
                .putInt("lunch_end_minute", timePickerEnd.minute)
                .apply()
            
            updateUITime(timePickerStart.hour, timePickerStart.minute, "lunch_start")
            updateUITime(timePickerEnd.hour, timePickerEnd.minute, "lunch_end")
            
            if (automationSwitch.isChecked) scheduler.scheduleAutomationAlarms()
            updateStatusDisplay()
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun getDefaultHour(type: String): Int {
        return when (type) {
            "start" -> 9
            "lunch_start" -> 12
            "lunch_end" -> 13
            "end" -> 16
            else -> 9
        }
    }

    private fun getDefaultMinute(type: String): Int {
        return when (type) {
            "lunch_start" -> 15
            else -> 0
        }
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
            Toast.makeText(this, "Automation Disabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatusDisplay() {
        val isEnabled = appSettings.getBoolean("automation_enabled", false)
        val isSkipped = automationPrefs.getBoolean("skipped_today", false)
        val isFocusActive = automationPrefs.getBoolean("manual_focus_active", false)

        val currentTime = Calendar.getInstance()
        val dayOfWeek = currentTime.get(Calendar.DAY_OF_WEEK)
        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)
        val now = currentHour * 60 + currentMinute

        // Fetch settings for end time comparison
        val startHour = appSettings.getInt("start_hour", 9)
        val startMinute = appSettings.getInt("start_minute", 0)
        val endHour = appSettings.getInt("end_hour", 16)
        val endMinute = appSettings.getInt("end_minute", 0)
        val end = endHour * 60 + endMinute

        var targetColor = Color.parseColor("#34C759") // Default Green
        var statusLabel = ""
        var actionLabel = ""

        when {
            isFocusActive -> {
                val fEndH = automationPrefs.getInt("focus_end_hour", 0)
                val fEndM = automationPrefs.getInt("focus_end_minute", 0)
                statusLabel = "FOCUS MODE ACTIVE"
                actionLabel = "Ending at ${formatTime(fEndH, fEndM)}"
                targetColor = Color.parseColor("#5856D6")
            }
            isWeekend -> {
                statusLabel = "SYSTEM IDLE"
                actionLabel = "Enjoy your weekend! 🌴"
                targetColor = Color.parseColor("#34C759")
            }
            !isEnabled -> {
                statusLabel = "AUTOMATION DISABLED"
                actionLabel = "Turn on to start scheduling"
                targetColor = Color.GRAY
            }
            now >= end -> {
                statusLabel = "SYSTEM ACTIVE"
                actionLabel = "Schedule ended for today"
                targetColor = Color.parseColor("#34C759")
            }
            isSkipped -> {
                statusLabel = "SKIPPED FOR TODAY"
                actionLabel = "Automation will resume tomorrow"
                targetColor = Color.parseColor("#FF9500")
            }
            else -> {
                val start = startHour * 60 + startMinute
                val lunchStartHour = appSettings.getInt("lunch_start_hour", 12)
                val lunchStartMinute = appSettings.getInt("lunch_start_minute", 15)
                val lunchEndHour = appSettings.getInt("lunch_end_hour", 13)
                val lunchEndMinute = appSettings.getInt("lunch_end_minute", 0)
                
                val lunchStart = lunchStartHour * 60 + lunchStartMinute
                val lunchEnd = lunchEndHour * 60 + lunchEndMinute

                when {
                    now < start -> {
                        statusLabel = "SYSTEM ACTIVE"
                        actionLabel = "Next: Silent at ${formatTime(startHour, startMinute)}"
                        targetColor = Color.parseColor("#34C759")
                    }
                    now >= start && now < lunchStart -> {
                        statusLabel = "SILENT MODE ACTIVE"
                        actionLabel = "Next: Normal at ${formatTime(lunchStartHour, lunchStartMinute)}"
                        targetColor = Color.parseColor("#FF3B30")
                    }
                    now >= lunchStart && now < lunchEnd -> {
                        statusLabel = "NORMAL MODE ACTIVE"
                        actionLabel = "Next: Silent at ${formatTime(lunchEndHour, lunchEndMinute)}"
                        targetColor = Color.parseColor("#34C759")
                    }
                    now >= lunchEnd && now < end -> {
                        statusLabel = "SILENT MODE ACTIVE"
                        actionLabel = "Next: Normal at ${formatTime(endHour, endMinute)}"
                        targetColor = Color.parseColor("#FF3B30")
                    }
                }
            }
        }

        tsCurrentStatus.setText(statusLabel)
        tsNextAction.setText(actionLabel)
        animateStatusUI(targetColor)
    }

    private fun animateStatusUI(targetColor: Int) {
        if (lastStatusColor == targetColor) return
        colorAnimator?.cancel()
        
        val colorAnim = ValueAnimator.ofObject(ArgbEvaluator(), lastStatusColor, targetColor)
        colorAnim.duration = 500
        colorAnim.addUpdateListener { animator ->
            val color = animator.animatedValue as Int
            statusDot.backgroundTintList = ColorStateList.valueOf(color)
            (statusCard as? com.google.android.material.card.MaterialCardView)?.let {
                it.strokeColor = color
            }
        }
        colorAnim.start()
        colorAnimator = colorAnim
        lastStatusColor = targetColor
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return SimpleDateFormat("hh:mm a", Locale.US).format(calendar.time)
    }

    private fun hasDNDPermission(): Boolean {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    private fun requestDNDPermission() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        Toast.makeText(this, "Please grant Do Not Disturb access", Toast.LENGTH_LONG).show()
    }

    private fun checkFirstLaunchPermissions() {
        val isFirstLaunch = appSettings.getBoolean("is_first_launch", true)
        if (isFirstLaunch) {
            showPermissionDialog()
            appSettings.edit().putBoolean("is_first_launch", false).apply()
        }
    }

    private fun showPermissionDialog() {
        val dialog = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.dialog_ios_alert, null)
        
        view.findViewById<TextView>(R.id.tvAlertTitle).text = "Permissions Required"
        view.findViewById<TextView>(R.id.tvAlertMessage).text = "To silence your phone automatically, Smart Silence needs 'Do Not Disturb' access and 'Notification' permissions."
        
        view.findViewById<TextView>(R.id.btnAlertPositive).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
            if (!hasDNDPermission()) {
                requestDNDPermission()
            }
            dialog.dismiss()
        }
        
        dialog.setView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun checkBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                showBatteryOptimizationDialog()
            }
        }
    }

    private fun showBatteryOptimizationDialog() {
        val dialog = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.dialog_ios_alert, null)
        
        view.findViewById<TextView>(R.id.tvAlertTitle).text = "Battery Optimization"
        view.findViewById<TextView>(R.id.tvAlertMessage).text = "To ensure alarms trigger exactly on time, please disable battery optimization for Smart Silence."
        
        view.findViewById<TextView>(R.id.btnAlertPositive).setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            dialog.dismiss()
        }
        
        dialog.setView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}