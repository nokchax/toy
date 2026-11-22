package com.nokchax.toy.crawler.extract

import com.nokchax.toy.common.domain.crawl.SelectorType
import com.nokchax.toy.common.exception.BotException
import com.nokchax.toy.crawler.config.CrawlProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ExtractorTest {
    private val extractor = Extractor(CrawlProperties())

    private val html = """
        <ul class="list">
          <li><a href="/p/103">모니터 특가</a></li>
          <li><a href="https://other.com/p/102"> 충전기   할인 </a></li>
          <li><span class="title">링크 없는 글</span></li>
          <li><a href="/p/103">모니터 특가 (중복)</a></li>
        </ul>
    """.trimIndent()

    @Test
    @DisplayName("CSS selector 로 텍스트와 절대 경로 링크를 뽑는다")
    fun css() {
        val items = extractor.extract(html, "https://shop.example.com/hotdeal", "ul.list li a", SelectorType.CSS)

        assertThat(items.map { it.text }).containsExactly("모니터 특가", "충전기 할인")
        assertThat(items.map { it.url }).containsExactly("https://shop.example.com/p/103", "https://other.com/p/102")
    }

    @Test
    @DisplayName("링크가 같으면 같은 항목으로 본다 (해시 = url)")
    fun dedupByUrl() {
        val items = extractor.extract(html, "https://shop.example.com", "a", SelectorType.CSS)

        assertThat(items).hasSize(2)
    }

    @Test
    @DisplayName("선택한 요소 안의 링크도 찾고, 링크가 없으면 텍스트만 쓴다")
    fun linkInsideAndNoLink() {
        val items = extractor.extract(html, "https://shop.example.com", "ul.list li", SelectorType.CSS)

        assertThat(items.map { it.url }).containsExactly("https://shop.example.com/p/103", "https://other.com/p/102", null)
        assertThat(items[2].text).isEqualTo("링크 없는 글")
    }

    @Test
    @DisplayName("XPath 도 된다")
    fun xpath() {
        val items = extractor.extract(html, "https://shop.example.com", "//ul[@class='list']/li/a", SelectorType.XPATH)

        assertThat(items).hasSize(2)
    }

    @Test
    @DisplayName("selector 문법이 틀리면 사용자에게 보여줄 오류를 던진다")
    fun invalidSelector() {
        assertThatThrownBy { extractor.extract(html, "https://a.b", "ul[[[", SelectorType.CSS) }
            .isInstanceOf(BotException::class.java)
    }
}
