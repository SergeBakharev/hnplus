package com.sergebakharev.hnplus.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.OnTouchListener
import android.view.Window
import com.sergebakharev.hnplus.view.SpotlightView

/**
 * Created by jmaltz on 12/23/13.
 */
class SpotlightActivity : Activity() {
    private var mXStart = 0f
    private var mYStart = 0f
    private var mXEnd = 0f
    private var mYEnd = 0f

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        mXStart = intent.getFloatExtra(KEY_X_START, -1f)
        mXEnd = intent.getFloatExtra(KEY_X_SIZE, -1f) + mXStart
        mYStart = intent.getFloatExtra(KEY_Y_START, -1f)
        mYEnd = intent.getFloatExtra(KEY_Y_SIZE, -1f) + mYStart
        val text = intent.getStringExtra(KEY_TEXT_STRING)
        val view = SpotlightView(this, null, mXStart, mYStart, mXEnd, mYEnd, text)
        view.setOnTouchListener(mSpotlightTouchListener)
        setContentView(view)
    }

    private val mSpotlightTouchListener: OnTouchListener = View.OnTouchListener { v: View, event: android.view.MotionEvent ->
        if (event.x > mXStart && event.x < mXEnd && event.y > mYStart && event.y < mYEnd) {
            setResult(RESULT_OK)
        } else {
            setResult(RESULT_CANCELED)
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
        true
    }

    companion object {
        const val KEY_X_START: String = "x_start"
        const val KEY_X_SIZE: String = "x_size"
        const val KEY_Y_START: String = "y_start"
        const val KEY_Y_SIZE: String = "y_size"
        const val KEY_TEXT_STRING: String = "text_string"

        fun intentForSpotlightActivity(
            context: Context?, xStart: Float, xSize: Float, yStart: Float,
            ySize: Float, text: String?
        ): Intent {
            val intent = Intent(context, SpotlightActivity::class.java)
            intent.putExtra(KEY_X_START, xStart)
            intent.putExtra(KEY_X_SIZE, xSize)
            intent.putExtra(KEY_Y_START, yStart)
            intent.putExtra(KEY_Y_SIZE, ySize)
            intent.putExtra(KEY_TEXT_STRING, text)
            return intent
        }
    }
}
