package com.nokchax.toy.crawl.fetch

import com.nokchax.toy.crawl.config.CrawlProperties
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class HttpFetcher(
    private val properties: CrawlProperties,
) : Fetcher {

    override fun fetch(url: String): FetchedPage {
        try {
            val response = Jsoup.connect(url)
                .userAgent(properties.userAgent)
                .timeout(properties.timeout.toMillis().toInt())
                .maxBodySize(properties.maxBodyBytes)
                .followRedirects(true)
                .execute()

            return FetchedPage(response.url().toString(), response.body())
        } catch (e: HttpStatusException) {
            throw FetchException("HTTP ${e.statusCode}", e)
        } catch (e: IOException) {
            throw FetchException("${e.javaClass.simpleName}: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw FetchException("잘못된 url: $url", e)
        }
    }
}
