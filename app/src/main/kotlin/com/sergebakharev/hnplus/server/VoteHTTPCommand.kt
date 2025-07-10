package com.sergebakharev.hnplus.server

import android.content.Context
import cz.msebera.android.httpclient.client.HttpClient
import cz.msebera.android.httpclient.client.ResponseHandler
import cz.msebera.android.httpclient.client.methods.HttpUriRequest
import cz.msebera.android.httpclient.client.params.HttpClientParams
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient

class VoteHTTPCommand(
    url: String,
    queryParams: HashMap<String?, String?>?,
    type: IAPICommand.RequestType,
    notifyFinishedBroadcast: Boolean,
    notificationBroadcastIntentID: String?,
    applicationContext: Context?
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
    override fun modifyHttpClient(client: DefaultHttpClient) {
        super.modifyHttpClient(client)
        HttpClientParams.setRedirecting(client.params, false)
    }

    override fun setRequestData(request: HttpUriRequest): HttpUriRequest {
        return request
    }

    override fun getResponseHandler(client: HttpClient?): ResponseHandler<String> {
        return GetHNUserTokenResponseHandler(this as IAPICommand<String>, client)
    }
}
