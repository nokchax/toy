package com.nokchax.toy.bot.telegram

import com.nokchax.toy.bot.command.Button
import com.nokchax.toy.bot.command.CommandRegistry
import com.nokchax.toy.bot.config.TelegramProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * 텔레그램으로 메시지를 보내는 유일한 창구.
 * 명령어 응답과 크롤 알림 모두 여기를 거치므로 전송 한도(같은 채팅 초당 1건)를 한곳에서 지킨다.
 */
@Component
class TelegramSender(
    private val properties: TelegramProperties,
) {
    private val log = KotlinLogging.logger {}
    private val client by lazy { OkHttpTelegramClient(properties.token) }
    private val lastSentAt = ConcurrentHashMap<Long, Long>()
    private val locks = ConcurrentHashMap<Long, Any>()

    val enabled: Boolean get() = properties.enabled

    fun send(chatId: Long, text: String, buttons: List<List<Button>> = emptyList(), preview: Boolean = false) =
        throttled(chatId) {
            client.execute(
                SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .disableWebPagePreview(!preview)
                    .apply { if (buttons.isNotEmpty()) replyMarkup(buttons.toMarkup()) }
                    .build()
            )
        }

    fun edit(chatId: Long, messageId: Int, text: String, buttons: List<List<Button>> = emptyList()) =
        throttled(chatId) {
            client.execute(
                EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(text)
                    .disableWebPagePreview(true)
                    .apply { if (buttons.isNotEmpty()) replyMarkup(buttons.toMarkup()) }
                    .build()
            )
        }

    /** 버튼을 누른 뒤 도는 로딩 표시를 끈다. 실패해도 무시. */
    fun answerCallback(callbackQueryId: String) {
        try {
            client.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackQueryId).build())
        } catch (e: TelegramApiException) {
            log.debug { "answerCallback failed: ${e.message}" }
        }
    }

    /** 텔레그램 입력창의 "/" 메뉴에 명령어 목록을 등록한다. */
    fun registerCommands(registry: CommandRegistry) {
        try {
            client.execute(
                SetMyCommands.builder()
                    .commands(registry.commands.map { BotCommand(it.name, it.description.ifBlank { it.name }) })
                    .build()
            )
        } catch (e: TelegramApiException) {
            log.warn { "setMyCommands failed: ${e.message}" }
        }
    }

    private fun throttled(chatId: Long, block: () -> Unit) {
        synchronized(locks.computeIfAbsent(chatId) { Any() }) {
            val wait = (lastSentAt[chatId] ?: 0) + MIN_INTERVAL_MS - System.currentTimeMillis()
            if (wait > 0) Thread.sleep(wait)
            try {
                block()
            } catch (e: TelegramApiRequestException) {
                throw e.toSendException()
            } catch (e: TelegramApiException) {
                throw SendException("${e.javaClass.simpleName}: ${e.message}", permanent = false, cause = e)
            } finally {
                lastSentAt[chatId] = System.currentTimeMillis()
            }
        }
    }

    private fun TelegramApiRequestException.toSendException(): SendException {
        val description = apiResponse ?: message ?: "알 수 없는 오류"
        return when {
            description.contains("message is not modified") -> SendException(description, permanent = true, ignorable = true)
            errorCode == 429 -> SendException(description, permanent = false, retryAfter = parameters?.retryAfter?.let { Duration.ofSeconds(it.toLong()) })
            errorCode == 403 || errorCode == 400 -> SendException(description, permanent = true)
            else -> SendException(description, permanent = false)
        }.also { it.initCause(this) }
    }

    private fun List<List<Button>>.toMarkup(): InlineKeyboardMarkup =
        InlineKeyboardMarkup(
            map { row ->
                InlineKeyboardRow(row.map { InlineKeyboardButton.builder().text(it.text).callbackData(it.callback).build() })
            }
        )

    companion object {
        private const val MIN_INTERVAL_MS = 1_000L
    }
}

class SendException(
    message: String,
    /** 다시 보내도 소용없는 실패 (봇 차단, 잘못된 요청) */
    val permanent: Boolean,
    val retryAfter: Duration? = null,
    /** "내용이 같아서 수정할 게 없음" 처럼 무시해도 되는 실패 */
    val ignorable: Boolean = false,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
