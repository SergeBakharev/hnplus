package com.manuelmaly.hn.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.w3c.dom.Node
import java.net.URI
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants

abstract class BaseHTMLParser<T> {
    @Throws(Exception::class)
    fun parse(input: String?): T {
        return parseDocument(Jsoup.parse(input))
    }

    @Throws(Exception::class)
    abstract fun parseDocument(doc: Element?): T

    companion object {
        const val UNDEFINED: Int = -1

        fun getDomainName(url: String): String {
            val uri: URI
            try {
                uri = URI(url)
                val domain = uri.host
                return if (domain.startsWith("www.")) domain.substring(4) else domain
            } catch (e: Exception) {
                return url
            }
        }

        fun getFirstTextValueInElementChildren(element: Element?): String {
            if (element == null) return ""

            for (node in element.childNodes()) if (node is TextNode) return node.text()
            return ""
        }

        fun getStringValue(query: String?, source: Node?, xpath: XPath): String {
            try {
                return (xpath.evaluate(query, source, XPathConstants.NODE) as Node).nodeValue
            } catch (e: Exception) {
                //TODO insert Crash Analytics tracking here?
            }
            return ""
        }

        fun getIntValueFollowedBySuffix(value: String?, suffix: String?): Int {
            if (value == null || suffix == null) return 0

            val suffixWordIdx = value.indexOf(suffix)
            if (suffixWordIdx >= 0) {
                val extractedValue =
                    value.substring(0, suffixWordIdx).replace("\\u00A0".toRegex(), "")
                        .trim { it <= ' ' }
                return try {
                    extractedValue.toInt()
                } catch (e: NumberFormatException) {
                    UNDEFINED
                }
            }
            return UNDEFINED
        }

        fun getStringValuePrefixedByPrefix(value: String, prefix: String): String? {
            val prefixWordIdx = value.indexOf(prefix)
            if (prefixWordIdx >= 0) {
                return value.substring(prefixWordIdx + prefix.length)
            }
            return null
        }
    }
}
