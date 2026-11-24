package com.nokchax.toy.internal.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** 배포 후 살아 있는지 확인용. API 키 없이 열린다. */
@RestController
class HealthController {
    @GetMapping("/health")
    fun health() = mapOf("status" to "ok")
}
