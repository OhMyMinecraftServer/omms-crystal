package net.zhuruoling.omms.crystal.main

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.zhuruoling.omms.crystal.command.CommandSourceStack
import net.zhuruoling.omms.crystal.console.ConsoleHandler
import net.zhuruoling.omms.crystal.event.Event
import net.zhuruoling.omms.crystal.event.EventDispatcher
import net.zhuruoling.omms.crystal.event.EventHandler
import net.zhuruoling.omms.crystal.event.EventLoop
import net.zhuruoling.omms.crystal.parser.MinecraftParser
import net.zhuruoling.omms.crystal.server.ServerThreadDaemon
import java.lang.reflect.Method
import java.util.*
import kotlin.collections.HashMap

object SharedConstants {
    var serverThreadDaemon: ServerThreadDaemon? = null
    lateinit var eventDispatcher: EventDispatcher
    lateinit var eventLoop: EventLoop
    lateinit var consoleHandler: ConsoleHandler
    lateinit var serverVersion: String
    lateinit var language:String

    val pluginRegisteredCommandTable: HashMap<String, ArrayList<LiteralArgumentBuilder<CommandSourceStack>>> = hashMapOf()
    val pluginRegisteredEventTable: HashMap<String, HashMap<String, Event>> = hashMapOf()
    val pluginDeclaredApiMethodMap: HashMap<String, HashMap<Pair<String, MutableList<Class<*>>>, Method>> = hashMapOf()
    val pluginDeclaredEventHandlerMap: HashMap<String, HashMap<Event, EventHandler>> = hashMapOf()
    val pluginDeclaredParserMap: HashMap<String, HashMap<String, MinecraftParser>> = hashMapOf()
}

object DebugOptions {
    /*
    #debug options:
 #   N/O:None/Off
 #   A:All
 #   E:Event
 #   M:Main
 #   P:Plugin
 #   S:Server
     */
    private var off = true
    private var all = false
    private var event = false
    private var main = false
    private var plugin = false
    private var server = false
    fun parse(options: String) {
        val o = options.uppercase(Locale.getDefault())
        off = o.contains("N") or o.contains("O")
        all = o.contains("A")
        event = o.contains("E")
        main = o.contains("M")
        plugin = o.contains("P")
        server = o.contains("S")
    }

    fun allDebug() = !off and all
    fun eventDebug() = !off and (all or event)
    fun mainDebug() = !off and (all or main)
    fun pluginDebug() = !off and (all or plugin)
    fun serverDebug() = !off and (all or server)

    override fun toString(): String {
        return if (off) "OFF " else (if (allDebug()) "ALL " else ((if (eventDebug()) "EVENT " else "") + (if (mainDebug()) "MAIN " else "") + (if (pluginDebug()) "PLUGIN " else "") + (if (serverDebug()) "SERVER " else "")))
    }
}