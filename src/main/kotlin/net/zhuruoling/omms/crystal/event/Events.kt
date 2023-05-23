package net.zhuruoling.omms.crystal.event

import net.zhuruoling.omms.crystal.main.SharedConstants
import net.zhuruoling.omms.crystal.parser.Info
import net.zhuruoling.omms.crystal.plugin.PluginInstance
import net.zhuruoling.omms.crystal.plugin.CrystalInterface


//base
open class Event(open val id: String, val priority: Int) {
    override fun hashCode(): Int {
        return id.hashCode() + priority.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Event) return false
        if (id != other.id) return false
        if (priority != other.priority) return false
        return true
    }
}

open class EventArgs

//plugin
open class PluginEvent(id: String, open val pluginId: String, priority: Int = 1) : Event(id, priority) {
    override fun hashCode(): Int {
        return super.hashCode() + pluginId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PluginEvent) return false
        if (!super.equals(other)) return false

        if (pluginId != other.pluginId) return false

        return true
    }
}


/*
crystal.server.info         v
crystal.server.start        v
crystal.server.starting     v
crystal.server.started      v
crystal.server.stopping     v
crystal.server.stopped      v
crystal.server.player.info  v
crystal.server.player.join  v
crystal.server.player.left  v
crystal.plugin.load         v
crystal.plugin.unload       v
*/


//server
object ServerInfoEvent : Event("crystal.server.info", 1000)
class ServerInfoEventArgs(val info: Info) : EventArgs() {
    override fun toString(): String {
        return "ServerInfoEventArgs(info=$info)"
    }
}

object ServerStartEvent : Event("crystal.server.start", 1000)
class ServerStartEventArgs(val startupCmd: String, val workingDir: String) : EventArgs() {
    override fun toString(): String {
        return "ServerStartEventArgs(startupCmd='$startupCmd', workingDir='$workingDir')"
    }
}

object ServerStartingEvent : Event("crystal.server.starting", 1000)
class ServerStartingEventArgs(val pid: Long, val version: String) : EventArgs() {
    override fun toString(): String {
        return "ServerStartingEventArgs(pid=$pid)"
    }
}

object ServerStartedEvent : Event("crystal.server.started", 1000)
class ServerStartedEventArgs(val timeUsed: Double) : EventArgs()

object ServerStopEvent : Event("crystal.server.stop", 1000)
class ServerStopEventArgs(val id: String, val force: Boolean) : EventArgs() {
    override fun toString(): String {
        return "ServerStopEventArgs(id='$id', force=$force)"
    }
}

object ServerStoppingEvent : Event("crystal.server.stopping", 1000)
class ServerStoppingEventArgs : EventArgs()

object ServerStoppedEvent : Event("crystal.server.stopped", 1000)
class ServerStoppedEventArgs(val retValue: Int, val who: String) : EventArgs() {
    override fun toString(): String {
        return "ServerStoppedEventArgs(retValue=$retValue, who='$who')"
    }
}

object ServerOverloadEvent : Event("crystal.server.overload", 100);
class ServerOverloadEventArgs(val ticks: Long, val time: Long) : EventArgs()


//player
object PlayerInfoEvent : Event("crystal.server.player.info", 100)
class PlayerInfoEventArgs(val content: String, val player: String) : EventArgs() {
    override fun toString(): String {
        return "PlayerInfoEventArgs(content='$content', player='$player')"
    }
}

object PlayerJoinEvent : Event("crystal.server.player.join", 100)
class PlayerJoinEventArgs(val player: String) : EventArgs() {
    override fun toString(): String {
        return "PlayerJoinEventArgs(player='$player')"
    }
}

object PlayerLeftEvent : Event("crystal.server.player.left", 100)
class PlayerLeftEventArgs(val player: String) : EventArgs() {
    override fun toString(): String {
        return "PlayerLeftEventArgs(player='$player')"
    }
}


//plugin
class PluginLoadEvent(override val pluginId: String) : PluginEvent("crystal.plugin.load", pluginId, 10)
class PluginLoadEventArgs(
    val pluginId: String,
    val crystalInterface: CrystalInterface,
    val pluginInstance: PluginInstance
) : EventArgs() {
    override fun toString(): String {
        return "PluginLoadEventArgs(pluginId='$pluginId', serverInterface=$crystalInterface, pluginInstance=$pluginInstance)"
    }
}

class PluginUnloadEvent(override val pluginId: String) : PluginEvent("crystal.plugin.unload", pluginId, 10)
class PluginUnloadEventArgs(
    val pluginId: String,
    val crystalInterface: CrystalInterface,
    val pluginInstance: PluginInstance
) : EventArgs() {
    override fun toString(): String {
        return "PluginUnloadEventArgs(pluginId='$pluginId', serverInterface=$crystalInterface, pluginInstance=$pluginInstance)"
    }
}

class PluginCustomEvent(pluginId: String, override val id: String) : PluginEvent(id, pluginId, 10)
class PluginCustomEventArgs : EventArgs() {
    private val hashMap: HashMap<String, Any> = hashMapOf()
    fun insert(key: String, value: Any) {
        hashMap[key] = value
    }

    fun get(key: String): Any? {
        return if (hashMap.containsKey(key)) hashMap[key] else throw NoSuchElementException("")
    }

    override fun toString(): String {
        return "PluginEventArgs(hashMap=$hashMap)"
    }
}

//misc

object ServerConsoleInputEvent : Event("crystal.server.console.input", 1)
class ServerConsoleInputEventArgs(val content: String) : EventArgs() {
    override fun toString(): String {
        return "ServerConsoleInputEventArgs(content='$content')"
    }
}

val eventMap = hashMapOf<String, Event>()

fun registerEvents() {
    eventMap.run {
        this["crystal.server.info"] = ServerInfoEvent
        this["crystal.server.start"] = ServerStartEvent
        this["crystal.server.starting"] = ServerStartingEvent
        this["crystal.server.started"] = ServerStartedEvent
        this["crystal.server.stop"] = ServerStopEvent
        this["crystal.server.stopping"] = ServerStoppingEvent
        this["crystal.server.stopped"] = ServerStoppedEvent
        this["crystal.server.overload"] = ServerOverloadEvent
        this["crystal.server.player.info"] = PlayerInfoEvent
        this["crystal.server.player.join"] = PlayerJoinEvent
        this["crystal.server.player.left"] = PlayerLeftEvent
        this["crystal.server.console.input"] = ServerConsoleInputEvent
    }
}

fun getEventById(id: String): Event {
    if (eventMap.containsKey(id)) {
        return eventMap[id]!!
    } else {
        var event: Event
        SharedConstants.pluginRegisteredEventTable.values.forEach {
            if (it.containsKey(id)) {
                event = it[id]!!
                return event
            }
        }
        throw IllegalArgumentException("Illegal event id!")
    }
}