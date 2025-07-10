package com.sergebakharev.hnplus.parser

import android.graphics.Color
import com.sergebakharev.hnplus.App
import com.sergebakharev.hnplus.Settings
import com.sergebakharev.hnplus.model.HNComment
import com.sergebakharev.hnplus.model.HNPostComments
import com.sergebakharev.hnplus.util.HNHelper
import org.jsoup.nodes.Element

class HNCommentsParser : BaseHTMLParser<HNPostComments?>() {
    @Throws(Exception::class)
    override fun parseDocument(doc: Element?): HNPostComments {
        if (doc == null) return HNPostComments()

        val comments = ArrayList<HNComment>()

        val tableRows = doc.select("table tr table tr:has(table)")

        val currentUser = Settings.getUserName(App.getInstance()!!)

        var text: String? = null
        var author: String? = null
        var level = 0
        var timeAgo: String? = null
        var url: String? = null
        val isDownvoted = false
        var upvoteUrl: String? = null
        var downvoteUrl: String? = null

        val endParsing = false
        for (row in tableRows.indices) {
            val mainRowElement = tableRows[row].select("td:eq(2)").first()
            val rowLevelElement = tableRows[row].select("td:eq(0)").first()
            if (mainRowElement == null) continue

            val mainCommentDiv = mainRowElement.select("div.commtext").first() ?: continue

            // Parse the class attribute to get the comment color
            val commentColor = getCommentColor(mainCommentDiv.classNames())

            // In order to eliminate whitespace at the end of multi-line comments,
            // <p> tags are replaced with double <br/> tags.
            text = mainCommentDiv.html()
                .replace("<span> </span>", "")
                .replace("<p>", "<br/><br/>")
                .replace("</p>", "")

            val comHeadElement = mainRowElement.select("span.comhead").first()
            author = comHeadElement.select("a[href*=user]").text()
            timeAgo = comHeadElement.select("a[href*=item")
                .text() //getFirstTextValueInElementChildren(comHeadElement);
            //            if (timeAgoRaw.length() > 0)
//                timeAgo = timeAgoRaw.substring(0, timeAgoRaw.indexOf("|"));
            val urlElement = comHeadElement.select("a[href*=item]").first()
            if (urlElement != null) url = urlElement.attr("href")

            val levelSpacerWidth = rowLevelElement.select("img").first().attr("width")
            if (levelSpacerWidth != null) level = levelSpacerWidth.toInt() / 40

            val voteElements = tableRows[row].select("td:eq(1) a")
            upvoteUrl = getVoteUrl(voteElements.first())

            // We want to test for size because unlike first() calling .get(1)
            // Will throw an error if there are not two elements
            if (voteElements.size > 1) downvoteUrl = getVoteUrl(voteElements[1])

            comments.add(
                HNComment(
                    timeAgo, author,
                    url!!, text, commentColor, level, isDownvoted, upvoteUrl, downvoteUrl
                )
            )

            if (endParsing) break
        }

        // Just using table:eq(0) would return an extra table, so we use
        // get(0) instead, which only returns only the one we want
        val header = doc.select("body table:eq(0)  tbody > tr:eq(2) > td:eq(0) > table")[0]
        var headerHtml: String? = null

        // Five table rows is what it takes for the title, post information
        // And other boilerplate stuff.  More than five means we have something
        // Special
        if (header.select("tr").size > 5) {
            val headerParser = HeaderParser()
            headerHtml = headerParser.parseDocument(header)
        }


        return HNPostComments(comments, headerHtml, currentUser)
    }

    /**
     * Parses out the url for voting from a given element
     * @param voteElement The element from which to parse out the voting url
     * @return The relative url to vote in the given direction for that comment
     */
    private fun getVoteUrl(voteElement: Element?): String? {
        if (voteElement != null) {
            return if (voteElement.attr("href").contains("auth=")) HNHelper.resolveRelativeHNURL(
                voteElement.attr("href")
            ) else null
        }

        return null
    }

    private fun getCommentColor(classNames: Set<String>): Int {
        return if (classNames.contains("c00")) {
            Color.BLACK
        } else if (classNames.contains("c5a")) {
            Color.rgb(0x5A, 0x5A, 0x5A)
        } else if (classNames.contains("c73")) {
            Color.rgb(0x73, 0x73, 0x73)
        } else if (classNames.contains("c82")) {
            Color.rgb(0x82, 0x82, 0x82)
        } else if (classNames.contains("c88")) {
            Color.rgb(0x88, 0x88, 0x88)
        } else if (classNames.contains("c9c")) {
            Color.rgb(0x9C, 0x9C, 0x9C)
        } else if (classNames.contains("cae")) {
            Color.rgb(0xAE, 0xAE, 0xAE)
        } else if (classNames.contains("cbe")) {
            Color.rgb(0xBE, 0xBE, 0xBE)
        } else if (classNames.contains("cce")) {
            Color.rgb(0xCE, 0xCE, 0xCE)
        } else if (classNames.contains("cdd")) {
            Color.rgb(0xDD, 0xDD, 0xDD)
        } else {
            Color.BLACK
        }
    }
}
