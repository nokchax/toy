package com.nokchax.toy.internal.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

/**
 * /health 를 뺀 모든 요청(REST API, MCP)에 API 키를 요구한다.
 * 키는 `Authorization: Bearer <키>` 또는 `X-Api-Key: <키>` 로 보낸다.
 * 외부에 공개하지 않는 앱이지만, 같은 NAS 의 다른 컨테이너 · 프로세스도 막기 위해 키를 둔다.
 */
@Component
class ApiKeyFilter(
    private val properties: InternalProperties,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean = request.requestURI == "/health"

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val expected = properties.apiKey
        val actual = request.getHeader("X-Api-Key") ?: request.getHeader("Authorization")?.removePrefix("Bearer ")?.trim()

        if (expected.isBlank() || actual == null || !MessageDigest.isEqual(expected.toByteArray(), actual.toByteArray())) {
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.contentType = "application/json"
            response.writer.write("""{"error":"unauthorized"}""")
            return
        }
        chain.doFilter(request, response)
    }
}
