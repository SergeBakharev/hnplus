package com.manuelmaly.hn.util

import android.app.Activity
import android.os.Handler
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object Run {
    private val backgroundExecutor: Executor = Executors.newCachedThreadPool()

    fun onUiThread(r: Runnable?, a: Activity) {
        a.runOnUiThread(r)
    }

    fun inBackground(r: Runnable?) {
        backgroundExecutor.execute(r)
    }

    fun delayed(r: Runnable, delayMillis: Long) {
        val h = Handler()
        h.postDelayed(r, delayMillis)
    }
}
