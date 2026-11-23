package com.nokchax.toy.common.domain.crawl

import com.nokchax.toy.common.exception.botError

enum class SelectorType {
    CSS,
    XPATH,
}

enum class NotifyMode(val description: String, private vararg val aliases: String) {
    NEW_ITEMS("처음 보는 항목이 생기면", "new"),
    CHANGED("결과가 바뀌면", "changed", "change"),
    ALWAYS("실행할 때마다", "always"),
    ;

    companion object {
        fun from(value: String): NotifyMode = entries.firstOrNull { it.name.equals(value, true) || it.aliases.any { alias -> alias.equals(value, true) } }
            ?: botError("알림 모드는 ${entries.joinToString { it.aliases.first() }} 중 하나다옹")
    }
}

enum class FilterField {
    TEXT,
    URL,
    ;

    companion object {
        fun from(value: String): FilterField = entries.firstOrNull { it.name.equals(value, true) } ?: botError("필터 대상은 text, url 중 하나다옹")
    }
}

enum class FilterOp(private vararg val aliases: String) {
    CONTAINS("contains", "has"),
    NOT_CONTAINS("not_contains", "exclude", "excludes"),
    EQUALS("equals", "eq"),
    NOT_EQUALS("not_equals", "ne"),
    STARTS_WITH("starts_with", "starts"),
    ENDS_WITH("ends_with", "ends"),
    REGEX("regex", "re"),
    ;

    fun matches(value: String, pattern: String): Boolean = when (this) {
        CONTAINS -> value.contains(pattern, ignoreCase = true)
        NOT_CONTAINS -> !value.contains(pattern, ignoreCase = true)
        EQUALS -> value.equals(pattern, ignoreCase = true)
        NOT_EQUALS -> !value.equals(pattern, ignoreCase = true)
        STARTS_WITH -> value.startsWith(pattern, ignoreCase = true)
        ENDS_WITH -> value.endsWith(pattern, ignoreCase = true)
        REGEX -> Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(value)
    }

    fun validate(pattern: String) {
        if (pattern.isBlank()) botError("필터 값이 비어있다옹")
        if (this == REGEX) {
            runCatching { Regex(pattern) }.onFailure { botError("정규식이 잘못됐다옹: $pattern") }
        }
    }

    val keyword: String get() = aliases.first()

    companion object {
        fun from(value: String): FilterOp = entries.firstOrNull { op -> op.aliases.any { it.equals(value, true) } || op.name.equals(value, true) }
            ?: botError("필터 조건은 ${entries.joinToString { it.keyword }} 중 하나다옹")
    }
}

enum class CrawlStatus {
    SUCCESS,
    FAILURE,
}
