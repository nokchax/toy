package com.nokchax.toy.bot.command

/** 명령어 핸들러 표시. 클래스에 @BotCommand 를 붙이고 메서드에 @SubCommand 를 붙인다. */
interface CommandHandler

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class BotCommand(
    val name: String,
    val aliases: Array<String> = [],
    val description: String = "",
)

/**
 * 하위 명령. name 이 빈 문자열이면 하위 명령 없이 호출됐을 때의 기본 처리.
 * 메서드는 인자가 없거나 BotRequest 하나를 받고, String 또는 BotResponse 를 돌려준다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SubCommand(
    val name: String,
    val aliases: Array<String> = [],
    val usage: String = "",
    val description: String = "",
)
