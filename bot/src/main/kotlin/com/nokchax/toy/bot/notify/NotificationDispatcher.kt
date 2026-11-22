package com.nokchax.toy.bot.notify

import com.nokchax.toy.bot.telegram.SendException
import com.nokchax.toy.bot.telegram.TelegramSender
import com.nokchax.toy.common.domain.notification.NotificationMessage
import com.nokchax.toy.common.domain.notification.NotificationRepository
import com.nokchax.toy.common.domain.notification.NotificationStatus
import com.nokchax.toy.common.domain.notification.NotificationsEnqueued
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Clock
import java.time.ZonedDateTime
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 알림 큐(notification_messages)의 PENDING 을 꺼내 텔레그램으로 보낸다.
 * - crawler 가 커밋하면 바로 보낸다 (AFTER_COMMIT 이벤트)
 * - 실패한 건은 30초마다 다시 훑어서 재시도한다
 */
@Component
class NotificationDispatcher(
    private val repository: NotificationRepository,
    private val sender: TelegramSender,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}
    private val lock = ReentrantLock()

    @TransactionalEventListener
    fun onEnqueued(event: NotificationsEnqueued) = dispatchPending()

    @Scheduled(fixedDelayString = "\${toy.notification.sweep-interval:30s}")
    fun sweep() = dispatchPending()

    fun dispatchPending(): Int {
        if (!sender.enabled) return 0

        return lock.withLock {
            repository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderById(NotificationStatus.PENDING, ZonedDateTime.now(clock))
                .count { deliver(it) }
        }
    }

    private fun deliver(message: NotificationMessage): Boolean {
        val sent = try {
            sender.send(message.chatId, message.body, preview = true)
            message.markSent(ZonedDateTime.now(clock))
            true
        } catch (e: SendException) {
            log.warn { "notification#${message.id} failed (permanent=${e.permanent}): ${e.message}" }
            if (e.permanent) message.markFailed(e.message ?: "") else message.markRetry(ZonedDateTime.now(clock), e.message ?: "", e.retryAfter)
            false
        }

        repository.save(message)
        return sent
    }
}
