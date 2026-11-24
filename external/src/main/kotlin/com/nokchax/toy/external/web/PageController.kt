package com.nokchax.toy.external.web

import com.nokchax.toy.common.config.ToyProperties
import com.nokchax.toy.common.exception.BotException
import com.nokchax.toy.crawl.service.CrawlService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus

/** 읽기 전용 페이지. 크롤 변경은 봇 명령이나 인터널 API · MCP 로 한다. */
@Controller
class PageController(
    private val crawlService: CrawlService,
    toyProperties: ToyProperties,
) {
    private val chatId = toyProperties.ownerChatId

    @GetMapping("/")
    fun index(model: Model): String {
        model.addAttribute("crawls", crawlService.list(chatId))
        return "index"
    }

    @GetMapping("/crawls/{id}")
    fun crawl(@PathVariable id: Long, model: Model): String {
        model.addAttribute("crawl", crawlService.get(chatId, id))
        model.addAttribute("history", crawlService.history(chatId, id))
        return "crawl"
    }

    /** 없는 크롤 등 */
    @ExceptionHandler(BotException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    fun notFound(e: BotException): String = e.message ?: "없음"

    /** 배포 후 살아 있는지 확인용. 로그인 없이 열린다. */
    @GetMapping("/health")
    @ResponseBody
    fun health() = mapOf("status" to "ok")
}
