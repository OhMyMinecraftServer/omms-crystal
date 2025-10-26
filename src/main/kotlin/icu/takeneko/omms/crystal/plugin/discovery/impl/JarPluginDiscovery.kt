package icu.takeneko.omms.crystal.plugin.discovery.impl

import icu.takeneko.omms.crystal.crystalspi.ICrystalPluginDiscovery
import icu.takeneko.omms.crystal.plugin.PluginManager
import icu.takeneko.omms.crystal.plugin.container.JarPluginContainer
import icu.takeneko.omms.crystal.plugin.container.PluginContainer
import icu.takeneko.omms.crystal.util.LoggerUtil
import icu.takeneko.omms.crystal.util.constants.DebugOptions
import kotlin.io.path.pathString

class JarPluginDiscovery : ICrystalPluginDiscovery {
    private val logger = LoggerUtil.createLogger("JarPluginDiscovery", DebugOptions.pluginDebug())
    override fun collectPlugins(): List<PluginContainer> {
        return PluginManager.pluginFiles.map {
            logger.info("Considering plugin file {}", it.pathString)
            JarPluginContainer(PluginManager.pluginClassLoader, it)
        }
    }

    override fun key(): String = "jar"
}