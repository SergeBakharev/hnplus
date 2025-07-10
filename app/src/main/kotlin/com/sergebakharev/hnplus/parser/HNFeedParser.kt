package com.sergebakharev.hnplus.parser

import android.util.Log
import com.sergebakharev.hnplus.App
import com.sergebakharev.hnplus.Settings
import com.sergebakharev.hnplus.model.HNFeed
import com.sergebakharev.hnplus.model.HNFeedPost
import com.sergebakharev.hnplus.util.HNHelper
import org.jsoup.nodes.Element

class HNFeedParser : BaseHTMLParser<HNFeed?>() {
    @Throws(Exception::class)
    override fun parseDocument(doc: Element?): HNFeed {
        if (doc == null) return HNFeed()

        val currentUser = Settings.getUserName(App.getInstance()!!)

        val posts = ArrayList<HNFeedPost>()

        // clumsy, but hopefully stable query - first element retrieved is the
        // top table, we have to skip that:
        val tableRows = doc.select("table tr table tr")
        Log.d("HNFeedParser", "Found ${tableRows.size} table rows")
        if (tableRows.size > 0) {
            tableRows.removeAt(0)
            Log.d("HNFeedParser", "After removing first row, ${tableRows.size} rows remain")
        }

        var nextPageURLElements = tableRows.select("a:matches(^More$)")

        // In case there are multiple "More" elements, select only the one which is a relative link:
        if (nextPageURLElements.size > 1) {
            nextPageURLElements = nextPageURLElements.select("a[href^=/]")
        }

        var nextPageURL: String? = null
        if (nextPageURLElements.size > 0) nextPageURL =
            HNHelper.resolveRelativeHNURL(nextPageURLElements.attr("href"))

        var url: String? = null
        var title: String? = null
        var author: String? = null
        var commentsCount = 0
        var points = 0
        var urlDomain: String? = null
        var postID: String? = null
        var upvoteURL: String? = null

        var endParsing = false
        for (row in tableRows.indices) {
            val rowInPost = row % 3
            val rowElement = tableRows[row]

                            when (rowInPost) {
                    0 -> {
                        Log.d("HNFeedParser", "Processing row $row (rowInPost: $rowInPost)")
                        val e1 = rowElement.select(".title > .titleline > a").first()
                        if (e1 == null) {
                            Log.d("HNFeedParser", "No title link found, ending parsing")
                            endParsing = true
                            break
                        }

                                            title = e1.text()
                        url = HNHelper.resolveRelativeHNURL(e1.attr("href"))
                        urlDomain = getDomainName(url ?: "")
                        Log.d("HNFeedParser", "Found title: '$title', url: '$url', domain: '$urlDomain'")

                    val e4 = rowElement.select(".votelinks a").first()
                    if (e4 != null) {
                        upvoteURL = e4.attr("href")
                        upvoteURL = if (!upvoteURL.contains("auth="))  // HN changed authentication
                            null
                        else HNHelper.resolveRelativeHNURL(upvoteURL)
                    }
                }

                1 -> {
                    Log.d("HNFeedParser", "Processing row $row (rowInPost: $rowInPost)")
                    points = getIntValueFollowedBySuffix(
                        rowElement.select(".subline > span.score").text(), "p"
                    )
                    author = rowElement.select(".subline > a[href*=user]").text()
                    val e2 = rowElement.select(".subline > a[href*=item]")
                        .last() // assuming the the last link is the comments link
                    if (e2 != null) {
                        commentsCount = getIntValueFollowedBySuffix(e2.text(), "c")
                        if (commentsCount == UNDEFINED && e2.text()
                                .contains("discuss")
                        ) commentsCount = 0
                        postID = getStringValuePrefixedByPrefix(e2.attr("href"), "id=")
                        Log.d("HNFeedParser", "Found author: '$author', points: $points, comments: $commentsCount, postID: '$postID'")
                    } else {
                        commentsCount = UNDEFINED
                        Log.d("HNFeedParser", "No comments link found")
                    }

                    val safeUrl: String = url ?: ""
                    val safeTitle: String = title ?: ""
                    val safeUrlDomain: String = urlDomain ?: ""
                    val safeAuthor: String? = author
                    val safePostID: String? = postID
                    val safeCommentsCount: Int = commentsCount
                    val safePoints: Int = points
                    val safeUpvoteURL: String? = upvoteURL
                    Log.d("HNFeedParser", "safeUrl: $safeUrl, type: ${safeUrl.let { it::class.qualifiedName }}")
                    Log.d("HNFeedParser", "safeTitle: $safeTitle, type: ${safeTitle::class.qualifiedName}")
                    Log.d("HNFeedParser", "safeUrlDomain: $safeUrlDomain, type: ${safeUrlDomain::class.qualifiedName}")
                    Log.d("HNFeedParser", "safeAuthor: $safeAuthor, type: ${safeAuthor?.let { it::class.qualifiedName } ?: "null"}")
                    Log.d("HNFeedParser", "safePostID: $safePostID, type: ${safePostID?.let { it::class.qualifiedName } ?: "null"}")
                    Log.d("HNFeedParser", "safeCommentsCount: $safeCommentsCount, type: ${safeCommentsCount::class.qualifiedName}")
                    Log.d("HNFeedParser", "safePoints: $safePoints, type: ${safePoints::class.qualifiedName}")
                    Log.d("HNFeedParser", "safeUpvoteURL: $safeUpvoteURL, type: ${safeUpvoteURL?.let { it::class.qualifiedName } ?: "null"}")
                    posts.add(
                        HNFeedPost(
                            uRL = safeUrl,
                            title = safeTitle,
                            uRLDomain = safeUrlDomain,
                            author = safeAuthor,
                            postID = safePostID,
                            commentsCount = safeCommentsCount,
                            points = safePoints,
                            mUpvoteURL = safeUpvoteURL
                        )
                    )
                }

                else -> {}
            }

            if (endParsing) break
        }

        return HNFeed(posts, nextPageURL, currentUser)
    }
}
