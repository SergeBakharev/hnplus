package com.manuelmaly.hn.parser

import org.jsoup.nodes.Element

/**
 * Returns the hidden FNID form parameter returned by the HN login page.
 * @author manuelmaly
 */
class HNNewsLoginParser : BaseHTMLParser<String?>() {
    @Throws(Exception::class)
    override fun parseDocument(doc: Element?): String? {
        if (doc == null) return null

        val hiddenInput = doc.select("input[type=hidden]")
        if (hiddenInput.size == 0) return null

        return hiddenInput[0].attr("value")
    }
}
