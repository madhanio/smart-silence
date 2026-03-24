package com.madhan.smartsilence

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.appLogo)
        val tvAppName = findViewById<android.widget.TextView>(R.id.tvAppName)

        // Set initial states
        logo.alpha = 0f
        logo.scaleX = 0.6f
        logo.scaleY = 0.6f
        
        tvAppName.alpha = 0f
        tvAppName.translationY = 40f

        // 1. Animate Logo with Overshoot (Spring effect)
        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1000)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            .start()

        // 2. Animate Text with slight delay
        tvAppName.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(500)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                tvAppName.postDelayed({
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }, 800)
            }
            .start()
    }
}