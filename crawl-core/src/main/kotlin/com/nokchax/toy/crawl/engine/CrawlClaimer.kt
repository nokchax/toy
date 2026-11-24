package com.nokchax.toy.crawl.engine

import com.nokchax.toy.common.domain.crawl.CrawlRepository
import com.nokchax.toy.common.domain.crawl.CronSchedule
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class CrawlClaimer(
    private val crawlRepository: CrawlRepository,
) {
    /** 실행할 때가 된 크롤을 찾아 다음 실행 시각을 먼저 옮기고(선점) id 를 돌려준다. */
    @Transactional
    fun claimDue(now: ZonedDateTime): List<Long> = crawlRepository.findDue(now).filter { crawl ->
        crawlRepository.claim(crawl.id, now, CronSchedule.next(crawl.cron, now)) == 1
    }.map { it.id }
}
