package com.madhan.smartsilence

import android.content.Context
import android.media.AudioManager

class SoundManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val soundPrefs = context.getSharedPreferences("SoundSettings", Context.MODE_PRIVATE)
    private val appSettings = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

    private val affectedStreams = intArrayOf(
        AudioManager.STREAM_RING,
        AudioManager.STREAM_NOTIFICATION
    )

    private val allStreams = intArrayOf(
        AudioManager.STREAM_RING,
        AudioManager.STREAM_NOTIFICATION,
        AudioManager.STREAM_ALARM
    )

    fun setSilentMode() {

        val editor = soundPrefs.edit()

        // save ONLY ringer + notification volume
        for (stream in affectedStreams) {
            editor.putInt("vol_$stream", audioManager.getStreamVolume(stream))
        }

        editor.putBoolean("is_silent_active", true)
        editor.apply()

        // mute ONLY ringer + notification
        for (stream in affectedStreams) {
            audioManager.setStreamVolume(stream, 0, 0)
        }

        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
    }

    fun setNormalMode() {

        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL

        for (stream in affectedStreams) {

            val savedVol = soundPrefs.getInt("vol_$stream", -1)

            if (savedVol >= 0) {

                val maxVol = audioManager.getStreamMaxVolume(stream)

                val targetVol =
                    if (savedVol > maxVol) maxVol else savedVol

                audioManager.setStreamVolume(stream, targetVol, 0)
            }
        }

        soundPrefs.edit().putBoolean("is_silent_active", false).apply()
    }
}
