package com.nokchax.toy.crawler.fetch

import com.nokchax.toy.crawler.config.CrawlProperties
import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress

/** 로컬 HTTP 서버로 실제 요청 경로를 확인한다 (외부 네트워크 사용 안 함). */
class HttpFetcherTest {
    private lateinit var server: HttpServer
    private val fetcher = HttpFetcher(CrawlProperties())
    private val base get() = "http://127.0.0.1:${server.address.port}"

    @BeforeEach
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/board") { exchange ->
                val ua = exchange.requestHeaders.getFirst("User-Agent") ?: ""
                val body = "<ul><li><a href=\"/p/1\">글 ($ua)</a></li></ul>".toByteArray()
                exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            createContext("/old") { exchange ->
                exchange.responseHeaders.add("Location", "/board")
                exchange.sendResponseHeaders(302, -1)
                exchange.close()
            }
            createContext("/down") { exchange ->
                exchange.sendResponseHeaders(503, -1)
                exchange.close()
            }
            start()
        }
    }

    @AfterEach
    fun stop() = server.stop(0)

    @Test
    fun `HTML 을 가져오고 브라우저 User-Agent 를 보낸다`() {
        val page = fetcher.fetch("$base/board")

        assertThat(page.html).contains("글 (Mozilla/5.0")
    }

    @Test
    fun `리다이렉트를 따라가고 최종 url 을 돌려준다`() {
        assertThat(fetcher.fetch("$base/old").url).isEqualTo("$base/board")
    }

    @Test
    fun `HTTP 오류와 연결 실패는 FetchException 이 된다`() {
        assertThatThrownBy { fetcher.fetch("$base/down") }.isInstanceOf(FetchException::class.java).hasMessage("HTTP 503")
        assertThatThrownBy { fetcher.fetch("http://127.0.0.1:1/") }.isInstanceOf(FetchException::class.java)
    }
}
