package com.sergebakharev.hnplus.reuse

abstract class CancelableRunnable : Runnable {
    @JvmField
    protected var mCancelled: Boolean = false

    fun cancel() {
        mCancelled = true
        onCancelled()
    }

    abstract fun onCancelled()
}
