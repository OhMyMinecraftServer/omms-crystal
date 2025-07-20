package icu.takeneko.omms.crystal.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent


object CommandHelpManager {

    val map = mutableMapOf<String, CommandHelpProvider>()

    fun init() {
        map.clear()
        registerBuiltinCommandHelp()
    }

    fun registerHelpMessage(command: String, textProvider: () -> String) {
        map[command] = CommandHelpProvider { textProvider() }
    }

    fun registerHelpMessage(command: String, helpProvider: CommandHelpProvider) {
        map[command] = helpProvider
    }

    fun displayAll(commandSourceStack: CommandSourceStack) {
        displayFiltered(commandSourceStack) { true }
    }

    fun displayFiltered(
        commandSourceStack: CommandSourceStack,
        predicate: String.() -> Boolean
    ) {
        map.forEach { (k, v) ->
            if (predicate(k)) {
                commandSourceStack.sendFeedback(Component.text("$k -> ${v()}"))
            }
        }
    }
}