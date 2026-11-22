package com.nokchax.toy.common.domain.crawl

import com.nokchax.toy.common.exception.botError
import org.springframework.scheduling.support.CronExpression
import java.time.Duration
import java.time.ZonedDateTime

/**
 * 크롤 주기.
 * - Spring cron 6자리 (초 분 시 일 월 요일)
 * - 일반 cron 5자리 (분 시 일 월 요일). 앞에 초 0 을 붙인다
 * - 줄임말: "10m", "1h", "3h"
 */
object CronSchedule {
    val MIN_INTERVAL: Duration = Duration.ofMinutes(1)
    private val SHORTHAND = Regex("""^(\d+)([mh])$""")

    fun normalize(input: String): String {
        val value = input.trim()
        SHORTHAND.matchEntire(value.lowercase())?.let { match ->
            val amount = match.groupValues[1].toInt()
            return when (match.groupValues[2]) {
                "m" -> if (amount in 1..59) "0 */$amount * * * *" else botError("분 단위는 1~59m 로 적어줘라옹")
                else -> if (amount in 1..23) "0 0 */$amount * * *" else botError("시간 단위는 1~23h 로 적어줘라옹")
            }
        }

        val fields = value.split(Regex("\\s+"))
        val cron = when (fields.size) {
            5 -> "0 $value"
            6 -> value
            else -> botError("cron 형식이 아니다옹: $input (예: 10m, \"*/10 * * * *\")")
        }

        val expression = parse(cron)
        val now = ZonedDateTime.now()
        val first = expression.next(now) ?: botError("다음 실행 시각이 없는 cron 이다옹: $input")
        val second = expression.next(first) ?: return cron
        if (Duration.between(first, second) < MIN_INTERVAL) {
            botError("크롤 주기는 최소 1분이다옹: $input")
        }

        return cron
    }

    fun next(cron: String, from: ZonedDateTime): ZonedDateTime? = parse(cron).next(from)

    private fun parse(cron: String): CronExpression =
        try {
            CronExpression.parse(cron)
        } catch (e: IllegalArgumentException) {
            botError("cron 형식이 잘못됐다옹: $cron")
        }
}
