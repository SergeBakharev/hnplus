package com.sergebakharev.hnplus.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.BlurMaskFilter.Blur
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View

/**
 * Created by jmaltz on 12/23/13.
 */
class SpotlightView @JvmOverloads constructor(
    ctx: Context?,
    attrs: AttributeSet?,
    private val mXStart: Float = 0f,
    private val mYStart: Float = 0f,
    private val mXEnd: Float = 0f,
    private val mYEnd: Float = 0f,
    text: String? = ""
) :
    View(ctx, attrs) {
    private val mClearPaint = Paint()
    private val mBackgroundPaint: Paint
    private val mTextPaint: TextPaint

    private val mClearRect: Rect
    var mBitmap: Bitmap? = null

    private var mTextLayout: StaticLayout? = null
    var mText: String?

    init {
        mClearPaint.color = Color.TRANSPARENT
        mClearPaint.isAntiAlias = true
        mClearPaint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.CLEAR))
        mClearPaint.setMaskFilter(BlurMaskFilter(2 * resources.displayMetrics.density, Blur.NORMAL))

        mTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        mTextPaint.color = Color.WHITE
        mTextPaint.textSize = (resources.displayMetrics.density * 20 + .5).toFloat()
        mTextPaint.setShadowLayer(1.5f, 1f, 1f, Color.BLACK)

        mBackgroundPaint = Paint()
        mBackgroundPaint.color = Color.BLACK
        mBackgroundPaint.alpha = 200

        mClearRect = Rect(mXStart.toInt(), mYStart.toInt(), mXEnd.toInt(), mYEnd.toInt())
        mText = text
    }

    public override fun onDraw(canvas: Canvas) {
        mBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, 0f, 0f, null)
        }
    }

    public override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val density = resources.displayMetrics.density
        mTextLayout = StaticLayout(
            mText, mTextPaint, w - (density * 20 + .5).toInt(), Layout.Alignment.ALIGN_CENTER, 1f,
            0f, false
        )
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mBitmap?.let { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.drawRect(Rect(0, 0, w, h), mBackgroundPaint)
            canvas.drawRect(mClearRect, mClearPaint)
            canvas.translate(0f, mYEnd + (density * 10 + .5).toInt())
            mTextLayout!!.draw(canvas)
        }
    }
}
