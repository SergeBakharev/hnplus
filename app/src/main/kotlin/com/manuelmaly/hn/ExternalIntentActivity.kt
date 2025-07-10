package com.manuelmaly.hn

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.manuelmaly.hn.model.HNFeedPost

class ExternalIntentActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data
        val uriString = uri.toString().replace("/$".toRegex(), "")

        var i: Intent? = null
        if (uriString.endsWith("news.ycombinator.com")) { // Front page
            i = Intent(this, MainActivity::class.java)
        } else if (uriString.contains("item")) { // Comment
            val postId = uri?.getQueryParameter("id") ?: ""
            val postToOpen = HNFeedPost(
                uriString,
                title = "",
                uRLDomain = "",
                author = null,
                postID = postId,
                commentsCount = 0,
                points = 0,
                mUpvoteURL = null
            )
            i = Intent(this, CommentsActivity::class.java)
            i.putExtra(CommentsActivity.Companion.EXTRA_HNPOST, postToOpen)
        }

        if (i != null) {
            startActivity(i)
        } else {
            Toast.makeText(
                this,
                "This seems not to be a valid Hacker News item!",
                Toast.LENGTH_LONG
            ).show()
        }

        finish()
    }
}
