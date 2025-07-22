package icu.takeneko.omms.crystal.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.ParseResults
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.suggestion.Suggestions
import icu.takeneko.omms.crystal.util.command.CommandSourceStack
import java.util.concurrent.CompletableFuture

@Suppress("unused")
object CommandManager {
    private val dispatcher = CommandDispatcher<CommandSourceStack>()

    val completer
        get() = BrigadierCommandCompleter()

    fun register(node: LiteralArgumentBuilder<CommandSourceStack>) {
        dispatcher.register(node)
    }

    fun register(vararg nodes: LiteralArgumentBuilder<CommandSourceStack>) {
        nodes.forEach { node -> dispatcher.register(node) }
    }

    fun unregister(node: LiteralArgumentBuilder<CommandSourceStack>) {
        unregisterCommand(node, dispatcher)
    }

    fun unregister(vararg nodes: LiteralArgumentBuilder<CommandSourceStack>) {
        nodes.forEach { node -> unregisterCommand(node, dispatcher) }
    }

    fun execute(command: String, sourceStack: CommandSourceStack): Int =
        dispatcher.execute(command, sourceStack)

    fun parse(s: String, commandSourceStack: CommandSourceStack): ParseResults<CommandSourceStack> =
        dispatcher.parse(s, commandSourceStack)

    fun suggest(s: String, commandSourceStack: CommandSourceStack): CompletableFuture<Suggestions?>? =
        dispatcher.getCompletionSuggestions(parse(s, commandSourceStack))

    fun <S> unregisterCommand(command: LiteralArgumentBuilder<S>, dispatcher: CommandDispatcher<S>): String? =
        CommandUtil.unregisterCommand(command, dispatcher)
}
