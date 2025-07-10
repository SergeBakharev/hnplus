package com.manuelmaly.hn.server

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

object ConnectivityUtils {
    /**
     * Returns the online status of the device. Note that a request to a server
     * can still fail or time-out due to network or server problems!
     *
     * @param context
     * of application
     * @return boolean true if online
     */
    fun isDeviceOnline(context: Context): Boolean {
        val cMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cMgr.activeNetworkInfo
        if (netInfo == null || netInfo.state == null) return false
        return netInfo.state == NetworkInfo.State.CONNECTED
    }
}
