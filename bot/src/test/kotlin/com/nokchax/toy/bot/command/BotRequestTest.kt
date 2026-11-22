package com.nokchax.toy.bot.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BotRequestTest {

    @Test
    fun `명령어 인자 옵션을 나눈다`() {
        val request = BotRequest.parse("/crawl add 핫딜 https://a.b \"ul.list li a\" -cron 10m -xpath", chatId = 1)!!

        assertThat(request.command).isEqualTo("crawl")
        assertThat(request.args).containsExactly("add", "핫딜", "https://a.b", "ul.list li a")
        assertThat(request.options).containsEntry("cron", "10m").containsEntry("xpath", "true")
    }

    @Test
    fun `따옴표 안의 옵션 모양 값과 공백을 유지한다`() {
        val request = BotRequest.parse("/cr cron 1 -cron \"*/10 * * * *\" '-x'", chatId = 1)!!

        assertThat(request.option("cron")).isEqualTo("*/10 * * * *")
        assertThat(request.args).containsExactly("cron", "1", "-x")
    }

    @Test
    fun `봇 이름이 붙은 명령과 음수, 아이폰 따옴표`() {
        val request = BotRequest.parse("/Crawl@toy_bot filter 3 add contains “특가 세일” -5", chatId = 1)!!

        assertThat(request.command).isEqualTo("crawl")
        assertThat(request.args).containsExactly("filter", "3", "add", "contains", "특가 세일", "-5")
    }

    @Test
    fun `따옴표 없이 적은 selector 는 rest 로 이어 붙인다`() {
        val request = BotRequest.parse("/cr add 핫딜 https://a.b ul.list li a", chatId = 1)!!.shift()

        assertThat(request.rest(2, "usage")).isEqualTo("ul.list li a")
    }

    @Test
    fun `명령이 아닌 메시지는 무시한다`() {
        assertThat(BotRequest.parse("안녕", chatId = 1)).isNull()
        assertThat(BotRequest.parse("/", chatId = 1)).isNull()
    }
}
