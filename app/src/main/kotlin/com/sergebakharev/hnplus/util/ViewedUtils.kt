package com.sergebakharev.hnplus.util

import android.app.Activity
import android.content.Context

object ViewedUtils {
    private const val PREFS_VIEWED_ACTIVITES = "viewed_activites"

    fun setActivityViewed(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS_VIEWED_ACTIVITES, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(activity.javaClass.canonicalName, true)
        editor.commit()
    }

    fun getActivityViewed(activity: Activity): Boolean {
        val prefs = activity.getSharedPreferences(PREFS_VIEWED_ACTIVITES, Context.MODE_PRIVATE)
        return prefs.getBoolean(activity.javaClass.canonicalName, false)
    }
}
