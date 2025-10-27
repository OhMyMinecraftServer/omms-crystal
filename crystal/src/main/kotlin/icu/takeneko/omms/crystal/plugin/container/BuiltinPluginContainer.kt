package icu.takeneko.omms.crystal.plugin.container

import icu.takeneko.omms.crystal.plugin.metadata.PluginMetadata

class BuiltinPluginContainer(
    val id: String,
    val version: String,
    val pluginInstanceClass: Any? = null
) : PluginContainer() {
    private val metadata = PluginMetadata(
        id,
        version,
        listOf()
    )

    override fun constructPlugin() {
        if (pluginInstanceClass == null) return
        this.pluginEventBus.register(pluginInstanceClass)
    }

    override fun getMetadata(): PluginMetadata = metadata
}