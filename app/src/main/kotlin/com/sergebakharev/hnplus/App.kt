package com.sergebakharev.hnplus

import android.app.Application

class App : Application() {
    companion object {
        private var mInstance: App? = null
        
        fun getInstance(): App? {
            return mInstance
        }
    }

    override fun onCreate() {
        super.onCreate()
        mInstance = this
    }
}
