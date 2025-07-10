package com.sergebakharev.hnplus.server

import android.content.Context
import cz.msebera.android.httpclient.client.CookieStore
import cz.msebera.android.httpclient.client.HttpClient
import cz.msebera.android.httpclient.client.ResponseHandler
import cz.msebera.android.httpclient.client.methods.HttpUriRequest

class StringDownloadCommand(
    url: String,
    queryParams: HashMap<String?, String?>?,
    type: IAPICommand.RequestType,
    notifyFinishedBroadcast: Boolean,
    notificationBroadcastIntentID: String?,
    applicationContext: Context?,
    cookieStore: CookieStore?
) :
    BaseHTTPCommand<String>(
        url,
        queryParams,
        type,
        notifyFinishedBroadcast,
        notificationBroadcastIntentID,
        applicationContext,
        60000,
        60000,
        null
    ) {
    init {
        setCookieStore(cookieStore)
    }

    override fun setRequestData(request: HttpUriRequest): HttpUriRequest {
        request.setHeader(IAPICommand.Companion.ACCEPT_HEADER, IAPICommand.Companion.HTML_MIME)
        return request
    }

    override fun getResponseHandler(client: HttpClient?): ResponseHandler<String> {
        return HTMLResponseHandler(this as IAPICommand<String>, client)
    }
}
