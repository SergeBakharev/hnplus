package com.sergebakharev.hnplus

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.text.Editable
import android.text.Html
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.MenuItemCompat
import com.sergebakharev.hnplus.databinding.CommentsActivityBinding
import com.sergebakharev.hnplus.login.LoginActivity
import com.sergebakharev.hnplus.model.HNComment
import com.sergebakharev.hnplus.model.HNCommentTreeNode
import com.sergebakharev.hnplus.model.HNFeedPost
import com.sergebakharev.hnplus.model.HNPostComments
import com.sergebakharev.hnplus.reuse.LinkifiedTextView
import com.sergebakharev.hnplus.task.HNCommentDeleteTask
import com.sergebakharev.hnplus.task.HNCommentPostTask
import com.sergebakharev.hnplus.task.HNPostCommentsTask
import com.sergebakharev.hnplus.task.HNVoteTask
import com.sergebakharev.hnplus.task.ITaskFinishedHandler
import com.sergebakharev.hnplus.util.DisplayHelper
import com.sergebakharev.hnplus.util.FileUtil
import com.sergebakharev.hnplus.util.FontHelper
import com.sergebakharev.hnplus.util.SpotlightActivity
import com.sergebakharev.hnplus.util.ViewedUtils
import com.sergebakharev.hnplus.util.installActionBarBackOnSwipe
import java.util.*
import android.content.DialogInterface

class CommentsActivity : BaseListActivity(), ITaskFinishedHandler<HNPostComments?> {
    private lateinit var binding: CommentsActivityBinding
    private lateinit var mInflater: LayoutInflater
    
    private var mCommentHeader: LinearLayout? = null
    private var mCommentHeaderText: TextView? = null
    private var mEmptyView: TextView? = null
    private var mActionbarTitle: TextView? = null
    
    private var mPost: HNFeedPost? = null
    private var mComments: HNPostComments = HNPostComments()
    private var mCommentsListAdapter: CommentsAdapter? = null
    private var mHaveLoadedPosts = false
    
    private var mCurrentFontSize: String? = null
    private var mFontSizeText = 0
    private var mFontSizeMetadata = 0
    private var mCommentLevelIndentPx = 0
    
    private var mUpvotedComments: MutableSet<HNComment> = mutableSetOf()
    
    private val LIST_STATE = "listState"
    private var mListState: Parcelable? = null
    
    private var mPendingVote: HNComment? = null
    private var mVotedComments: MutableSet<HNComment> = mutableSetOf()
    private var mPendingReplyTo: HNComment? = null
    private var mPendingAddComment = false
    private var mShowComposeAfterLoad = false
    private var mIsPostingComment = false
    private var mIsComposing = false
    private var mComposeParentId: String? = null
    private var mComposeDraft = ""
    
    private var mShouldShowRefreshing = false
    
    private val TASKCODE_VOTE = 100
    private val TASKCODE_POST_COMMENT = 101
    private val TASKCODE_DELETE_COMMENT = 102
    private val ACTIVITY_LOGIN = 136
    private val ACTIVITY_SPOTLIGHT = 137
    
    companion object {
        const val EXTRA_HNPOST = "HNPOST"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CommentsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        mInflater = layoutInflater
        installActionBarBackOnSwipe()
        init()
    }
    
