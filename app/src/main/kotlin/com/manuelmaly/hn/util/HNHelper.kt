package com.manuelmaly.hn.util

object HNHelper {
    fun resolveRelativeHNURL(url: String?): String? {
        if (url == null) return null

        val hnurl = "https://news.ycombinator.com/"

        return if (url.startsWith("http") || url.startsWith("ftp")) {
            url
        } else if (url.startsWith("/")) hnurl + url.substring(1)
        else hnurl + url
    }
}
