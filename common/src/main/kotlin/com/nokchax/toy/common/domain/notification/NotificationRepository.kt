package com.nokchax.toy.common.domain.notification

import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface NotificationRepository : JpaRepository<NotificationMessage, Long> {
    fun findTop50ByStatusAndNextAttemptAtLessThanEqualOrderById(
        status: NotificationStatus,
        now: ZonedDateTime,
    ): List<NotificationMessage>
}
