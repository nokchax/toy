package com.nokchax.toy.bot.handler

import com.nokchax.toy.common.domain.crawl.CrawlStatus
import com.nokchax.toy.common.domain.crawl.FetchMethod
import com.nokchax.toy.common.domain.crawl.SelectorType
import com.nokchax.toy.common.function.ellipsis
import com.nokchax.toy.crawl.engine.CrawlRunResult
import com.nokchax.toy.crawl.service.CrawlView
import com.nokchax.toy.crawl.service.HistoryView
import com.nokchax.toy.crawl.service.Preview
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object CrawlFormats {
    private val TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm")

    fun ZonedDateTime?.short(): String = this?.format(TIME) ?: "-"

    fun summary(crawl: CrawlView): String = "${if (crawl.active) "🟢" else "⚪"} #${crawl.id} ${crawl.name} · ${crawl.cron.humanCron()}" +
        (if (crawl.fetchMethod == FetchMethod.BROWSER) " · 브라우저" else "") +
        if (crawl.filters.isEmpty()) "" else " · 필터 ${crawl.filters.size}"

    fun detail(crawl: CrawlView): String = buildString {
        appendLine("${if (crawl.active) "🟢 켜짐" else "⚪ 꺼짐"}  #${crawl.id} ${crawl.name}")
        appendLine()
        appendLine("url: ${crawl.url}")
        appendLine("selector: ${crawl.selector}${if (crawl.selectorType == SelectorType.XPATH) " (xpath)" else ""}")
        appendLine("주기: ${crawl.cron.humanCron()} (${crawl.cron})")
        appendLine("알림: ${crawl.notifyMode.description}")
        appendLine("가져오기: ${crawl.fetchMethod.description}")
        appendLine("다음 실행: ${if (crawl.active) crawl.nextRunAt.short() else "-"} · 마지막: ${crawl.lastRunAt.short()}")
        if (crawl.failCount > 0) appendLine("연속 실패: ${crawl.failCount}")
        appendLine()
        if (crawl.filters.isEmpty()) {
            append("필터 없음 (모든 항목)")
        } else {
            append("필터 (모두 만족해야 통과)")
            crawl.filters.forEach { append("\n  ${it.seq}. ${it.field.name.lowercase()} ${it.op.keyword} \"${it.pattern}\"") }
        }
    }

    fun preview(preview: Preview): String = buildString {
        append("항목 ${preview.total}개")
        if (preview.total > preview.items.size) append(" (앞 ${preview.items.size}개)")
        preview.items.forEachIndexed { i, item ->
            append("\n${i + 1}. ${item.text.ellipsis(80)}")
            item.url?.let { append("\n   ").append(it.ellipsis(120)) }
        }
    }

    fun history(crawl: CrawlView, histories: List<HistoryView>): String = buildString {
        append("🧾 #${crawl.id} ${crawl.name} 최근 실행")
        if (histories.isEmpty()) append("\n아직 실행 기록이 없다옹")
        histories.forEach {
            append("\n${it.startedAt.short()} ")
            if (it.status == CrawlStatus.SUCCESS) {
                append("✅ 항목 ${it.itemCount} · 알림 ${it.newCount} (${it.durationMs}ms)")
            } else {
                append("❌ ${it.error?.ellipsis(80)}")
            }
        }
    }

    fun run(result: CrawlRunResult): String = if (result.status == CrawlStatus.SUCCESS) {
        "🚀 실행 완료다옹\n항목 ${result.itemCount}개 · 필터 통과 ${result.passedCount}개 · 알림 ${result.notifiedCount}개"
    } else {
        "❌ 실행 실패다옹: ${result.error}" + if (result.disabled) "\n연속 실패로 크롤을 껐다옹" else ""
    }

    /** 자주 쓰는 모양만 사람이 읽기 쉽게 바꾼다. */
    fun String.humanCron(): String {
        Regex("""^0 \*/(\d+) \* \* \* \*$""").matchEntire(this)?.let { return "${it.groupValues[1]}분마다" }
        Regex("""^0 0 \*/(\d+) \* \* \*$""").matchEntire(this)?.let { return "${it.groupValues[1]}시간마다" }
        if (this == "0 * * * * *") return "1분마다"
        if (this == "0 0 * * * *") return "1시간마다"
        return this
    }
}
