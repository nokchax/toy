package com.nokchax.toy.crawler.extract

import com.nokchax.toy.common.domain.crawl.SelectorType
import com.nokchax.toy.common.exception.botError
import com.nokchax.toy.common.function.ellipsis
import com.nokchax.toy.common.function.sha256
import com.nokchax.toy.crawler.config.CrawlProperties
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.jsoup.select.Selector
import org.springframework.stereotype.Component

data class ExtractedItem(
    val text: String,
    val url: String?,
) {
    /** 항목을 구분하는 기준 (새 항목 판별용). 링크가 있으면 링크, 없으면 텍스트. */
    val hash: String = (url ?: text).sha256()

    /** 항목의 내용 (변경 감지용). 링크가 같아도 텍스트가 바뀌면 달라진다. */
    val contentHash: String = "$text\n${url ?: ""}".sha256()
}

/** CHANGED 모드에서 직전 결과와 비교하는 해시 */
fun List<ExtractedItem>.resultHash(): String = joinToString("\n") { it.contentHash }.sha256()

@Component
class Extractor(
    private val properties: CrawlProperties,
) {

    fun extract(html: String, baseUri: String, selector: String, type: SelectorType): List<ExtractedItem> {
        val document = Jsoup.parse(html, baseUri)

        return select(document, selector, type)
            .mapNotNull { it.toItem() }
            .distinctBy { it.hash }
            .take(properties.maxItems)
    }

    private fun select(root: Element, selector: String, type: SelectorType): Elements =
        try {
            when (type) {
                SelectorType.CSS -> root.select(selector)
                SelectorType.XPATH -> root.selectXpath(selector)
            }
        } catch (e: Selector.SelectorParseException) {
            botError("selector 문법이 잘못됐다옹: ${e.message}")
        } catch (e: IllegalStateException) {
            botError("selector 문법이 잘못됐다옹: ${e.message}")
        }

    private fun Element.toItem(): ExtractedItem? {
        val text = text().trim().ifBlank { attr("title").trim() }.ifBlank { attr("alt").trim() }
        val url = link()
        if (text.isBlank() && url == null) return null

        return ExtractedItem(text.ifBlank { url!! }.ellipsis(1024), url?.ellipsis(2048))
    }

    /** 자기 자신 → 조상 → 자손 순서로 링크를 찾는다. */
    private fun Element.link(): String? {
        val anchor = closest("a[href]") ?: selectFirst("a[href]") ?: return null
        return anchor.absUrl("href").ifBlank { null }
    }
}
