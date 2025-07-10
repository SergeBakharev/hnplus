package com.sergebakharev.hnplus.task

import android.app.Activity
import android.util.Log
import com.sergebakharev.hnplus.App
import com.sergebakharev.hnplus.model.HNPostComments
import com.sergebakharev.hnplus.parser.HNCommentsParser
import com.sergebakharev.hnplus.reuse.CancelableRunnable
import com.sergebakharev.hnplus.server.HNCredentials.getCookieStore
import com.sergebakharev.hnplus.server.IAPICommand
import com.sergebakharev.hnplus.server.StringDownloadCommand
import com.sergebakharev.hnplus.util.FileUtil
import com.sergebakharev.hnplus.util.Run

class HNPostCommentsTask private constructor(// for which post shall comments be loaded?
    private val mPostID: String, taskCode: Int
) :
    BaseTask<HNPostComments?>(BROADCAST_INTENT_ID, taskCode) {
    override val task: CancelableRunnable
        get() = HNPostCommentsTaskRunnable()

    internal inner class HNPostCommentsTaskRunnable : CancelableRunnable() {
        var mFeedDownload: StringDownloadCommand? = null

        override fun run() {
            val queryParams = HashMap<String?, String?>()
            queryParams["id"] = mPostID
            mFeedDownload = StringDownloadCommand(
                "https://news.ycombinator.com/item",
                queryParams,
                IAPICommand.RequestType.GET,
                false,
                null,
                App.Companion.getInstance(),
                getCookieStore(App.Companion.getInstance())
            )
            mFeedDownload!!.run()

            this@HNPostCommentsTask.errorCode = if (mCancelled) IAPICommand.ERROR_CANCELLED_BY_USER
            else mFeedDownload!!.errorCode

            if (!mCancelled && this@HNPostCommentsTask.errorCode == IAPICommand.ERROR_NONE) {
                val commentsParser = HNCommentsParser()
                try {
                    this@HNPostCommentsTask.result = commentsParser.parse(mFeedDownload!!.responseContent)
                    Run.inBackground { FileUtil.setLastHNPostComments(this@HNPostCommentsTask.result!!, mPostID) }
                } catch (e: Exception) {
                    Log.e("HNFeedTask", "Parse error!", e)
                }
            }

            if (this@HNPostCommentsTask.result == null) this@HNPostCommentsTask.result = HNPostComments()
        }

        override fun onCancelled() {
            mFeedDownload!!.cancel()
        }
    }

    companion object {
        const val BROADCAST_INTENT_ID: String = "HNPostComments"
        private val runningInstances = HashMap<String, HNPostCommentsTask>()

        /**
         * I know, Singleton is generally a no-no, but the only other option would
         * be to store the currently running HNPostCommentsTasks in the App object,
         * which I consider far worse. If you find a better solution, please tweet
         * me at @manuelmaly
         *
         * @return
         */
        private fun getInstance(postID: String, taskCode: Int): HNPostCommentsTask? {
            synchronized(HNPostCommentsTask::class.java) {
                if (!runningInstances.containsKey(postID)) runningInstances[postID] =
                    HNPostCommentsTask(postID, taskCode)
            }
            return runningInstances[postID]
        }

        fun startOrReattach(
            activity: Activity?, finishedHandler: ITaskFinishedHandler<HNPostComments?>,
            postID: String, taskCode: Int
        ) {
            val task = getInstance(postID, taskCode)
            task!!.setOnFinishedHandler(activity, finishedHandler, HNPostComments::class.java as Class<HNPostComments?>)
            if (!task.isRunning) task.startInBackground()
        }

        fun stopCurrent(postID: String) {
            getInstance(postID, 0)!!.cancel()
        }

        fun isRunning(postID: String): Boolean {
            return getInstance(postID, 0)!!.isRunning
        }
    }
}
