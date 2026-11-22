package com.nokchax.toy.bot.command

import com.nokchax.toy.bot.config.TelegramProperties
import com.nokchax.toy.common.exception.botError
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CommandRegistryTest {

    @BotCommand("echo", aliases = ["e"], description = "따라하기")
    class EchoHandler : CommandHandler {
        @SubCommand("")
        fun default() = "기본"

        @SubCommand("say", aliases = ["s"], usage = "/echo say <말>")
        fun say(request: BotRequest) = request.rest(0, "/echo say <말>")

        @SubCommand("buttons")
        fun buttons() = TextResponse("버튼", buttons(listOf(Button("a", "/echo s a"))))

        @SubCommand("fail")
        fun fail(): String = botError("사용자 오류")

        @SubCommand("crash")
        fun crash(): String = error("버그")
    }

    @BotCommand("dup")
    class DuplicateHandler : CommandHandler {
        @SubCommand("")
        fun default() = ""
    }

    private val dispatcher = CommandDispatcher(
        CommandRegistry(listOf(EchoHandler())),
        TelegramProperties(token = "", allowedChatIds = setOf(1)),
    )

    private fun reply(text: String) = (dispatcher.dispatch(BotRequest.parse(text, chatId = 1)!!) as TextResponse).text

    @Test
    fun `하위 명령과 별칭으로 찾아가고 없으면 기본 처리`() {
        assertThat(reply("/echo")).isEqualTo("기본")
        assertThat(reply("/e say 안녕 하세요")).isEqualTo("안녕 하세요")
        assertThat(reply("/echo s 안녕")).isEqualTo("안녕")
        assertThat(reply("/echo 모르는거")).isEqualTo("기본")
    }

    @Test
    fun `BotException 은 메시지 그대로, 다른 예외는 일반 오류로`() {
        assertThat(reply("/echo fail")).isEqualTo("사용자 오류")
        assertThat(reply("/echo crash")).contains("망가졌다옹", "IllegalStateException")
    }

    @Test
    fun `모르는 명령`() {
        assertThat(reply("/nope")).contains("/help")
    }

    @Test
    fun `허용 목록이 비어 있으면 chat id 를 알려준다`() {
        val open = CommandDispatcher(CommandRegistry(listOf(EchoHandler())), TelegramProperties())
        val response = open.dispatch(BotRequest.parse("/echo", chatId = 42)!!) as TextResponse

        assertThat(response.text).contains("42")
    }

    @Test
    fun `명령어 이름이 겹치면 시작할 때 실패한다`() {
        @BotCommand("echo")
        class Another : CommandHandler

        assertThatThrownBy { CommandRegistry(listOf(EchoHandler(), Another())) }.hasMessageContaining("/echo")
        assertThat(CommandRegistry(listOf(EchoHandler(), DuplicateHandler())).commands).hasSize(2)
    }

    @Test
    fun `버튼 callback 은 64바이트를 넘을 수 없다`() {
        assertThatThrownBy { Button("x", "/cr " + "가".repeat(30)) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
