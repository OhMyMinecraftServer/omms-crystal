package icu.takeneko.omms.crystal.event.server

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import icu.takeneko.omms.crystal.command.LiteralCommand
import icu.takeneko.omms.crystal.event.Event
import icu.takeneko.omms.crystal.util.command.CommandSourceStack

data class RegisterCommandEvent(private val dispatcher: CommandDispatcher<CommandSourceStack>) : Event {
    fun register(command: LiteralArgumentBuilder<CommandSourceStack>) {
        dispatcher.register(command)
    }

    fun register(command: LiteralCommand){
        dispatcher.register(command.node)
    }
}
