package com.nokchax.toy.crawl.fetch

data class FetchedPage(
    /** 리다이렉트 이후 최종 url. 상대 링크를 절대 경로로 바꿀 때 쓴다. */
    val url: String,
    val html: String,
)

class FetchException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** 페이지를 가져오는 방법. 1단계는 HTTP, 2단계에서 브라우저(Selenium) 구현을 추가한다. */
interface Fetcher {
    fun fetch(url: String): FetchedPage
}
