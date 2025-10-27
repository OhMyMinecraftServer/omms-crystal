package icu.takeneko.omms.crystal.plugin

import icu.takeneko.omms.crystal.CrystalServer
import icu.takeneko.omms.crystal.crystalspi.ICrystalPluginDiscovery
import icu.takeneko.omms.crystal.event.PluginBusEvent
import icu.takeneko.omms.crystal.plugin.container.PluginContainer
import icu.takeneko.omms.crystal.plugin.support.JarClassLoader
import icu.takeneko.omms.crystal.service.CrystalServiceManager
import icu.takeneko.omms.crystal.util.LoggerUtil
import icu.takeneko.omms.crystal.util.constants.DebugOptions
import java.lang.reflect.InvocationTargetException
import java.nio.file.Path
import kotlin.io.path.*

object PluginManager {
    internal val pluginClassLoader = JarClassLoader(Thread.currentThread().contextClassLoader)
    private val discoveries = mutableMapOf<String, ICrystalPluginDiscovery>()
    private val unconstructedPlugins = mutableMapOf<String, PluginContainer>()
    private val plugins = mutableMapOf<String, PluginContainer>()
    private val _pluginFiles = mutableListOf<Path>()
    val pluginFiles: List<Path>
        get() = _pluginFiles
    private val pluginDir = Path(".") / Path(CrystalServer.config.pluginDirectory)
    private val logger = LoggerUtil.createLogger("PluginManager", DebugOptions.pluginDebug())

    fun addUnconstructedPlugin(container: PluginContainer) {
        unconstructedPlugins += container.key() to container
    }

    fun bootstrap() {
        if (pluginDir.notExists()) {
            pluginDir.createDirectory()
        }
        _pluginFiles += pluginDir.listDirectoryEntries()
            .filter { it.isRegularFile() }
            .filter { it.extension.lowercase() == "jar" }
        discoveries += CrystalServiceManager.load(ICrystalPluginDiscovery::class.java)
        discoveries.values.forEach(ICrystalPluginDiscovery::bootstrap)
        _pluginFiles
            .map { it.toFile() }
            .forEach(pluginClassLoader::loadJar)
        CrystalServiceManager.clearService(ICrystalPluginDiscovery::class.java)
        CrystalServiceManager.load(ICrystalPluginDiscovery::class.java).forEach { t, u ->
            if (t !in discoveries) {
                u.bootstrap()
                discoveries += t to u
            }
        }
    }

    fun loadAll() {
        discoveries.values.flatMap {
            try {
                it.collectPlugins()
            } catch (t: Throwable) {
                throw RuntimeException("Failed to collect plugins", t)
            }
        }.forEach { addUnconstructedPlugin(it) }
        unconstructedPlugins.forEach { (key, value) ->
            logger.info("Dispatching plugin construction: {}", key)
            try {
                value.constructPlugin()
            } catch (t: Throwable) {
                if (t is InvocationTargetException) {
                    throw RuntimeException("Failed to create plugin instance, id: $key")
                }
            }
            plugins += key to value
        }
        logger.info("Loaded plugins: ")
        plugins.forEach { (key, value) ->
            logger.info("    - {} {}", key, value.getMetadata().version)
        }
    }

    fun postEvent(e: PluginBusEvent) {
        plugins.values.forEach {
            it.pluginEventBus.dispatch(e)
        }
    }
}
