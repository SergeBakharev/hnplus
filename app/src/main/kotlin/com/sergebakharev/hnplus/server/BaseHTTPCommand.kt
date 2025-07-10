package com.sergebakharev.hnplus.server

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cz.msebera.android.httpclient.client.CookieStore
import cz.msebera.android.httpclient.client.HttpClient
import cz.msebera.android.httpclient.client.ResponseHandler
import cz.msebera.android.httpclient.client.methods.HttpDelete
import cz.msebera.android.httpclient.client.methods.HttpGet
import cz.msebera.android.httpclient.client.methods.HttpPost
import cz.msebera.android.httpclient.client.methods.HttpPut
import cz.msebera.android.httpclient.client.methods.HttpRequestBase
import cz.msebera.android.httpclient.client.methods.HttpUriRequest
import cz.msebera.android.httpclient.impl.client.BasicCookieStore
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient
import cz.msebera.android.httpclient.params.BasicHttpParams
import cz.msebera.android.httpclient.params.HttpConnectionParams
import cz.msebera.android.httpclient.params.HttpParams
import java.io.Serializable

/**
 * Generic base for HTTP calls via [HttpClient], ideally to be started in
 * a background thread. When the call has finished, listeners are notified via
 * an intent sent to [LocalBroadcastManager], i.e. they must first
 * register (intent name is configurable via notificationBroadcastIntentID).
 * Response and errors are also sent via the intent.
 *
 * @author manuelmaly
 *
 * @param <T>
 * class of response
</T> */
abstract class BaseHTTPCommand<T : Serializable?>(
    private val mUrl: String,
    params: HashMap<String?, String?>?,
    type: IAPICommand.RequestType,
    notifyFinishedBroadcast: Boolean,
    notificationBroadcastIntentID: String?,
    applicationContext: Context?,
    socketTimeoutMS: Int,
    httpTimeoutMS: Int,
    val body: Map<String, String>?
) :
    IAPICommand<T?> {
    private val mNotificationBroadcastIntentID: String
    private var mURLQueryParams: String? = null
    private val mType: IAPICommand.RequestType
    override var actualStatusCode: Int = 0
        protected set
    private val mApplicationContext: Context?
    override var errorCode: Int = 0
        protected set
    override var responseContent: T? = null
        protected set
    var tag: Any? = null
    private val mSocketTimeoutMS: Int
    private val mHttpTimeoutMS: Int
    private val mNotifyFinishedBroadcast: Boolean
    var mRequest: HttpRequestBase? = null
    private var mCookieStore: CookieStore? = null

    init {
        if (params != null) {
            val sb = StringBuilder()
            for (param in params.keys) {
                if (sb.length > 0) sb.append("&")
                sb.append(Uri.encode(param)).append("=").append(
                    Uri.encode(
                        params[param]
                    )
                )
            }
            mURLQueryParams = sb.toString()
        }

        mType = type
        mNotificationBroadcastIntentID =
            notificationBroadcastIntentID ?: IAPICommand.Companion.DEFAULT_BROADCAST_INTENT_ID
        mApplicationContext = applicationContext
        mSocketTimeoutMS = socketTimeoutMS
        mHttpTimeoutMS = httpTimeoutMS
        mNotifyFinishedBroadcast = notifyFinishedBroadcast
    }

    override fun run() {
        try {
            errorCode = IAPICommand.Companion.ERROR_UNKNOWN
            // Check if Device is currently offline:
            if (cancelBecauseDeviceOffline()) {
                onFinished()
                return
            }

            // Start request, handle response in separate handler:
            val httpclient = DefaultHttpClient(httpParams)
            if (mCookieStore == null) mCookieStore = BasicCookieStore()
            httpclient.cookieStore = mCookieStore
            modifyHttpClient(httpclient)
            mRequest = createRequest()

            httpclient.execute(setRequestData(mRequest!!), getResponseHandler(httpclient))
        } catch (e: Exception) {
            errorCode = IAPICommand.Companion.ERROR_GENERIC_COMMUNICATION_ERROR
            onFinished()
        }
    }

    /**
     * Override this to make changes to the HTTP client before it executes the
     * request.
     *
     * @param client
     */
    protected open fun modifyHttpClient(client: DefaultHttpClient) {
        // Override this if you need it.
    }

    /**
     * Notify all registered observers
     */
    protected fun onFinished() {
        if (!mNotifyFinishedBroadcast) return

        val broadcastIntent = Intent(mNotificationBroadcastIntentID)
        broadcastIntent.putExtra(IAPICommand.Companion.BROADCAST_INTENT_EXTRA_ERROR, errorCode)
        broadcastIntent.putExtra(
            IAPICommand.Companion.BROADCAST_INTENT_EXTRA_RESPONSE,
            responseContent
        )
        LocalBroadcastManager.getInstance(mApplicationContext!!).sendBroadcast(broadcastIntent)
    }

    /**
     * Returns TRUE if OFFLINE.
     *
     * @return boolean true if offline, or false if online.
     */
    protected fun cancelBecauseDeviceOffline(): Boolean {
        if (mApplicationContext != null && !ConnectivityUtils.isDeviceOnline(mApplicationContext)) {
            errorCode = IAPICommand.Companion.ERROR_DEVICE_OFFLINE
            return true
        }
        return false
    }

    fun cancel() {
        if (mRequest != null) mRequest!!.abort()
    }

    /**
     * Create a request object according to the request type set.
     *
     * @return HttpRequestBase request object.
     */
    protected fun createRequest(): HttpRequestBase {
        return when (mType) {
            IAPICommand.RequestType.GET -> HttpGet(urlWithParams)
            IAPICommand.RequestType.PUT -> HttpPut(urlWithParams)
            IAPICommand.RequestType.DELETE -> HttpDelete(urlWithParams)
            else -> HttpPost(urlWithParams)
        }
    }

    protected val urlWithParams: String
        get() = mUrl + (if (mURLQueryParams != null && mURLQueryParams != "") "?$mURLQueryParams" else "")

    override fun responseHandlingFinished(parsedResponse: T?, responseHttpStatus: Int) {
        actualStatusCode = responseHttpStatus
        responseContent = parsedResponse
        if (actualStatusCode < 200 || actualStatusCode >= 400) errorCode =
            IAPICommand.Companion.ERROR_SERVER_RETURNED_ERROR
        else if (responseContent == null) errorCode =
            IAPICommand.Companion.ERROR_RESPONSE_PARSE_ERROR
        else errorCode = IAPICommand.Companion.ERROR_NONE
        onFinished()
    }

    private val httpParams: HttpParams
        get() {
            val httpParameters: HttpParams = BasicHttpParams()
            HttpConnectionParams.setConnectionTimeout(
                httpParameters,
                mHttpTimeoutMS
            )
            HttpConnectionParams.setSoTimeout(
                httpParameters,
                mSocketTimeoutMS
            )
            return httpParameters
        }

    /**
     * Update the given request before it is sent over the wire.
     *
     * @param request
     */
    protected abstract fun setRequestData(request: HttpUriRequest): HttpUriRequest

    protected abstract fun getResponseHandler(client: HttpClient?): ResponseHandler<T>

    fun setCookieStore(cookieStore: CookieStore?) {
        mCookieStore = cookieStore
    }
}
