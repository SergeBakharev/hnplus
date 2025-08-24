package com.sergebakharev.hnplus.util

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.Window

object DisplayHelper {
    private var scale: Float? = null

    fun dpToPixel(dp: Int, context: Context): Int {
        if (scale == null) scale = context.resources.displayMetrics.density
        return (dp.toFloat() * scale!!).toInt()
    }

    @Suppress("deprecation")
    fun bitmapWithConstraints(
        bitmapResource: Int,
        ctx: Context,
        dpConstraintWidthAndHeight: Int,
        padding: Int
    ): BitmapDrawable {
        val options = BitmapFactory.Options()
        options.inScaled = false
        val bitmapOrg = BitmapFactory.decodeResource(ctx.resources, bitmapResource, options)

        val width = bitmapOrg.width
        val height = bitmapOrg.height
        val newWidth = dpToPixel(dpConstraintWidthAndHeight, ctx) - 2 * dpToPixel(padding, ctx)
        val newHeight = newWidth
        val scaleWidth = (newWidth.toFloat()) / width
        val scaleHeight = (newHeight.toFloat()) / height

        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)

        val resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true)
        return BitmapDrawable(resizedBitmap)
    }

    @Suppress("deprecation")
    fun getScreenWidth(a: Activity): Int {
        val display = a.windowManager.defaultDisplay
        return display.width
    }

    @Suppress("deprecation")
    fun getScreenHeight(a: Activity): Int {
        val display = a.windowManager.defaultDisplay
        return display.height
    }

    fun setDialogParams(
        d: Dialog,
        a: Activity,
        hasTitleBar: Boolean,
        layout: View,
        marginHorizontalDP: Int
    ) {
        if (!hasTitleBar) d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.setContentView(layout)
        val lp = d.window!!.attributes
        lp.copyFrom(d.window!!.attributes)
        lp.dimAmount = 0.8f
        lp.width = getScreenWidth(a) - 2 * dpToPixel(marginHorizontalDP, a)
    }
}
