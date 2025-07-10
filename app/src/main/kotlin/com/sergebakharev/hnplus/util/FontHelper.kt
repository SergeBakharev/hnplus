package com.sergebakharev.hnplus.util

import android.content.Context
import android.graphics.Typeface

object FontHelper {
    private var mComfortaaRegular: Typeface? = null
    private var mComfortaaBold: Typeface? = null
    private val lock = Any()

    fun getComfortaa(context: Context, bold: Boolean): Typeface? {
        synchronized(lock) {
            if (!bold && mComfortaaRegular == null) mComfortaaRegular =
                Typeface.createFromAsset(context.assets, "Comfortaa-Regular.ttf")
            else if (bold && mComfortaaBold == null) mComfortaaBold =
                Typeface.createFromAsset(context.assets, "Comfortaa-Bold.ttf")
        }
        return if (bold) mComfortaaBold else mComfortaaRegular
    }
}
