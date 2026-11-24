package com.nokchax.toy.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("toy")
data class ToyProperties(
    /**
     * 인터널 · 익스터널이 다루는 크롤의 주인 chat id (텔레그램 chat id).
     * 크롤은 chat 단위로 저장되고 알림도 이 chat 으로 가므로, 봇에서 쓰는 내 chat id 와 같게 둔다.
     */
    val ownerChatId: Long = 0,
)
