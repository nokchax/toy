package com.nokchax.toy.crawler

import com.nokchax.toy.crawl.config.CrawlProperties
import com.nokchax.toy.crawl.engine.CrawlClaimer
import com.nokchax.toy.crawl.engine.CrawlRunner
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

/**
 * 매분 0초에 실행할 크롤을 골라 가상 스레드로 돌린다.
 * "next_run_at <= 지금" 으로 고르기 때문에 재시작 중 놓친 크롤도 다음 tick 에 실행된다.
 * 알림은 알림 큐에 쌓기만 하고, 텔레그램 발송은 bot 앱이 한다.
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
