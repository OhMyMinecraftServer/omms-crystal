package icu.takeneko.omms.crystal.event.server

import icu.takeneko.omms.crystal.event.PluginBusEvent
import icu.takeneko.omms.crystal.main.CrystalServer
import icu.takeneko.omms.crystal.parser.Info
import icu.takeneko.omms.crystal.parser.PlayerInfo
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

class ServerOverloadEvent(val ticks: Long, val time: Long) : PluginBusEvent

class ServerLoggingEvent(val info: Info) : PluginBusEvent

class PlayerChatEvent(val info: PlayerInfo, val content: String = info.content, val player: String = info.player) :
    PluginBusEvent {
    fun sendFeedback(vararg text: Component) {
        for (component in text) {
            CrystalServer.input("tellraw ${info.player} ${GsonComponentSerializer.gson().serialize(component)}")
        }
    }
}

class PlayerLeftEvent(val player: String) : PluginBusEvent

class PlayerJoinEvent(val player: String) : PluginBusEvent

