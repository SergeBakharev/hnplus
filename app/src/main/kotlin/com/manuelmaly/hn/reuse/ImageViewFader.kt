package com.manuelmaly.hn.reuse

import android.R
import android.app.Activity
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.ImageView

object ImageViewFader {
    fun startFadeOverToImage(
        view: ImageView, toImageRes: Int, durationMillis: Long,
        activity: Activity?
    ) {
        val fadeOut = AnimationUtils.loadAnimation(activity, R.anim.fade_out)
        fadeOut.duration = durationMillis
        fadeOut.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {
            }

            override fun onAnimationRepeat(animation: Animation) {
            }

            override fun onAnimationEnd(animation: Animation) {
                view.setImageResource(toImageRes)
                val fadeIn = AnimationUtils.loadAnimation(activity, R.anim.fade_in)
                fadeIn.duration = durationMillis
                view.startAnimation(fadeIn)
            }
        })
        view.startAnimation(fadeOut)
    }
}
