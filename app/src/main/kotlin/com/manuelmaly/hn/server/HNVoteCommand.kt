package com.manuelmaly.hn.server

import android.content.Context
import cz.msebera.android.httpclient.client.CookieStore

class HNVoteCommand(
    url: String,
    queryParams: HashMap<String?, String?>?,
    type: IAPICommand.RequestType,
    notifyFinishedBroadcast: Boolean,
    notificationBroadcastIntentID: String?,
    applicationContext: Context?,
    cookieStore: CookieStore?
) :
    NoResponseCommand(
        url,
        queryParams,
        type,
        notifyFinishedBroadcast,
        notificationBroadcastIntentID,
        applicationContext,
        cookieStore
    ) {
    override fun validateResponseContent(content: String): Boolean {
        if (!content.contains("You have to be logged in to vote.")) return true
        return false
    }
}
