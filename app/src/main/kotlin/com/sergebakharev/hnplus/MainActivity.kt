package com.sergebakharev.hnplus

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.MenuItemCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sergebakharev.hnplus.databinding.MainBinding
import com.sergebakharev.hnplus.model.HNFeed
import com.sergebakharev.hnplus.model.HNFeedPost
import com.sergebakharev.hnplus.parser.BaseHTMLParser
import com.sergebakharev.hnplus.server.HNCredentials
import com.sergebakharev.hnplus.task.HNFeedTaskLoadMore
import com.sergebakharev.hnplus.task.HNFeedTaskMainFeed
import com.sergebakharev.hnplus.task.HNVoteTask
import com.sergebakharev.hnplus.task.ITaskFinishedHandler
import com.sergebakharev.hnplus.util.CustomTabActivityHelper
import com.sergebakharev.hnplus.util.FileUtil
import com.sergebakharev.hnplus.util.FontHelper
import java.lang.reflect.Field
import java.util.*
import android.content.DialogInterface

class MainActivity : BaseListActivity(), ITaskFinishedHandler<HNFeed?> {
    private lateinit var binding: MainBinding
    private lateinit var mInflater: LayoutInflater
    
    private var mEmptyListPlaceholder: TextView? = null
    private var mFeed: HNFeed = HNFeed()
    private var mPostsListAdapter: PostsAdapter? = null
    private var mUpvotedPosts: MutableSet<HNFeedPost> = mutableSetOf()
    private var mAlreadyRead: MutableSet<Int> = mutableSetOf()
    
    private var mCurrentFontSize: String? = null
    private var mFontSizeTitle = 0
    private var mFontSizeDetails = 0
    private var mTitleColor = 0
    private var mTitleReadColor = 0
    
    private val TASKCODE_LOAD_FEED = 10
    private val TASKCODE_LOAD_MORE_POSTS = 20
    private val TASKCODE_VOTE = 100
    
    private val LIST_STATE = "listState"
    private val ALREADY_READ_ARTICLES_KEY = "HN_ALREADY_READ"
    private var mListState: Parcelable? = null
    
    private var mShouldShowRefreshing = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Make sure that we show the overflow menu icon
        try {
            val config = ViewConfiguration.get(this)
            val menuKeyField = ViewConfiguration::class.java.getDeclaredField("sHasPermanentMenuKey")
            
            if (menuKeyField != null) {
                menuKeyField.isAccessible = true
                menuKeyField.setBoolean(config, false)
            }
        } catch (e: Exception) {
            // presumably, not relevant
        }
        
