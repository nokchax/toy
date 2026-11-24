package com.nokchax.toy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/** 크롤러: 주기에 맞춰 크롤을 돌리고 결과·알림을 DB 에 저장한다. 웹 서버 없음. */
@SpringBootApplication
@ConfigurationPropertiesScan
class CrawlerApplication

fun main(args: Array<String>) {
    runApplication<CrawlerApplication>(*args)
}
