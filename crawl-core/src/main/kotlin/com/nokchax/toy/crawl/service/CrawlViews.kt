package com.nokchax.toy.crawl.service

import com.nokchax.toy.common.domain.crawl.*
import com.nokchax.toy.crawl.extract.ExtractedItem
import java.time.ZonedDateTime

/** 트랜잭션 밖(텔레그램 핸들러, MCP)으로 내보내는 읽기 전용 모델 */
data class CrawlView(
    val id: Long,
    val name: String,
    val url: String,
    val selector: String,
    val selectorType: SelectorType,
    val cron: String,
    val notifyMode: NotifyMode,
    val active: Boolean,
    val nextRunAt: ZonedDateTime?,
    val lastRunAt: ZonedDateTime?,
    val failCount: Int,
    val filters: List<FilterView>,
) {
    companion object {
        fun of(crawl: Crawl) = CrawlView(
            id = crawl.id,
            name = crawl.name,
            url = crawl.page.url,
            selector = crawl.selector,
            selectorType = crawl.selectorType,
            cron = crawl.cron,
            notifyMode = crawl.notifyMode,
            active = crawl.active,
            nextRunAt = crawl.nextRunAt,
            lastRunAt = crawl.lastRunAt,
            failCount = crawl.failCount,
            filters = crawl.filters.sortedBy { it.seq }.map { FilterView(it.seq, it.op, it.pattern, it.field) },
        )
    }
}

data class FilterView(
    val seq: Int,
    val op: FilterOp,
    val pattern: String,
    val field: FilterField,
)

data class Preview(
    val url: String,
    val total: Int,
    val items: List<ExtractedItem>,
)

data class HistoryView(
    val startedAt: ZonedDateTime,
    val status: CrawlStatus,
    val durationMs: Long,
    val itemCount: Int,
    val newCount: Int,
    val error: String?,
)

data class RegisterCrawl(
    val chatId: Long,
    val name: String,
    val url: String,
    val selector: String,
    val selectorType: SelectorType = SelectorType.CSS,
    val cron: String = "10m",
    val notifyMode: NotifyMode = NotifyMode.NEW_ITEMS,
)
