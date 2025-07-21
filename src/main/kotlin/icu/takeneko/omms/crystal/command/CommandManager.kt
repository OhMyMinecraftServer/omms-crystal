package icu.takeneko.omms.crystal.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.ParseResults
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.CommandNode
import icu.takeneko.omms.crystal.util.command.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.jline.reader.Completer
import org.jline.reader.impl.completer.AggregateCompleter
import org.jline.reader.impl.completer.ArgumentCompleter
import org.jline.reader.impl.completer.NullCompleter
import org.jline.reader.impl.completer.StringsCompleter
import java.util.concurrent.atomic.AtomicInteger

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

    fun execute(command: String, sourceStack: CommandSourceStack): Int {
        val ret = dispatcher.execute(command, sourceStack)
        return ret
    }

    fun parse(s: String, commandSourceStack: CommandSourceStack): ParseResults<CommandSourceStack> {
        return dispatcher.parse(s, commandSourceStack)
    }

    fun suggest(s: String, commandSourceStack: CommandSourceStack) =
        dispatcher.getCompletionSuggestions(parse(s, commandSourceStack))


    fun <S> unregisterCommand(command: LiteralArgumentBuilder<S>, dispatcher: CommandDispatcher<S>): String? =
        CommandUtil.unregisterCommand(command, dispatcher)
}
