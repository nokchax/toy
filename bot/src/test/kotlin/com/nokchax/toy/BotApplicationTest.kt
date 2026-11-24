package com.nokchax.toy

import com.nokchax.toy.bot.command.BotRequest
import com.nokchax.toy.bot.command.CommandDispatcher
import com.nokchax.toy.bot.command.CommandRegistry
import com.nokchax.toy.bot.command.TextResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class BotApplicationTest(
    @Autowired private val registry: CommandRegistry,
    @Autowired private val dispatcher: CommandDispatcher,
) {

    @Test
    fun `앱이 뜨고 명령어가 등록된다`() {
        assertThat(registry.commands.map { it.name }).containsExactly("crawl", "help")
        assertThat(registry.find("cr")?.name).isEqualTo("crawl")
    }

    @Test
    fun `허용된 채팅에서 crawl list 가 동작한다`() {
        val response = dispatcher.dispatch(BotRequest.parse("/cr list", chatId = 100)!!) as TextResponse

        assertThat(response.text).contains("등록된 크롤이 없다옹")
    }

    @Test
    fun `허용되지 않은 채팅은 무시한다`() {
        assertThat(dispatcher.dispatch(BotRequest.parse("/cr list", chatId = 999)!!)).isNull()
    }

    @Test
    fun `사용법이 틀리면 사용법을 알려준다`() {
        val response = dispatcher.dispatch(BotRequest.parse("/cr show abc", chatId = 100)!!) as TextResponse

        assertThat(response.text).isEqualTo("/crawl show <id>")
    }
}
