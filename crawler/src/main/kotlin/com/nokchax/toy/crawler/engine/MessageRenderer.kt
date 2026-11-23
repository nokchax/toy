package com.nokchax.toy.crawler.engine

import com.nokchax.toy.common.domain.crawl.NotifyMode
import com.nokchax.toy.common.domain.notification.NotificationMessage
import com.nokchax.toy.common.function.ellipsis
import com.nokchax.toy.crawler.extract.ExtractedItem
import com.nokchax.toy.crawler.extract.SNAPSHOT_LINE_LENGTH

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

    /**
     * CHANGED 모드 알림. 이전 결과(previous)와 비교해서
     * - 값 하나 → 하나: "이전 / 현재" 와 마지막 숫자의 변동폭
     * - 목록: 추가된 항목(+)과 사라진 항목(-)
     * previous 가 없으면(이전 버전에서 등록한 크롤) 현재 목록만 보여준다.
     */
    fun changed(name: String, previous: List<String>?, current: List<ExtractedItem>, maxItems: Int): String {
        if (previous == null) return items(name, NotifyMode.CHANGED, current, maxItems)
        val before = previous.filter { it.isNotBlank() }
        val after = current.map { it.text.ellipsis(SNAPSHOT_LINE_LENGTH) }

        if (before.size == 1 && after.size == 1) {
            return buildString {
                append("🔄 [$name] 값이 바뀌었다옹\n")
                append("\n이전: ").append(before[0])
                append("\n현재: ").append(after[0])
                delta(before[0], after[0])?.let { append("\n변동: ").append(it) }
                current[0].url?.let { append("\n").append(it) }
            }.ellipsis(NotificationMessage.MAX_LENGTH)
        }

        val added = current.filter { it.text.ellipsis(SNAPSHOT_LINE_LENGTH) !in before }
        val removed = before.filter { it !in after }
        if (added.isEmpty() && removed.isEmpty()) {
            return items(name, NotifyMode.CHANGED, current, maxItems).replaceFirst("결과가 바뀌었다옹", "순서나 링크가 바뀌었다옹")
        }

        return buildString {
            append("🔄 [$name] 결과가 바뀌었다옹 (추가 ${added.size} · 사라짐 ${removed.size})")
            if (added.isNotEmpty()) {
                append("\n")
                added.take(maxItems).forEach { item ->
                    append("\n+ ").append(item.text)
                    item.url?.let { append("\n  ").append(it) }
                }
                if (added.size > maxItems) append("\n…외 ${added.size - maxItems}개")
            }
            if (removed.isNotEmpty()) {
                append("\n")
                removed.take(maxItems).forEach { append("\n- ").append(it) }
                if (removed.size > maxItems) append("\n…외 ${removed.size - maxItems}개")
            }
        }.ellipsis(NotificationMessage.MAX_LENGTH)
    }

    private val NUMBER = Regex("""-?\d[\d,]*(\.\d+)?""")

    /** 두 값의 마지막 숫자를 비교한다. "2,249,000원" → "2,204,000원" 이면 "-45,000 (-2.0%)" */
    fun delta(before: String, after: String): String? {
        val b = NUMBER.findAll(before).lastOrNull()?.value?.replace(",", "")?.toBigDecimalOrNull() ?: return null
        val a = NUMBER.findAll(after).lastOrNull()?.value?.replace(",", "")?.toBigDecimalOrNull() ?: return null
        val diff = a - b
        if (diff.signum() == 0) return null
        val sign = if (diff.signum() > 0) "+" else "-"
        val amount = "%,d".format(diff.abs().toBigInteger())
        val percent = if (b.signum() == 0) "" else " (%s%.1f%%)".format(sign, diff.abs().multiply(java.math.BigDecimal(100)).divide(b.abs(), 1, java.math.RoundingMode.HALF_UP))
        return "$sign$amount$percent"
    }

    fun disabled(name: String, crawlId: Long, failures: Int, error: String?): String = "⚠️ [$name] 크롤이 ${failures}번 연속 실패해서 껐다옹\n마지막 오류: ${error ?: "알 수 없음"}\n\n다시 켜려면 /crawl on $crawlId"
}
