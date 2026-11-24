package com.nokchax.toy.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import java.net.CookieManager
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/** 실제 서버를 띄워 로그인 요구 → 폼 로그인 → 목록 페이지 흐름을 확인한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ExternalApplicationTest {

    @LocalServerPort
    private var port: Int = 0

    private fun client() = HttpClient.newBuilder().cookieHandler(CookieManager()).build()

    private fun get(client: HttpClient, path: String) = client.send(
        HttpRequest.newBuilder(URI("http://localhost:$port$path")).GET().build(),
        HttpResponse.BodyHandlers.ofString(),
    )

    private fun login(client: HttpClient, password: String): HttpResponse<String> {
        val csrf = Regex("""name="_csrf"[^>]*value="([^"]+)"""").find(get(client, "/login").body())!!.groupValues[1]
        val form = mapOf("username" to "owner", "password" to password, "_csrf" to csrf)
            .entries.joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, Charsets.UTF_8)}" }
        return client.send(
            HttpRequest.newBuilder(URI("http://localhost:$port/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )
    }

    @Test
    fun `health 는 로그인 없이 열린다`() {
        assertThat(get(client(), "/health").statusCode()).isEqualTo(200)
    }

    @Test
    fun `로그인하지 않으면 로그인 페이지로 보낸다`() {
        val response = get(client(), "/")

        assertThat(response.statusCode()).isEqualTo(302)
        assertThat(response.headers().firstValue("Location").orElse("")).endsWith("/login")
    }

    @Test
    fun `비밀번호가 틀리면 로그인되지 않는다`() {
        val client = client()

        assertThat(login(client, "wrong").headers().firstValue("Location").orElse("")).contains("/login?error")
        assertThat(get(client, "/").statusCode()).isEqualTo(302)
    }

    @Test
    fun `로그인하면 크롤 목록 페이지를 본다`() {
        val client = client()
        login(client, "secret")

        val page = get(client, "/")

        assertThat(page.statusCode()).isEqualTo(200)
        assertThat(page.body()).contains("등록된 크롤이 없습니다")
    }
}
