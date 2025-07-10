package com.manuelmaly.hn.reuse

import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation

object ViewRotator {
    fun startRotating(view: View) {
        // Why this layout listener craziness? Because the view's dimensions are only known
        // after layout is finished. requestLayout() makes sure that the listener is called.
        view.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val anim = RotateAnimation(0f, 350f, view.width / 2.0f, view.height / 2.0f)
                anim.interpolator = LinearInterpolator()
                anim.repeatCount = Animation.INFINITE
                anim.duration = 1000
                view.startAnimation(anim)
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        view.requestLayout()
    }

    fun stopRotating(view: View) {
        view.clearAnimation()
    }
}
