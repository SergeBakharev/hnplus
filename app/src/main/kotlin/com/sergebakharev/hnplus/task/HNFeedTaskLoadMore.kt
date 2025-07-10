package com.sergebakharev.hnplus.task

import android.app.Activity
import android.content.Context
import com.sergebakharev.hnplus.model.HNFeed

class HNFeedTaskLoadMore private constructor(taskCode: Int) :
    HNFeedTaskBase(BROADCAST_INTENT_ID, taskCode) {
    private var mFeedToAttachResultsTo: HNFeed? = null

    override val feedURL: String?
        get() = mFeedToAttachResultsTo!!.nextPageURL

    fun setFeedToAttachResultsTo(feedToAttachResultsTo: HNFeed?) {
        mFeedToAttachResultsTo = feedToAttachResultsTo
    }

    companion object {
        private var instance: HNFeedTaskLoadMore? = null
        const val BROADCAST_INTENT_ID: String = "HNFeedLoadMore"

        private fun getInstance(taskCode: Int): HNFeedTaskLoadMore {
            synchronized(HNFeedTaskLoadMore::class.java) {
                if (instance == null) instance = HNFeedTaskLoadMore(taskCode)
            }
            return instance!!
        }

        fun start(
            activity: Activity?, finishedHandler: ITaskFinishedHandler<HNFeed?>,
            feedToAttachResultsTo: HNFeed?, taskCode: Int
        ) {
            val task = getInstance(taskCode)
            @Suppress("UNCHECKED_CAST")
            task.setOnFinishedHandler(activity, finishedHandler, HNFeed::class.java as Class<HNFeed?>)
            task.setFeedToAttachResultsTo(feedToAttachResultsTo)
            if (task.isRunning) task.cancel()
            task.startInBackground()
        }

        fun stopCurrent(applicationContext: Context?, taskCode: Int) {
            getInstance(taskCode).cancel()
        }

        fun isRunning(applicationContext: Context?, taskCode: Int): Boolean {
            return getInstance(taskCode).isRunning
        }
    }
}
