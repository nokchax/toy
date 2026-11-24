package com.nokchax.toy.internal.api

import com.nokchax.toy.common.config.ToyProperties
import com.nokchax.toy.common.domain.crawl.FilterField
import com.nokchax.toy.common.domain.crawl.FilterOp
import com.nokchax.toy.common.domain.crawl.NotifyMode
import com.nokchax.toy.common.domain.crawl.SelectorType
import com.nokchax.toy.common.exception.BotException
import com.nokchax.toy.crawl.engine.CrawlRunResult
import com.nokchax.toy.crawl.service.CrawlService
import com.nokchax.toy.crawl.service.CrawlView
import com.nokchax.toy.crawl.service.HistoryView
import com.nokchax.toy.crawl.service.Preview
import com.nokchax.toy.crawl.service.RegisterCrawl
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 크롤 관리 REST API. 모든 크롤은 toy.owner-chat-id 소유로 다룬다.
 * 실행 결과 알림은 알림 큐에 쌓이고 텔레그램 발송은 bot 앱이 한다.
 */
@RestController
@RequestMapping("/api/crawls")
class CrawlApiController(
    private val crawlService: CrawlService,
    toyProperties: ToyProperties,
) {
    private val chatId = toyProperties.ownerChatId

    @GetMapping
    fun list(): List<CrawlView> = crawlService.list(chatId)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): CrawlView = crawlService.get(chatId, id)

    @GetMapping("/{id}/history")
    fun history(@PathVariable id: Long): List<HistoryView> = crawlService.history(chatId, id)

    @PostMapping("/preview")
    fun preview(@RequestBody request: PreviewRequest): Preview = crawlService.preview(request.url, request.selector, request.selectorType)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody request: RegisterRequest): RegisterResponse {
        val (crawl, preview) = crawlService.register(
            RegisterCrawl(
                chatId = chatId,
                name = request.name,
                url = request.url,
                selector = request.selector,
                selectorType = request.selectorType,
                cron = request.cron,
                notifyMode = request.notifyMode,
            ),
        )
        return RegisterResponse(crawl, preview)
    }

    @PostMapping("/{id}/run")
    fun run(@PathVariable id: Long): CrawlRunResult = crawlService.runNow(chatId, id)

    @PutMapping("/{id}/active")
    fun setActive(@PathVariable id: Long, @RequestBody request: ActiveRequest): CrawlView = crawlService.setActive(chatId, id, request.active)

    @PutMapping("/{id}/cron")
    fun changeCron(@PathVariable id: Long, @RequestBody request: CronRequest): CrawlView = crawlService.changeCron(chatId, id, request.cron)

    @PutMapping("/{id}/mode")
    fun changeMode(@PathVariable id: Long, @RequestBody request: ModeRequest): CrawlView = crawlService.changeMode(chatId, id, request.notifyMode)

    @PostMapping("/{id}/filters")
    fun addFilter(@PathVariable id: Long, @RequestBody request: FilterRequest): CrawlView = crawlService.addFilter(chatId, id, FilterOp.from(request.op), request.pattern, request.field)

    @DeleteMapping("/{id}/filters/{seq}")
    fun removeFilter(@PathVariable id: Long, @PathVariable seq: Int): CrawlView = crawlService.removeFilter(chatId, id, seq)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): Map<String, String> = mapOf("deleted" to crawlService.delete(chatId, id))

    /** 입력 오류 · 없는 크롤 등 사용자 오류 */
    @ExceptionHandler(BotException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun badRequest(e: BotException): Map<String, String?> = mapOf("error" to e.message)
}

data class PreviewRequest(
    val url: String,
    val selector: String,
    val selectorType: SelectorType = SelectorType.CSS,
)

data class RegisterRequest(
    val name: String,
    val url: String,
    val selector: String,
    val selectorType: SelectorType = SelectorType.CSS,
    val cron: String = "10m",
    val notifyMode: NotifyMode = NotifyMode.NEW_ITEMS,
)

data class RegisterResponse(
    val crawl: CrawlView,
    val preview: Preview,
)

data class ActiveRequest(val active: Boolean)

data class CronRequest(val cron: String)

data class ModeRequest(val notifyMode: NotifyMode)

data class FilterRequest(
    /** contains, not_contains, equals, not_equals, starts_with, ends_with, regex */
    val op: String,
    val pattern: String,
    val field: FilterField = FilterField.TEXT,
)
