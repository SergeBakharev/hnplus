package com.manuelmaly.hn

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuItemCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.manuelmaly.hn.databinding.ArticleActivityBinding
import com.manuelmaly.hn.model.HNFeedPost
import com.manuelmaly.hn.util.CustomTabActivityHelper
import com.manuelmaly.hn.util.FontHelper
import com.manuelmaly.hn.util.SpotlightActivity
import com.manuelmaly.hn.util.ViewedUtils
import java.net.URLEncoder

class ArticleReaderActivity : AppCompatActivity(), CustomTabActivityHelper.CustomTabFallback {
    private lateinit var binding: ArticleActivityBinding
    private lateinit var mInflater: LayoutInflater
    
    private var mActionbarTitle: TextView? = null
    private var mPost: HNFeedPost? = null
    private var mHtmlProvider: String? = null
    private var mShouldShowRefreshing = false
    private var mWebViewSavedState: Bundle? = null
    private var mWebViewIsLoading = false
    private var mCameFromComments = false
    
    private val ACTIVITY_LOGIN = 137
    private val WEB_VIEW_SAVED_STATE_KEY = "webViewSavedState"
    
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
        binding = ArticleActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        if (savedInstanceState != null) {
            mWebViewSavedState = savedInstanceState.getBundle(WEB_VIEW_SAVED_STATE_KEY)
        }
        
        mInflater = layoutInflater
        init()
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun init() {
        mActionbarTitle = supportActionBar?.customView?.findViewById(R.id.actionbar_title)
        
        mPost = intent.getSerializableExtra(EXTRA_HNPOST) as? HNFeedPost
        mCameFromComments = intent.getBooleanExtra(EXTRA_CAME_FROM_COMMENTS, false)
        
        if (mPost != null && mPost?.uRL != null) {
            val htmlProviderOverride = intent.getStringExtra(EXTRA_HTMLPROVIDER_OVERRIDE)
            mHtmlProvider = htmlProviderOverride ?: Settings.getHtmlProvider(this)
            mHtmlProvider?.let { provider ->
                binding.articleWebview.loadUrl(getArticleViewURL(mPost, provider, this))
            }
        }
        
        binding.articleWebview.settings.apply {
            builtInZoomControls = true
            loadWithOverviewMode = true
            useWideViewPort = true
            javaScriptEnabled = true
        }
        binding.articleWebview.webViewClient = HNReaderWebViewClient()
        
        mWebViewSavedState?.let { state ->
            binding.articleWebview.restoreState(state)
        }
        
        toggleSwipeRefreshLayout()
        
        binding.articleSwiperefreshlayout.setOnRefreshListener {
            mHtmlProvider?.let { provider ->
                binding.articleWebview.loadUrl(getArticleViewURL(mPost, provider, this))
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        mActionbarTitle?.typeface = FontHelper.getComfortaa(this, true)
        mActionbarTitle?.text = getString(R.string.article)
        mActionbarTitle?.setOnClickListener {
            launchCommentsActivity()
        }
        
        if (!ViewedUtils.getActivityViewed(this)) {
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                val posArray = IntArray(2)
                mActionbarTitle?.getLocationInWindow(posArray)
                val intent = SpotlightActivity.intentForSpotlightActivity(
                    this, posArray[0].toFloat(), (mActionbarTitle?.width ?: 0).toFloat(),
                    0f, (supportActionBar?.height ?: 0).toFloat(), getString(R.string.click_on_article)
                )
                startActivityForResult(intent, ACTIVITY_LOGIN)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }, 250)
            ViewedUtils.setActivityViewed(this)
        }
        
        // User may have toggled pull-down refresh, so toggle the SwipeRefreshLayout.
        toggleSwipeRefreshLayout()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_share_refresh, menu)
        return super.onCreateOptionsMenu(menu)
    }
    
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val refreshItem = menu.findItem(R.id.menu_refresh)
        
        if (!mShouldShowRefreshing) {
            MenuItemCompat.setActionView(refreshItem, null)
        } else {
            val refreshView = mInflater.inflate(R.layout.refresh_icon, null)
            MenuItemCompat.setActionView(refreshItem, refreshView)
        }
        
        return super.onPrepareOptionsMenu(menu)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                handleBackNavigation()
                true
            }
            R.id.menu_refresh -> {
                if (mWebViewIsLoading) {
                    binding.articleWebview.stopLoading()
                } else {
                    mHtmlProvider?.let { provider ->
                        binding.articleWebview.loadUrl(getArticleViewURL(mPost, provider, this))
                    }
                }
                true
            }
            R.id.menu_share -> {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, mPost?.title)
                shareIntent.putExtra(Intent.EXTRA_TEXT, binding.articleWebview.url ?: "")
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
        binding.articleSwiperefreshlayout.isEnabled = Settings.isPullDownRefresh(this)
    }
    
    override fun onBackPressed() {
        if (binding.articleWebview.canGoBack()) {
            binding.articleWebview.goBack()
        } else {
            handleBackNavigation()
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        val webViewSavedState = Bundle()
        binding.articleWebview.saveState(webViewSavedState)
        outState.putBundle(WEB_VIEW_SAVED_STATE_KEY, webViewSavedState)
        super.onSaveInstanceState(outState)
    }
    
    override fun onDestroy() {
        binding.articleWebview.loadData("", "text/html", "utf-8") // Destroy any players (e.g. Youtube, Soundcloud) if any
        // Calling binding.articleWebview.destroy(); would not always work according to here:
        // http://stackoverflow.com/questions/6201615/how-do-i-stop-flash-after-leaving-a-webview?rq=1
        
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
        
        if (binding.articleSwiperefreshlayout.isEnabled && (!binding.articleSwiperefreshlayout.isRefreshing || !showRefreshing)) {
            binding.articleSwiperefreshlayout.isRefreshing = showRefreshing
        }
    }
    
    override fun openUri(activity: android.app.Activity, post: HNFeedPost?, overrideHtmlProvider: String?) {
        val i = Intent(activity, ArticleReaderActivity::class.java)
        i.putExtra(EXTRA_HNPOST, post)
        if (overrideHtmlProvider != null) {
            i.putExtra(EXTRA_HTMLPROVIDER_OVERRIDE, overrideHtmlProvider)
        }
        activity.startActivity(i)
    }
    
    inner class HNReaderWebViewClient : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            setShowRefreshing(true)
            mWebViewIsLoading = true
            super.onPageStarted(view, url, favicon)
        }
        
        override fun onPageFinished(view: WebView?, url: String?) {
            setShowRefreshing(false)
            mWebViewIsLoading = false
            super.onPageFinished(view, url)
        }
        
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            url?.let { nonNullUrl ->
                view?.loadUrl(nonNullUrl)
            }
            return true
        }
    }
}
