package com.madhan.smartsilence

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

@SuppressLint("ClickableViewAccessibility")
fun View.addTouchFeedback() {
    val pressedAlpha = 0.94f
    val normalAlpha = 1.0f
    val pressedColor = Color.argb(10, 0, 0, 0) // 4% black tint
    val normalColor = Color.TRANSPARENT
    
    this.setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.animate()
                    .alpha(pressedAlpha)
                    .setDuration(120)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
                
                animateBackgroundColor(v, normalColor, pressedColor)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.animate()
                    .alpha(normalAlpha)
                    .setDuration(120)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
                
                animateBackgroundColor(v, pressedColor, normalColor)
            }
        }
        false
    }
}

private fun animateBackgroundColor(view: View, fromColor: Int, toColor: Int) {
    ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor).apply {
        duration = 120
        setInterpolator(AccelerateDecelerateInterpolator())
        addUpdateListener { animator ->
            view.setBackgroundColor(animator.animatedValue as Int)
        }
        start()
    }
}
