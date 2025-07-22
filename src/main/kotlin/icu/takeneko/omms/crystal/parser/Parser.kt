package icu.takeneko.omms.crystal.parser

abstract class MinecraftParser {
    abstract fun parseToBareInfo(raw: String): Info?
    abstract fun parseServerStartedInfo(raw: String): ServerStartedInfo?
    abstract fun parsePlayerInfo(raw: String): PlayerInfo?
    abstract fun parseRconStartInfo(raw: String): RconInfo?
    abstract fun parseServerOverloadInfo(raw: String): ServerOverloadInfo?
    abstract fun parseServerStartingInfo(raw: String): ServerStartingInfo?
    abstract fun parsePlayerJoinInfo(raw: String): PlayerJoinInfo?
    abstract fun parsePlayerLeftInfo(raw: String): PlayerLeftInfo?
    abstract fun parseServerStoppingInfo(raw: String): ServerStoppingInfo?
}

class UnableToParseException(message: String) : UnsupportedOperationException(message)

object ParserManager {
    private val parser = mutableMapOf<String, MinecraftParser>()

    fun registerParser(id: String, minecraftParser: MinecraftParser, override: Boolean = false) {
        if (parser.containsKey(id) and !override) {
            throw UnableToParseException("This parser($minecraftParser) already exists.")
        } else {
            parser[id] = minecraftParser
        }
    }

    fun unregisterParser(id: String) {
        if (parser.containsKey(id)) {
            parser.remove(id)
        } else {
            error("illegal parser id: $id")
        }
    }

    fun getParser(id: String): MinecraftParser? = parser[id]

    init {
        parser["vanilla"] = BuiltinParser()
    }
}
