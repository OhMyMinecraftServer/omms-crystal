package icu.takeneko.omms.crystal.parser

class Info(
    val info: String,
    val thread: String,
    val level: org.slf4j.event.Level
)

data class PlayerInfo(
    val isNotSecure: Boolean,
    val player: String,
    val content: String
)

data class PlayerJoinInfo(
    val player: String
)

data class RconInfo(
    val port: Int
)

data class PlayerLeftInfo(
    val player: String
)

data class ServerOverloadInfo(
    val ticks: Long,
    val time: Long
)

data class ServerStartingInfo(val version: String)
//data class ServerStartingInfo(val ip: String, val port: Int)
data class ServerStartedInfo(val timeElapsed: Double)

class ServerStoppingInfo