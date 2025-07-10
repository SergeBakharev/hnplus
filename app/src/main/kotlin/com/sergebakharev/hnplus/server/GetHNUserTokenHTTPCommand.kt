package com.sergebakharev.hnplus.server

import android.content.Context
import cz.msebera.android.httpclient.NameValuePair
import cz.msebera.android.httpclient.client.HttpClient
import cz.msebera.android.httpclient.client.ResponseHandler
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity
import cz.msebera.android.httpclient.client.methods.HttpPost
import cz.msebera.android.httpclient.client.methods.HttpUriRequest
import cz.msebera.android.httpclient.client.params.HttpClientParams
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient
import cz.msebera.android.httpclient.message.BasicNameValuePair
import java.io.UnsupportedEncodingException

class GetHNUserTokenHTTPCommand(
    url: String,
    queryParams: HashMap<String?, String?>?,
    type: IAPICommand.RequestType,
    notifyFinishedBroadcast: Boolean,
    notificationBroadcastIntentID: String?,
    applicationContext: Context?,
    body: Map<String, String>?
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
        body
    ) {
    override fun modifyHttpClient(client: DefaultHttpClient) {
        super.modifyHttpClient(client)
        HttpClientParams.setRedirecting(client.params, false)
    }

    override fun setRequestData(request: HttpUriRequest): HttpUriRequest {
        val params: MutableList<NameValuePair> = ArrayList(2)
        val body = body
        if (body != null) {
            for (key in body.keys) {
                params.add(BasicNameValuePair(key, body[key]))
            }
        }

        try {
            (request as HttpPost).entity = (UrlEncodedFormEntity(params, "UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        return request
    }

    override fun getResponseHandler(client: HttpClient?): ResponseHandler<String> {
        return GetHNUserTokenResponseHandler(this as IAPICommand<String>, client)
    }
}
