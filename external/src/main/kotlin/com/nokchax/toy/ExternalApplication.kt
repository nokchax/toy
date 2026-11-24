package com.nokchax.toy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/** 익스터널: 웹 페이지. NAS 리버스 프록시로 외부에 공개되므로 /health 외에는 로그인이 필요하다. */
@SpringBootApplication
@ConfigurationPropertiesScan
class ExternalApplication

fun main(args: Array<String>) {
    runApplication<ExternalApplication>(*args)
}
