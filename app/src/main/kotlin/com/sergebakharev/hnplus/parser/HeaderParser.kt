package com.sergebakharev.hnplus.parser

import org.jsoup.nodes.Element

class HeaderParser : BaseHTMLParser<String?>() {
    @Throws(Exception::class)
    override fun parseDocument(doc: Element?): String? {
        if (doc == null) return null
        val headerRows = doc.select("tr")

        // Six rows means that this is just a Ask HN post or a poll with
        // no options.  In either case, the content we want is in the fourth row
        if (headerRows.size == 6) {
            return headerRows[3].select("td")[1].html()
        }

        return null
    }
}
