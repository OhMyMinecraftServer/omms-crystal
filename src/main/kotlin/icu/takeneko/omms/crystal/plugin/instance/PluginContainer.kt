package icu.takeneko.omms.crystal.plugin.instance

import icu.takeneko.omms.crystal.event.EventBus
import icu.takeneko.omms.crystal.foundation.ActionHost
import icu.takeneko.omms.crystal.foundation.Keyable
import icu.takeneko.omms.crystal.plugin.PluginState
import icu.takeneko.omms.crystal.plugin.metadata.PluginMetadata
import kotlinx.coroutines.CoroutineScope

abstract class PluginContainer: Keyable, ActionHost, CoroutineScope {
    var pluginState: PluginState = PluginState.UNKNOWN
        private set

    val pluginEventBus = EventBus(this, )

    abstract fun constructPlugin()

    abstract fun getMetadata(): PluginMetadata
}