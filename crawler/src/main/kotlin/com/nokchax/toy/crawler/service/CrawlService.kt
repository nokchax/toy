package com.nokchax.toy.crawler.service

import com.nokchax.toy.common.domain.crawl.*
import com.nokchax.toy.common.exception.botError
import com.nokchax.toy.crawler.config.CrawlProperties
import com.nokchax.toy.crawler.engine.CrawlRunResult
import com.nokchax.toy.crawler.engine.CrawlRunner
import com.nokchax.toy.crawler.engine.applyFilters
import com.nokchax.toy.crawler.extract.ExtractedItem
import com.nokchax.toy.crawler.extract.Extractor
import com.nokchax.toy.crawler.extract.resultHash
import com.nokchax.toy.crawler.extract.snapshot
import com.nokchax.toy.crawler.fetch.FetchException
import com.nokchax.toy.crawler.fetch.Fetcher
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.net.URI
import java.time.Clock
import java.time.ZonedDateTime

/**
 * 크롤 설정을 다루는 유일한 입구.
 * 텔레그램 명령어, (추후) MCP 도구 모두 이 서비스를 호출하므로 검증 규칙은 여기에만 둔다.
 * 모든 조회·수정은 chatId 로 소유자를 확인한다.
 */
@Service
class CrawlService(
    private val pageRepository: PageRepository,
    private val crawlRepository: CrawlRepository,
    private val itemRepository: CrawlItemRepository,
    private val historyRepository: CrawlHistoryRepository,
    private val fetcher: Fetcher,
    private val extractor: Extractor,
    private val runner: CrawlRunner,
    private val transaction: TransactionTemplate,
    private val properties: CrawlProperties,
    private val clock: Clock,
) {

    /** 저장하지 않고 바로 가져와서 selector 결과를 보여준다. */
    fun preview(url: String, selector: String, selectorType: SelectorType = SelectorType.CSS): Preview {
        val items = fetchItems(validateUrl(url), selector, selectorType)
        return Preview(url, items.size, items.take(properties.previewSize))
    }

    fun register(command: RegisterCrawl): Pair<CrawlView, Preview> {
        val name = command.name.trim().ifBlank { botError("이름이 비어있다옹") }
        if (name.length > 64) botError("이름은 64자까지다옹")
        if (command.selector.length > 512) botError("selector 는 512자까지다옹")
        val url = validateUrl(command.url)
        val cron = CronSchedule.normalize(command.cron)
        if (crawlRepository.countByChatId(command.chatId) >= properties.maxCrawlsPerChat) {
            botError("크롤은 ${properties.maxCrawlsPerChat}개까지 등록할 수 있다옹")
        }

        // 네트워크 작업은 트랜잭션 밖에서
        val items = fetchItems(url, command.selector, command.selectorType)

        val view = transaction.execute {
            val now = ZonedDateTime.now(clock)
            val page = pageRepository.findFirstByUrl(url) ?: pageRepository.save(Page(url))
            val crawl = crawlRepository.save(
                Crawl(
                    page = page,
                    chatId = command.chatId,
                    name = name,
                    selector = command.selector,
                    selectorType = command.selectorType,
                    cron = cron,
                    notifyMode = command.notifyMode,
                ),
            )
            // 지금 보이는 항목은 기준값. 첫 실행 때 알림이 쏟아지지 않게 한다.
            itemRepository.saveAll(items.map { CrawlItem(crawl.id, it.hash, it.text, it.url) })
            val passed = items.applyFilters(crawl.filters)
            crawl.markBaseline(now, passed.resultHash(), passed.snapshot())
            CrawlView.of(crawl)
        }!!

        return view to Preview(url, items.size, items.take(properties.previewSize))
    }

    fun list(chatId: Long): List<CrawlView> = crawlRepository.findAllByChatIdOrderById(chatId).map { CrawlView.of(it) }

    fun get(chatId: Long, id: Long): CrawlView = CrawlView.of(load(chatId, id))

    fun addFilter(chatId: Long, id: Long, op: FilterOp, pattern: String, field: FilterField = FilterField.TEXT): CrawlView = modify(chatId, id) { it.addFilter(op, pattern, field) }

    fun removeFilter(chatId: Long, id: Long, seq: Int): CrawlView = modify(chatId, id) { it.removeFilter(seq) }

    fun setActive(chatId: Long, id: Long, active: Boolean): CrawlView = modify(chatId, id) {
        if (active) it.activate(ZonedDateTime.now(clock)) else it.deactivate()
    }

    fun changeCron(chatId: Long, id: Long, cron: String): CrawlView = modify(chatId, id) {
        it.changeCron(cron, ZonedDateTime.now(clock))
    }

    fun changeMode(chatId: Long, id: Long, mode: NotifyMode): CrawlView = modify(chatId, id) { it.notifyMode = mode }

    fun delete(chatId: Long, id: Long): String = transaction.execute {
        val crawl = load(chatId, id)
        itemRepository.deleteAllByCrawlId(crawl.id)
        historyRepository.deleteAllByCrawlId(crawl.id)
        crawlRepository.delete(crawl)
        crawl.name
    }!!

    fun history(chatId: Long, id: Long): List<HistoryView> {
        load(chatId, id)
        return historyRepository.findTop10ByCrawlIdOrderByIdDesc(id).map {
            HistoryView(it.startedAt, it.status, it.durationMs, it.itemCount, it.newCount, it.error)
        }
    }

    /** 스케줄과 상관없이 지금 한 번 실행한다. 알림 규칙은 스케줄 실행과 같다. */
    fun runNow(chatId: Long, id: Long): CrawlRunResult {
        load(chatId, id)
        return runner.run(id)
    }

    private fun load(chatId: Long, id: Long): Crawl = crawlRepository.findWithPageById(id)?.takeIf { it.chatId == chatId } ?: botError("${id}번 크롤이 없다옹")

    private fun modify(chatId: Long, id: Long, block: (Crawl) -> Unit): CrawlView = transaction.execute {
        val crawl = load(chatId, id)
        block(crawl)
        crawlRepository.flush()
        CrawlView.of(crawl)
    }!!

    private fun fetchItems(url: String, selector: String, selectorType: SelectorType): List<ExtractedItem> {
        if (selector.isBlank()) botError("selector 가 비어있다옹")
        val page = try {
            fetcher.fetch(url)
        } catch (e: FetchException) {
            botError("페이지를 가져오지 못했다옹: ${e.message}")
        }
        val items = extractor.extract(page.html, page.url, selector, selectorType)
        if (items.isEmpty()) botError("selector 에 걸리는 항목이 없다옹: $selector")
        return items
    }

    private fun validateUrl(url: String): String {
        val uri = runCatching { URI(url.trim()) }.getOrNull()
        if (uri == null || uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) {
            botError("http(s) 주소만 된다옹: $url")
        }
        if (url.length > 2048) botError("url 이 너무 길다옹")
        return url.trim()
    }
}
