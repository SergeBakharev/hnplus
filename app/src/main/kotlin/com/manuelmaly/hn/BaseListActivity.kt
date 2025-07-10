package com.manuelmaly.hn

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.manuelmaly.hn.util.FontHelper

open class BaseListActivity : AppCompatActivity() {
    private var mLoadingView: TextView? = null

    protected fun getEmptyTextView(parent: ViewGroup?): TextView? {
        if (mLoadingView == null) {
            val root = layoutInflater.inflate(R.layout.panel_loading, parent, false)
            mLoadingView = root.findViewById(android.R.id.empty) as TextView
            mLoadingView!!.visibility = View.GONE
            mLoadingView!!.typeface = FontHelper.getComfortaa(this, true)
        }
        return mLoadingView
    }
}