package com.nokchax.toy.common.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Optional

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
class DbConfig {

    @Bean
    @ConditionalOnMissingBean
    fun clock(): Clock = Clock.system(ZoneId.of("Asia/Seoul"))

    @Bean
    fun auditingDateTimeProvider(clock: Clock) = DateTimeProvider { Optional.of(ZonedDateTime.now(clock)) }
}
