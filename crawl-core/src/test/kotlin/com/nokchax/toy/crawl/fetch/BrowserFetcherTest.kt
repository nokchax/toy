package com.nokchax.toy.crawl.fetch

import com.nokchax.toy.crawl.config.CrawlProperties
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration

/** 실제 브라우저가 필요한 경우는 테스트하지 않는다 (CI 에 브라우저가 없음). 연결 실패 처리만 본다. */
class BrowserFetcherTest {

    @Test
    fun `브라우저 주소가 없으면 FetchException 이 된다`() {
        val fetcher = BrowserFetcher(CrawlProperties())

        assertThatThrownBy { fetcher.fetch("https://example.com", null) }
            .isInstanceOf(FetchException::class.java).hasMessageContaining("설정되지 않음")
    }

    @Test
    fun `브라우저에 연결하지 못하면 FetchException 이 된다`() {
        val fetcher = BrowserFetcher(CrawlProperties(browser = CrawlProperties.Browser(url = "http://127.0.0.1:1", readTimeout = Duration.ofSeconds(3))))

        assertThatThrownBy { fetcher.fetch("https://example.com", null) }
            .isInstanceOf(FetchException::class.java).hasMessageContaining("연결하지 못함")
    }
}
