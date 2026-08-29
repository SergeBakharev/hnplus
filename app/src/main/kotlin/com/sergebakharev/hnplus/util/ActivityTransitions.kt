package com.sergebakharev.hnplus.util

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

/**
 * Routes the system back swipe through the same [AppCompatActivity.finish] path as
 * the action bar Up button, so both use the activity-close animation instead of
 * the predictive back "card" animation.
 */
fun AppCompatActivity.installActionBarBackOnSwipe(onBack: () -> Unit = { finish() }) {
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBack()
        }
    })
}
