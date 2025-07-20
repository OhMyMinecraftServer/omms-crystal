package icu.takeneko.omms.crystal.main

import java.util.*

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
        return if (off)
            "OFF "
        else (
                if (allDebug())
                    "ALL "
                else (
                        (if (eventDebug()) "EVENT " else "")
                                + (if (mainDebug()) "MAIN " else "")
                                + (if (pluginDebug()) "PLUGIN " else "")
                                + (if (serverDebug()) "SERVER " else "")
                        )
                )

    }
}