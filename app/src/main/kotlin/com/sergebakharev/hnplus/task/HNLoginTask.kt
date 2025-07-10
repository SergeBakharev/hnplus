package com.sergebakharev.hnplus.task

import android.app.Activity
import android.util.Log
import com.sergebakharev.hnplus.App
import com.sergebakharev.hnplus.Settings
import com.sergebakharev.hnplus.parser.HNNewsLoginParser
import com.sergebakharev.hnplus.reuse.CancelableRunnable
import com.sergebakharev.hnplus.server.GetHNUserTokenHTTPCommand
import com.sergebakharev.hnplus.server.IAPICommand
import com.sergebakharev.hnplus.server.StringDownloadCommand

class HNLoginTask(taskCode: Int) :
    BaseTask<Boolean?>(BROADCAST_INTENT_ID, taskCode) {
    private var mUsername: String? = null
    private var mPassword: String? = null
    private val mFNID: String? = null

    override val task: CancelableRunnable
        get() = HNLoginTaskRunnable()

    fun setData(username: String?, password: String?) {
        mUsername = username
        mPassword = password
    }

    internal inner class HNLoginTaskRunnable : CancelableRunnable() {
        var newsLoginDownload: StringDownloadCommand? = null
        var getUserTokenCommand: GetHNUserTokenHTTPCommand? = null

        override fun run() {
//            mFNID = getFNID();
//            if (mFNID == null)
//                return;

            val userToken = userToken
            if (userToken != null && userToken != "") {
                this@HNLoginTask.result = true
                Settings.setUserData(mUsername!!, userToken, App.Companion.getInstance()!!)
            } else this@HNLoginTask.result = false
        }

        private val fNID: String?
            get() {
                newsLoginDownload = StringDownloadCommand(
                    NEWSLOGIN_URL,
                    HashMap<String?, String?>(),
                    IAPICommand.RequestType.GET,
                    false,
                    null,
                    App.Companion.getInstance(),
                    null
                )
                newsLoginDownload!!.run()

                this@HNLoginTask.errorCode = if (mCancelled) IAPICommand.ERROR_CANCELLED_BY_USER
                else newsLoginDownload!!.errorCode

                if (!mCancelled && this@HNLoginTask.errorCode == IAPICommand.ERROR_NONE) {
                    val loginParser = HNNewsLoginParser()
                    try {
                        return loginParser.parse(newsLoginDownload!!.responseContent)
                    } catch (e: Exception) {
                        Log.e("HNFeedTask", "Login Page Parser Error :(", e)
                    }
                }
                return null
            }

        private val userToken: String?
            get() {
                val queryParams =
                    HashMap<String?, String?>()
                queryParams["goto"] = "news"
                val body =
                    HashMap<String, String>()
                body["goto"] = "news"
                body["acct"] = mUsername ?: ""
                body["pw"] = mPassword ?: ""

                getUserTokenCommand = GetHNUserTokenHTTPCommand(
                    GET_USERTOKEN_URL,
                    queryParams,
                    IAPICommand.RequestType.POST,
                    false,
                    null,
                    App.Companion.getInstance(),
                    body
                )
                getUserTokenCommand!!.run()

                this@HNLoginTask.errorCode = if (mCancelled) IAPICommand.ERROR_CANCELLED_BY_USER
                else getUserTokenCommand!!.errorCode

                if (!mCancelled && this@HNLoginTask.errorCode == IAPICommand.ERROR_NONE) {
                    return getUserTokenCommand!!.responseContent
                }
                return null
            }

        override fun onCancelled() {
            if (newsLoginDownload != null) newsLoginDownload!!.cancel()
            if (getUserTokenCommand != null) getUserTokenCommand!!.cancel()
        }
    }

    companion object {
        private const val NEWSLOGIN_URL = "https://news.ycombinator.com/login"
        private const val GET_USERTOKEN_URL = "https://news.ycombinator.com/login"
        const val BROADCAST_INTENT_ID: String = "HNLoginTask"

        private var instance: HNLoginTask? = null

        private fun getInstance(taskCode: Int): HNLoginTask {
            synchronized(HNLoginTask::class.java) {
                if (instance == null) instance = HNLoginTask(taskCode)
            }
            return instance!!
        }

        fun start(
            username: String?, password: String?, activity: Activity?,
            finishedHandler: ITaskFinishedHandler<Boolean?>, taskCode: Int
        ) {
            val task = getInstance(taskCode)
            task.setOnFinishedHandler(activity, finishedHandler, Boolean::class.java as Class<Boolean?>)
            task.setData(username, password)
            if (task.isRunning) task.cancel()
            task.startInBackground()
        }
    }
}
