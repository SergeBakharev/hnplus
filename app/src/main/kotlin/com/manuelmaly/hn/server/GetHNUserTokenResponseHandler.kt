package com.manuelmaly.hn.server

import cz.msebera.android.httpclient.HttpResponse
import cz.msebera.android.httpclient.client.ClientProtocolException
import cz.msebera.android.httpclient.client.HttpClient
import cz.msebera.android.httpclient.client.ResponseHandler
import java.io.IOException

/**
 * Handles HTML response of a request to login to HN and retrieving its user token.
 * @author manuelmaly
 */
class GetHNUserTokenResponseHandler(
    private val mCommand: IAPICommand<String>,
    client: HttpClient?
) :
    ResponseHandler<String> {
    @Throws(ClientProtocolException::class, IOException::class)
    override fun handleResponse(response: HttpResponse): String {
        var responseString: String? = null
        var redirectToLocation: String? = null

        val headers = response.getHeaders("Location")
        if (headers.size > 0) {
            val headerElements = headers[0].elements
            if (headerElements.size > 0) {
                redirectToLocation = headerElements[0].name
            }
        }

        if (redirectToLocation != null && redirectToLocation == "news") {
            responseString = getUserID(response)
        }

        val result = responseString ?: ""
        mCommand.responseHandlingFinished(result, response.statusLine.statusCode)
        return result
    }

    private fun getUserID(response: HttpResponse): String? {
        for (header in response.getHeaders("Set-Cookie")) {
            val elements = header.elements
            if (elements.size > 0 && elements[0].name == "user") {
                return elements[0].value
            }
        }
        return null
    }
}
