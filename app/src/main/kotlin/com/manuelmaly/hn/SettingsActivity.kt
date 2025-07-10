package com.manuelmaly.hn

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import android.view.View
import com.manuelmaly.hn.login.LoginActivity
import com.manuelmaly.hn.server.HNCredentials
import com.manuelmaly.hn.util.Run

class SettingsActivity : PreferenceActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    enum class FONTSIZE {
        FONTSIZE_SMALL, FONTSIZE_NORMAL, FONTSIZE_BIG
    }
    
    enum class HTMLPROVIDER {
        HTMLPROVIDER_ORIGINAL_ARTICLE_URL,
        HTMLPROVIDER_GOOGLE,
        HTMLPROVIDER_VIEWTEXT,
        HTMLPROVIDER_INSTAPAPER
    }
    
    enum class HTMLVIEWER {
        HTMLVIEWER_WITHINAPP, HTMLVIEWER_BROWSER
    }
    
    private val REQUEST_LOGIN = 100
    private var mUserPref: Preference? = null
    
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        addPreferencesFromResource(R.xml.preferences)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        
        val fontSizePref = findPreference(Settings.PREF_FONTSIZE)
        fontSizePref?.summary = sharedPref.getString(Settings.PREF_FONTSIZE, "Undefined")
        
        val htmlProviderPref = findPreference(Settings.PREF_HTMLPROVIDER)
        htmlProviderPref?.summary = sharedPref.getString(Settings.PREF_HTMLPROVIDER, "Undefined")
        
        val htmlViewerPref = findPreference(Settings.PREF_HTMLVIEWER)
        htmlViewerPref?.summary = sharedPref.getString(Settings.PREF_HTMLVIEWER, "Undefined")
        
        mUserPref = findPreference(Settings.PREF_USER) as? UserPreference
        mUserPref?.setOnPreferenceClickListener { preference ->
            val userName = Settings.getUserName(this)
            
            if (userName.isNullOrEmpty()) {
                onLoginClick()
            } else {
                onLogoutClick()
            }
            
            false
        }
        

        
        updateUserItem()
        
        val backView = findViewById<View>(R.id.actionbar_back)
        backView?.setOnClickListener {
            finish()
        }
    }
    
    @Suppress("DEPRECATION")
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        key?.let { keyName ->
            Run.onUiThread({
                when (keyName) {
                    Settings.PREF_FONTSIZE, Settings.PREF_HTMLPROVIDER, Settings.PREF_HTMLVIEWER -> {
                        findPreference(keyName)?.summary = sharedPreferences.getString(keyName, "Undefined")
                    }
                    Settings.PREF_USER -> {
                        HNCredentials.invalidate()
                        updateUserItem()
                    }
                }
            }, this)
        }
    }
    
    private fun updateUserItem() {
        val userName = Settings.getUserName(this)
        mUserPref?.summary = if (!userName.isNullOrEmpty()) userName else " "
    }
    
    private fun onLoginClick() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivityForResult(intent, REQUEST_LOGIN)
    }
    
    private fun onLogoutClick() {
        val logoutDialog = AlertDialog.Builder(this).create()
        logoutDialog.setTitle(R.string.logout_question_lbl)
        logoutDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok)) { _, _ ->
            Settings.clearUserData(this)
        }
        logoutDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), null as android.os.Message?)
        logoutDialog.show()
    }
    
    @Suppress("DEPRECATION")
    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }
    
    @Suppress("DEPRECATION")
    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_LOGIN -> {
                // If there was a successful login, then we want to show that to the user
                if (resultCode == RESULT_OK) {
                    updateUserItem()
                }
            }
        }
        
        super.onActivityResult(requestCode, resultCode, data)
    }
}
