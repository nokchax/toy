package com.nokchax.toy.crawler.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("toy.crawl")
data class CrawlProperties(
    val userAgent: String = "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36",
    val timeout: Duration = Duration.ofSeconds(10),
    val maxBodyBytes: Int = 5 * 1024 * 1024,
    /** 한 번에 다루는 최대 항목 수 */
    val maxItems: Int = 200,
    /** 연속으로 이만큼 실패하면 크롤을 끈다 */
    val maxFailures: Int = 5,
    val maxCrawlsPerChat: Int = 30,
    val previewSize: Int = 5,
    /** 알림 한 건에 담는 최대 항목 수 */
    val maxItemsPerMessage: Int = 10,
    /** 동시에 실행하는 크롤 수 */
    val concurrency: Int = 4,
)
