package icu.takeneko.omms.crystal.event.server

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import icu.takeneko.omms.crystal.command.CommandSourceStack
import icu.takeneko.omms.crystal.event.Event

data class RegisterCommandEvent(private val dispatcher: CommandDispatcher<CommandSourceStack>): Event {
    fun register(command: LiteralArgumentBuilder<CommandSourceStack>) {
        dispatcher.register(command)
    }
}
