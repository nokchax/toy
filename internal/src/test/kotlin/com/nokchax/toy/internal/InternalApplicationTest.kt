package com.nokchax.toy.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/** 실제 서버를 띄워 API 키 검사, REST API, MCP 도구 목록을 HTTP 로 확인한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InternalApplicationTest {

    @LocalServerPort
    private var port: Int = 0

    private val client = HttpClient.newHttpClient()
    private val key = "test-key"

    private fun get(path: String, apiKey: String? = null): HttpResponse<String> {
        val request = HttpRequest.newBuilder(URI("http://localhost:$port$path")).GET()
        apiKey?.let { request.header("Authorization", "Bearer $it") }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString())
    }

    private fun mcp(body: String, sessionId: String? = null): HttpResponse<String> {
        val request = HttpRequest.newBuilder(URI("http://localhost:$port/mcp"))
            .header("X-Api-Key", key)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString(body))
        sessionId?.let { request.header("Mcp-Session-Id", it) }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString())
    }

    @Test
    fun `health 는 키 없이 열린다`() {
        assertThat(get("/health").statusCode()).isEqualTo(200)
    }

    @Test
    fun `키가 없거나 틀리면 401`() {
        assertThat(get("/api/crawls").statusCode()).isEqualTo(401)
        assertThat(get("/api/crawls", apiKey = "wrong").statusCode()).isEqualTo(401)
    }

    @Test
    fun `키가 맞으면 크롤 목록을 준다`() {
        val response = get("/api/crawls", apiKey = key)

        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(response.body()).isEqualTo("[]")
    }

    @Test
    fun `없는 크롤은 400 과 이유를 준다`() {
        val response = get("/api/crawls/999", apiKey = key)

        assertThat(response.statusCode()).isEqualTo(400)
        assertThat(response.body()).contains("999번 크롤이 없다옹")
    }

    @Test
    fun `MCP 로 크롤 도구 목록을 받는다`() {
        val init = mcp(
            """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1"}}}""",
        )
        assertThat(init.statusCode()).isEqualTo(200)
        val sessionId = init.headers().firstValue("Mcp-Session-Id").orElseThrow()

        mcp("""{"jsonrpc":"2.0","method":"notifications/initialized"}""", sessionId)
        val tools = mcp("""{"jsonrpc":"2.0","id":2,"method":"tools/list"}""", sessionId)

        assertThat(tools.statusCode()).isEqualTo(200)
        assertThat(tools.body()).contains("list_crawls", "register_crawl", "run_crawl", "preview_crawl")
    }

    @Test
    fun `MCP 도 키가 없으면 401`() {
        val response = client.send(
            HttpRequest.newBuilder(URI("http://localhost:$port/mcp"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        assertThat(response.statusCode()).isEqualTo(401)
    }
}
