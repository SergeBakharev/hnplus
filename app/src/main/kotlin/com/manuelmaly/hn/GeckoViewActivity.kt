package com.manuelmaly.hn

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuItemCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.manuelmaly.hn.databinding.GeckoviewActivityBinding
import com.manuelmaly.hn.model.HNFeedPost
import com.manuelmaly.hn.util.CustomTabActivityHelper
import com.manuelmaly.hn.util.FontHelper
import com.manuelmaly.hn.util.SpotlightActivity
import com.manuelmaly.hn.util.ViewedUtils
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import java.net.URLEncoder

class GeckoViewActivity : AppCompatActivity(), CustomTabActivityHelper.CustomTabFallback {
    private lateinit var binding: GeckoviewActivityBinding
    private lateinit var mInflater: LayoutInflater
    
    private var mActionbarTitle: TextView? = null
    private var mPost: HNFeedPost? = null
    private var mHtmlProvider: String? = null
    private var mShouldShowRefreshing = false
    private var mGeckoSession: GeckoSession? = null
    private var mGeckoRuntime: GeckoRuntime? = null
    private var mCameFromComments = false
    
    private val ACTIVITY_LOGIN = 137
    
    companion object {
        const val EXTRA_HNPOST = "HNPOST"
        const val EXTRA_HTMLPROVIDER_OVERRIDE = "HTMLPROVIDER_OVERRIDE"
        const val EXTRA_CAME_FROM_COMMENTS = "CAME_FROM_COMMENTS"
        
        private const val HTMLPROVIDER_PREFIX_INSTAPAPER = "https://www.instapaper.com/text?u="
        private const val HTMLPROVIDER_PRFIX_TEXTISE = "https://www.textise.net/showText.aspx?strURL="
        
        @Suppress("DEPRECATION")
        fun getArticleViewURL(post: HNFeedPost?, htmlProvider: String?, c: Context): String {
            if (post?.uRL == null) return ""
            
            val encodedURL = URLEncoder.encode(post.uRL!!)
            return when (htmlProvider) {
                c.getString(R.string.pref_htmlprovider_instapaper) -> HTMLPROVIDER_PREFIX_INSTAPAPER + encodedURL
                c.getString(R.string.pref_htmlprovider_textise) -> HTMLPROVIDER_PRFIX_TEXTISE + encodedURL
                else -> post.uRL!!
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GeckoviewActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        mInflater = layoutInflater
        init()
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun init() {
        mActionbarTitle = supportActionBar?.customView?.findViewById(R.id.actionbar_title)
        
        // NOTE: The generic version of getSerializableExtra requires minSdk 33+.
        // Keeping the deprecated method for broader compatibility.
        mPost = intent.getSerializableExtra(EXTRA_HNPOST) as? HNFeedPost
        mCameFromComments = intent.getBooleanExtra(EXTRA_CAME_FROM_COMMENTS, false)
        
        // Initialize GeckoView with uBlock Origin extension
        initGeckoView()
        
        if (mPost != null && mPost?.uRL != null) {
            val htmlProviderOverride = intent.getStringExtra(EXTRA_HTMLPROVIDER_OVERRIDE)
            mHtmlProvider = htmlProviderOverride ?: Settings.getHtmlProvider(this)
            mHtmlProvider?.let { provider ->
                val url = getArticleViewURL(mPost, provider, this)
                mGeckoSession?.loadUri(url)
            }
        }
        
        toggleSwipeRefreshLayout()
        
        binding.geckoviewSwiperefreshlayout.setOnRefreshListener {
            mHtmlProvider?.let { provider ->
                val url = getArticleViewURL(mPost, provider, this)
                mGeckoSession?.loadUri(url)
            }
        }
    }
    
    private fun initGeckoView() {
        // Create GeckoSession
        mGeckoSession = GeckoSession()
        // Get or create GeckoRuntime
        mGeckoRuntime = GeckoRuntime.getDefault(this)
        // Open session with runtime
        mGeckoRuntime?.let { runtime ->
            mGeckoSession?.open(runtime)
        }
        // Install uBlock Origin and assign a PromptDelegate
        mGeckoRuntime?.webExtensionController?.promptDelegate = BlockifyPromptDelegate()
        mGeckoRuntime?.webExtensionController
            ?.install("resource://android/assets/extensions/uBlock0_1.64.0.firefox.signed.xpi")
            ?.accept(
                { extension ->
                    Log.i("MessageDelegate", "Extension installed: $extension")
                },
                { e ->
                    Log.e("MessageDelegate", "Error registering WebExtension", e)
                }
            )
        // Set session to GeckoView
        mGeckoSession?.let { session ->
            binding.geckoview.setSession(session)
        }
        // Set up progress delegate to handle loading states
        mGeckoSession?.setProgressDelegate(object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                setShowRefreshing(true)
            }
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                setShowRefreshing(false)
            }
        })
    }

// Add the BlockifyPromptDelegate class
class BlockifyPromptDelegate : WebExtensionController.PromptDelegate {
    override fun onInstallPromptRequest(
        extension: WebExtension,
        permissions: Array<out String?>,
        origins: Array<out String?>
    ): GeckoResult<WebExtension.PermissionPromptResponse?>? {
        val name = extension.metaData.name
        if (name != null && name == "uBlock Origin") {
            Log.i("PromptDelegate", "Allow uBlock Origin")
            return GeckoResult.fromValue(WebExtension.PermissionPromptResponse(true, true))
        }
        return super.onInstallPromptRequest(extension, permissions, origins)
    }
}
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_share_refresh, menu)
        
        val refreshItem = menu.findItem(R.id.menu_refresh)
        refreshItem?.let { item ->
            val actionView = item.actionView
            if (actionView != null) {
                actionView.setOnClickListener {
                    mHtmlProvider?.let { provider ->
                        val url = getArticleViewURL(mPost, provider, this)
                        mGeckoSession?.loadUri(url)
                    }
                }
            }
        }
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                handleBackNavigation()
                true
            }
            R.id.menu_refresh -> {
                mHtmlProvider?.let { provider ->
                    val url = getArticleViewURL(mPost, provider, this)
                    mGeckoSession?.loadUri(url)
                }
                true
            }
            R.id.menu_share -> {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, mPost?.title)
                shareIntent.putExtra(Intent.EXTRA_TEXT, mPost?.uRL ?: "")
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_article_url)))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun handleBackNavigation() {
        if (mCameFromComments) {
            // If we came from CommentsActivity, go back to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        } else {
            // Normal back navigation
            finish()
        }
    }
    
    private fun toggleSwipeRefreshLayout() {
        binding.geckoviewSwiperefreshlayout.isEnabled = Settings.isPullDownRefresh(this)
    }
    
    override fun onBackPressed() {
        // Note: canGoBack() is not available in this version of GeckoView
        // We'll just handle back navigation normally
        handleBackNavigation()
    }
    
    override fun onDestroy() {
        // Clean up GeckoView resources
        mGeckoSession?.close()
        super.onDestroy()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ACTIVITY_LOGIN -> {
                if (resultCode == RESULT_OK) {
                    launchCommentsActivity()
                }
            }
        }
    }
    
    private fun launchCommentsActivity() {
        val i = Intent(this, CommentsActivity::class.java)
        i.putExtra(CommentsActivity.EXTRA_HNPOST, mPost)
        intent.getStringExtra(EXTRA_HTMLPROVIDER_OVERRIDE)?.let { override ->
            i.putExtra(EXTRA_HTMLPROVIDER_OVERRIDE, override)
        }
        startActivity(i)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
    
    private fun setShowRefreshing(showRefreshing: Boolean) {
        if (!Settings.isPullDownRefresh(this)) {
            mShouldShowRefreshing = showRefreshing
            supportInvalidateOptionsMenu()
        }
        
        if (binding.geckoviewSwiperefreshlayout.isEnabled && (!binding.geckoviewSwiperefreshlayout.isRefreshing || !showRefreshing)) {
            binding.geckoviewSwiperefreshlayout.isRefreshing = showRefreshing
        }
    }
    
    override fun openUri(activity: android.app.Activity, post: HNFeedPost?, overrideHtmlProvider: String?) {
        val i = Intent(activity, GeckoViewActivity::class.java)
        i.putExtra(EXTRA_HNPOST, post)
        if (overrideHtmlProvider != null) {
            i.putExtra(EXTRA_HTMLPROVIDER_OVERRIDE, overrideHtmlProvider)
        }
        activity.startActivity(i)
    }
} 