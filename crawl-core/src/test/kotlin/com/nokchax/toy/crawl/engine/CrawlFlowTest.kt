package com.nokchax.toy.crawl.engine

import com.nokchax.toy.common.domain.crawl.*
import com.nokchax.toy.common.domain.notification.NotificationRepository
import com.nokchax.toy.common.exception.BotException
import com.nokchax.toy.crawl.FakeFetcher
import com.nokchax.toy.crawl.FakeFetcherConfig
import com.nokchax.toy.crawl.service.CrawlService
import com.nokchax.toy.crawl.service.RegisterCrawl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.ZonedDateTime

/** 등록 → 실행 → 새 항목 판별 → 알림 큐 저장 까지 실제 DB(H2)로 확인한다. */
@SpringBootTest
@Import(FakeFetcherConfig::class)
class CrawlFlowTest(
    @Autowired private val crawlService: CrawlService,
    @Autowired private val runner: CrawlRunner,
    @Autowired private val claimer: CrawlClaimer,
    @Autowired private val fetcher: FakeFetcher,
    @Autowired private val pageRepository: PageRepository,
    @Autowired private val crawlRepository: CrawlRepository,
    @Autowired private val itemRepository: CrawlItemRepository,
    @Autowired private val historyRepository: CrawlHistoryRepository,
    @Autowired private val notificationRepository: NotificationRepository,
) {
    private val url = "https://shop.example.com/hotdeal"
    private val chatId = 100L

    private fun board(vararg titles: String) = titles.withIndex().joinToString("", "<ul class=\"list\">", "</ul>") { (i, title) ->
        "<li><a href=\"/p/${titles.size - i}\">$title</a></li>"
    }

    private fun register(mode: NotifyMode = NotifyMode.NEW_ITEMS) = crawlService.register(RegisterCrawl(chatId, "핫딜", url, "ul.list li a", cron = "10m", notifyMode = mode)).first

    @BeforeEach
    fun clean() {
        notificationRepository.deleteAll()
        historyRepository.deleteAll()
        itemRepository.deleteAll()
        crawlRepository.deleteAll()
        pageRepository.deleteAll()
        fetcher.requests.clear()
    }

    @Test
    @DisplayName("등록하면 지금 보이는 항목을 기준값으로 저장하고 알림은 보내지 않는다")
    fun registerSavesBaseline() {
        fetcher.page(url, board("마우스 특가", "키보드 할인"))

        val crawl = register()

        assertThat(itemRepository.countByCrawlId(crawl.id)).isEqualTo(2)
        assertThat(notificationRepository.count()).isZero()
        assertThat(crawl.nextRunAt).isNotNull()
        assertThat(crawl.lastRunAt).isNotNull()
    }

    @Test
    @DisplayName("새 글 중 필터를 통과한 것만 알림 큐에 들어가고, 같은 글은 다시 알리지 않는다")
    fun newItemsWithFilter() {
        fetcher.page(url, board("마우스 특가", "키보드 할인"))
        val crawl = register()
        crawlService.addFilter(chatId, crawl.id, FilterOp.CONTAINS, "특가")

        fetcher.page(url, board("모니터 특가", "충전기 할인", "마우스 특가", "키보드 할인"))
        val first = runner.run(crawl.id)

        assertThat(first.status).isEqualTo(CrawlStatus.SUCCESS)
        assertThat(first.notifiedCount).isEqualTo(1)
        val message = notificationRepository.findAll().single()
        assertThat(message.chatId).isEqualTo(chatId)
        assertThat(message.body).contains("모니터 특가", "https://shop.example.com/p/4").doesNotContain("충전기", "마우스")

        // 다음 실행에서는 이미 본 글이라 알리지 않는다
        val second = runner.run(crawl.id)
        assertThat(second.notifiedCount).isZero()
        assertThat(notificationRepository.count()).isEqualTo(1)
    }

    @Test
    @DisplayName("필터에 걸러진 글도 이미 본 글로 저장해서, 필터를 바꿔도 예전 글이 쏟아지지 않는다")
    fun filteredItemsAreAlsoSeen() {
        fetcher.page(url, board("키보드 할인"))
        val crawl = register()
        crawlService.addFilter(chatId, crawl.id, FilterOp.CONTAINS, "특가")

        fetcher.page(url, board("충전기 할인", "키보드 할인"))
        runner.run(crawl.id)
        crawlService.removeFilter(chatId, crawl.id, 1)

        assertThat(runner.run(crawl.id).notifiedCount).isZero()
    }

    @Test
    @DisplayName("CHANGED 모드는 결과가 바뀔 때만 알린다")
    fun changedMode() {
        fetcher.page(url, board("재고 없음"))
        val crawl = register(NotifyMode.CHANGED)

        assertThat(runner.run(crawl.id).notifiedCount).isZero()

        fetcher.page(url, board("재고 있음"))
        assertThat(runner.run(crawl.id).notifiedCount).isEqualTo(1)
        assertThat(notificationRepository.findAll().single().body)
            .contains("값이 바뀌었다옹", "이전: 재고 없음", "현재: 재고 있음")
    }

    @Test
    @DisplayName("CHANGED 모드 가격 변동: 이전 값, 현재 값, 변동폭을 알린다")
    fun changedPrice() {
        val price = { visible: String -> "<div class=\"price_real\"><div style=\"display:none;\">0</div>$visible<span>원</span></div>" }
        fetcher.page(url, price("2,249,000"))
        val crawl = crawlService.register(
            RegisterCrawl(chatId, "5070ti", url, "div.price_real", cron = "30m", notifyMode = NotifyMode.CHANGED),
        ).first

        fetcher.page(url, price("2,204,000"))
        runner.run(crawl.id)

        assertThat(notificationRepository.findAll().single().body)
            .contains("이전: 2,249,000원", "현재: 2,204,000원", "변동: -45,000 (-2.0%)")

        // 같은 값이면 다시 알리지 않고, 다음 변동은 직전 값 기준으로 비교한다
        runner.run(crawl.id)
        fetcher.page(url, price("2,300,000"))
        runner.run(crawl.id)
        assertThat(notificationRepository.findAll().map { it.body }).hasSize(2)
            .last().asString().contains("이전: 2,204,000원", "변동: +96,000 (+4.4%)")
    }

    @Test
    @DisplayName("CHANGED 모드 목록: 추가된 항목과 사라진 항목을 알린다")
    fun changedList() {
        fetcher.page(url, board("a", "b", "c"))
        val crawl = register(NotifyMode.CHANGED)

        fetcher.page(url, board("d", "a", "b"))
        runner.run(crawl.id)

        assertThat(notificationRepository.findAll().single().body)
            .contains("추가 1 · 사라짐 1", "+ d", "- c")
            .doesNotContain("+ a", "- a")
    }

    @Test
    @DisplayName("DB 에 직접 넣은 크롤도 첫 실행은 기준값만 저장한다")
    fun firstRunIsBaseline() {
        fetcher.page(url, board("a", "b", "c"))
        val page = pageRepository.save(Page(url))
        val crawl = crawlRepository.save(Crawl(page, chatId, "direct", "ul.list li a", cron = "0 */10 * * * *"))

        val result = runner.run(crawl.id)

        assertThat(result.status).isEqualTo(CrawlStatus.SUCCESS)
        assertThat(result.notifiedCount).isZero()
        assertThat(itemRepository.countByCrawlId(crawl.id)).isEqualTo(3)
    }

    @Test
    @DisplayName("연속 5번 실패하면 크롤을 끄고 알림을 남긴다. 성공하면 실패 횟수는 초기화된다")
    fun failures() {
        fetcher.page(url, board("a"))
        val crawl = register()

        fetcher.fail(url, "HTTP 503")
        repeat(4) { runner.run(crawl.id) }
        assertThat(crawlRepository.findFailCount(crawl.id)).isEqualTo(4)
        assertThat(notificationRepository.count()).isZero()

        val fifth = runner.run(crawl.id)
        assertThat(fifth.disabled).isTrue()
        assertThat(crawlService.get(chatId, crawl.id).active).isFalse()
        assertThat(notificationRepository.findAll().single().body).contains("5번 연속 실패", "HTTP 503")

        fetcher.page(url, board("a"))
        crawlService.setActive(chatId, crawl.id, true)
        runner.run(crawl.id)
        assertThat(crawlRepository.findFailCount(crawl.id)).isZero()
        assertThat(historyRepository.findTop10ByCrawlIdOrderByIdDesc(crawl.id).first().status).isEqualTo(CrawlStatus.SUCCESS)
    }

    @Test
    @DisplayName("selector 에 아무것도 안 걸리면 실패로 기록한다 (페이지 구조 변경 감지)")
    fun emptyResultIsFailure() {
        fetcher.page(url, board("a"))
        val crawl = register()

        fetcher.page(url, "<div>개편된 페이지</div>")

        assertThat(runner.run(crawl.id).status).isEqualTo(CrawlStatus.FAILURE)
    }

    @Test
    @DisplayName("실행할 때가 된 크롤은 한 번만 선점된다")
    fun claimOnce() {
        fetcher.page(url, board("a"))
        val crawl = register()
        val later = crawl.nextRunAt!!.plusSeconds(1)

        assertThat(claimer.claimDue(later)).containsExactly(crawl.id)
        assertThat(claimer.claimDue(later)).isEmpty()
        assertThat(claimer.claimDue(crawl.nextRunAt.minusSeconds(1))).isEmpty()
    }

    @Test
    @DisplayName("꺼진 크롤은 선점하지 않는다")
    fun inactiveNotClaimed() {
        fetcher.page(url, board("a"))
        val crawl = register()
        crawlService.setActive(chatId, crawl.id, false)

        assertThat(claimer.claimDue(ZonedDateTime.now().plusDays(1))).isEmpty()
    }

    @Test
    @DisplayName("다른 채팅의 크롤은 보이지 않는다")
    fun ownership() {
        fetcher.page(url, board("a"))
        val crawl = register()

        assertThat(crawlService.list(999L)).isEmpty()
        assertThatThrownBy { crawlService.get(999L, crawl.id) }.isInstanceOf(BotException::class.java)
    }

    @Test
    @DisplayName("가져오는 방법을 지정하지 않으면 HTTP, 지정하면 그 방법으로 등록 · 실행한다")
    fun fetchMethodOnRegister() {
        fetcher.page(url, board("a"))

        val default = register()
        val browser = crawlService.register(RegisterCrawl(chatId, "브라우저", url, "ul.list li a", fetchMethod = FetchMethod.BROWSER)).first
        runner.run(browser.id)

        assertThat(default.fetchMethod).isEqualTo(FetchMethod.HTTP)
        assertThat(browser.fetchMethod).isEqualTo(FetchMethod.BROWSER)
        assertThat(fetcher.requests.map { it.method }).containsExactly(FetchMethod.HTTP, FetchMethod.BROWSER, FetchMethod.BROWSER)
        assertThat(fetcher.requests.last().waitFor?.selector).isEqualTo("ul.list li a")
    }

    @Test
    @DisplayName("가져오는 방법을 바꾸면 그 방법으로 다시 가져와 기준값을 잡아서, 원래 있던 항목은 알리지 않는다")
    fun changeFetchMethodRebaselines() {
        fetcher.page(url, board("a"))
        val crawl = register()

        // 브라우저로 보면 JS 로 그린 항목 b 가 더 보인다고 가정
        fetcher.page(url, board("b", "a"))
        val changed = crawlService.changeFetchMethod(chatId, crawl.id, FetchMethod.BROWSER)

        assertThat(changed.fetchMethod).isEqualTo(FetchMethod.BROWSER)
        assertThat(fetcher.requests.last().method).isEqualTo(FetchMethod.BROWSER)
        assertThat(itemRepository.countByCrawlId(crawl.id)).isEqualTo(2)
        assertThat(runner.run(crawl.id).notifiedCount).isZero()

        fetcher.page(url, board("c", "b", "a"))
        assertThat(runner.run(crawl.id).notifiedCount).isEqualTo(1)
    }

    @Test
    @DisplayName("바꾼 방법으로 가져오지 못하면 바꾸지 않는다")
    fun changeFetchMethodFails() {
        fetcher.page(url, board("a"))
        val crawl = register()

        fetcher.fail(url, "브라우저에 연결하지 못함")

        assertThatThrownBy { crawlService.changeFetchMethod(chatId, crawl.id, FetchMethod.BROWSER) }
            .isInstanceOf(BotException::class.java).hasMessageContaining("브라우저에 연결하지 못함")
        assertThat(crawlService.get(chatId, crawl.id).fetchMethod).isEqualTo(FetchMethod.HTTP)
    }

    @Test
    @DisplayName("등록할 때 selector 에 걸리는 게 없으면 등록하지 않는다")
    fun registerRequiresItems() {
        fetcher.page(url, "<div>empty</div>")

        assertThatThrownBy { register() }.isInstanceOf(BotException::class.java).hasMessageContaining("항목이 없다옹")
        assertThat(crawlRepository.count()).isZero()
    }
}
