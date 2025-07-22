package icu.takeneko.omms.crystal.plugin.instance

import icu.takeneko.omms.crystal.event.EventBus
import icu.takeneko.omms.crystal.event.PluginBusEvent
import icu.takeneko.omms.crystal.foundation.ActionHost
import icu.takeneko.omms.crystal.foundation.Keyable
import icu.takeneko.omms.crystal.main.CrystalServer
import icu.takeneko.omms.crystal.plugin.PluginState
import icu.takeneko.omms.crystal.plugin.metadata.PluginMetadata
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

abstract class PluginContainer : Keyable, ActionHost, CoroutineScope {
    var pluginState: PluginState = PluginState.UNKNOWN
        private set

    val pluginEventBus = EventBus(this, PluginBusEvent::class.java)

    abstract fun constructPlugin()

    abstract fun getMetadata(): PluginMetadata

    override fun key(): String = getMetadata().id

    override val coroutineContext: CoroutineContext
        get() = CrystalServer.coroutineContext
}
