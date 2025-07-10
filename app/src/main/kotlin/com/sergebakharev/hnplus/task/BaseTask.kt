package com.sergebakharev.hnplus.task

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.sergebakharev.hnplus.App
import com.sergebakharev.hnplus.reuse.CancelableRunnable
import com.sergebakharev.hnplus.server.IAPICommand
import com.sergebakharev.hnplus.task.ITaskFinishedHandler.TaskResultCode
import com.sergebakharev.hnplus.util.Run
import java.io.Serializable
import java.lang.ref.SoftReference

/**
 * Generic base for tasks performed asynchronously. Unlike [AsyncTask],
 * its on-finished-notification will be passed to every Activity instance which
 * has registered (listeners are notified via an intent sent to
 * [LocalBroadcastManager]). Meaning, there will be no Zombie tasks
 * performing stuff for nothing (e.g. because their callback Activity has been
 * destroyed because of orientation change).
 *
 * @author manuelmaly
 * @param <T>
 * result type
</T> */
abstract class BaseTask<T : Serializable?>(
    protected var mNotificationBroadcastIntentID: String,
    protected var mTaskCode: Int
) :
    Runnable {
    var result: T? = null
        protected set
    var errorCode: Int = 0
        protected set
    var isRunning: Boolean = false
        protected set
    protected var mTaskRunnable: CancelableRunnable? = null
    protected var mTag: Any? = null

    protected fun startInBackground() {
        Run.inBackground(this)
    }

    /**
     * The broadcast will be received by listeners on the main thread
     * implicitly.
     */
    fun notifyFinished(errorCode: Int, result: Serializable?) {
        val broadcastIntent = Intent(mNotificationBroadcastIntentID)
        broadcastIntent.putExtra(BROADCAST_INTENT_EXTRA_ERROR, errorCode)
        broadcastIntent.putExtra(BROADCAST_INTENT_EXTRA_RESULT, result)
        App.getInstance()?.let { app ->
            LocalBroadcastManager.getInstance(app).sendBroadcast(broadcastIntent)
        }
    }

    /**
     *
     * @param tag
     */
    fun setTag(tag: Any?) {
        mTag = tag
    }

    /**
     * Registers the given [BroadcastReceiver] to this task's
     * finished-notification.
     *
     * @param receiver
     */
    fun registerForFinishedNotification(receiver: BroadcastReceiver) {
        val filter = IntentFilter(mNotificationBroadcastIntentID)
        App.getInstance()?.let { app ->
            LocalBroadcastManager.getInstance(app).registerReceiver(receiver, filter)
        }
    }

    /**
     * Schedules behaviour to be executed when this task has finished, for the
     * given Activity.
     *
     * @param activity
     * @param finishedHandler
     * @param resultClazz
     */
    fun setOnFinishedHandler(
        activity: Activity?, finishedHandler: ITaskFinishedHandler<T>?,
        resultClazz: Class<T>
    ) {
        val activityRef = SoftReference(activity)
        val finishedHandlerRef = SoftReference(
            finishedHandler
        )
        val finishedListener: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                App.getInstance()?.let { app ->
                    LocalBroadcastManager.getInstance(app).unregisterReceiver(
                        this
                    )
                }

                if (activityRef?.get() == null || finishedHandlerRef == null || finishedHandlerRef.get() == null) return

                // Make hard references until the end of processing, so we don't
                // lose those objects:
                val activity = activityRef.get()
                val finishedHandler = finishedHandlerRef.get()

                val lowLevelErrorCode = intent.getIntExtra(
                    BROADCAST_INTENT_EXTRA_ERROR,
                    IAPICommand.ERROR_NONE
                )
                val errorCode: Int
                val result: T?
                val rawResult = intent.getSerializableExtra(BROADCAST_INTENT_EXTRA_RESULT)
                if (resultClazz.isInstance(rawResult)) {
                    result = resultClazz.cast(rawResult)
                    errorCode = lowLevelErrorCode
                } else {
                    result = null
                    errorCode = if (lowLevelErrorCode == IAPICommand.ERROR_NONE) {
                        // We have no error so far, but cannot cast the result data:
                        IAPICommand.ERROR_UNKNOWN
                    } else {
                        lowLevelErrorCode
                    }
                }

                val r = Runnable {
                    finishedHandler!!
                        .onTaskFinished(
                            mTaskCode,
                            TaskResultCode.Companion.fromErrorCode(errorCode),
                            result!!,
                            mTag
                        )
                }
                Run.onUiThread(r, activity!!)
            }
        }
        this.registerForFinishedNotification(finishedListener)
    }

    override fun run() {
        isRunning = true
        mTaskRunnable = task
        mTaskRunnable!!.run()
        isRunning = false
        notifyFinished(errorCode, result)
    }

    fun cancel() {
        Run.inBackground { if (mTaskRunnable != null) mTaskRunnable!!.cancel() }
    }

    abstract val task: CancelableRunnable

    companion object {
        const val BROADCAST_INTENT_EXTRA_ERROR: String = "error"
        const val BROADCAST_INTENT_EXTRA_RESULT: String = "result"
    }
}
