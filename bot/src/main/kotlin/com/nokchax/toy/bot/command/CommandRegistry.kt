package com.nokchax.toy.bot.command

import com.nokchax.toy.common.exception.botError
import org.springframework.aop.support.AopUtils
import org.springframework.stereotype.Component
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/** @BotCommand / @SubCommand 를 읽어서 "명령어 → 메서드" 표를 만든다. */
@Component
class CommandRegistry(handlers: List<CommandHandler>) {

    val commands: List<CommandSpec> = handlers.map { CommandSpec.of(it) }.sortedBy { it.name }

    private val byName: Map<String, CommandSpec> = buildMap {
        commands.forEach { spec ->
            (listOf(spec.name) + spec.aliases).forEach { key ->
                require(put(key, spec) == null) { "명령어 이름이 겹침: /$key" }
            }
        }
    }

    fun find(name: String): CommandSpec? = byName[name.lowercase()]
}

class CommandSpec(
    val name: String,
    val aliases: List<String>,
    val description: String,
    val subCommands: List<SubCommandSpec>,
) {
    private val default = subCommands.firstOrNull { it.name.isEmpty() }
    private val byName = buildMap {
        subCommands.filter { it.name.isNotEmpty() }.forEach { sub ->
            (listOf(sub.name) + sub.aliases).forEach { key ->
                require(put(key, sub) == null) { "/$name 의 하위 명령 이름이 겹침: $key" }
            }
        }
    }

    /** 첫 번째 인자가 하위 명령이면 그것을, 아니면 기본 처리를 고른다. */
    fun resolve(request: BotRequest): Pair<SubCommandSpec, BotRequest> {
        request.arg(0)?.lowercase()?.let { first -> byName[first]?.let { return it to request.shift() } }
        return (default ?: botError(usage())) to request
    }

    fun usage(): String = buildString {
        append("/").append(name)
        if (description.isNotBlank()) append(" — ").append(description)
        subCommands.filter { it.usage.isNotBlank() }.forEach { append("\n").append(it.usage) }
    }

    companion object {
        fun of(handler: CommandHandler): CommandSpec {
            val type = AopUtils.getTargetClass(handler)
            val command = type.getAnnotation(BotCommand::class.java)
                ?: error("${type.simpleName} 에 @BotCommand 가 없음")

            val subs = type.declaredMethods.mapNotNull { method ->
                method.getAnnotation(SubCommand::class.java)?.let { SubCommandSpec(it, method, handler) }
            }.sortedBy { it.order }

            return CommandSpec(command.name, command.aliases.toList(), command.description, subs)
        }
    }
}

class SubCommandSpec(
    annotation: SubCommand,
    private val method: Method,
    private val bean: Any,
) {
    val name: String = annotation.name
    val aliases: List<String> = annotation.aliases.toList()
    val usage: String = annotation.usage
    val description: String = annotation.description
    val order: Int = if (name.isEmpty()) -1 else 0

    init {
        val params = method.parameterTypes
        require(params.isEmpty() || (params.size == 1 && params[0] == BotRequest::class.java)) {
            "${method.name}: 인자는 없거나 BotRequest 하나여야 함"
        }
        method.isAccessible = true
    }

    fun invoke(request: BotRequest): Any? = try {
        if (method.parameterCount == 0) method.invoke(bean) else method.invoke(bean, request)
    } catch (e: InvocationTargetException) {
        throw e.targetException
    }
}
