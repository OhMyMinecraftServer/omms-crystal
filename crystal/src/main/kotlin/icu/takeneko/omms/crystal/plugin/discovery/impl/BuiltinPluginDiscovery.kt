package icu.takeneko.omms.crystal.plugin.discovery.impl

import icu.takeneko.omms.crystal.CrystalServer
import icu.takeneko.omms.crystal.crystalspi.ICrystalPluginDiscovery
import icu.takeneko.omms.crystal.plugin.container.BuiltinPluginContainer
import icu.takeneko.omms.crystal.plugin.container.PluginContainer
import icu.takeneko.omms.crystal.util.BuildProperties

class BuiltinPluginDiscovery : ICrystalPluginDiscovery {
    val plugins = listOf<PluginContainer>(
        BuiltinPluginContainer(
            "crystal",
            BuildProperties["version"]!!,
            CrystalServer
        ),
        BuiltinPluginContainer(
            "java",
            Runtime.version().toString()
        )
    )

    override fun collectPlugins(): List<PluginContainer> = plugins

    override fun key(): String = "builtin"
}