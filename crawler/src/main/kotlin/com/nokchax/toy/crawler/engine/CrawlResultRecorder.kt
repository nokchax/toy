package com.nokchax.toy.crawler.engine

import com.nokchax.toy.common.domain.crawl.*
import com.nokchax.toy.common.domain.notification.NotificationMessage
import com.nokchax.toy.common.domain.notification.NotificationRepository
import com.nokchax.toy.common.domain.notification.NotificationsEnqueued
import com.nokchax.toy.common.function.ellipsis
import com.nokchax.toy.crawler.config.CrawlProperties
import com.nokchax.toy.crawler.extract.ExtractedItem
import com.nokchax.toy.crawler.extract.resultHash
import com.nokchax.toy.crawler.extract.snapshot
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime

/**
 * 크롤 결과를 하나의 트랜잭션으로 저장한다.
 * 이미 본 항목, 보낼 알림, 실행 기록이 함께 커밋되므로 알림이 유실되거나 중복되지 않는다.
 */
@Component
class CrawlResultRecorder(
    private val crawlRepository: CrawlRepository,
    private val itemRepository: CrawlItemRepository,
    private val historyRepository: CrawlHistoryRepository,
    private val notificationRepository: NotificationRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val properties: CrawlProperties,
    private val clock: Clock,
) {

    @Transactional
    fun recordSuccess(crawl: Crawl, items: List<ExtractedItem>, startedAt: ZonedDateTime): CrawlRunResult {
        val now = ZonedDateTime.now(clock)
        val seen = if (items.isEmpty()) emptySet() else itemRepository.findSeenHashes(crawl.id, items.map { it.hash })

        // 필터와 상관없이 뽑은 항목은 모두 "이미 본 항목"으로 저장한다.
        // 나중에 필터를 바꿔도 예전 글이 새 글로 쏟아지지 않게 하기 위함.
        itemRepository.saveAll(
            items.filter { it.hash !in seen }.map { CrawlItem(crawl.id, it.hash, it.text, it.url) }
        )

        val passed = items.applyFilters(crawl.filters)
        val hash = passed.resultHash()

        val toNotify = when {
            // 처음 실행이면 기준값만 저장하고 알리지 않는다 (DB 에 직접 등록한 경우 포함)
            crawl.lastRunAt == null -> null
            crawl.notifyMode == NotifyMode.NEW_ITEMS -> passed.filter { it.hash !in seen }.ifEmpty { null }
            crawl.notifyMode == NotifyMode.CHANGED -> passed.takeIf { hash != crawl.lastHash }
            else -> passed
        }

        toNotify?.let {
            val body = if (crawl.notifyMode == NotifyMode.CHANGED) {
                MessageRenderer.changed(crawl.name, crawl.lastSnapshot?.lines(), it, properties.maxItemsPerMessage)
            } else {
                MessageRenderer.items(crawl.name, crawl.notifyMode, it, properties.maxItemsPerMessage)
            }
            enqueue(crawl.chatId, crawl.id, body, now)
        }

        crawlRepository.markSuccess(crawl.id, now, hash, passed.snapshot())
        historyRepository.save(
            CrawlHistory(
                crawlId = crawl.id,
                startedAt = startedAt,
                durationMs = Duration.between(startedAt, now).toMillis(),
                status = CrawlStatus.SUCCESS,
                itemCount = items.size,
                newCount = toNotify?.size ?: 0,
            )
        )

        return CrawlRunResult(CrawlStatus.SUCCESS, items.size, passed.size, toNotify?.size ?: 0)
    }

    @Transactional
    fun recordFailure(crawl: Crawl, startedAt: ZonedDateTime, error: String): CrawlRunResult {
        val now = ZonedDateTime.now(clock)
        val message = error.ellipsis(1000)

        historyRepository.save(
            CrawlHistory(
                crawlId = crawl.id,
                startedAt = startedAt,
                durationMs = Duration.between(startedAt, now).toMillis(),
                status = CrawlStatus.FAILURE,
                error = message,
            )
        )
        crawlRepository.increaseFailCount(crawl.id, now)

        val failures = crawlRepository.findFailCount(crawl.id) ?: 0
        val disabled = failures >= properties.maxFailures
        if (disabled && crawlRepository.deactivate(crawl.id, now) > 0) {
            enqueue(crawl.chatId, crawl.id, MessageRenderer.disabled(crawl.name, crawl.id, failures, message), now)
        }

        return CrawlRunResult(CrawlStatus.FAILURE, error = message, disabled = disabled)
    }

    private fun enqueue(chatId: Long, crawlId: Long, body: String, now: ZonedDateTime) {
        notificationRepository.save(NotificationMessage(chatId, body, crawlId, now))
        eventPublisher.publishEvent(NotificationsEnqueued)
    }
}

data class CrawlRunResult(
    val status: CrawlStatus,
    val itemCount: Int = 0,
    val passedCount: Int = 0,
    val notifiedCount: Int = 0,
    val error: String? = null,
    val disabled: Boolean = false,
)
