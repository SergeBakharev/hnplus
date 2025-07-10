package com.sergebakharev.hnplus.model

import java.io.Serializable

class HNPostComments : Serializable {
    private var mTreeNodes: MutableList<HNCommentTreeNode>

    @Transient
    private var mCommentsCache: MutableList<HNComment?>? = null
    private var mIsTreeDirty = false
    var headerHtml: String? = null
        private set
    var userAcquiredFor: String? = null
        private set

    constructor() {
        mTreeNodes = ArrayList()
    }

    @JvmOverloads
    constructor(
        comments: List<HNComment>,
        headerHtml: String? = null,
        userAcquiredFor: String? = ""
    ) {
        mTreeNodes = ArrayList()
        for (comment in comments) {
            if (comment.commentLevel == 0) mTreeNodes.add(makeTreeNode(comment, comments))
        }
        this.headerHtml = headerHtml
        this.userAcquiredFor = userAcquiredFor
    }

    val treeNodes: List<HNCommentTreeNode>
        get() = mTreeNodes

    val comments: List<HNComment?>
        get() {
            if (mCommentsCache == null || mIsTreeDirty) {
                mCommentsCache = ArrayList()
                if (mTreeNodes == null) {
                    mTreeNodes = ArrayList()
                }
                for (node in mTreeNodes) mCommentsCache!!.addAll(node.visibleComments)
                mIsTreeDirty = false
            }
            return mCommentsCache ?: ArrayList()
        }

    private fun makeTreeNode(comment: HNComment, allComments: List<HNComment>): HNCommentTreeNode {
        val node = HNCommentTreeNode(comment)
        val nodeLevel = comment.commentLevel
        val nodeIndex = allComments.indexOf(comment)
        comment.treeNode = node
        for (i in nodeIndex + 1..<allComments.size) {
            val childComment = allComments[i]
            if (childComment.commentLevel > nodeLevel + 1) continue
            if (childComment.commentLevel <= nodeLevel) break
            node.addChild(makeTreeNode(childComment, allComments))
        }
        return node
    }

    fun toggleCommentExpanded(comment: HNComment?) {
        if (comment == null) return

        if (comment.treeNode != null) {
            comment.treeNode!!.toggleExpanded()
            mIsTreeDirty = true
        }
    }

    companion object {
        private const val serialVersionUID = -2305617988079011364L
    }
}
