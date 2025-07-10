package com.sergebakharev.hnplus.model

import java.io.Serializable

class HNFeed : Serializable {
    private val mPosts: MutableList<HNFeedPost>
    var nextPageURL: String? = null
    var userAcquiredFor: String? = null // this dictates if the upvote URLs are correct
        private set
    var isLoadedMore: Boolean =
        false // currently, we can perform only one "load-more" action reliably

    constructor() {
        mPosts = ArrayList()
    }

    constructor(posts: List<HNFeedPost>, nextPageURL: String?, userAcquiredFor: String?) {
        mPosts = posts.toMutableList()
        this.nextPageURL = nextPageURL
        this.userAcquiredFor = userAcquiredFor
    }

    fun addPost(post: HNFeedPost) {
        mPosts.add(post)
    }

    val posts: List<HNFeedPost>?
        get() = mPosts

    fun addPosts(posts: Collection<HNFeedPost>) {
        mPosts.addAll(posts)
    }

    fun appendLoadMoreFeed(feed: HNFeed?) {
        if (feed?.posts == null) return

        for (candidate in feed.posts!!) if (!mPosts.contains(candidate)) mPosts.add(candidate)
        nextPageURL = feed.nextPageURL
    }

    companion object {
        private const val serialVersionUID = -7957577448455303642L
    }
}
