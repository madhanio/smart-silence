package com.madhan.smartsilence

import android.content.Context
import android.media.AudioManager

class SoundManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val soundPrefs = context.getSharedPreferences("SoundSettings", Context.MODE_PRIVATE)

    private val affectedStreams = intArrayOf(
        AudioManager.STREAM_RING,
        AudioManager.STREAM_NOTIFICATION
    )

    fun setSilentMode() {
        val isAlreadySilent = soundPrefs.getBoolean("is_silent_active", false)
        val editor = soundPrefs.edit()

        // Only save volumes if we are not already in silent mode managed by the app
        // This prevents overwriting saved volumes with 0 if called twice
        if (!isAlreadySilent) {
            for (stream in affectedStreams) {
                val currentVol = audioManager.getStreamVolume(stream)
                // If current volume is 0 (manually silenced), we might want to save a default or just 0
                editor.putInt("vol_$stream", currentVol)
            }
        }

        editor.putBoolean("is_silent_active", true)
        editor.apply()

        // mute RING + NOTIFICATION
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
                val targetVol = if (savedVol > maxVol) maxVol else savedVol
                
                // If the saved volume was 0, maybe restore to a reasonable default like 50%?
                // But user asked to restore "exactly", so if it was 0, it stays 0.
                // However, usually "restore" implies going back to audible state.
                // Let's stick to exactly what was saved.
                audioManager.setStreamVolume(stream, targetVol, 0)
            }
        }

        soundPrefs.edit().putBoolean("is_silent_active", false).apply()
    }
}
