package com.sergebakharev.hnplus.parser

import com.sergebakharev.hnplus.model.HNCommentForm
import org.jsoup.nodes.Element

class HNCommentFormParser : BaseHTMLParser<HNCommentForm?>() {
    @Throws(Exception::class)
    override fun parseDocument(doc: Element?): HNCommentForm? {
        if (doc == null) return null

        val form = doc.select("form[action*=comment]").first()
            ?: doc.select("form:has(input[name=hmac])").first()
            ?: return null

        val parent = form.select("input[name=parent]").first()?.attr("value")
        val hmac = form.select("input[name=hmac]").first()?.attr("value")
        if (parent.isNullOrEmpty() || hmac.isNullOrEmpty()) return null

        val goto = form.select("input[name=goto]").first()?.attr("value")
            ?.takeIf { it.isNotEmpty() }
            ?: "item?id=$parent"

        return HNCommentForm(parent, hmac, goto)
    }
}
