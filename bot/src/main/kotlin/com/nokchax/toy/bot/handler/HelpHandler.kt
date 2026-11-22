package com.nokchax.toy.bot.handler

import com.nokchax.toy.bot.command.*
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

@Component
@BotCommand("help", aliases = ["start", "h"], description = "도움말")
class HelpHandler(
    private val registry: ObjectProvider<CommandRegistry>,
) : CommandHandler {

    @SubCommand("")
    fun help(): TextResponse {
        val text = registry.getObject().commands
            .filter { it.name != "help" }
            .joinToString("\n\n") { it.usage() }

        return TextResponse("무엇을 도와줄까옹?\n\n$text", buttons(listOf(Button("📋 크롤 목록", "/cr list"))))
    }
}
