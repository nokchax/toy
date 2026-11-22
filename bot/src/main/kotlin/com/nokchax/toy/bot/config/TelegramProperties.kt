package com.nokchax.toy.bot.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("toy.telegram")
data class TelegramProperties(
    /** 비어 있으면 텔레그램 연결 없이 뜬다 (로컬 개발, 테스트) */
    val token: String = "",
    /** 봇을 쓸 수 있는 chat id. 비어 있으면 누구에게나 자기 chat id 만 알려준다. */
    val allowedChatIds: Set<Long> = emptySet(),
) {
    val enabled: Boolean get() = token.isNotBlank()
}
