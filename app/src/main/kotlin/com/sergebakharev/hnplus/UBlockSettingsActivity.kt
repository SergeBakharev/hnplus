package com.sergebakharev.hnplus

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sergebakharev.hnplus.databinding.UblockSettingsActivityBinding
import com.sergebakharev.hnplus.util.FontHelper
import com.sergebakharev.hnplus.util.installActionBarBackOnSwipe
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession

class UBlockSettingsActivity : AppCompatActivity() {
    private lateinit var binding: UblockSettingsActivityBinding
    private var mActionbarTitle: TextView? = null
    private var mGeckoSession: GeckoSession? = null
    private var mCanGoBack = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UblockSettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mActionbarTitle = supportActionBar?.customView?.findViewById(R.id.actionbar_title)
        mActionbarTitle?.typeface = FontHelper.getComfortaa(this, true)
        mActionbarTitle?.text = getString(R.string.settings_ublock)

        installActionBarBackOnSwipe { handleBackNavigation() }

        val session = GeckoSession()
        mGeckoSession = session
        val runtime = GeckoRuntime.getDefault(this)
        session.open(runtime)
        binding.geckoview.setSession(session)
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                mCanGoBack = canGoBack
            }
        }

        UBlockOrigin.ensureInstalled(runtime).accept({ extension ->
            if (isDestroyed || extension == null) return@accept
            val url = UBlockOrigin.optionsPageUrl(extension)
            if (url != null) {
                mGeckoSession?.loadUri(url)
            } else {
                showOpenError()
            }
        }, { error ->
            Log.e(TAG, "Could not open uBlock Origin settings", error)
            if (!isDestroyed) {
                showOpenError()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                handleBackNavigation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        mGeckoSession?.close()
        mGeckoSession = null
        super.onDestroy()
    }

    private fun handleBackNavigation() {
        if (mCanGoBack) {
            mGeckoSession?.goBack()
        } else {
            finish()
        }
    }

    private fun showOpenError() {
        Toast.makeText(this, R.string.error_ublock_settings, Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        private const val TAG = "UBlockSettings"
    }
}
