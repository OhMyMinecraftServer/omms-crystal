package net.zhuruoling.omms.crystal.parser

import java.lang.IllegalArgumentException


abstract class MinecraftParser {
    abstract fun parseToBareInfo(raw: String): Info?
    abstract fun parseServerStartedInfo(raw: String): ServerStartedInfo?
    abstract fun parsePlayerInfo(raw: String): PlayerInfo?
    abstract fun parseServerOverloadInfo(raw: String): ServerOverloadInfo?
    abstract fun parseServerStartingInfo(raw: String): ServerStartingInfo?
    abstract fun parsePlayerJoinInfo(raw: String): PlayerJoinInfo?
    abstract fun parsePlayerLeftInfo(raw: String): PlayerLeftInfo?
    abstract fun parseServerStoppingInfo(raw: String):ServerStoppingInfo?
}

class UnableToParseException : UnsupportedOperationException()
object ParserManager {
    private val parser: HashMap<String, MinecraftParser> = hashMapOf()
    fun registerParser(id: String, minecraftParser: MinecraftParser, override: Boolean = false) {
        if (parser.containsKey(id) and !override) {
            throw java.lang.UnsupportedOperationException("This parser($minecraftParser) already exists.")
        } else {
            parser[id] = minecraftParser
        }
    }

    fun unregisterParser(id: String){
        if(parser.containsKey(id)){
            parser.remove(id)
        }else{
            throw IllegalArgumentException("illegal parser id: $id")
        }
    }

    fun getParser(id: String): MinecraftParser? {
        return parser[id]
    }

    init {
        parser["builtin"] = BuiltinParser()
    }
}