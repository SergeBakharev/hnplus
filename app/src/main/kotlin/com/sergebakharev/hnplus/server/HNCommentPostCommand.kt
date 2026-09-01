package com.sergebakharev.hnplus.server

import android.content.Context
import cz.msebera.android.httpclient.NameValuePair
import cz.msebera.android.httpclient.client.CookieStore
import cz.msebera.android.httpclient.client.HttpClient
import cz.msebera.android.httpclient.client.ResponseHandler
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity
import cz.msebera.android.httpclient.client.methods.HttpPost
import cz.msebera.android.httpclient.client.methods.HttpUriRequest
import cz.msebera.android.httpclient.message.BasicNameValuePair
import java.io.ByteArrayOutputStream
import java.io.UnsupportedEncodingException

class HNCommentPostCommand(
    url: String,
    type: IAPICommand.RequestType,
    applicationContext: Context?,
    cookieStore: CookieStore?,
    body: Map<String, String>?
) : BaseHTTPCommand<Boolean>(
    url,
    null,
    type,
    false,
    null,
    applicationContext,
    60000,
    60000,
    body
) {
    var responseHtml: String? = null
        private set

    init {
        setCookieStore(cookieStore)
    }

    override fun setRequestData(request: HttpUriRequest): HttpUriRequest {
        request.setHeader(IAPICommand.ACCEPT_HEADER, IAPICommand.HTML_MIME)
        val params: MutableList<NameValuePair> = ArrayList()
        val body = body
        if (body != null) {
            for (key in body.keys) {
                params.add(BasicNameValuePair(key, body[key]))
            }
        }
        try {
            (request as HttpPost).entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return request
    }

    override fun getResponseHandler(client: HttpClient?): ResponseHandler<Boolean> {
        return ResponseHandler { response ->
            val statusCode = response.statusLine.statusCode
            var result = statusCode in 200..399

            val out = ByteArrayOutputStream()
            response.entity.writeTo(out)
            val content = out.toString()
            responseHtml = content
            result = result && validateResponseContent(content)

            this@HNCommentPostCommand.responseHandlingFinished(result, statusCode)
            null
        }
    }

    private fun validateResponseContent(content: String): Boolean {
        return !content.contains("You have to be logged in") &&
            !content.contains("Unknown or expired link") &&
            !content.contains("Can't comment here") &&
            !content.contains("You're posting too fast") &&
            !content.contains("You can't delete that")
    }
}
