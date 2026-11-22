package com.nokchax.toy.common.domain.notification

import com.nokchax.toy.common.domain.BaseEntity
import com.nokchax.toy.common.function.ellipsis
import jakarta.persistence.*
import java.time.Duration
import java.time.ZonedDateTime

/**
 * 보낼 알림(아웃박스).
 * crawler 는 여기에 한 줄 남기는 것까지만 하고, 실제 발송은 bot 이 한다.
 */
@Entity
@Table(name = "notification_messages")
class NotificationMessage(
    @Column(nullable = false)
    val chatId: Long,

    body: String,

    val crawlId: Long? = null,

    nextAttemptAt: ZonedDateTime,
) : BaseEntity() {

    @Column(nullable = false, length = MAX_LENGTH)
    val body: String = body.ellipsis(MAX_LENGTH)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: NotificationStatus = NotificationStatus.PENDING
        protected set

    @Column(nullable = false)
    var retryCount: Int = 0
        protected set

    @Column(nullable = false)
    var nextAttemptAt: ZonedDateTime = nextAttemptAt
        protected set

    var sentAt: ZonedDateTime? = null
        protected set

    @Column(length = 1000)
    var lastError: String? = null
        protected set

    fun markSent(now: ZonedDateTime) {
        status = NotificationStatus.SENT
        sentAt = now
    }

    /** 재시도할 수 없는 실패(봇 차단 등) */
    fun markFailed(error: String) {
        status = NotificationStatus.FAILED
        lastError = error.ellipsis(1000)
    }

    /** 일시적인 실패. 1분, 2분, 4분 … 간격으로 다시 시도하고, 한도를 넘으면 FAILED. */
    fun markRetry(now: ZonedDateTime, error: String, retryAfter: Duration? = null) {
        retryCount += 1
        lastError = error.ellipsis(1000)
        if (retryCount >= MAX_RETRIES) {
            status = NotificationStatus.FAILED
            return
        }
        nextAttemptAt = now.plus(retryAfter ?: Duration.ofMinutes(1L shl (retryCount - 1)))
    }

    companion object {
        const val MAX_LENGTH = 4096
        const val MAX_RETRIES = 6
    }
}

enum class NotificationStatus {
    PENDING,
    SENT,
    FAILED,
}
