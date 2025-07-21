package icu.takeneko.omms.crystal.command

import com.mojang.brigadier.CommandDispatcher
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
        sourceStack.sendFeedback(Component.text("ao").color(NamedTextColor.LIGHT_PURPLE))
        return ret
    }

    fun completer(): AggregateCompleter {
        val list = buildList {
            dispatcher.root.children.forEach {
                add(parseTree(it, AtomicInteger(0)))
            }
        }
        return AggregateCompleter(*list.toTypedArray(), NullCompleter.INSTANCE)
    }

    private fun parseTree(node: CommandNode<CommandSourceStack>, depth: AtomicInteger): Completer {
        if (depth.get() > 64) return NullCompleter.INSTANCE
        val argList = buildList {
            node.children.forEach {
                add(StringsCompleter(it.name))
                if (it.children.isNotEmpty()) {
                    add(parseTree(it, AtomicInteger(depth.get())))
                }
            }
        }

        depth.addAndGet(1)
        return ArgumentCompleter(
            StringsCompleter(node.name),
            ArgumentCompleter(*argList.toTypedArray()),
            NullCompleter.INSTANCE
        )
    }

    fun <S> unregisterCommand(command: LiteralArgumentBuilder<S>, dispatcher: CommandDispatcher<S>): String? =
        CommandUtil.unregisterCommand(command, dispatcher)
}
