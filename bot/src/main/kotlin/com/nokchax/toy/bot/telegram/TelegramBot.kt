package com.nokchax.toy.bot.telegram

import com.nokchax.toy.bot.command.BotRequest
import com.nokchax.toy.bot.command.CommandDispatcher
import com.nokchax.toy.bot.command.CommandRegistry
import com.nokchax.toy.bot.command.TextResponse
import com.nokchax.toy.bot.config.TelegramProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.concurrent.Executors

/**
 * 텔레그램 메시지 수신 (long polling).
 * 메시지와 버튼 콜백을 BotRequest 로 바꿔 dispatcher 에 넘기고, 응답은 TelegramSender 로 보낸다.
 */
@Component
class TelegramBot(
    private val properties: TelegramProperties,
    private val dispatcher: CommandDispatcher,
    private val registry: CommandRegistry,
    private val sender: TelegramSender,
) : LongPollingSingleThreadUpdateConsumer,
    SmartLifecycle {

    private val log = KotlinLogging.logger {}
    private val executor = Executors.newVirtualThreadPerTaskExecutor()
    private var application: TelegramBotsLongPollingApplication? = null

    override fun start() {
        if (!properties.enabled) {
            log.warn { "toy.telegram.token 이 비어 있어서 텔레그램에 연결하지 않음 (allowed chats: ${properties.allowedChatIds})" }
            return
        }
        application = TelegramBotsLongPollingApplication().also { it.registerBot(properties.token, this) }
        sender.registerCommands(registry)
        log.info { "telegram bot started (allowed chats: ${properties.allowedChatIds})" }
    }

    override fun stop() {
        application?.close()
        application = null
        executor.shutdown()
    }

    override fun isRunning(): Boolean = application != null

    /** 처리 시간이 긴 명령(크롤 미리보기 등)이 다른 메시지를 막지 않도록 메시지마다 가상 스레드에서 처리한다. */
    override fun consume(update: Update) {
        executor.submit { handle(update) }
    }

    private fun handle(update: Update) {
        val request = update.toRequest() ?: return
        log.info { "chat=${request.chatId} /${request.command} ${request.args} ${request.options}${if (request.isCallback) " (button)" else ""}" }

        request.callbackQueryId?.let { sender.answerCallback(it) }

        val response = dispatcher.dispatch(request) as? TextResponse ?: return
        try {
            if (request.isCallback && request.messageId != null) {
                sender.edit(request.chatId, request.messageId, response.text, response.buttons)
            } else {
                sender.send(request.chatId, response.text, response.buttons)
            }
        } catch (e: SendException) {
            if (!e.ignorable) log.warn { "reply failed chat=${request.chatId}: ${e.message}" }
        }
    }

    private fun Update.toRequest(): BotRequest? {
        if (hasCallbackQuery()) {
            val query = callbackQuery
            val message = query.message ?: return null
            return BotRequest.parse(
                text = query.data ?: return null,
                chatId = message.chatId,
                userId = query.from?.id,
                messageId = message.messageId,
                callbackQueryId = query.id,
            )
        }

        if (hasMessage() && message.hasText()) {
            return BotRequest.parse(
                text = message.text,
                chatId = message.chatId,
                userId = message.from?.id,
                messageId = message.messageId,
            )
        }

        return null
    }
}
