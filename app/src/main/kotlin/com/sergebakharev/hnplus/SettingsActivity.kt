package com.sergebakharev.hnplus

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.sergebakharev.hnplus.login.LoginActivity
import com.sergebakharev.hnplus.server.HNCredentials
import com.sergebakharev.hnplus.util.DisplayHelper
import com.sergebakharev.hnplus.util.FontHelper
import com.sergebakharev.hnplus.Settings

class SettingsActivity : AppCompatActivity() {
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
    private var mActionbarTitle: TextView? = null
    private var mSettingsList: ListView? = null
    private var mSettingsAdapter: SettingsAdapter? = null
    private var mSharedPref: SharedPreferences? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        
        mActionbarTitle = supportActionBar?.customView?.findViewById(R.id.actionbar_title)
        mActionbarTitle?.typeface = FontHelper.getComfortaa(this, true)
        mActionbarTitle?.text = getString(R.string.settings)
        
        // Initialize settings
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        mSettingsList = findViewById(R.id.settings_list)
        mSettingsAdapter = SettingsAdapter()
        mSettingsList?.adapter = mSettingsAdapter
        
        // Prevent overlapping with action bar
        val swipeRefreshLayout = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.settings_swiperefreshlayout)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_LOGIN -> {
                if (resultCode == RESULT_OK) {
                    mSettingsAdapter?.notifyDataSetChanged()
                }
            }
        }
    }
    
    private inner class SettingsAdapter : BaseAdapter() {
        private val settingsItems = listOf(
            SettingsItem("Font Size", getFontSizeSummary(), 0),
            SettingsItem("HTML Provider", getHtmlProviderSummary(), 1),
            SettingsItem("HTML Viewer", getHtmlViewerSummary(), 2),
            SettingsItem("User", getUserSummary(), 3)
        )
        
        override fun getCount(): Int = settingsItems.size
        override fun getItem(position: Int): Any = settingsItems[position]
        override fun getItemId(position: Int): Long = position.toLong()
        
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@SettingsActivity)
                .inflate(R.layout.settings_list_item, parent, false)
            
            val item = settingsItems[position]
            val titleView = view.findViewById<TextView>(R.id.settings_item_title)
            val summaryView = view.findViewById<TextView>(R.id.settings_item_summary)
            
            titleView.text = item.title
            titleView.typeface = FontHelper.getComfortaa(this@SettingsActivity, true)
            titleView.setTextColor(resources.getColor(android.R.color.black))
            
            summaryView.text = item.summary
            summaryView.typeface = FontHelper.getComfortaa(this@SettingsActivity, false)
            summaryView.setTextColor(Color.parseColor("#787067"))
            
            val fontSize = Settings.getFontSize(this@SettingsActivity)
            val titleSize = when (fontSize) {
                getString(R.string.pref_fontsize_small) -> 15
                getString(R.string.pref_fontsize_normal) -> 18
                else -> 22
            }
            val detailsSize = when (fontSize) {
                getString(R.string.pref_fontsize_small) -> 11
                getString(R.string.pref_fontsize_normal) -> 12
                else -> 15
            }
            
            titleView.textSize = titleSize.toFloat()
            summaryView.textSize = detailsSize.toFloat()
            
            view.setOnClickListener {
                onSettingItemClick(item, position)
            }
            
            return view
        }
        
        private fun onSettingItemClick(item: SettingsItem, position: Int) {
            when (position) {
                0 -> showFontSizeDialog()
                1 -> showHtmlProviderDialog()
                2 -> showHtmlViewerDialog()
                3 -> handleUserClick()
            }
        }
        
        private fun showFontSizeDialog() {
            val options = arrayOf("Small", "Normal", "Big")
            val currentIndex = when (Settings.getFontSize(this@SettingsActivity)) {
                getString(R.string.pref_fontsize_small) -> 0
                getString(R.string.pref_fontsize_big) -> 2
                else -> 1
            }
            
            AlertDialog.Builder(this@SettingsActivity)
                .setTitle("Font Size")
                .setSingleChoiceItems(options, currentIndex) { _, which ->
                    val newValue = when (which) {
                        0 -> getString(R.string.pref_fontsize_small)
                        2 -> getString(R.string.pref_fontsize_big)
                        else -> getString(R.string.pref_fontsize_normal)
                    }
                    setFontSize(newValue)
                    notifyDataSetChanged()
                }
                .show()
        }
        
        private fun showHtmlProviderDialog() {
            val options = arrayOf("Original URL", "Instapaper", "Textise")
            val currentIndex = when (Settings.getHtmlProvider(this@SettingsActivity)) {
                getString(R.string.pref_htmlprovider_instapaper) -> 1
                getString(R.string.pref_htmlprovider_textise) -> 2
                else -> 0
            }
            
            AlertDialog.Builder(this@SettingsActivity)
                .setTitle("HTML Provider")
                .setSingleChoiceItems(options, currentIndex) { _, which ->
                    val newValue = when (which) {
                        1 -> getString(R.string.pref_htmlprovider_instapaper)
                        2 -> getString(R.string.pref_htmlprovider_textise)
                        else -> getString(R.string.pref_htmlprovider_original_url)
                    }
                    setHtmlProvider(newValue)
                    notifyDataSetChanged()
                }
                .show()
        }
        
        private fun showHtmlViewerDialog() {
            val options = arrayOf("Android Webview", "Custom Tab", "System Browser", "GeckoView")
            val currentIndex = when (Settings.getHtmlViewer(this@SettingsActivity)) {
                getString(R.string.pref_htmlviewer_customtabs) -> 1
                getString(R.string.pref_htmlviewer_browser) -> 2
                getString(R.string.pref_htmlviewer_geckoview) -> 3
                else -> 0
            }
            
            AlertDialog.Builder(this@SettingsActivity)
                .setTitle("HTML Viewer")
                .setSingleChoiceItems(options, currentIndex) { _, which ->
                    val newValue = when (which) {
                        1 -> getString(R.string.pref_htmlviewer_customtabs)
                        2 -> getString(R.string.pref_htmlviewer_browser)
                        3 -> getString(R.string.pref_htmlviewer_geckoview)
                        else -> getString(R.string.pref_htmlviewer_app)
                    }
                    setHtmlViewer(newValue)
                    notifyDataSetChanged()
                }
                .show()
        }
        
        private fun handleUserClick() {
            val userName = Settings.getUserName(this@SettingsActivity)
            if (userName.isNullOrEmpty()) {
                val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
                startActivityForResult(intent, REQUEST_LOGIN)
            } else {
                showLogoutDialog()
            }
        }
        
        private fun showLogoutDialog() {
            AlertDialog.Builder(this@SettingsActivity)
                .setTitle(R.string.logout_question_lbl)
                .setPositiveButton(R.string.ok) { _, _ ->
                    Settings.clearUserData(this@SettingsActivity)
                    notifyDataSetChanged()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        
        private fun getFontSizeSummary(): String {
            return when (Settings.getFontSize(this@SettingsActivity)) {
                getString(R.string.pref_fontsize_small) -> "Small"
                getString(R.string.pref_fontsize_big) -> "Big"
                else -> "Normal"
            }
        }
        
        private fun getHtmlProviderSummary(): String {
            return when (Settings.getHtmlProvider(this@SettingsActivity)) {
                getString(R.string.pref_htmlprovider_instapaper) -> "Instapaper"
                getString(R.string.pref_htmlprovider_textise) -> "Textise"
                else -> "Original URL"
            }
        }
        
        private fun getHtmlViewerSummary(): String {
            return when (Settings.getHtmlViewer(this@SettingsActivity)) {
                getString(R.string.pref_htmlviewer_customtabs) -> "Custom Tab"
                getString(R.string.pref_htmlviewer_browser) -> "System Browser"
                getString(R.string.pref_htmlviewer_geckoview) -> "GeckoView"
                else -> "Android Webview"
            }
        }
        
        private fun getUserSummary(): String {
            val userName = Settings.getUserName(this@SettingsActivity)
            return if (!userName.isNullOrEmpty()) userName else "Not logged in"
        }
        
        private fun setFontSize(value: String) {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(this@SettingsActivity)
            sharedPref.edit().putString(Settings.PREF_FONTSIZE, value).apply()
        }
        
        private fun setHtmlProvider(value: String) {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(this@SettingsActivity)
            sharedPref.edit().putString(Settings.PREF_HTMLPROVIDER, value).apply()
        }
        
        private fun setHtmlViewer(value: String) {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(this@SettingsActivity)
            sharedPref.edit().putString(Settings.PREF_HTMLVIEWER, value).apply()
        }
    }
    
    private data class SettingsItem(val title: String, val summary: String, val key: Int)
}