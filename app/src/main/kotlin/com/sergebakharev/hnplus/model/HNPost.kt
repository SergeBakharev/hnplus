package com.sergebakharev.hnplus.model

import java.io.Serializable

class HNFeedPost(
    val uRL: String,
    val title: String,
    val uRLDomain: String,
    val author: String?, // as found in link to comments
    val postID: String?,
    val commentsCount: Int,
    val points: Int,
    private val mUpvoteURL: String?
) :
    Serializable {
    fun getUpvoteURL(currentUserName: String?): String? {
        if (mUpvoteURL == null || !mUpvoteURL.contains("auth="))  // HN changed authentication
            return null
        return mUpvoteURL
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + (if (author == null) 0 else author.hashCode())
        result = prime * result + (if (postID == null) 0 else postID.hashCode())
        result = prime * result + (if (uRL == null) 0 else uRL.hashCode())
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null) return false
        if (javaClass != obj.javaClass) return false
        val other = obj as HNFeedPost
        if (author == null) {
            if (other.author != null) return false
        } else if (author != other.author) return false
        if (postID == null) {
            if (other.postID != null) return false
        } else if (postID != other.postID) return false
        if (uRL == null) {
            if (other.uRL != null) return false
        } else if (uRL != other.uRL) return false
        return true
    }


    companion object {
        private const val serialVersionUID = -6764758363164898276L
    }
}
