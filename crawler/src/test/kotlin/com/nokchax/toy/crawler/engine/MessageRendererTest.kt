package com.nokchax.toy.crawler.engine

import com.nokchax.toy.crawler.extract.ExtractedItem
import com.nokchax.toy.crawler.extract.snapshot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MessageRendererTest {

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "2,249,000원 | 2,204,000원 | -45,000 (-2.0%)",
            "0 2,249,000원 | 0 2,300,000원 | +51,000 (+2.3%)",
            "가격 1000 | 가격 1500 | +500 (+50.0%)",
            "재고 없음 | 재고 있음 | ",
            "1,000원 | 1,000원 | ",
        ],
    )
    fun `마지막 숫자의 변동폭`(before: String, after: String, expected: String?) {
        assertThat(MessageRenderer.delta(before, after)).isEqualTo(expected)
    }

    @Test
    fun `이전 결과가 없으면(예전 버전에서 등록) 현재 목록만 보여준다`() {
        val body = MessageRenderer.changed("t", null, listOf(ExtractedItem("새 값", null)), 10)

        assertThat(body).contains("결과가 바뀌었다옹", "새 값").doesNotContain("이전:")
    }

    @Test
    fun `순서만 바뀌면 그렇게 알려준다`() {
        val body = MessageRenderer.changed("t", listOf("a", "b"), listOf(ExtractedItem("b", null), ExtractedItem("a", null)), 10)

        assertThat(body).contains("순서나 링크가 바뀌었다옹")
    }

    @Test
    fun `스냅샷은 줄 단위로 저장하고 전체 길이를 넘지 않는다`() {
        val items = (1..100).map { ExtractedItem("항목 $it " + "가".repeat(100), null) }

        val snapshot = items.snapshot()

        assertThat(snapshot.length).isLessThanOrEqualTo(4000)
        assertThat(snapshot.lines().first()).startsWith("항목 1 ")
    }
}
