package com.sergebakharev.hnplus.task

import android.app.Activity
import android.util.Log
import com.sergebakharev.hnplus.App
import com.sergebakharev.hnplus.model.HNCommentForm
import com.sergebakharev.hnplus.parser.HNCommentFormParser
import com.sergebakharev.hnplus.reuse.CancelableRunnable
import com.sergebakharev.hnplus.server.HNCommentPostCommand
import com.sergebakharev.hnplus.server.HNCredentials.getCookieStore
import com.sergebakharev.hnplus.server.IAPICommand
import com.sergebakharev.hnplus.server.StringDownloadCommand

class HNCommentPostTask(taskCode: Int) :
    BaseTask<Boolean?>(BROADCAST_INTENT_ID, taskCode) {
    private var mText: String? = null
    private var mParentId: String? = null
    private var mStoryId: String? = null

    override val task: CancelableRunnable
        get() = HNCommentPostTaskRunnable()

    fun setData(text: String, parentId: String, storyId: String) {
        mText = text
        mParentId = parentId
        mStoryId = storyId
    }

    internal inner class HNCommentPostTaskRunnable : CancelableRunnable() {
        var mFormDownload: StringDownloadCommand? = null
        var mPostCommand: HNCommentPostCommand? = null

        override fun run() {
            this@HNCommentPostTask.result = postComment()
        }

        private fun postComment(): Boolean {
            val form = fetchForm() ?: return false
            if (mCancelled) return false
            return submit(form)
        }

        private fun fetchForm(): HNCommentForm? {
            val queryParams = HashMap<String?, String?>()
            queryParams["id"] = mParentId
            mFormDownload = StringDownloadCommand(
                ITEM_URL,
                queryParams,
                IAPICommand.RequestType.GET,
                false,
                null,
                App.getInstance(),
                getCookieStore(App.getInstance())
            )
            mFormDownload!!.run()

            this@HNCommentPostTask.errorCode = if (mCancelled) IAPICommand.ERROR_CANCELLED_BY_USER
            else mFormDownload!!.errorCode

            if (mCancelled || this@HNCommentPostTask.errorCode != IAPICommand.ERROR_NONE) return null

            return try {
                val form = HNCommentFormParser().parse(mFormDownload!!.responseContent)
                if (form == null) {
                    this@HNCommentPostTask.errorCode = IAPICommand.ERROR_RESPONSE_PARSE_ERROR
                }
                form
            } catch (e: Exception) {
                Log.e("HNCommentPostTask", "Form parse error", e)
                this@HNCommentPostTask.errorCode = IAPICommand.ERROR_RESPONSE_PARSE_ERROR
                null
            }
        }

        private fun submit(form: HNCommentForm, isRetry: Boolean = false): Boolean {
            val body = HashMap<String, String>()
            body["parent"] = form.parent
            body["goto"] = form.goto.ifEmpty { "item?id=${mStoryId ?: form.parent}" }
            body["hmac"] = form.hmac
            body["text"] = mText ?: ""

            mPostCommand = HNCommentPostCommand(
                COMMENT_URL,
                IAPICommand.RequestType.POST,
                App.getInstance(),
                getCookieStore(App.getInstance()),
                body
            )
            mPostCommand!!.run()

            this@HNCommentPostTask.errorCode = if (mCancelled) IAPICommand.ERROR_CANCELLED_BY_USER
            else mPostCommand!!.errorCode

            if (mCancelled || this@HNCommentPostTask.errorCode != IAPICommand.ERROR_NONE) return false

            val html = mPostCommand!!.responseHtml
            if (!isRetry && html != null && html.contains("Please confirm that this is your comment")) {
                return submit(form, isRetry = true)
            }

            return mPostCommand!!.responseContent == true
        }

        override fun onCancelled() {
            mFormDownload?.cancel()
            mPostCommand?.cancel()
        }
    }

    companion object {
        private const val ITEM_URL = "https://news.ycombinator.com/item"
        private const val COMMENT_URL = "https://news.ycombinator.com/comment"
        const val BROADCAST_INTENT_ID: String = "HNCommentPostTask"

        private var instance: HNCommentPostTask? = null

        private fun getInstance(taskCode: Int): HNCommentPostTask {
            synchronized(HNCommentPostTask::class.java) {
                if (instance == null) instance = HNCommentPostTask(taskCode)
            }
            return instance!!
        }

        fun start(
            text: String,
            parentId: String,
            storyId: String,
            activity: Activity?,
            finishedHandler: ITaskFinishedHandler<Boolean?>,
            taskCode: Int,
            tag: Any?
        ) {
            val task = getInstance(taskCode)
            task.setTag(tag)
            task.setOnFinishedHandler(activity, finishedHandler, java.lang.Boolean::class.java as Class<Boolean?>)
            if (task.isRunning) task.cancel()
            task.setData(text, parentId, storyId)
            task.startInBackground()
        }
    }
}
