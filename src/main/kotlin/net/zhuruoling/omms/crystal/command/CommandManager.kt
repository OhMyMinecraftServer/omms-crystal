package net.zhuruoling.omms.crystal.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.CommandNode
import net.zhuruoling.omms.crystal.text.Color
import net.zhuruoling.omms.crystal.text.Text
import net.zhuruoling.omms.crystal.util.unregisterCommand
import org.jline.reader.impl.completer.AggregateCompleter
import org.jline.reader.impl.completer.ArgumentCompleter
import org.jline.reader.impl.completer.NullCompleter
import org.jline.reader.impl.completer.StringsCompleter

object CommandManager {
    private val dispatcher = CommandDispatcher<CommandSourceStack>()

    fun register(node: LiteralArgumentBuilder<CommandSourceStack>){
        dispatcher.register(node)
    }

    fun unregister(node: LiteralArgumentBuilder<CommandSourceStack>){
        unregisterCommand(node, dispatcher)
    }

    fun execute(command:String, sourceStack: CommandSourceStack): Int {
        val ret = dispatcher.execute(command,sourceStack)
        sourceStack.sendFeedback(Text("ao").withColor(Color.light_purple))
        return ret
    }

    fun completer():AggregateCompleter{
        val list = mutableListOf<ArgumentCompleter>()
        dispatcher.root.children.forEach {
            list.add(parseTree(it))
        }
        return AggregateCompleter(*list.toTypedArray(),NullCompleter.INSTANCE)
    }

    private fun parseTree(node: CommandNode<CommandSourceStack>):ArgumentCompleter{
        val argList = mutableListOf<ArgumentCompleter>()
        node.children.forEach {
            argList.add(parseTree(it))
        }
        return ArgumentCompleter(StringsCompleter(node.name), ArgumentCompleter(*argList.toTypedArray()))
    }

}