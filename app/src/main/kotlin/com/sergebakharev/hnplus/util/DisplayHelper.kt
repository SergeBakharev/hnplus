package com.sergebakharev.hnplus.util

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

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

    /**
     * Adjust content positioning to prevent action bar overlap
     * Call this in onCreate() after setContentView() for activities with action bars
     */
    fun adjustContentBelowActionBar(activity: AppCompatActivity, swipeRefreshLayout: SwipeRefreshLayout) {
        val actionBarHeight = getActionBarHeight(activity)
        val targetOffset = 120 // Optimal offset to prevent overlap while minimizing gap
        
        Log.d("DisplayHelper", "Adjusting content with offset: $targetOffset")
        
        // Set top margin on SwipeRefreshLayout to position content below action bar
        val params = swipeRefreshLayout.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = targetOffset
        swipeRefreshLayout.layoutParams = params
    }
    
    /**
     * Get the actual height of the action bar
     */
    private fun getActionBarHeight(activity: AppCompatActivity): Int {
        val typedValue = TypedValue()
        val success = activity.theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)
        
        Log.d("DisplayHelper", "Theme resolve success: $success")
        Log.d("DisplayHelper", "TypedValue type: ${typedValue.type}")
        Log.d("DisplayHelper", "TypedValue data: ${typedValue.data}")
        
        return if (success && typedValue.type == TypedValue.TYPE_DIMENSION) {
            val height = TypedValue.complexToDimensionPixelSize(typedValue.data, activity.resources.displayMetrics)
            Log.d("DisplayHelper", "Calculated height: $height")
            height
        } else {
            // Try alternative method using support action bar
            val actionBar = activity.supportActionBar
            if (actionBar != null) {
                val height = actionBar.height
                Log.d("DisplayHelper", "ActionBar height: $height")
                if (height > 0) height else getFallbackHeight(activity)
            } else {
                Log.d("DisplayHelper", "Using fallback height")
                getFallbackHeight(activity)
            }
        }
    }
    
    private fun getFallbackHeight(activity: AppCompatActivity): Int {
        // Fallback to standard action bar height (56dp)
        val height = (56 * activity.resources.displayMetrics.density).toInt()
        Log.d("DisplayHelper", "Fallback height: $height")
        return height
    }
}
