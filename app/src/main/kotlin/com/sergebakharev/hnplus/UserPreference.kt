package com.sergebakharev.hnplus

import android.content.Context
import android.preference.Preference
import android.util.AttributeSet

class UserPreference : Preference {
    private var mUsertoken: String? = null
    private var mUsername: String? = null

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    val data: String?
        get() {
            if (mUsername == null || mUsertoken == null) return null

            return mUsername + Settings.USER_DATA_SEPARATOR + mUsertoken
        }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any) {
        if (restorePersistedValue) {
            val userData = getPersistedString("")
            mUsername = getUserNameFromUserdata(userData)
            mUsertoken = getUserTokenFromUserdata(userData)
        } else {
            mUsername = null
            mUsertoken = null
            persistString(data)
        }
        super.onSetInitialValue(restorePersistedValue, defaultValue)
    }

    companion object {
        fun getUserNameFromUserdata(userData: String): String? {
            val splitData = userData.split(Settings.USER_DATA_SEPARATOR.toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
            if (splitData.size > 0) return splitData[0]
            return null
        }

        fun getUserTokenFromUserdata(userData: String): String? {
            val splitData = userData.split(Settings.USER_DATA_SEPARATOR.toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
            if (splitData.size > 1) return splitData[1]
            return null
        }
    }
}