        mInflater = layoutInflater
        init()
    }
    
    private fun init() {
        mFeed = HNFeed()
        mPostsListAdapter = PostsAdapter()
        mUpvotedPosts = mutableSetOf()
        
        mEmptyListPlaceholder = getEmptyTextView(binding.mainRoot)
        binding.mainList.emptyView = mEmptyListPlaceholder
        binding.mainList.adapter = mPostsListAdapter
        
        mEmptyListPlaceholder?.typeface = FontHelper.getComfortaa(this, true)
        
        mTitleColor = resources.getColor(R.color.dark_gray_post_title, null)
        mTitleReadColor = resources.getColor(R.color.gray_post_title_read, null)
        
        toggleSwipeRefreshLayout()
        
        binding.mainSwiperefreshlayout.setOnRefreshListener {
            startFeedLoading()
        }
        
        loadAlreadyReadCache()
        loadIntermediateFeedFromStore()
        startFeedLoading()
    }
    
    override fun onResume() {
        super.onResume()
        
        val registeredUserChanged = mFeed.userAcquiredFor != null &&
                (mFeed.userAcquiredFor != Settings.getUserName(this))
        
        // We want to reload the feed if a new user logged in
        if (HNCredentials.isInvalidated || registeredUserChanged) {
            showFeed(HNFeed())
            startFeedLoading()
        }
        
        // refresh if font size changed
        if (refreshFontSizes()) {
            mPostsListAdapter?.notifyDataSetChanged()
        }
        
        // restore vertical scrolling position if applicable
        if (mListState != null) {
            binding.mainList.onRestoreInstanceState(mListState)
        }
        mListState = null
        
        // User may have toggled pull-down refresh, so toggle the SwipeRefreshLayout.
        toggleSwipeRefreshLayout()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }
    
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.menu_refresh)
        
        if (!mShouldShowRefreshing) {
            MenuItemCompat.setActionView(item, null)
        } else {
            val v = mInflater.inflate(R.layout.refresh_icon, null)
            MenuItemCompat.setActionView(item, v)
        }
        
        return super.onPrepareOptionsMenu(menu)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.menu_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            R.id.menu_refresh -> {
                startFeedLoading()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun toggleSwipeRefreshLayout() {
        binding.mainSwiperefreshlayout.isEnabled = Settings.isPullDownRefresh(this)
    }
    
    override fun onTaskFinished(taskCode: Int, code: ITaskFinishedHandler.TaskResultCode, result: HNFeed?, tag: Any?) {
        when (taskCode) {
            TASKCODE_LOAD_FEED -> {
                if (code == ITaskFinishedHandler.TaskResultCode.Success && mPostsListAdapter != null) {
                    showFeed(result)
                } else if (code != ITaskFinishedHandler.TaskResultCode.Success) {
                    Toast.makeText(this, getString(R.string.error_unable_to_retrieve_feed), Toast.LENGTH_SHORT).show()
                }
            }
            TASKCODE_LOAD_MORE_POSTS -> {
                if (code != ITaskFinishedHandler.TaskResultCode.Success || result == null || result.posts?.isEmpty() != false) {
                    Toast.makeText(this, getString(R.string.error_unable_to_load_more), Toast.LENGTH_SHORT).show()
                    mFeed.isLoadedMore = true // reached the end.
                }
                
                result?.let { mFeed.appendLoadMoreFeed(it) }
                mPostsListAdapter?.notifyDataSetChanged()
            }
        }
        
        setShowRefreshing(false)
    }
    
    private fun loadAlreadyReadCache() {
        if (mAlreadyRead == null) {
            mAlreadyRead = mutableSetOf()
        }
        
        val sharedPref = getSharedPreferences(ALREADY_READ_ARTICLES_KEY, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val read = sharedPref.all
        val now = Date().time
        
        for ((key, value) in read) {
            val readAt = value as Long
            val diff = (now - readAt) / (24 * 60 * 60 * 1000)
            if (diff >= 2) {
                editor.remove(key)
            } else {
                mAlreadyRead.add(key.hashCode())
            }
        }
        editor.apply()
    }
    
    private fun markAsRead(post: HNFeedPost) {
        val now = Date().time
        val title = post.title
        val editor = getSharedPreferences(ALREADY_READ_ARTICLES_KEY, Context.MODE_PRIVATE).edit()
        editor.putLong(title, now)
        editor.apply()
        
        mAlreadyRead.add(title.hashCode())
    }
    
    private fun showFeed(feed: HNFeed?) {
        Log.d("MainActivity", "showFeed called with feed: posts=${feed?.posts?.size}, userAcquiredFor=${feed?.userAcquiredFor}")
        feed?.let { mFeed = it }
        mPostsListAdapter?.notifyDataSetChanged()
    }
    
    private fun loadIntermediateFeedFromStore() {
        GetLastHNFeedTask().execute()
        val start = System.currentTimeMillis()
        
        Log.i("", "Loading intermediate feed took ms: ${System.currentTimeMillis() - start}")
    }
    
    inner class GetLastHNFeedTask : FileUtil.GetLastHNFeedTask() {
        private var progress: AlertDialog? = null
        
        override fun onPreExecute() {
            progress = AlertDialog.Builder(this@MainActivity)
                .setMessage("Loading")
                .setCancelable(false)
                .create()
            progress?.show()
        }
        
        override fun onPostExecute(result: HNFeed?) {
            progress?.dismiss()
            
            if (result != null && result.userAcquiredFor != null && 
                result.userAcquiredFor == Settings.getUserName(App.getInstance()!!)) {
                showFeed(result)
            }
        }
    }
    
    private fun startFeedLoading() {
        setShowRefreshing(true)
        HNFeedTaskMainFeed.startOrReattach(this, this, TASKCODE_LOAD_FEED)
    }
    
    private fun refreshFontSizes(): Boolean {
        val fontSize = Settings.getFontSize(this)
        if (mCurrentFontSize == null || mCurrentFontSize != fontSize) {
            mCurrentFontSize = fontSize
            mFontSizeTitle = when (fontSize) {
                getString(R.string.pref_fontsize_small) -> 15
                getString(R.string.pref_fontsize_normal) -> 18
                else -> 22
            }
            mFontSizeDetails = when (fontSize) {
                getString(R.string.pref_fontsize_small) -> 11
                getString(R.string.pref_fontsize_normal) -> 12
                else -> 15
            }
            return true
        }
        return false
    }
    
    private fun vote(voteURL: String?, post: HNFeedPost) {
        HNVoteTask.start(voteURL, this, VoteTaskFinishedHandler(), TASKCODE_VOTE, post)
    }
    
    override fun onRestoreInstanceState(state: Bundle) {
        super.onRestoreInstanceState(state)
        mListState = state.getParcelable(LIST_STATE)
    }
    
    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        mListState = binding.mainList.onSaveInstanceState()
        state.putParcelable(LIST_STATE, mListState)
    }
    
    inner class VoteTaskFinishedHandler : ITaskFinishedHandler<Boolean?> {
        override fun onTaskFinished(taskCode: Int, code: ITaskFinishedHandler.TaskResultCode, result: Boolean?, tag: Any?) {
            if (taskCode == TASKCODE_VOTE) {
                if (result == true) {
                    Toast.makeText(this@MainActivity, R.string.vote_success, Toast.LENGTH_SHORT).show()
                    val post = tag as? HNFeedPost
                    if (post != null) {
                        mUpvotedPosts.add(post)
                    }
                } else {
                    Toast.makeText(this@MainActivity, R.string.vote_error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    inner class PostsAdapter : BaseAdapter() {
        private val VIEWTYPE_POST = 0
        private val VIEWTYPE_LOADMORE = 1
        private val HACKERNEWS_URLDOMAIN = "news.ycombinator.com"
        
        override fun getCount(): Int {
            val posts = mFeed.posts?.size ?: 0
            Log.d("MainActivity", "Adapter getCount: posts=$posts, isLoadedMore=${mFeed.isLoadedMore}")
            return if (posts == 0) 0 else posts + (if (mFeed.isLoadedMore) 0 else 1)
        }
        
        override fun getItem(position: Int): HNFeedPost? {
            return if (getItemViewType(position) == VIEWTYPE_POST) {
                mFeed.posts?.get(position)
            } else {
                null
            }
        }
        
        override fun getItemId(position: Int): Long = 0
        
        override fun getItemViewType(position: Int): Int {
            return if (position < (mFeed.posts?.size ?: 0)) VIEWTYPE_POST else VIEWTYPE_LOADMORE
        }
        
        override fun getViewTypeCount(): Int = 2
        
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return when (getItemViewType(position)) {
                VIEWTYPE_POST -> {
                    val view = convertView ?: mInflater.inflate(R.layout.main_list_item, null).apply {
                        tag = PostViewHolder().apply {
                            titleView = findViewById(R.id.main_list_item_title)
                            urlView = findViewById(R.id.main_list_item_url)
                            textContainer = findViewById(R.id.main_list_item_textcontainer)
                            commentsButton = findViewById(R.id.main_list_item_comments_button)
                            commentsButton?.typeface = FontHelper.getComfortaa(this@MainActivity, false)
                            pointsView = findViewById(R.id.main_list_item_points)
                            pointsView?.typeface = FontHelper.getComfortaa(this@MainActivity, true)
                        }
                    }
                    
                    val item = getItem(position)
                    val holder = view.tag as PostViewHolder
                    
                    Log.d("MainActivity", "Binding item at position $position: title='${item?.title}', comments=${item?.commentsCount}, points=${item?.points}")
                    
                    holder.titleView?.textSize = mFontSizeTitle.toFloat()
                    holder.titleView?.text = item?.title
                    holder.titleView?.setTextColor(if (isRead(item)) mTitleReadColor else mTitleColor)
                    Log.d("MainActivity", "Set title text to: '${item?.title}', textView: ${holder.titleView}")
                    
                    holder.urlView?.textSize = mFontSizeDetails.toFloat()
                    holder.urlView?.text = item?.uRLDomain
                    
                    holder.pointsView?.textSize = mFontSizeDetails.toFloat()
                    holder.pointsView?.text = if (item?.points != BaseHTMLParser.UNDEFINED) item?.points.toString() else "-"
                    
                    holder.commentsButton?.textSize = mFontSizeTitle.toFloat()
                    if (item?.commentsCount != BaseHTMLParser.UNDEFINED) {
                        holder.commentsButton?.visibility = View.VISIBLE
                        holder.commentsButton?.text = item?.commentsCount.toString()
                        Log.d("MainActivity", "Set comments button text to: '${item?.commentsCount}', button: ${holder.commentsButton}")
                    } else {
                        holder.commentsButton?.visibility = View.INVISIBLE
                        Log.d("MainActivity", "Comments button set to INVISIBLE")
                    }
                    
                    holder.commentsButton?.setOnClickListener {
                        startCommentActivity(position)
                    }
                    
                    holder.textContainer?.setOnClickListener {
                        item?.let { markAsRead(it) }
                        if (getItem(position)?.uRLDomain == HACKERNEWS_URLDOMAIN) {
                            startCommentActivity(position)
                        } else {
                            when (Settings.getHtmlViewer(this@MainActivity)) {
                                getString(R.string.pref_htmlviewer_browser) -> {
                                    getItem(position)?.let { post ->
                                        openURLInBrowser(getArticleViewURL(post), this@MainActivity)
                                    }
                                }
                                getString(R.string.pref_htmlviewer_customtabs) -> {
                                    openURLInCustomTabs(getItem(position), null, this@MainActivity)
                                }
                                getString(R.string.pref_htmlviewer_geckoview) -> {
                                    openPostInGeckoView(getItem(position), null, this@MainActivity, false)
                                }
                                else -> {
                                    openPostInApp(getItem(position), null, this@MainActivity)
                                }
                            }
                        }
                    }
                    
                    holder.textContainer?.setOnLongClickListener {
                        val post = getItem(position)
                        if (post != null) {
                            val builder = AlertDialog.Builder(this@MainActivity)
                            val adapter = LongPressMenuListAdapter(post)
                            builder.setAdapter(adapter, DialogInterface.OnClickListener { _, which ->
                                adapter.onItemClick(which)
                            }).show()
                        }
                        true
                    }
                    
                    view
                }
                VIEWTYPE_LOADMORE -> {
                    val view = mInflater.inflate(R.layout.main_list_item_loadmore, null)
                    val textView = view.findViewById<TextView>(R.id.main_list_item_loadmore_text)
                    textView.typeface = FontHelper.getComfortaa(this@MainActivity, true)
                    val imageView = view.findViewById<ImageView>(R.id.main_list_item_loadmore_loadingimage)
                    
                    if (HNFeedTaskLoadMore.isRunning(this@MainActivity, TASKCODE_LOAD_MORE_POSTS)) {
                        textView.visibility = View.INVISIBLE
                        imageView.visibility = View.VISIBLE
                        view.isClickable = false
                    }
                    
                    view.setOnClickListener {
                        textView.visibility = View.INVISIBLE
                        imageView.visibility = View.VISIBLE
                        view.isClickable = false
                        HNFeedTaskLoadMore.start(this@MainActivity, this@MainActivity, mFeed, TASKCODE_LOAD_MORE_POSTS)
                        setShowRefreshing(true)
                    }
                    
                    view
                }
                else -> convertView ?: View(this@MainActivity)
            }
        }
        
        private fun isRead(post: HNFeedPost?): Boolean {
            return post?.title?.hashCode()?.let { mAlreadyRead.contains(it) } ?: false
        }
        
        private fun startCommentActivity(position: Int) {
            val post = getItem(position)
            Log.d("MainActivity", "Starting CommentsActivity with post: $post, postID: ${post?.postID}, title: ${post?.title}")
            val i = Intent(this@MainActivity, CommentsActivity::class.java)
            i.putExtra(CommentsActivity.EXTRA_HNPOST, post)
            startActivity(i)
        }
    }
    
    inner class LongPressMenuListAdapter(private val mPost: HNFeedPost) : ListAdapter {
        private val mIsLoggedIn: Boolean = Settings.isUserLoggedIn(this@MainActivity)
        private val mUpVotingEnabled: Boolean = !mIsLoggedIn || 
            (mPost.getUpvoteURL(Settings.getUserName(this@MainActivity)) != null && !mUpvotedPosts.contains(mPost))
        private val mItems: ArrayList<CharSequence> = ArrayList()
        
        init {
            if (mUpVotingEnabled) {
                mItems.add(getString(R.string.upvote))
            } else {
                mItems.add(getString(R.string.already_upvoted))
            }
            mItems.addAll(listOf(
                getString(R.string.pref_htmlprovider_original_url),
                getString(R.string.pref_htmlprovider_instapaper),
                getString(R.string.pref_htmlprovider_textise),
                getString(R.string.external_browser),
                getString(R.string.share_article_url)
            ))
        }
        
        override fun getCount(): Int = mItems.size
        override fun getItem(position: Int): CharSequence = mItems[position]
        override fun getItemId(position: Int): Long = 0
        override fun getItemViewType(position: Int): Int = 0
        override fun getViewTypeCount(): Int = 1
        override fun hasStableIds(): Boolean = false
        override fun isEmpty(): Boolean = false
        override fun registerDataSetObserver(observer: android.database.DataSetObserver) {}
        override fun unregisterDataSetObserver(observer: android.database.DataSetObserver) {}
        override fun areAllItemsEnabled(): Boolean = false
        override fun isEnabled(position: Int): Boolean = !(!mUpVotingEnabled && position == 4)
        
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = mInflater.inflate(android.R.layout.simple_list_item_1, null) as TextView
            view.text = getItem(position)
            if (!mUpVotingEnabled && position == 0) {
                view.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            }
            return view
        }
        
        fun onItemClick(item: Int) {
            when (item) {
                0 -> {
                    if (!mIsLoggedIn) {
                        Toast.makeText(this@MainActivity, R.string.please_log_in, Toast.LENGTH_LONG).show()
                    } else if (mUpVotingEnabled) {
                        vote(mPost.getUpvoteURL(Settings.getUserName(this@MainActivity)), mPost)
                    }
                }
                1, 2, 3, 4 -> {
                    openPostInApp(mPost, getItem(item).toString(), this@MainActivity)
                    markAsRead(mPost)
                }
                5 -> {
                    openURLInBrowser(getArticleViewURL(mPost), this@MainActivity)
                    markAsRead(mPost)
                }
                6 -> {
                    shareUrl(mPost, this@MainActivity)
                }
            }
        }
    }
    
    private fun getArticleViewURL(post: HNFeedPost?): String {
        return ArticleReaderActivity.getArticleViewURL(post, Settings.getHtmlProvider(this), this)
    }
    
    companion object {
        fun openURLInBrowser(url: String, a: android.app.Activity) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            a.startActivity(browserIntent)
        }
        
        fun openURLInCustomTabs(post: HNFeedPost?, overrideHtmlProvider: String?, a: android.app.Activity) {
            post?.let { nonNullPost ->
                val commentsIntent = Intent(a, CommentsActivity::class.java)
                commentsIntent.putExtra(CommentsActivity.EXTRA_HNPOST, nonNullPost)
                
                var flags = PendingIntent.FLAG_CANCEL_CURRENT
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    flags = flags or PendingIntent.FLAG_IMMUTABLE
                }
                
                val pendingIntent = PendingIntent.getActivity(a, 1, commentsIntent, flags)
                val icon = BitmapFactory.decodeResource(a.resources, R.drawable.ic_launcher)
                
                val builder = CustomTabsIntent.Builder()
                builder.setActionButton(icon, a.getString(R.string.comments), pendingIntent)
                val customTabsIntent = builder.build()
                CustomTabActivityHelper.openCustomTab(a, customTabsIntent, nonNullPost, overrideHtmlProvider, ArticleReaderActivity())
            }
        }
        
        fun openPostInApp(post: HNFeedPost?, overrideHtmlProvider: String?, a: android.app.Activity) {
            val i = Intent(a, ArticleReaderActivity::class.java)
            i.putExtra(ArticleReaderActivity.EXTRA_HNPOST, post)
            if (overrideHtmlProvider != null) {
                i.putExtra(ArticleReaderActivity.EXTRA_HTMLPROVIDER_OVERRIDE, overrideHtmlProvider)
            }
            a.startActivity(i)
        }
        
        fun openPostInGeckoView(post: HNFeedPost?, overrideHtmlProvider: String?, a: android.app.Activity, cameFromComments: Boolean = false) {
            val i = Intent(a, GeckoViewActivity::class.java)
            i.putExtra(GeckoViewActivity.EXTRA_HNPOST, post)
            if (overrideHtmlProvider != null) {
                i.putExtra(GeckoViewActivity.EXTRA_HTMLPROVIDER_OVERRIDE, overrideHtmlProvider)
            }
            i.putExtra(GeckoViewActivity.EXTRA_CAME_FROM_COMMENTS, cameFromComments)
            a.startActivity(i)
        }
        
        fun shareUrl(post: HNFeedPost?, a: android.app.Activity) {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, post?.title)
            shareIntent.putExtra(Intent.EXTRA_TEXT, post?.uRL ?: "")
            a.startActivity(Intent.createChooser(shareIntent, a.getString(R.string.share_article_url)))
        }
    }
    
    private fun setShowRefreshing(showRefreshing: Boolean) {
        if (!Settings.isPullDownRefresh(this)) {
            mShouldShowRefreshing = showRefreshing
            supportInvalidateOptionsMenu()
        }
        
        if (binding.mainSwiperefreshlayout.isEnabled && (!binding.mainSwiperefreshlayout.isRefreshing || !showRefreshing)) {
            binding.mainSwiperefreshlayout.isRefreshing = showRefreshing
        }
    }
    
    data class PostViewHolder(
        var titleView: TextView? = null,
        var urlView: TextView? = null,
        var pointsView: TextView? = null,
        var commentsCountView: TextView? = null,
        var textContainer: LinearLayout? = null,
        var commentsButton: Button? = null
    )
}
