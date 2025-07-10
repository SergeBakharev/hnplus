package com.manuelmaly.hn

import android.content.Context
import android.content.SharedPreferences

object Settings {
    const val PREF_FONTSIZE = "pref_fontsize"
    const val PREF_HTMLPROVIDER = "pref_htmlprovider"
    const val PREF_HTMLVIEWER = "pref_htmlviewer"
    const val PREF_USER = "pref_user"
    const val PREF_PULLDOWNREFRESH = "pref_pulldownrefresh"

    // Make this public for use in other files
    const val USER_DATA_SEPARATOR = ":"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("com.manuelmaly.hn_preferences", Context.MODE_PRIVATE)
    }

    fun getFontSize(c: Context): String {
        val sharedPref = getSharedPreferences(c)
        return sharedPref.getString(PREF_FONTSIZE, c.getString(R.string.pref_default_fontsize)) ?: c.getString(R.string.pref_default_fontsize)
    }

    fun getHtmlProvider(c: Context): String {
        val sharedPref = getSharedPreferences(c)
        return sharedPref.getString(PREF_HTMLPROVIDER, c.getString(R.string.pref_default_htmlprovider)) ?: c.getString(R.string.pref_default_htmlprovider)
    }

    fun getHtmlViewer(c: Context): String {
        val sharedPref = getSharedPreferences(c)
        return sharedPref.getString(PREF_HTMLVIEWER, c.getString(R.string.pref_default_htmlviewer)) ?: c.getString(R.string.pref_default_htmlviewer)
    }

    fun isPullDownRefresh(c: Context): Boolean {
        val sharedPref = getSharedPreferences(c)
        return sharedPref.getBoolean(PREF_PULLDOWNREFRESH, false)
    }

    fun isUserLoggedIn(c: Context): Boolean {
        return getUserName(c) != ""
    }

    fun getUserName(c: Context): String {
        val sharedPref = getSharedPreferences(c)
        val userData = sharedPref.getString(PREF_USER, "")?.split(USER_DATA_SEPARATOR) ?: listOf("")
        return if (userData.isNotEmpty()) userData[0] else ""
    }

    fun getUserToken(c: Context): String? {
        val sharedPref = getSharedPreferences(c)
        val userData = sharedPref.getString(PREF_USER, "")?.split(USER_DATA_SEPARATOR) ?: listOf("")
        return if (userData.size > 1) userData[1] else null
    }

    fun setUserData(userName: String, userToken: String, c: Context) {
        val sharedPref = getSharedPreferences(c)
        sharedPref.edit()
            .putString(PREF_USER, "$userName$USER_DATA_SEPARATOR$userToken")
            .commit()
    }

    fun clearUserData(c: Context) {
        val sharedPref = getSharedPreferences(c)
        sharedPref.edit()
            .remove(PREF_USER)
            .commit()
    }
} 