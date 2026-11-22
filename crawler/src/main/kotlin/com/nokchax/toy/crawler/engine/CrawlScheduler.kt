package com.nokchax.toy.crawler.engine

import com.nokchax.toy.common.domain.crawl.CronSchedule
import com.nokchax.toy.common.domain.crawl.CrawlRepository
import com.nokchax.toy.crawler.config.CrawlProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

@Component
class CrawlClaimer(
    private val crawlRepository: CrawlRepository,
) {
    /** 실행할 때가 된 크롤을 찾아 다음 실행 시각을 먼저 옮기고(선점) id 를 돌려준다. */
    @Transactional
    fun claimDue(now: ZonedDateTime): List<Long> =
        crawlRepository.findDue(now).filter { crawl ->
            crawlRepository.claim(crawl.id, now, CronSchedule.next(crawl.cron, now)) == 1
        }.map { it.id }
}

/**
 * 매분 0초에 실행할 크롤을 골라 가상 스레드로 돌린다.
 * "next_run_at <= 지금" 으로 고르기 때문에 재시작 중 놓친 크롤도 다음 tick 에 실행된다.
 */
@Component
class CrawlScheduler(
    private val claimer: CrawlClaimer,
    private val runner: CrawlRunner,
    private val clock: Clock,
    properties: CrawlProperties,
) {
    private val log = KotlinLogging.logger {}
    private val executor = Executors.newVirtualThreadPerTaskExecutor()
    private val permits = Semaphore(properties.concurrency)
    private val running = ConcurrentHashMap.newKeySet<Long>()

    @Scheduled(cron = "\${toy.crawl.tick-cron:0 * * * * *}")
    fun tick() {
        val ids = claimer.claimDue(ZonedDateTime.now(clock))
        if (ids.isNotEmpty()) log.info { "due crawls: $ids" }

        ids.filter { running.add(it) }.forEach { id ->
            executor.submit {
                permits.acquire()
                try {
                    runner.run(id)
                } catch (e: Exception) {
                    log.error(e) { "crawl#$id crashed" }
                } finally {
                    permits.release()
                    running.remove(id)
                }
            }
        }
    }

    @PreDestroy
    fun shutdown() {
        executor.shutdown()
    }
}
