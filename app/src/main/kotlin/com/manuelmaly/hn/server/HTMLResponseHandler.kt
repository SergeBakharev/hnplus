package com.manuelmaly.hn.server

import cz.msebera.android.httpclient.HttpResponse
import cz.msebera.android.httpclient.client.ClientProtocolException
import cz.msebera.android.httpclient.client.HttpClient
import cz.msebera.android.httpclient.client.ResponseHandler
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Handles HTML response of a [HttpClient].
 * @author manuelmaly
 */
class HTMLResponseHandler(private val mCommand: IAPICommand<String>, client: HttpClient?) :
    ResponseHandler<String> {
    @Throws(ClientProtocolException::class, IOException::class)
    override fun handleResponse(response: HttpResponse): String {
        val out = ByteArrayOutputStream()
        response.entity.writeTo(out)
        val statusLine = response.statusLine
        val responseString = out.toString()
        out.close()
        val statusCode = statusLine.statusCode

        mCommand.responseHandlingFinished(responseString, statusCode)
        return responseString
    }
}
