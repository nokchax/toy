package com.nokchax.toy.bot.notify

import com.nokchax.toy.bot.telegram.TelegramSender
import com.nokchax.toy.common.domain.notification.NotificationMessage
import com.nokchax.toy.common.domain.notification.NotificationRepository
import com.nokchax.toy.common.domain.notification.NotificationStatus
import com.nokchax.toy.common.domain.notification.NotificationsEnqueued
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZonedDateTime

@SpringBootTest
@ActiveProfiles("test")
class NotificationDispatcherTest(
    @Autowired private val dispatcher: NotificationDispatcher,
    @Autowired private val repository: NotificationRepository,
    @Autowired private val eventPublisher: ApplicationEventPublisher,
    @Autowired private val transaction: TransactionTemplate,
) {
    @MockitoBean
    private lateinit var sender: TelegramSender

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
        `when`(sender.enabled).thenReturn(true)
    }

    @Test
    fun `커밋 직후 보낸 알림은 SENT 로 저장되고 sweep 에서 다시 보내지 않는다`() {
        // crawler 의 CrawlResultRecorder 와 같은 방식: 트랜잭션 안에서 저장하고 이벤트를 발행한다
        val id =
            transaction.execute {
                val message = repository.save(NotificationMessage(100, "새 항목", 1, ZonedDateTime.now()))
                eventPublisher.publishEvent(NotificationsEnqueued)
                message.id
            }

        assertThat(repository.findById(id).get().status).isEqualTo(NotificationStatus.SENT)

        dispatcher.sweep()
        verify(sender, times(1)).send(anyLong(), anyString(), anyList(), anyBoolean())
    }
}
