package net.zhuruoling.omms.crystal.command

import net.kyori.adventure.text.TextComponent

object CommandHelpManager {

    val map = mutableMapOf<String,  () -> TextComponent>()
    fun init() {
        map.clear()
    }

    fun registerHelpMessage(command: String, textProvider: () -> TextComponent){
        map[command] = textProvider
    }

    fun displayAll(commandSourceStack: CommandSourceStack,) {
        displayFiltered(commandSourceStack) { true }
    }

    fun displayFiltered(
        commandSourceStack: CommandSourceStack,
        predicate: String.() -> Boolean
    ) {
        map.forEach{ (k, v) ->
            if (predicate(k)){
                commandSourceStack.sendFeedback(v())
            }
        }
    }
}