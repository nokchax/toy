package com.nokchax.toy.common.exception

/**
 * 사용자에게 그대로 보여줘도 되는 예외.
 * 그 외의 예외는 로그만 남기고 사용자에게는 일반 오류 메시지를 보낸다.
 */
class BotException(message: String) : RuntimeException(message)

fun botError(message: String): Nothing = throw BotException(message)
