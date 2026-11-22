package com.nokchax.toy.crawler.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableConfigurationProperties(CrawlProperties::class)
class CrawlerConfig

/** 테스트에서는 toy.scheduling.enabled=false 로 스케줄러를 끈다. */
@Configuration
@EnableScheduling
@ConditionalOnProperty("toy.scheduling.enabled", havingValue = "true", matchIfMissing = true)
class SchedulingConfig
