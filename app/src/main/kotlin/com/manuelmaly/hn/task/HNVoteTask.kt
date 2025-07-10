package com.manuelmaly.hn.task

import android.app.Activity
import com.manuelmaly.hn.App
import com.manuelmaly.hn.reuse.CancelableRunnable
import com.manuelmaly.hn.server.HNCredentials.getCookieStore
import com.manuelmaly.hn.server.HNVoteCommand
import com.manuelmaly.hn.server.IAPICommand

class HNVoteTask(taskCode: Int) :
    BaseTask<Boolean?>(BROADCAST_INTENT_ID, taskCode) {
    private var mVoteURL: String? = null

    override val task: CancelableRunnable
        get() = HNVoteTaskRunnable()

    fun setVoteURL(voteURL: String?) {
        mVoteURL = voteURL
    }

    internal inner class HNVoteTaskRunnable : CancelableRunnable() {
        var mVoteCommand: HNVoteCommand? = null

        override fun run() {
            this@HNVoteTask.result = vote()
        }

        private fun vote(): Boolean? {
            mVoteCommand = HNVoteCommand(
                mVoteURL!!, null, IAPICommand.RequestType.GET, false, null,
                App.Companion.getInstance(), getCookieStore(App.Companion.getInstance())
            )
            mVoteCommand!!.run()

            if (mCancelled || this@HNVoteTask.errorCode != IAPICommand.ERROR_NONE) return null

            return mVoteCommand!!.responseContent
        }

        override fun onCancelled() {
            if (mVoteCommand != null) mVoteCommand!!.cancel()
        }
    }

    companion object {
        const val BROADCAST_INTENT_ID: String = "HNVoteTask"

        private var instance: HNVoteTask? = null

        private fun getInstance(taskCode: Int): HNVoteTask {
            synchronized(HNVoteTask::class.java) {
                if (instance == null) instance = HNVoteTask(taskCode)
            }
            return instance!!
        }

        fun start(
            voteURL: String?, activity: Activity?,
            finishedHandler: ITaskFinishedHandler<Boolean?>, taskCode: Int, tag: Any?
        ) {
            val task = getInstance(taskCode)
            task.setTag(tag)
            task.setOnFinishedHandler(activity, finishedHandler, Boolean::class.java as Class<Boolean?>)
            if (task.isRunning) task.cancel()
            task.setVoteURL(voteURL)
            task.startInBackground()
        }
    }
}
