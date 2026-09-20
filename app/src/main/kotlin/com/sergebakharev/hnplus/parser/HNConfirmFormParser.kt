package com.sergebakharev.hnplus.parser

import com.sergebakharev.hnplus.model.HNConfirmForm
import com.sergebakharev.hnplus.util.HNHelper
import org.jsoup.nodes.Element

class HNConfirmFormParser : BaseHTMLParser<HNConfirmForm?>() {
    @Throws(Exception::class)
    override fun parseDocument(doc: Element?): HNConfirmForm? {
        if (doc == null) return null

        val form = doc.select("form:has(input[value=Yes])").first()
            ?: doc.select("form").first()
            ?: return null

        val fields = LinkedHashMap<String, String>()
        for (input in form.select("input")) {
            val name = input.attr("name")
            if (name.isEmpty()) continue
            val type = input.attr("type").lowercase()
            if (type == "submit" || type == "button") continue
            fields[name] = input.attr("value")
        }
        fields["b"] = "Yes"

        val action = form.attr("action")
        val actionUrl = if (action.isEmpty()) "" else HNHelper.resolveRelativeHNURL(action) ?: ""
        return HNConfirmForm(actionUrl, fields)
    }
}
