package com.nokchax.toy.crawler.engine

import com.nokchax.toy.common.domain.crawl.NotifyMode
import com.nokchax.toy.common.domain.notification.NotificationMessage
import com.nokchax.toy.crawler.extract.ExtractedItem

object MessageRenderer {

    fun items(name: String, mode: NotifyMode, items: List<ExtractedItem>, maxItems: Int): String {
        val header = when (mode) {
            NotifyMode.NEW_ITEMS -> "🔔 [$name] 새 항목 ${items.size}개"
            NotifyMode.CHANGED -> "🔄 [$name] 결과가 바뀌었다옹 (${items.size}개)"
            NotifyMode.ALWAYS -> "📋 [$name] ${items.size}개"
        }
        if (items.isEmpty()) return "$header\n(조건에 맞는 항목 없음)"

        val builder = StringBuilder(header).append('\n')
        val shown = items.take(maxItems)
        for (item in shown) {
            val line = buildString {
                append("\n• ").append(item.text)
                item.url?.let { append('\n').append(it) }
            }
            if (builder.length + line.length > NotificationMessage.MAX_LENGTH - 40) break
            builder.append(line)
        }

        val rest = items.size - shown.size
        if (rest > 0) builder.append("\n\n…외 ${rest}개")

        return builder.toString()
    }

    fun disabled(name: String, crawlId: Long, failures: Int, error: String?): String =
        "⚠️ [$name] 크롤이 ${failures}번 연속 실패해서 껐다옹\n마지막 오류: ${error ?: "알 수 없음"}\n\n다시 켜려면 /crawl on $crawlId"
}
