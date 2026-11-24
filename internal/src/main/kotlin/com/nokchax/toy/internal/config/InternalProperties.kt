package com.nokchax.toy.internal.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("toy.internal")
data class InternalProperties(
    /** REST API · MCP 호출에 필요한 키. 비어 있으면 /health 외 모든 요청을 거부한다. */
    val apiKey: String = "",
)
