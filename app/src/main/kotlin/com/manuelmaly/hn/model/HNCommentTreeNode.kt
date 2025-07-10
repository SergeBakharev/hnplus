package com.manuelmaly.hn.model

import java.io.Serializable

class HNCommentTreeNode(val comment: HNComment?) : Serializable {
    val children: ArrayList<HNCommentTreeNode>? = ArrayList()
    var parent: HNCommentTreeNode? = null
    var isExpanded: Boolean = true
        private set

    fun addChild(child: HNCommentTreeNode) {
        children!!.add(child)
        child.parent = this
    }

    val rootNode: HNCommentTreeNode
        get() {
            val mRootNode: HNCommentTreeNode
            if (parent == null) {
                mRootNode = this
            } else {
                var mCandidateRootNode: HNCommentTreeNode? = this
                while (mCandidateRootNode!!.parent != null) {
                    mCandidateRootNode = mCandidateRootNode.parent
                }
                mRootNode = mCandidateRootNode
            }

            return mRootNode
        }

    val visibleComments: ArrayList<HNComment?>
        get() {
            val visibleComments = ArrayList<HNComment?>()
            visibleComments.add(comment)
            if (isExpanded) {
                for (child in children!!) visibleComments.addAll(child.visibleCommentsList())
            }
            return visibleComments
        }

    fun visibleCommentsList(): ArrayList<HNComment?> {
        return visibleComments
    }

    fun hasChildren(): Boolean {
        return children != null && children.size > 0
    }

    fun toggleExpanded() {
        isExpanded = !isExpanded
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + (if (comment == null) 0 else comment.hashCode())
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null) return false
        if (javaClass != obj.javaClass) return false
        val other = obj as HNCommentTreeNode
        if (comment == null) {
            if (other.comment != null) return false
        } else if (comment != other.comment) return false
        return true
    }


    companion object {
        private const val serialVersionUID = 1089928137259687565L
    }
}
