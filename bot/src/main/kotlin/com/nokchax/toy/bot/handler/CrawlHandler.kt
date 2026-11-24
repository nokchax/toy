package com.nokchax.toy.bot.handler

import com.nokchax.toy.bot.command.*
import com.nokchax.toy.bot.handler.CrawlFormats.humanCron
import com.nokchax.toy.common.domain.crawl.FilterField
import com.nokchax.toy.common.domain.crawl.FilterOp
import com.nokchax.toy.common.domain.crawl.NotifyMode
import com.nokchax.toy.common.domain.crawl.SelectorType
import com.nokchax.toy.common.exception.botError
import com.nokchax.toy.crawl.service.CrawlService
import com.nokchax.toy.crawl.service.CrawlView
import com.nokchax.toy.crawl.service.RegisterCrawl
import org.springframework.stereotype.Component

@Component
@BotCommand("crawl", aliases = ["cr"], description = "페이지 크롤 관리")
class CrawlHandler(
    private val crawlService: CrawlService,
) : CommandHandler {

    @SubCommand("")
    fun usage() = TextResponse(USAGE, buttons(listOf(Button("📋 목록", "/cr list"))))

    @SubCommand("test", aliases = ["t"], usage = "/crawl test <url> <selector> [-xpath]", description = "저장하지 않고 미리보기")
    fun test(request: BotRequest): String {
        val usage = "/crawl test <url> <selector> [-xpath]"
        val preview = crawlService.preview(request.arg(0, usage), request.rest(1, usage), request.selectorType())
        return "🔍 미리보기\n${CrawlFormats.preview(preview)}"
    }

    @SubCommand(
        "add",
        aliases = ["a"],
        usage = "/crawl add <이름> <url> <selector> [-cron 10m] [-mode new|changed|always] [-xpath]",
        description = "크롤 등록",
    )
    fun add(request: BotRequest): TextResponse {
        val usage = "/crawl add <이름> <url> <selector> [-cron 10m] [-mode new|changed|always] [-xpath]"
        val (crawl, preview) = crawlService.register(
            RegisterCrawl(
                chatId = request.chatId,
                name = request.arg(0, usage),
                url = request.arg(1, usage),
                selector = request.rest(2, usage),
                selectorType = request.selectorType(),
                cron = request.option("cron") ?: "10m",
                notifyMode = request.option("mode")?.let { NotifyMode.from(it) } ?: NotifyMode.NEW_ITEMS,
            ),
        )

        return TextResponse(
            "✅ 등록 완료다옹 (#${crawl.id} ${crawl.name}, ${crawl.cron.humanCron()})\n" +
                "지금 보이는 항목은 기준값으로 저장했다옹. 다음 실행: ${with(CrawlFormats) { crawl.nextRunAt.short() }}\n\n" +
                CrawlFormats.preview(preview) +
                "\n\n필터 추가: /cr filter ${crawl.id} add contains 키워드",
            detailButtons(crawl),
        )
    }

    @SubCommand("list", aliases = ["ls", "l"], usage = "/crawl list", description = "목록")
    fun list(request: BotRequest): TextResponse {
        val crawls = crawlService.list(request.chatId)
        if (crawls.isEmpty()) return TextResponse("등록된 크롤이 없다옹\n$ADD_HINT")

        return TextResponse(
            "📋 크롤 ${crawls.size}개\n\n" + crawls.joinToString("\n") { CrawlFormats.summary(it) },
            crawls.map { Button("${if (it.active) "🟢" else "⚪"} ${it.name}", "/cr show ${it.id}") }.chunked(2),
        )
    }

    @SubCommand("show", aliases = ["s"], usage = "/crawl show <id>", description = "상세")
    fun show(request: BotRequest): TextResponse {
        val crawl = crawlService.get(request.chatId, request.id("/crawl show <id>"))
        return TextResponse(CrawlFormats.detail(crawl), detailButtons(crawl))
    }

    @SubCommand("on", usage = "/crawl on <id>", description = "켜기")
    fun on(request: BotRequest) = detail(crawlService.setActive(request.chatId, request.id("/crawl on <id>"), true))

    @SubCommand("off", usage = "/crawl off <id>", description = "끄기")
    fun off(request: BotRequest) = detail(crawlService.setActive(request.chatId, request.id("/crawl off <id>"), false))

    @SubCommand("run", aliases = ["r"], usage = "/crawl run <id>", description = "지금 실행")
    fun run(request: BotRequest): TextResponse {
        val id = request.id("/crawl run <id>")
        val result = crawlService.runNow(request.chatId, id)
        return TextResponse(CrawlFormats.run(result), buttons(listOf(Button("◀️ 상세", "/cr show $id"))))
    }

    @SubCommand("cron", usage = "/crawl cron <id> <10m | 1h | cron식>", description = "주기 변경")
    fun cron(request: BotRequest): TextResponse {
        val usage = "/crawl cron <id> <10m | 1h | \"*/10 * * * *\">"
        return detail(crawlService.changeCron(request.chatId, request.id(usage), request.rest(1, usage)))
    }

    @SubCommand("mode", usage = "/crawl mode <id> <new|changed|always>", description = "알림 모드 변경")
    fun mode(request: BotRequest): TextResponse {
        val usage = "/crawl mode <id> <new|changed|always>"
        return detail(crawlService.changeMode(request.chatId, request.id(usage), NotifyMode.from(request.arg(1, usage))))
    }

    @SubCommand(
        "filter",
        aliases = ["f"],
        usage = "/crawl filter <id> add <조건> <값> [-field url] | del <번호>",
        description = "필터 관리",
    )
    fun filter(request: BotRequest): TextResponse {
        val usage = "/crawl filter <id> add <${FilterOp.entries.joinToString("|") { it.keyword }}> <값> [-field url]\n/crawl filter <id> del <번호>"
        val id = request.id(usage)

        val crawl = when (request.arg(1)?.lowercase()) {
            null, "list", "ls" -> crawlService.get(request.chatId, id)

            "add", "a" -> crawlService.addFilter(
                chatId = request.chatId,
                id = id,
                op = FilterOp.from(request.arg(2, usage)),
                pattern = request.rest(3, usage),
                field = request.option("field")?.let { FilterField.from(it) } ?: FilterField.TEXT,
            )

            "del", "delete", "rm", "d" -> crawlService.removeFilter(
                request.chatId,
                id,
                request.arg(2, usage).toIntOrNull() ?: botError(usage),
            )

            else -> botError(usage)
        }

        return detail(crawl)
    }

    @SubCommand("history", aliases = ["hist", "log"], usage = "/crawl history <id>", description = "최근 실행 기록")
    fun history(request: BotRequest): TextResponse {
        val id = request.id("/crawl history <id>")
        val crawl = crawlService.get(request.chatId, id)
        return TextResponse(
            CrawlFormats.history(crawl, crawlService.history(request.chatId, id)),
            buttons(listOf(Button("◀️ 상세", "/cr show $id"))),
        )
    }

    @SubCommand("del", aliases = ["delete", "rm"], usage = "/crawl del <id>", description = "삭제")
    fun delete(request: BotRequest): TextResponse {
        val id = request.id("/crawl del <id>")
        if (!request.hasOption("y")) {
            val crawl = crawlService.get(request.chatId, id)
            return TextResponse(
                "정말 #${crawl.id} ${crawl.name} 을(를) 삭제할까옹? 기록과 이미 본 항목도 함께 지워진다옹",
                buttons(listOf(Button("🗑 삭제", "/cr del $id -y"), Button("취소", "/cr show $id"))),
            )
        }

        val name = crawlService.delete(request.chatId, id)
        return TextResponse("🗑 $name 삭제했다옹", buttons(listOf(Button("📋 목록", "/cr list"))))
    }

    private fun detail(crawl: CrawlView) = TextResponse(CrawlFormats.detail(crawl), detailButtons(crawl))

    private fun detailButtons(crawl: CrawlView) = buttons(
        listOf(
            if (crawl.active) Button("⏸ 끄기", "/cr off ${crawl.id}") else Button("▶️ 켜기", "/cr on ${crawl.id}"),
            Button("🚀 지금 실행", "/cr run ${crawl.id}"),
        ),
        listOf(
            Button("🧾 기록", "/cr history ${crawl.id}"),
            Button("🗑 삭제", "/cr del ${crawl.id}"),
        ),
        listOf(Button("📋 목록", "/cr list")),
    )

    private fun BotRequest.id(usage: String): Long = longArg(0, usage)

    private fun BotRequest.selectorType() = if (hasOption("xpath")) SelectorType.XPATH else SelectorType.CSS

    companion object {
        private const val ADD_HINT = "/cr add 핫딜 https://example.com/board \"ul.list li a\" -cron 10m"

        private val USAGE = """
            🕷 크롤 명령어 (/crawl, 줄여서 /cr)

            미리보기  /cr test <url> <selector>
            등록      /cr add <이름> <url> <selector> [-cron 10m] [-mode new]
            목록      /cr list
            상세      /cr show <id>
            필터      /cr filter <id> add contains 특가
                      /cr filter <id> del <번호>
            주기      /cr cron <id> 30m
            모드      /cr mode <id> new|changed|always
            켜기/끄기 /cr on|off <id>
            실행      /cr run <id>
            기록      /cr history <id>
            삭제      /cr del <id>

            selector 에 공백이 있으면 따옴표로 감싸도 되고 그냥 이어 적어도 된다옹.
            예) $ADD_HINT
        """.trimIndent()
    }
}
