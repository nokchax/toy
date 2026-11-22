package com.nokchax.toy.bot.command

import com.nokchax.toy.bot.config.TelegramProperties
import com.nokchax.toy.common.exception.BotException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/** 권한 확인 → 명령어 찾기 → 실행 → 응답 변환. 예외도 여기서 사용자 메시지로 바꾼다. */
@Component
class CommandDispatcher(
    private val registry: CommandRegistry,
    private val properties: TelegramProperties,
) {
    private val log = KotlinLogging.logger {}

    /** null 이면 아무 응답도 하지 않는다 (허용되지 않은 채팅). */
    fun dispatch(request: BotRequest): BotResponse? {
        if (properties.allowedChatIds.isEmpty()) {
            return TextResponse("아직 허용된 채팅이 없다옹.\n이 채팅의 id: ${request.chatId}\nTOY_TELEGRAM_ALLOWED_CHAT_IDS 에 추가해줘라옹")
        }
        if (request.chatId !in properties.allowedChatIds) {
            log.warn { "ignored chat=${request.chatId} command=${request.command}" }
            return null
        }

        val command = registry.find(request.command)
            ?: return TextResponse("모르는 명령이다옹. /help 를 봐줘라옹")

        return try {
            val (sub, subRequest) = command.resolve(request)
            when (val result = sub.invoke(subRequest)) {
                is BotResponse -> result
                is String -> TextResponse(result)
                else -> TextResponse("완료다옹")
            }
        } catch (e: BotException) {
            TextResponse(e.message ?: "잘못된 요청이다옹")
        } catch (e: Exception) {
            log.error(e) { "command failed: /${request.command} ${request.args}" }
            TextResponse("망가졌다옹… (${e.javaClass.simpleName}) 로그를 확인해줘라옹")
        }
    }
}
