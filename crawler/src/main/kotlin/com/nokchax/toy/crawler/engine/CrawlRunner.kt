package com.nokchax.toy.crawler.engine

import com.nokchax.toy.common.domain.crawl.CrawlRepository
import com.nokchax.toy.common.domain.crawl.CrawlStatus
import com.nokchax.toy.common.exception.BotException
import com.nokchax.toy.crawler.extract.Extractor
import com.nokchax.toy.crawler.fetch.FetchException
import com.nokchax.toy.crawler.fetch.Fetcher
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.ZonedDateTime

/**
 * 크롤 하나를 실행한다: 가져오기 → 뽑기 → (Recorder) 거르기 → 새 항목 판별 → 알림 저장.
 * 네트워크 작업은 트랜잭션 밖에서 하고, 결과 저장만 트랜잭션으로 묶는다.
 */
@Component
class CrawlRunner(
    private val crawlRepository: CrawlRepository,
    private val fetcher: Fetcher,
    private val extractor: Extractor,
    private val recorder: CrawlResultRecorder,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    fun run(crawlId: Long): CrawlRunResult {
        val crawl = crawlRepository.findWithPageById(crawlId)
            ?: return CrawlRunResult(CrawlStatus.FAILURE, error = "크롤이 없다옹")
        val startedAt = ZonedDateTime.now(clock)

        val error = try {
            val page = fetcher.fetch(crawl.page.url)
            val items = extractor.extract(page.html, page.url, crawl.selector, crawl.selectorType)
            if (items.isNotEmpty()) {
                return recorder.recordSuccess(crawl, items, startedAt)
                    .also { log.info { "crawl#$crawlId ok items=${it.itemCount} notified=${it.notifiedCount}" } }
            }
            "selector 에 걸리는 항목이 없음 (페이지 구조가 바뀌었을 수 있음)"
        } catch (e: FetchException) {
            e.message ?: "페이지를 가져오지 못함"
        } catch (e: BotException) {
            e.message ?: "크롤 설정 오류"
        } catch (e: Exception) {
            log.error(e) { "crawl#$crawlId unexpected error" }
            "${e.javaClass.simpleName}: ${e.message}"
        }

        log.warn { "crawl#$crawlId failed: $error" }
        return recorder.recordFailure(crawl, startedAt, error)
    }
}
