package com.nokchax.toy.crawl.fetch

import com.nokchax.toy.common.domain.crawl.FetchMethod
import com.nokchax.toy.common.domain.crawl.SelectorType
import org.springframework.stereotype.Component

data class FetchedPage(
    /** 리다이렉트 이후 최종 url. 상대 링크를 절대 경로로 바꿀 때 쓴다. */
    val url: String,
    val html: String,
)

/** 브라우저로 가져올 때 이 요소가 나타날 때까지 기다린다 (JS 로 내용을 늦게 그리는 페이지). */
data class WaitFor(
    val selector: String,
    val selectorType: SelectorType = SelectorType.CSS,
)

data class FetchRequest(
    val url: String,
    val method: FetchMethod = FetchMethod.HTTP,
    val waitFor: WaitFor? = null,
)

class FetchException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** 페이지를 가져오는 입구. 크롤 실행 · 등록 · 미리보기가 모두 이걸 쓴다. */
interface Fetcher {
    fun fetch(request: FetchRequest): FetchedPage
}

/** 요청의 FetchMethod 에 맞는 방법으로 가져온다. */
@Component
class FetcherRouter(
    private val http: HttpFetcher,
    private val browser: BrowserFetcher,
) : Fetcher {
    override fun fetch(request: FetchRequest): FetchedPage = when (request.method) {
        FetchMethod.HTTP -> http.fetch(request.url)
        FetchMethod.BROWSER -> browser.fetch(request.url, request.waitFor)
    }
}
