package com.sergebakharev.hnplus.task

import android.app.Activity
import android.util.Log
import com.sergebakharev.hnplus.App
import com.sergebakharev.hnplus.model.HNConfirmForm
import com.sergebakharev.hnplus.parser.HNConfirmFormParser
import com.sergebakharev.hnplus.reuse.CancelableRunnable
import com.sergebakharev.hnplus.server.HNCommentPostCommand
import com.sergebakharev.hnplus.server.HNCredentials.getCookieStore
import com.sergebakharev.hnplus.server.IAPICommand
import com.sergebakharev.hnplus.server.StringDownloadCommand

class HNCommentDeleteTask(taskCode: Int) :
    BaseTask<Boolean?>(BROADCAST_INTENT_ID, taskCode) {
    private var mDeleteUrl: String? = null

    override val task: CancelableRunnable
        get() = HNCommentDeleteTaskRunnable()

    fun setDeleteUrl(deleteUrl: String) {
        mDeleteUrl = deleteUrl
    }

    internal inner class HNCommentDeleteTaskRunnable : CancelableRunnable() {
        var mFormDownload: StringDownloadCommand? = null
        var mPostCommand: HNCommentPostCommand? = null

        override fun run() {
            this@HNCommentDeleteTask.result = deleteComment()
        }

        private fun deleteComment(): Boolean {
            val form = fetchForm() ?: return false
            if (mCancelled) return false
            return submit(form)
        }

        private fun fetchForm(): HNConfirmForm? {
            val deleteUrl = mDeleteUrl ?: return null
            mFormDownload = StringDownloadCommand(
                deleteUrl,
                null,
                IAPICommand.RequestType.GET,
                false,
                null,
                App.getInstance(),
                getCookieStore(App.getInstance())
            )
            mFormDownload!!.run()

            this@HNCommentDeleteTask.errorCode = if (mCancelled) IAPICommand.ERROR_CANCELLED_BY_USER
            else mFormDownload!!.errorCode

            if (mCancelled || this@HNCommentDeleteTask.errorCode != IAPICommand.ERROR_NONE) return null

            return try {
                val form = HNConfirmFormParser().parse(mFormDownload!!.responseContent)
                if (form == null) {
                    this@HNCommentDeleteTask.errorCode = IAPICommand.ERROR_RESPONSE_PARSE_ERROR
                }
                form
            } catch (e: Exception) {
                Log.e("HNCommentDeleteTask", "Form parse error", e)
                this@HNCommentDeleteTask.errorCode = IAPICommand.ERROR_RESPONSE_PARSE_ERROR
                null
            }
        }

        private fun submit(form: HNConfirmForm): Boolean {
            val actionUrl = form.actionUrl.ifEmpty { mDeleteUrl ?: return false }
            mPostCommand = HNCommentPostCommand(
                actionUrl,
                IAPICommand.RequestType.POST,
                App.getInstance(),
                getCookieStore(App.getInstance()),
                form.fields
            )
            mPostCommand!!.run()

            this@HNCommentDeleteTask.errorCode = if (mCancelled) IAPICommand.ERROR_CANCELLED_BY_USER
            else mPostCommand!!.errorCode

            if (mCancelled || this@HNCommentDeleteTask.errorCode != IAPICommand.ERROR_NONE) return false
            return mPostCommand!!.responseContent == true
        }

        override fun onCancelled() {
            mFormDownload?.cancel()
            mPostCommand?.cancel()
        }
    }

    companion object {
        const val BROADCAST_INTENT_ID: String = "HNCommentDeleteTask"

        private var instance: HNCommentDeleteTask? = null

        private fun getInstance(taskCode: Int): HNCommentDeleteTask {
            synchronized(HNCommentDeleteTask::class.java) {
                if (instance == null) instance = HNCommentDeleteTask(taskCode)
            }
            return instance!!
        }

        fun start(
            deleteUrl: String,
            activity: Activity?,
            finishedHandler: ITaskFinishedHandler<Boolean?>,
            taskCode: Int,
            tag: Any?
        ) {
            val task = getInstance(taskCode)
            task.setTag(tag)
            task.setOnFinishedHandler(activity, finishedHandler, java.lang.Boolean::class.java as Class<Boolean?>)
            if (task.isRunning) task.cancel()
            task.setDeleteUrl(deleteUrl)
            task.startInBackground()
        }
    }
}
