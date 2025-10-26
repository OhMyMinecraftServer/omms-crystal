package icu.takeneko.omms.crystal.plugin.container

import icu.takeneko.omms.crystal.plugin.metadata.PluginMetadata

class BuiltinPluginContainer(
    val id : String,
    val version : String
) : PluginContainer() {
    private val metadata = PluginMetadata(
        id,
        version
    )

    override fun constructPlugin() {
    }

    override fun getMetadata(): PluginMetadata = metadata
}