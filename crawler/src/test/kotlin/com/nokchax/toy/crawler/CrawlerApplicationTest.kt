package com.nokchax.toy.crawler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class CrawlerApplicationTest(
    @Autowired private val context: ApplicationContext,
) {

    @Test
    fun `앱이 뜨고 스케줄러가 등록된다`() {
        assertThat(context.getBean(CrawlScheduler::class.java)).isNotNull()
    }

    @Test
    fun `텔레그램으로 보내는 코드가 없다 (발송은 bot 앱만)`() {
        assertThat(context.beanDefinitionNames.filter { it.contains("telegram", ignoreCase = true) }).isEmpty()
        assertThat(runCatching { Class.forName("org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient") }.isFailure).isTrue()
    }
}
