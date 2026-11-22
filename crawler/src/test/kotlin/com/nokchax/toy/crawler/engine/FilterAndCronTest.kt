package com.nokchax.toy.crawler.engine

import com.nokchax.toy.common.domain.crawl.*
import com.nokchax.toy.common.exception.BotException
import com.nokchax.toy.crawler.extract.ExtractedItem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FilterAndCronTest {

    private fun crawl() = Crawl(Page("https://a.b"), 1L, "test", "a", cron = "0 */10 * * * *")

    private val items = listOf(
        ExtractedItem("모니터 특가", "https://a.b/p/1"),
        ExtractedItem("마우스 특가 (품절)", "https://a.b/p/2"),
        ExtractedItem("충전기 할인", "https://a.b/event/3"),
    )

    @Test
    @DisplayName("필터는 순서대로 모두 통과해야 남는다 (AND)")
    fun filtersAreAnd() {
        val crawl = crawl().apply {
            addFilter(FilterOp.CONTAINS, "특가")
            addFilter(FilterOp.NOT_CONTAINS, "품절")
        }

        assertThat(items.applyFilters(crawl.filters).map { it.text }).containsExactly("모니터 특가")
    }

    @Test
    @DisplayName("정규식으로 OR 조건, url 대상 필터")
    fun regexAndUrl() {
        val regex = crawl().apply { addFilter(FilterOp.REGEX, "모니터|충전기") }
        val url = crawl().apply { addFilter(FilterOp.CONTAINS, "/event/", FilterField.URL) }

        assertThat(items.applyFilters(regex.filters)).hasSize(2)
        assertThat(items.applyFilters(url.filters).map { it.text }).containsExactly("충전기 할인")
    }

    @Test
    @DisplayName("필터를 지우면 번호(seq)가 비어도 다음 번호는 계속 증가한다")
    fun filterSeq() {
        val crawl = crawl().apply {
            addFilter(FilterOp.CONTAINS, "a")
            addFilter(FilterOp.CONTAINS, "b")
            removeFilter(1)
        }

        assertThat(crawl.addFilter(FilterOp.CONTAINS, "c").seq).isEqualTo(3)
        assertThatThrownBy { crawl.removeFilter(1) }.isInstanceOf(BotException::class.java)
    }

    @Test
    @DisplayName("잘못된 정규식은 등록 시점에 막는다")
    fun invalidRegex() {
        assertThatThrownBy { crawl().addFilter(FilterOp.REGEX, "(") }.isInstanceOf(BotException::class.java)
    }

    @ParameterizedTest
    @CsvSource(
        "10m, 0 */10 * * * *",
        "1h, 0 0 */1 * * *",
        "*/5 * * * *, 0 */5 * * * *",
        "0 0 9 * * *, 0 0 9 * * *",
    )
    @DisplayName("주기는 줄임말, 5자리, 6자리 cron 을 받는다")
    fun cron(input: String, expected: String) {
        assertThat(CronSchedule.normalize(input)).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource("*/10 * * * * *", "0m", "abc", "1 2 3")
    @DisplayName("1분보다 짧거나 형식이 틀린 주기는 막는다")
    fun invalidCron(input: String) {
        assertThatThrownBy { CronSchedule.normalize(input) }.isInstanceOf(BotException::class.java)
    }
}
