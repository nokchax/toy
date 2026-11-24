package com.nokchax.toy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/** 인터널: 크롤을 다루는 REST API 와 MCP 서버. 외부에 공개하지 않는다 (NAS 127.0.0.1 에만 바인딩). */
@SpringBootApplication
@ConfigurationPropertiesScan
class InternalApplication

fun main(args: Array<String>) {
    runApplication<InternalApplication>(*args)
}
