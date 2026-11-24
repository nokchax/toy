package com.nokchax.toy.bot.command

import com.nokchax.toy.common.exception.botError

/**
 * 메시지와 버튼 콜백을 같은 모양으로 다룬다.
 *
 * "/crawl add 핫딜 https://a.b \"ul li a\" -cron 10m -xpath"
 *   → command=crawl, args=[add, 핫딜, https://a.b, ul li a], options={cron=10m, xpath=true}
 */
data class BotRequest(
    val chatId: Long,
    val userId: Long? = null,
    val messageId: Int? = null,
    val callbackQueryId: String? = null,
    val command: String,
    val args: List<String> = emptyList(),
    val options: Map<String, String> = emptyMap(),
) {
    val isCallback: Boolean get() = callbackQueryId != null

    fun arg(index: Int): String? = args.getOrNull(index)

    fun arg(index: Int, usage: String): String = arg(index) ?: botError(usage)

    fun longArg(index: Int, usage: String): Long = arg(index)?.toLongOrNull() ?: botError(usage)

    /** index 부터 끝까지 공백으로 이어 붙인다 (따옴표 없이 적은 selector, 필터 값 등) */
    fun rest(index: Int, usage: String): String = args.drop(index).joinToString(" ").ifBlank { botError(usage) }

    fun option(name: String): String? = options[name]

    fun hasOption(name: String): Boolean = options.containsKey(name)

    fun shift(): BotRequest = copy(args = args.drop(1))

    companion object {
        private val OPTION = Regex("^-([a-zA-Z][\\w-]*)$")

        fun parse(
            text: String,
            chatId: Long,
            userId: Long? = null,
            messageId: Int? = null,
            callbackQueryId: String? = null,
        ): BotRequest? {
            if (!text.startsWith("/")) return null
            val tokens = tokenize(text)
            if (tokens.isEmpty()) return null

            val command = tokens.first().raw.removePrefix("/").substringBefore('@').lowercase()
            if (command.isBlank()) return null

            val args = mutableListOf<String>()
            val options = linkedMapOf<String, String>()
            var i = 1
            while (i < tokens.size) {
                val token = tokens[i]
                val option = OPTION.matchEntire(token.raw)?.groupValues?.get(1)?.takeIf { !token.quoted }
                if (option == null) {
                    args.add(token.raw)
                    i += 1
                    continue
                }

                val next = tokens.getOrNull(i + 1)
                if (next != null && (next.quoted || !OPTION.matches(next.raw))) {
                    options[option.lowercase()] = next.raw
                    i += 2
                } else {
                    options[option.lowercase()] = "true"
                    i += 1
                }
            }

            return BotRequest(chatId, userId, messageId, callbackQueryId, command, args, options)
        }

        private data class Token(val raw: String, val quoted: Boolean)

        /** 공백으로 나누되 따옴표("…", '…') 안의 공백은 유지한다. */
        private fun tokenize(text: String): List<Token> {
            val tokens = mutableListOf<Token>()
            val current = StringBuilder()
            var quote: Char? = null
            var quoted = false

            fun flush() {
                if (current.isNotEmpty() || quoted) tokens.add(Token(current.toString(), quoted))
                current.clear()
                quoted = false
            }

            for (char in text.trim()) {
                when {
                    quote != null && char == quote -> quote = null

                    quote != null -> current.append(char)

                    char == '"' || char == '\'' || char == '“' || char == '”' -> {
                        quote = if (char == '“') '”' else char
                        quoted = true
                    }

                    char.isWhitespace() -> flush()

                    else -> current.append(char)
                }
            }
            flush()

            return tokens
        }
    }
}
