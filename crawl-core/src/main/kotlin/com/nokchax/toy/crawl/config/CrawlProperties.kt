package com.nokchax.toy.crawl.config

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
    val browser: Browser = Browser(),
) {
    /** FetchMethod.BROWSER 로 가져올 때 붙는 원격 브라우저 (compose 의 browser 서비스) */
    data class Browser(
        /** Selenium 주소. 예: http://browser:4444. 비어 있으면 브라우저로 가져오기는 실패한다 */
        val url: String? = null,
        val pageLoadTimeout: Duration = Duration.ofSeconds(30),
        /** selector 에 걸리는 요소가 나타날 때까지 기다리는 최대 시간 */
        val waitTimeout: Duration = Duration.ofSeconds(10),
        /** 브라우저 응답을 기다리는 최대 시간. 세션이 꽉 차서 줄을 서는 시간도 포함한다 */
        val readTimeout: Duration = Duration.ofMinutes(2),
    )
}
