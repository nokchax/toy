package com.nokchax.toy.bot.command

sealed interface BotResponse

/** 텍스트 응답. 버튼 콜백으로 들어온 요청이면 원래 메시지를 수정한다. */
data class TextResponse(
    val text: String,
    val buttons: List<List<Button>> = emptyList(),
) : BotResponse

data class Button(
    val text: String,
    /** 버튼을 누르면 이 명령이 그대로 들어온다. 텔레그램 제한 64바이트. */
    val callback: String,
) {
    init {
        require(callback.toByteArray().size <= 64) { "callback data 가 64바이트를 넘음: $callback" }
    }
}

fun buttons(vararg rows: List<Button>): List<List<Button>> = rows.filter { it.isNotEmpty() }
