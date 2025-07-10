package com.manuelmaly.hn.model

import java.io.Serializable

class HNComment(// do not want to parse this :P
    val timeAgo: String,
    val author: String,
    val commentLink: String,
    val text: String,
    val color: Int,
    val commentLevel: Int,
    val isDownvoted: Boolean,
    private val mUpvoteUrl: String?,
    private val mDownvoteUrl: String?
) : Serializable {
    var treeNode: HNCommentTreeNode? = null

    fun getUpvoteUrl(currentUserName: String?): String? {
        if (mUpvoteUrl == null || !mUpvoteUrl.contains("auth="))  // HN changed authentication
            return null
        return mUpvoteUrl
    }

    fun getDownvoteUrl(currentUserName: String?): String? {
        if (mDownvoteUrl == null || !mDownvoteUrl.contains("auth="))  // HN changed authentication
            return null
        return mDownvoteUrl
    }

    companion object {
        private const val serialVersionUID = 1286983917054008714L
    }
}
