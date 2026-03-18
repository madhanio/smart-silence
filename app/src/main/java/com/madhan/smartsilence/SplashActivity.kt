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

        // iOS-style entrance: subtle scale up and fade in
        logo.alpha = 0f
        logo.scaleX = 0.85f
        logo.scaleY = 0.85f

        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                // Short hold before transitioning
                logo.postDelayed({
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    // Smooth iOS-style fade transition between activities
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }, 300)
            }
            .start()
    }
}