package icu.takeneko.omms.crystal.parser

import org.slf4j.event.Level


open class BuiltinParser : MinecraftParser() {

    private val regexRawInfo =
        Regex("\\[(\\d{2}:\\d{2}:\\d{2})\\] \\[([A-Za-z0-9\\u0020！/.@#$%^&*\\(\\)+=_-]*)[/]([A-Za-z0-9_-]*)\\]: ([^\\f\\r\\n\\v]*\\w*)")

    override fun parseToBareInfo(raw: String): Info? {
        val matcher = regexRawInfo.toPattern().matcher(raw)
        if (!matcher.matches()) return null
        val level = Level.valueOf(matcher.group(3))
        return Info(matcher.group(4), matcher.group(2), level)
    }

    //Done (6.343s)! For help, type "help"
    private val regexServerStarted = Regex("Done \\(([0-9.]*)s\\)\\! For help\\, type \\\"help\\\"")
    override fun parseServerStartedInfo(raw: String): ServerStartedInfo? {
        val matcher = regexServerStarted.toPattern().matcher(raw)
        if (!matcher.matches()) return null
        val time = matcher.group(1).toDouble()
        return ServerStartedInfo(time)
    }

    private val regexPlayerInfo = Regex("<([0-9A-Za-z_]*)> ([^\\n\\r]*)")
    override fun parsePlayerInfo(raw: String): PlayerInfo? {
        val matcher = regexPlayerInfo.toPattern().matcher(raw.removePrefix("[Not Secure] "))
        if (!matcher.matches()) return null
        val player = matcher.group(1)
        val content = matcher.group(2)
        return PlayerInfo(player = player, content = content, isNotSecure = raw.contains("[Not Secure] "))
    }

    private val regexRconInfo = Regex("RCON running on ([0-9.]+):([0-9]+)")

    override fun parseRconStartInfo(raw: String): RconInfo? {
        val matcher = regexRconInfo.toPattern().matcher(raw)
        if (!matcher.matches()) return null
        val rconPort = matcher.group(2)
        return RconInfo(rconPort.toInt())
    }

    private val regexServerOverload =
        Regex("Can't keep up! Is the server overloaded\\? Running ([0-9]*)ms or ([0-9]*) ticks behind")

    override fun parseServerOverloadInfo(raw: String): ServerOverloadInfo? {
        val matcher = regexServerOverload.toPattern().matcher(raw)
        if (!matcher.matches()) return null
        val timeMillis = matcher.group(1).toLong()
        val ticks = matcher.group(2).toLong()
        return ServerOverloadInfo(ticks, timeMillis)
    }

    private val regexServerStarting = Regex("Starting minecraft server version ([a-zA-Z0-9_.-]*)")
    override fun parseServerStartingInfo(raw: String): ServerStartingInfo? {
        val matcher = regexServerStarting.toPattern().matcher(raw)
        if (!matcher.matches()) return null
        val version = matcher.group(1)
        return ServerStartingInfo(version)
    }

    //ZhuRuoLing joined the game
    private val regexPlayerJoin = Regex("([0-9A-Za-z_]*) joined the game")
    override fun parsePlayerJoinInfo(raw: String): PlayerJoinInfo? {
        val m = regexPlayerJoin.toPattern().matcher(raw)
        if (!m.matches()) return null
        val player = m.group(1)
        return PlayerJoinInfo(player)
    }

    //ZhuRuoLing left the game
    private val regexPlayerLeft = Regex("([0-9A-Za-z_]*) left the game")
    override fun parsePlayerLeftInfo(raw: String): PlayerLeftInfo? {
        val matcher = regexPlayerLeft.toPattern().matcher(raw)
        if (!matcher.matches()) return null
        val player = matcher.group(1)
        return PlayerLeftInfo(player)
    }

    override fun parseServerStoppingInfo(raw: String): ServerStoppingInfo? {
        return (
                if (raw == "Stopping server")
                    ServerStoppingInfo()
                else
                    null
                )
    }

}