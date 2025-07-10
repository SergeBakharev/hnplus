package com.sergebakharev.hnplus.task

import android.util.Log
import com.sergebakharev.hnplus.App
import com.sergebakharev.hnplus.model.HNFeed
import com.sergebakharev.hnplus.parser.HNFeedParser
import com.sergebakharev.hnplus.reuse.CancelableRunnable
import com.sergebakharev.hnplus.server.HNCredentials.getCookieStore
import com.sergebakharev.hnplus.server.IAPICommand
import com.sergebakharev.hnplus.server.StringDownloadCommand
import com.sergebakharev.hnplus.util.FileUtil
import com.sergebakharev.hnplus.util.Run

abstract class HNFeedTaskBase(notificationBroadcastIntentID: String, taskCode: Int) :
    BaseTask<HNFeed?>(notificationBroadcastIntentID, taskCode) {
    override val task: CancelableRunnable
        get() = HNFeedTaskRunnable()

    protected abstract val feedURL: String?

    internal inner class HNFeedTaskRunnable : CancelableRunnable() {
        var mFeedDownload: StringDownloadCommand? = null

        override fun run() {
            mFeedDownload = StringDownloadCommand(
                this@HNFeedTaskBase.feedURL ?: "",
                HashMap<String?, String?>(),
                IAPICommand.RequestType.GET, false, null,
                App.Companion.getInstance(), getCookieStore(App.Companion.getInstance())
            )

            mFeedDownload!!.run()

            errorCode = if (mCancelled) IAPICommand.ERROR_CANCELLED_BY_USER
            else mFeedDownload!!.errorCode

            if (!mCancelled && errorCode == IAPICommand.ERROR_NONE) {
                val feedParser = HNFeedParser()
                try {
                    result = feedParser.parse(mFeedDownload!!.responseContent)
                    Run.inBackground { FileUtil.saveLastHNFeed(result) }
                } catch (e: Exception) {
                    result = null
                    Log.e("HNFeedTask", "HNFeed Parser Error :(", e)
                }
            }

            if (result == null) result = HNFeed()
        }

        override fun onCancelled() {
            mFeedDownload!!.cancel()
        }
    }
}
