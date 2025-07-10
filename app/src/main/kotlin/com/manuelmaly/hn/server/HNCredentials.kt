package com.manuelmaly.hn.server

import android.content.Context
import com.manuelmaly.hn.Settings
import cz.msebera.android.httpclient.client.CookieStore
import cz.msebera.android.httpclient.impl.client.BasicCookieStore
import cz.msebera.android.httpclient.impl.cookie.BasicClientCookie

object HNCredentials {
    private var cookieStore: CookieStore? = null
    var isInvalidated: Boolean = false
        private set

    private const val COOKIE_USER = "user"

    @JvmStatic
    @Synchronized
    fun getCookieStore(c: Context?): CookieStore? {
        if (cookieStore != null && !isInvalidated) return cookieStore

        cookieStore = BasicCookieStore()
        val userToken = Settings.getUserToken(c!!)

        if (userToken != null) {
            val cookie = BasicClientCookie(COOKIE_USER, userToken)
            cookie.domain = "news.ycombinator.com"
            cookie.path = "/"
            cookieStore?.addCookie(cookie)
        }

        isInvalidated = false

        return cookieStore
    }

    @JvmStatic
    fun invalidate() {
        cookieStore = null
        isInvalidated = true
    }
}
