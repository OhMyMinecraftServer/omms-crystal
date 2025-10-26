package icu.takeneko.omms.crystal.plugin

import icu.takeneko.omms.crystal.crystalspi.ICrystalPluginDiscovery
import icu.takeneko.omms.crystal.CrystalServer
import icu.takeneko.omms.crystal.plugin.instance.PluginContainer
import icu.takeneko.omms.crystal.plugin.support.JarClassLoader
import icu.takeneko.omms.crystal.service.CrystalServiceManager
import icu.takeneko.omms.crystal.util.LoggerUtil
import icu.takeneko.omms.crystal.util.constants.DebugOptions
import org.slf4j.LoggerFactory
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.notExists

object PluginManager {
    internal val pluginClassLoader = JarClassLoader(Thread.currentThread().contextClassLoader)
    private val discoveries = mutableListOf<ICrystalPluginDiscovery>()
    private val unconstructedPlugins = mutableMapOf<String, PluginContainer>()
    private val plugins = mutableMapOf<String, PluginContainer>()
    private val pluginDir = Path(".") / Path(CrystalServer.config.pluginDirectory)
    private val logger = LoggerUtil.createLogger("PluginManager", DebugOptions.pluginDebug())

    fun addUnconstructedPlugin(container: PluginContainer) {
        unconstructedPlugins += container.key() to container
    }

    fun bootstrap() {
        if (pluginDir.notExists()) {
            pluginDir.createDirectory()
        }
        pluginDir.listDirectoryEntries()
            .filter { it.isRegularFile() }
            .filter { it.extension.lowercase() == "jar" }
            .map { it.toFile() }
            .forEach(pluginClassLoader::loadJar)
        discoveries += CrystalServiceManager.load(ICrystalPluginDiscovery::class.java).values
    }

    fun loadAll() {
        discoveries.flatMap { it.collectPlugins() }
            .forEach { addUnconstructedPlugin(it) }
        unconstructedPlugins.forEach { (key, value) ->
            logger.info("Dispatching plugin construction: {}", key)
            value.constructPlugin()
            plugins += key to value
        }
    }
}
