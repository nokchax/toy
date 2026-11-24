package com.nokchax.toy.internal.mcp

import com.nokchax.toy.common.config.ToyProperties
import com.nokchax.toy.common.domain.crawl.FilterField
import com.nokchax.toy.common.domain.crawl.FilterOp
import com.nokchax.toy.common.domain.crawl.NotifyMode
import com.nokchax.toy.common.domain.crawl.SelectorType
import com.nokchax.toy.crawl.engine.CrawlRunResult
import com.nokchax.toy.crawl.service.CrawlService
import com.nokchax.toy.crawl.service.CrawlView
import com.nokchax.toy.crawl.service.HistoryView
import com.nokchax.toy.crawl.service.Preview
import com.nokchax.toy.crawl.service.RegisterCrawl
import com.nokchax.toy.internal.api.RegisterResponse
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.stereotype.Component

/**
 * MCP 도구 (Claude 등 MCP 클라이언트에서 크롤을 다룬다). 엔드포인트: POST /mcp
 * 검증 규칙은 CrawlService 에 있으므로 여기서는 인자만 넘긴다.
 */
@Component
class CrawlTools(
    private val crawlService: CrawlService,
    toyProperties: ToyProperties,
) {
    private val chatId = toyProperties.ownerChatId

    @McpTool(name = "list_crawls", description = "등록된 크롤 목록 (id, 이름, url, selector, 주기, 알림 모드, 상태, 필터)")
    fun listCrawls(): List<CrawlView> = crawlService.list(chatId)

    @McpTool(name = "get_crawl", description = "크롤 하나의 상세")
    fun getCrawl(@McpToolParam(description = "크롤 id") id: Long): CrawlView = crawlService.get(chatId, id)

    @McpTool(name = "crawl_history", description = "크롤의 최근 실행 기록 10개 (성공/실패, 실패 사유)")
    fun crawlHistory(@McpToolParam(description = "크롤 id") id: Long): List<HistoryView> = crawlService.history(chatId, id)

    @McpTool(name = "preview_crawl", description = "저장하지 않고 페이지를 가져와 selector 에 걸리는 항목을 미리 본다")
    fun previewCrawl(
        @McpToolParam(description = "페이지 url (http/https)") url: String,
        @McpToolParam(description = "항목을 고르는 CSS selector (xpath 면 selectorType 을 XPATH 로)") selector: String,
        @McpToolParam(description = "CSS 또는 XPATH. 기본 CSS", required = false) selectorType: SelectorType?,
    ): Preview = crawlService.preview(url, selector, selectorType ?: SelectorType.CSS)

    @McpTool(name = "register_crawl", description = "크롤 등록. 지금 보이는 항목은 기준값으로 저장하고, 다음 실행부터 조건에 맞는 새 항목을 텔레그램으로 알린다")
    fun registerCrawl(
        @McpToolParam(description = "크롤 이름 (64자 이내)") name: String,
        @McpToolParam(description = "페이지 url (http/https)") url: String,
        @McpToolParam(description = "항목을 고르는 selector") selector: String,
        @McpToolParam(description = "CSS 또는 XPATH. 기본 CSS", required = false) selectorType: SelectorType?,
        @McpToolParam(description = "주기. 10m, 1h 같은 줄임말 또는 cron 식. 기본 10m", required = false) cron: String?,
        @McpToolParam(description = "알림 모드 NEW_ITEMS(새 항목) / CHANGED(결과가 바뀌면) / ALWAYS. 기본 NEW_ITEMS", required = false) notifyMode: NotifyMode?,
    ): RegisterResponse {
        val (crawl, preview) = crawlService.register(
            RegisterCrawl(
                chatId = chatId,
                name = name,
                url = url,
                selector = selector,
                selectorType = selectorType ?: SelectorType.CSS,
                cron = cron ?: "10m",
                notifyMode = notifyMode ?: NotifyMode.NEW_ITEMS,
            ),
        )
        return RegisterResponse(crawl, preview)
    }

    @McpTool(name = "run_crawl", description = "크롤을 지금 한 번 실행한다. 새 항목 알림은 텔레그램 봇이 보낸다")
    fun runCrawl(@McpToolParam(description = "크롤 id") id: Long): CrawlRunResult = crawlService.runNow(chatId, id)

    @McpTool(name = "set_crawl_active", description = "크롤 켜기/끄기")
    fun setCrawlActive(
        @McpToolParam(description = "크롤 id") id: Long,
        @McpToolParam(description = "true 면 켜기, false 면 끄기") active: Boolean,
    ): CrawlView = crawlService.setActive(chatId, id, active)

    @McpTool(name = "change_crawl_cron", description = "크롤 주기 변경")
    fun changeCrawlCron(
        @McpToolParam(description = "크롤 id") id: Long,
        @McpToolParam(description = "10m, 1h 같은 줄임말 또는 cron 식") cron: String,
    ): CrawlView = crawlService.changeCron(chatId, id, cron)

    @McpTool(name = "add_crawl_filter", description = "필터 추가. 필터는 모두 통과해야 알린다 (AND)")
    fun addCrawlFilter(
        @McpToolParam(description = "크롤 id") id: Long,
        @McpToolParam(description = "contains, not_contains, equals, not_equals, starts_with, ends_with, regex") op: String,
        @McpToolParam(description = "비교할 값") pattern: String,
        @McpToolParam(description = "TEXT(항목 글자) 또는 URL. 기본 TEXT", required = false) field: FilterField?,
    ): CrawlView = crawlService.addFilter(chatId, id, FilterOp.from(op), pattern, field ?: FilterField.TEXT)

    @McpTool(name = "remove_crawl_filter", description = "필터 삭제")
    fun removeCrawlFilter(
        @McpToolParam(description = "크롤 id") id: Long,
        @McpToolParam(description = "필터 번호 (get_crawl 의 filters[].seq)") seq: Int,
    ): CrawlView = crawlService.removeFilter(chatId, id, seq)
}
