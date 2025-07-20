package icu.takeneko.omms.crystal.event.server

import icu.takeneko.omms.crystal.event.PluginBusEvent
import icu.takeneko.omms.crystal.parser.Info
import icu.takeneko.omms.crystal.parser.PlayerInfo

class ServerOverloadEvent(val ticks: Long, val time: Long) : PluginBusEvent

class ServerLoggingEvent(val info: Info) : PluginBusEvent

class PlayerChatEvent(val content: String, val info: PlayerInfo) : PluginBusEvent

class PlayerLeftEvent(val player: String) : PluginBusEvent

class PlayerJoinEvent(val player: String) : PluginBusEvent

