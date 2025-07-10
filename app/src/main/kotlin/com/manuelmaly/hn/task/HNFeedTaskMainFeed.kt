package com.manuelmaly.hn.task

import android.app.Activity
import android.content.Context
import com.manuelmaly.hn.model.HNFeed

class HNFeedTaskMainFeed private constructor(taskCode: Int) :
    HNFeedTaskBase(BROADCAST_INTENT_ID, taskCode) {
    override val feedURL: String?
        get() = "https://news.ycombinator.com/"

    companion object {
        private var instance: HNFeedTaskMainFeed? = null
        const val BROADCAST_INTENT_ID: String = "HNFeedMain"

        private fun getInstance(taskCode: Int): HNFeedTaskMainFeed {
            synchronized(HNFeedTaskBase::class.java) {
                if (instance == null) instance = HNFeedTaskMainFeed(taskCode)
            }
            return instance!!
        }

        fun startOrReattach(
            activity: Activity?,
            finishedHandler: ITaskFinishedHandler<HNFeed?>,
            taskCode: Int
        ) {
            val task = getInstance(taskCode)
            @Suppress("UNCHECKED_CAST")
            task.setOnFinishedHandler(activity, finishedHandler, HNFeed::class.java as Class<HNFeed?>)
            if (!task.isRunning) task.startInBackground()
        }

        fun stopCurrent(applicationContext: Context?) {
            getInstance(0).cancel()
        }

        fun isRunning(applicationContext: Context?): Boolean {
            return getInstance(0).isRunning
        }
    }
}
