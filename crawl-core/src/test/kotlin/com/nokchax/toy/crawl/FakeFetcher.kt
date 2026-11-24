package com.nokchax.toy.crawl

import com.nokchax.toy.crawl.fetch.FetchException
import com.nokchax.toy.crawl.fetch.FetchedPage
import com.nokchax.toy.crawl.fetch.Fetcher
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/** 네트워크 대신 미리 넣어둔 HTML 을 돌려준다. */
class FakeFetcher : Fetcher {
    private val pages = mutableMapOf<String, String>()
    private val failures = mutableMapOf<String, String>()

    fun page(url: String, html: String) {
        failures.remove(url)
        pages[url] = html
    }

    fun fail(url: String, message: String) {
        failures[url] = message
    }

    override fun fetch(url: String): FetchedPage {
        failures[url]?.let { throw FetchException(it) }
        return FetchedPage(url, pages[url] ?: throw FetchException("HTTP 404"))
    }
}

@TestConfiguration
class FakeFetcherConfig {
    @Bean
    @Primary
    fun fakeFetcher() = FakeFetcher()
}