    private fun init() {
        mPost = intent.getSerializableExtra(EXTRA_HNPOST) as? HNFeedPost
        Log.d("CommentsActivity", "Received post: $mPost, postID: ${mPost?.postID}, title: ${mPost?.title}")
        if (mPost == null || mPost?.postID == null) {
            Toast.makeText(this, "The belonging post has not been loaded", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        mCommentLevelIndentPx = minOf(DisplayHelper.getScreenHeight(this), DisplayHelper.getScreenWidth(this)) / 30
        
        initCommentsHeader()
        mComments = HNPostComments()
        mVotedComments = mutableSetOf()
        mCommentsListAdapter = CommentsAdapter()
        mEmptyView = getEmptyTextView(binding.commentsRoot)
        binding.commentsList.emptyView = mEmptyView
        // Add the header for "Ask HN" text. If there is no text, this will just be empty
        binding.commentsList.addHeaderView(mCommentHeader, null, false)
        binding.commentsList.itemsCanFocus = true
        binding.commentsList.adapter = mCommentsListAdapter
        
        mActionbarTitle = supportActionBar?.customView?.findViewById(R.id.actionbar_title)
        
        // Set the action bar title immediately
        mActionbarTitle?.typeface = FontHelper.getComfortaa(this, true)
        mActionbarTitle?.text = getString(R.string.comments)
        mActionbarTitle?.setOnClickListener {
            when (Settings.getHtmlViewer(this)) {
                getString(R.string.pref_htmlviewer_browser) -> {
                    mPost?.uRL?.let { MainActivity.openURLInBrowser(it, this) }
                }
                else -> {
                    openArticleReader()
                }
            }
        }
        
        toggleSwipeRefreshLayout()
        
        binding.commentsSwiperefreshlayout.setOnRefreshListener {
            startFeedLoading()
        }
        
        loadIntermediateCommentsFromStore()
        startFeedLoading()
    }
    
    override fun onResume() {
        super.onResume()
        
        // refresh if font size changed
        if (refreshFontSizes()) {
            mCommentsListAdapter?.notifyDataSetChanged()
        }
        
        // restore vertical scrolling position if applicable
        if (mListState != null) {
            binding.commentsList.onRestoreInstanceState(mListState)
        }
        mListState = null
        
        // Only show the spotlight effect the first time
        if (!ViewedUtils.getActivityViewed(this)) {
            val handler = Handler(Looper.getMainLooper())
            // If we don't delay this there are weird race conditions
            handler.postDelayed({
                showCommentsSpotlight()
                ViewedUtils.setActivityViewed(this)
            }, 250)
        }
        
        // User may have toggled pull-down refresh, so toggle the SwipeRefreshLayout.
        toggleSwipeRefreshLayout()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_comments, menu)
        return super.onCreateOptionsMenu(menu)
    }
    
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val refreshItem = menu.findItem(R.id.menu_refresh)
        
        if (mShouldShowRefreshing) {
            val refreshView = mInflater.inflate(R.layout.refresh_icon, null)
            MenuItemCompat.setActionView(refreshItem, refreshView)
        } else {
            MenuItemCompat.setActionView(refreshItem, null)
        }
        
        return super.onPrepareOptionsMenu(menu)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_refresh -> {
                startFeedLoading()
                true
            }
            android.R.id.home -> {
                // Go back to MainActivity
                finish()
                true
            }
            R.id.menu_add_comment -> {
                requestCompose(null)
                true
            }
            R.id.menu_share -> {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "${mPost?.title} | Hacker News")
                shareIntent.putExtra(Intent.EXTRA_TEXT, "https://news.ycombinator.com/item?id=${mPost?.postID}")
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_comments_url)))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun toggleSwipeRefreshLayout() {
        binding.commentsSwiperefreshlayout.isEnabled = Settings.isPullDownRefresh(this)
    }
    
    override fun onTaskFinished(taskCode: Int, code: ITaskFinishedHandler.TaskResultCode, result: HNPostComments?, tag: Any?) {
        if (code == ITaskFinishedHandler.TaskResultCode.Success && mCommentsListAdapter != null) {
            showComments(result)
        } else if (code != ITaskFinishedHandler.TaskResultCode.Success) {
            Toast.makeText(this, getString(R.string.error_unable_to_retrieve_comments), Toast.LENGTH_SHORT).show()
        }
        updateEmptyView()
        setShowRefreshing(false)
        if (mShowComposeAfterLoad) {
            mShowComposeAfterLoad = false
            mPendingReplyTo = null
            mPendingAddComment = false
            mCommentsListAdapter?.notifyDataSetChanged()
            scrollToCompose()
        }
    }
    
    private fun showComments(comments: HNPostComments?) {
        comments?.let { commentsData ->
            if (commentsData.headerHtml != null && mCommentHeaderText?.visibility != View.VISIBLE) {
                mCommentHeaderText?.visibility = View.VISIBLE
                // We trim it here to get rid of pesky newlines that come from closing <p> tags
                mCommentHeaderText?.text = Html.fromHtml(commentsData.headerHtml).toString().trim()
                
                // Linkify.ALL does some highlighting where we don't want it
                // (i.e if you just put certain tlds in) so we use this custom regex.
                mCommentHeaderText?.let { Linkify.addLinks(it, Linkify.WEB_URLS) }
            }
            
            mComments = commentsData
            mCommentsListAdapter?.notifyDataSetChanged()
        }
    }
    
    private fun loadIntermediateCommentsFromStore() {
        GetLastHNPostCommentsTask().execute(mPost?.postID)
    }
    
    inner class GetLastHNPostCommentsTask : FileUtil.GetLastHNPostCommentsTask() {
        override fun onPostExecute(result: HNPostComments?) {
            val registeredUserChanged = result != null && result.userAcquiredFor != null &&
                    (result.userAcquiredFor != Settings.getUserName(this@CommentsActivity))
            // Only show comments if we last fetched them for the current user and we have comments
            if (!registeredUserChanged && result != null) {
                showComments(result)
            } else {
                updateEmptyView()
            }
        }
    }
    
    private fun startFeedLoading() {
        mHaveLoadedPosts = false
        setShowRefreshing(true)
        HNPostCommentsTask.startOrReattach(this, this, mPost?.postID ?: "", 0)
    }
    
    private fun refreshFontSizes(): Boolean {
        val fontSize = Settings.getFontSize(this)
        if (mCurrentFontSize == null || mCurrentFontSize != fontSize) {
            mCurrentFontSize = fontSize
            mFontSizeText = when (fontSize) {
                getString(R.string.pref_fontsize_small) -> 14
                getString(R.string.pref_fontsize_normal) -> 16
                else -> 20
            }
            mFontSizeMetadata = when (fontSize) {
                getString(R.string.pref_fontsize_small) -> 12
                getString(R.string.pref_fontsize_normal) -> 14
                else -> 18
            }
            return true
        }
        return false
    }
    
    private fun vote(voteURL: String?, comment: HNComment) {
        HNVoteTask.start(voteURL, this, VoteTaskFinishedHandler(), TASKCODE_VOTE, comment)
    }

    private fun requestCompose(replyTo: HNComment?) {
        if (!Settings.isUserLoggedIn(this)) {
            mPendingReplyTo = replyTo
            mPendingAddComment = replyTo == null
            startActivityForResult(Intent(this, LoginActivity::class.java), ACTIVITY_LOGIN)
            return
        }
        showComposeRow(replyTo)
    }

    private fun showComposeRow(replyTo: HNComment?) {
        val parentId = replyTo?.commentId
        if (mIsComposing && mComposeParentId == parentId) {
            hideCompose()
            return
        }
        mIsComposing = true
        mComposeParentId = parentId
        mCommentsListAdapter?.notifyDataSetChanged()
        scrollToCompose()
    }

    private fun hideCompose() {
        mIsComposing = false
        mComposeParentId = null
        mCommentsListAdapter?.notifyDataSetChanged()
    }

    private fun scrollToCompose() {
        val adapter = mCommentsListAdapter ?: return
        val index = adapter.composeRowIndex()
        if (index < 0) return
        val listIndex = index + binding.commentsList.headerViewsCount
        binding.commentsList.post {
            binding.commentsList.smoothScrollToPosition(listIndex)
            val first = binding.commentsList.firstVisiblePosition
            val child = binding.commentsList.getChildAt(listIndex - first)
            child?.findViewById<EditText>(R.id.comments_compose_text)?.requestFocus()
        }
    }

    private fun confirmPostComment() {
        val text = mComposeDraft.trim()
        if (text.isEmpty()) {
            Toast.makeText(this, R.string.comment_empty, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_post_comment)
            .setMessage(text)
            .setPositiveButton(R.string.post_comment) { _, _ ->
                postComment(text)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun postComment(text: String) {
        if (mIsPostingComment) return
        val parentId = mComposeParentId ?: mPost?.postID
        val storyId = mPost?.postID
        if (parentId.isNullOrEmpty() || storyId.isNullOrEmpty()) {
            Toast.makeText(this, R.string.comment_error, Toast.LENGTH_LONG).show()
            return
        }
        mIsPostingComment = true
        mCommentsListAdapter?.notifyDataSetChanged()
        setShowRefreshing(true)
        HNCommentPostTask.start(
            text,
            parentId,
            storyId,
            this,
            CommentPostTaskFinishedHandler(),
            TASKCODE_POST_COMMENT,
            null
        )
    }

    private fun confirmDeleteComment(comment: HNComment) {
        val deleteUrl = comment.getDeleteUrl()
        if (deleteUrl.isNullOrEmpty()) {
            Toast.makeText(this, R.string.delete_error, Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete_comment)
            .setMessage(Html.fromHtml(comment.text).toString().trim().ifEmpty {
                getString(R.string.confirm_delete_comment)
            })
            .setPositiveButton(R.string.delete_comment) { _, _ ->
                deleteComment(deleteUrl)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteComment(deleteUrl: String) {
        setShowRefreshing(true)
        HNCommentDeleteTask.start(
            deleteUrl,
            this,
            CommentDeleteTaskFinishedHandler(),
            TASKCODE_DELETE_COMMENT,
            null
        )
    }
    
    override fun onRestoreInstanceState(state: Bundle) {
        super.onRestoreInstanceState(state)
        mListState = state.getParcelable(LIST_STATE)
    }
    
    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        mListState = binding.commentsList.onSaveInstanceState()
        state.putParcelable(LIST_STATE, mListState)
    }
    
    private fun updateEmptyView() {
        if (mHaveLoadedPosts) {
            mEmptyView?.text = getString(R.string.no_comments)
        }
        mHaveLoadedPosts = true
    }
    
    private fun showCommentsSpotlight() {
        val posArray = IntArray(2)
        mActionbarTitle?.getLocationInWindow(posArray)
        
        // Calculate the center position of the action bar title
        val titleWidth = mActionbarTitle?.width ?: 0
        val titleHeight = mActionbarTitle?.height ?: 0
        val titleCenterX = posArray[0] + (titleWidth / 2)
        val titleCenterY = posArray[1] + (titleHeight / 2)
        
        // Create a spotlight that covers the action bar title area
        val spotlightWidth = titleWidth.toFloat()
        val spotlightHeight = titleHeight.toFloat()
        val spotlightX = posArray[0].toFloat()
        val spotlightY = posArray[1].toFloat()
        
        val intent = SpotlightActivity.intentForSpotlightActivity(
            this, spotlightX, spotlightWidth, spotlightY, spotlightHeight,
            getString(R.string.click_on_comments)
        )
        startActivityForResult(intent, ACTIVITY_SPOTLIGHT)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
    
    private fun openArticleReader() {
        MainActivity.openPostInGeckoView(mPost, this, true)
    }
    
    private fun initCommentsHeader() {
        // Don't worry about reallocating this stuff it has already been called
        if (mCommentHeader == null) {
            mCommentHeader = LinearLayout(this)
            mCommentHeader?.orientation = LinearLayout.VERTICAL
            mCommentHeaderText = TextView(this)
            mCommentHeader?.addView(mCommentHeaderText)
            // Division by 2 just gave the right feel, I'm unsure how well it will work across platforms
            mCommentHeaderText?.setPadding(mCommentLevelIndentPx, mCommentLevelIndentPx / 2, mCommentLevelIndentPx / 2, mCommentLevelIndentPx / 2)
            
            mCommentHeaderText?.setTextColor(resources.getColor(R.color.gray_comments_information, null))
            
            // Make it look like the header is just another list item.
            val v = View(this)
            v.setBackgroundColor(resources.getColor(R.color.gray_comments_divider, null))
            v.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
            mCommentHeader?.addView(v)
            mCommentHeaderText?.visibility = View.GONE
        }
    }
    
    private fun setShowRefreshing(showRefreshing: Boolean) {
        if (!Settings.isPullDownRefresh(this)) {
            mShouldShowRefreshing = showRefreshing
            supportInvalidateOptionsMenu()
        }
        
        if (binding.commentsSwiperefreshlayout.isEnabled && (!binding.commentsSwiperefreshlayout.isRefreshing || !showRefreshing)) {
            binding.commentsSwiperefreshlayout.isRefreshing = showRefreshing
        }
    }
    
    inner class LongPressMenuListAdapter(private val mComment: HNComment) : ListAdapter {
        private val mIsLoggedIn: Boolean = Settings.isUserLoggedIn(this@CommentsActivity)
        private val mUpVotingEnabled: Boolean = mIsLoggedIn && 
            (mComment.getUpvoteUrl(Settings.getUserName(this@CommentsActivity)) != null && !mVotedComments.contains(mComment))
        private val mDownVotingEnabled: Boolean = mIsLoggedIn && 
            (mComment.getDownvoteUrl(Settings.getUserName(this@CommentsActivity)) != null && !mVotedComments.contains(mComment))
        private val mItems: ArrayList<CharSequence> = ArrayList()
        
        init {
            if (mUpVotingEnabled) {
                mItems.add(getString(R.string.upvote))
            }
            if (mDownVotingEnabled) {
                mItems.add(getString(R.string.downvote))
            }
            
            if (!mUpVotingEnabled && !mDownVotingEnabled) {
                if (mIsLoggedIn) {
                    mItems.add(getString(R.string.already_voted_on))
                } else {
                    mItems.add(getString(R.string.please_log_in))
                }
            }

            if (mComment.commentId != null) {
                mItems.add(getString(R.string.reply))
            }
            if (mComment.getDeleteUrl() != null) {
                mItems.add(getString(R.string.delete_comment))
            }
            
            if (mComment.treeNode?.isExpanded == true) {
                mItems.add(getString(R.string.collapse_comment))
            } else {
                mItems.add(getString(R.string.expand_comment))
            }
            
            if (mComment.treeNode?.parent != null) {
                mItems.add(getString(R.string.collapse_thread))
            }
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
        override fun isEnabled(position: Int): Boolean = !(!mUpVotingEnabled && position == 0)
        
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = mInflater.inflate(android.R.layout.simple_list_item_1, null) as TextView
            view.text = getItem(position)
            if (!mUpVotingEnabled && position == 0) {
                view.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            }
            return view
        }
        
        fun onItemClick(item: Int) {
            val clickedText = getItem(item).toString()
            
            when {
                clickedText == applicationContext.getString(R.string.upvote) -> {
                    vote(mComment.getUpvoteUrl(Settings.getUserName(this@CommentsActivity)), mComment)
                }
                clickedText == applicationContext.getString(R.string.downvote) -> {
                    vote(mComment.getDownvoteUrl(Settings.getUserName(this@CommentsActivity)), mComment)
                }
                clickedText == applicationContext.getString(R.string.please_log_in) -> {
                    setCommentToUpvote(mComment)
                    startActivityForResult(Intent(applicationContext, LoginActivity::class.java), ACTIVITY_LOGIN)
                }
                clickedText == applicationContext.getString(R.string.reply) -> {
                    requestCompose(mComment)
                }
                clickedText == applicationContext.getString(R.string.delete_comment) -> {
                    confirmDeleteComment(mComment)
                }
                clickedText == applicationContext.getString(R.string.collapse_thread) -> {
                    val mRootNode = mComment.treeNode?.rootNode
                    mRootNode?.comment?.let { comment ->
                        mComments.toggleCommentExpanded(comment)
                        mCommentsListAdapter?.notifyDataSetChanged()
                    }
                }
                else -> {
                    mComments.toggleCommentExpanded(mComment)
                    mCommentsListAdapter?.notifyDataSetChanged()
                }
            }
        }
    }
    
    inner class VoteTaskFinishedHandler : ITaskFinishedHandler<Boolean?> {
        override fun onTaskFinished(taskCode: Int, code: ITaskFinishedHandler.TaskResultCode, result: Boolean?, tag: Any?) {
            if (taskCode == TASKCODE_VOTE) {
                if (result == true) {
                    Toast.makeText(this@CommentsActivity, R.string.vote_success, Toast.LENGTH_SHORT).show()
                    val comment = tag as? HNComment
                    if (comment != null) {
                        mVotedComments.add(comment)
                    }
                } else {
                    Toast.makeText(this@CommentsActivity, R.string.vote_error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    inner class CommentPostTaskFinishedHandler : ITaskFinishedHandler<Boolean?> {
        override fun onTaskFinished(taskCode: Int, code: ITaskFinishedHandler.TaskResultCode, result: Boolean?, tag: Any?) {
            if (taskCode != TASKCODE_POST_COMMENT) return
            mIsPostingComment = false
            if (result == true) {
                mComposeDraft = ""
                hideCompose()
                Toast.makeText(this@CommentsActivity, R.string.comment_success, Toast.LENGTH_SHORT).show()
                startFeedLoading()
            } else {
                setShowRefreshing(false)
                mCommentsListAdapter?.notifyDataSetChanged()
                Toast.makeText(this@CommentsActivity, R.string.comment_error, Toast.LENGTH_LONG).show()
            }
        }
    }

    inner class CommentDeleteTaskFinishedHandler : ITaskFinishedHandler<Boolean?> {
        override fun onTaskFinished(taskCode: Int, code: ITaskFinishedHandler.TaskResultCode, result: Boolean?, tag: Any?) {
            if (taskCode != TASKCODE_DELETE_COMMENT) return
            if (result == true) {
                Toast.makeText(this@CommentsActivity, R.string.delete_success, Toast.LENGTH_SHORT).show()
                startFeedLoading()
            } else {
                setShowRefreshing(false)
                Toast.makeText(this@CommentsActivity, R.string.delete_error, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    inner class CommentsAdapter : BaseAdapter() {
        private val VIEW_TYPE_COMMENT = 0
        private val VIEW_TYPE_COMPOSE = 1

        override fun getCount(): Int = rows().size

        override fun getItem(position: Int): Any = rows()[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getViewTypeCount(): Int = 2

        override fun getItemViewType(position: Int): Int {
            return when (rowAt(position)) {
                is CommentListRow.Comment -> VIEW_TYPE_COMMENT
                is CommentListRow.Compose -> VIEW_TYPE_COMPOSE
            }
        }

        fun composeRowIndex(): Int = rows().indexOfFirst { it is CommentListRow.Compose }

        private fun rows(): List<CommentListRow> {
            val comments = mComments.comments ?: emptyList()
            val rows = ArrayList<CommentListRow>()
            if (mIsComposing && mComposeParentId == null) {
                rows.add(CommentListRow.Compose(null, 0))
            }
            for (comment in comments) {
                if (comment == null) continue
                rows.add(CommentListRow.Comment(comment))
                if (mIsComposing && mComposeParentId != null && comment.commentId == mComposeParentId) {
                    rows.add(CommentListRow.Compose(comment, comment.commentLevel + 1))
                }
            }
            return rows
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return when (val row = rowAt(position)) {
                is CommentListRow.Comment -> bindCommentRow(row.comment, convertView, parent)
                is CommentListRow.Compose -> bindComposeRow(row, convertView, parent)
            }
        }

        private fun rowAt(position: Int): CommentListRow = rows()[position]

        private fun bindCommentRow(comment: HNComment, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: mInflater.inflate(R.layout.comments_list_item, parent, false)
            val holder = if (view.tag is CommentViewHolder) {
                view.tag as CommentViewHolder
            } else {
                CommentViewHolder().apply {
                    rootView = view
                    textView = view.findViewById(R.id.comments_list_item_text)
                    spacersContainer = view.findViewById(R.id.comments_list_item_spacerscontainer)
                    authorView = view.findViewById(R.id.comments_list_item_author)
                    timeAgoView = view.findViewById(R.id.comments_list_item_timeago)
                    deleteView = view.findViewById(R.id.comments_list_item_delete)
                    expandView = view.findViewById(R.id.comments_list_item_expand)
                }.also { view.tag = it }
            }

            holder.setOnClickListener {
                if (comment.treeNode?.hasChildren() == true) {
                    mComments.toggleCommentExpanded(comment)
                    notifyDataSetChanged()
                }
            }

            holder.setOnLongClickListener {
                val builder = AlertDialog.Builder(this@CommentsActivity)
                val adapter = LongPressMenuListAdapter(comment)
                val dialog = builder.setAdapter(adapter, DialogInterface.OnClickListener { _, which ->
                    adapter.onItemClick(which)
                }).create()
                dialog.setCanceledOnTouchOutside(true)
                dialog.show()
                true
            }

            holder.deleteView?.setOnClickListener {
                confirmDeleteComment(comment)
            }

            holder.setComment(
                comment,
                mCommentLevelIndentPx,
                this@CommentsActivity,
                mFontSizeText,
                mFontSizeMetadata
            )

            return view
        }

        private fun bindComposeRow(row: CommentListRow.Compose, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: mInflater.inflate(R.layout.comments_compose_item, parent, false)
            val holder = if (view.tag is ComposeViewHolder) {
                view.tag as ComposeViewHolder
            } else {
                ComposeViewHolder().apply {
                    rootView = view
                    authorView = view.findViewById(R.id.comments_compose_author)
                    timeAgoView = view.findViewById(R.id.comments_compose_timeago)
                    textView = view.findViewById(R.id.comments_compose_text)
                    postView = view.findViewById(R.id.comments_compose_post)
                    cancelView = view.findViewById(R.id.comments_compose_cancel)
                    spacersContainer = view.findViewById(R.id.comments_compose_spacerscontainer)
                }.also { view.tag = it }
            }

            holder.bind(
                indentLevel = row.indentLevel,
                commentLevelIndentPx = mCommentLevelIndentPx,
                fontSizeText = mFontSizeText,
                fontSizeMetadata = mFontSizeMetadata
            )
            return view
        }
    }

    private sealed class CommentListRow {
        data class Comment(val comment: HNComment) : CommentListRow()
        data class Compose(val replyTo: HNComment?, val indentLevel: Int) : CommentListRow()
    }
    
    data class CommentViewHolder(
        var rootView: View? = null,
        var textView: LinkifiedTextView? = null,
        var authorView: TextView? = null,
        var timeAgoView: TextView? = null,
        var deleteView: TextView? = null,
        var expandView: ImageView? = null,
        var spacersContainer: LinearLayout? = null
    ) {
        fun setComment(
            comment: HNComment?,
            commentLevelIndentPx: Int,
            c: Context,
            commentTextSize: Int,
            metadataTextSize: Int
        ) {
            textView?.textSize = commentTextSize.toFloat()
            textView?.setTextColor(comment?.color ?: Color.BLACK)
            textView?.setLinkTextColor(comment?.color ?: Color.BLACK)
            textView?.text = Html.fromHtml(comment?.text ?: "")
            textView?.movementMethod = LinkMovementMethod.getInstance()
            
            authorView?.textSize = metadataTextSize.toFloat()
            timeAgoView?.textSize = metadataTextSize.toFloat()
            deleteView?.textSize = metadataTextSize.toFloat()
            
            if (!TextUtils.isEmpty(comment?.author)) {
                authorView?.text = comment?.author
                timeAgoView?.text = ", ${comment?.timeAgo}"
            } else {
                authorView?.text = c.getString(R.string.deleted)
                // We set this here so that convertView doesn't reuse the old timeAgoView value
                timeAgoView?.text = ""
            }

            deleteView?.visibility = if (comment?.getDeleteUrl() != null) View.VISIBLE else View.GONE
            
            expandView?.visibility = if (comment?.treeNode?.isExpanded == true) View.INVISIBLE else View.VISIBLE
            
            spacersContainer?.removeAllViews()
            comment?.commentLevel?.let { level ->
                for (i in 0 until level) {
                    val spacer = View(c)
                    spacer.layoutParams = LinearLayout.LayoutParams(commentLevelIndentPx, ViewGroup.LayoutParams.MATCH_PARENT)
                    val spacerAlpha = maxOf(70 - i * 10, 10)
                    spacer.setBackgroundColor(Color.argb(spacerAlpha, 0, 0, 0))
                    spacersContainer?.addView(spacer, i)
                }
            }
        }
        
        fun setOnClickListener(onClickListener: View.OnClickListener) {
            rootView?.setOnClickListener(onClickListener)
            textView?.setOnClickListener(onClickListener)
        }
        
        fun setOnLongClickListener(onLongClickListener: View.OnLongClickListener) {
            rootView?.setOnLongClickListener(onLongClickListener)
            textView?.setOnLongClickListener(onLongClickListener)
        }
    }

    inner class ComposeViewHolder(
        var rootView: View? = null,
        var authorView: TextView? = null,
        var timeAgoView: TextView? = null,
        var textView: EditText? = null,
        var postView: TextView? = null,
        var cancelView: TextView? = null,
        var spacersContainer: LinearLayout? = null
    ) {
        private var watchingDraft = false

        private val draftWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mComposeDraft = s?.toString() ?: ""
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        fun bind(indentLevel: Int, commentLevelIndentPx: Int, fontSizeText: Int, fontSizeMetadata: Int) {
            val userName = Settings.getUserName(this@CommentsActivity)
            authorView?.textSize = fontSizeMetadata.toFloat()
            timeAgoView?.textSize = fontSizeMetadata.toFloat()
            authorView?.text = userName
            timeAgoView?.text = ", ${getString(R.string.comment_now)}"

            textView?.textSize = fontSizeText.toFloat()
            if (!watchingDraft) {
                textView?.addTextChangedListener(draftWatcher)
                watchingDraft = true
            }
            if (textView?.text?.toString() != mComposeDraft) {
                textView?.setText(mComposeDraft)
                textView?.setSelection(mComposeDraft.length)
            }
            textView?.isEnabled = !mIsPostingComment

            postView?.textSize = fontSizeMetadata.toFloat()
            postView?.isEnabled = !mIsPostingComment
            postView?.setOnClickListener { confirmPostComment() }
            cancelView?.textSize = fontSizeMetadata.toFloat()
            cancelView?.isEnabled = !mIsPostingComment
            cancelView?.setOnClickListener { hideCompose() }

            spacersContainer?.removeAllViews()
            for (i in 0 until indentLevel) {
                val spacer = View(this@CommentsActivity)
                spacer.layoutParams = LinearLayout.LayoutParams(commentLevelIndentPx, ViewGroup.LayoutParams.MATCH_PARENT)
                val spacerAlpha = maxOf(70 - i * 10, 10)
                spacer.setBackgroundColor(Color.argb(spacerAlpha, 0, 0, 0))
                spacersContainer?.addView(spacer, i)
            }
        }
    }
    
    private fun setCommentToUpvote(comment: HNComment) {
        mPendingVote = comment
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ACTIVITY_LOGIN -> {
                when (resultCode) {
                    RESULT_OK -> {
                        val pendingComment = mPendingAddComment || mPendingReplyTo != null
                        if (mPendingVote != null || pendingComment) {
                            if (pendingComment) {
                                mIsComposing = true
                                mComposeParentId = mPendingReplyTo?.commentId
                                mShowComposeAfterLoad = true
                            }
                            mComments = HNPostComments()
                            mCommentsListAdapter?.notifyDataSetChanged()
                            startFeedLoading()
                            Toast.makeText(this, getString(R.string.login_success_reloading), Toast.LENGTH_SHORT).show()
                        }
                    }
                    RESULT_CANCELED -> {
                        val pendingComment = mPendingAddComment || mPendingReplyTo != null
                        mPendingReplyTo = null
                        mPendingAddComment = false
                        val message = if (pendingComment) {
                            R.string.error_login_to_comment
                        } else {
                            R.string.error_login_to_vote
                        }
                        Toast.makeText(this, getString(message), Toast.LENGTH_LONG).show()
                    }
                }
            }
            ACTIVITY_SPOTLIGHT -> {
                // The user tapped in the spotlight area
                if (resultCode == RESULT_OK) {
                    openArticleReader()
                }
            }
        }
    }
}
