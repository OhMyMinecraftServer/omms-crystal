package net.zhuruoling.omms.crystal.plugin

import net.zhuruoling.omms.crystal.main.DebugOptions
import net.zhuruoling.omms.crystal.main.SharedConstants
import net.zhuruoling.omms.crystal.parser.ParserManager
import net.zhuruoling.omms.crystal.plugin.metadata.PluginDependency
import net.zhuruoling.omms.crystal.plugin.metadata.PluginDependencyRequirement
import net.zhuruoling.omms.crystal.plugin.metadata.PluginMetadata
import net.zhuruoling.omms.crystal.util.*
import java.io.File
import java.io.InputStream
import java.lang.module.ModuleDescriptor
import java.net.URL
import java.util.function.Function

private lateinit var pluginClassLoader: JarClassLoader
private val pluginFileUrlList = mutableListOf<URL>()
private val logger = createLogger("PluginManager")

object PluginManager : Manager<String, PluginInstance>(
    beforeInit = { pluginClassLoader = JarClassLoader(ClassLoader.getSystemClassLoader()) },
    afterInit = {
        checkRequirements()
        map.forEach { entry ->
            entry.value.eventListeners.forEach {
                SharedConstants.eventDispatcher.registerHandler(it.first, it.second)
            }
            entry.value.pluginParsers.forEach {
                ParserManager.registerParser(it.key, it.value)
            }
        }
    },
    fileNameFilter = {
        if (it.endsWith(".jar")) {
            pluginFileUrlList += File(joinFilePaths("plugins", it)).toURI().toURL()
            true
        } else false
    },
    scanFolder = "plugins",
    initializer = {
        pluginClassLoader.loadJar(File(it))
        PluginInstance(pluginClassLoader, it) { before, after ->
            if (DebugOptions.pluginDebug()) {
                logger.info("Plugin ${this.pluginMetadata.id} state changed from $before to $after")
                logger.info(
                    "[DEBUG] Plugin ${
                        if ((this.pluginMetadata.id == null) or (this.pluginMetadata.version == null))
                            "...${it.subSequence(it.length - 40, it.length)}"
                        else "${pluginMetadata.id}@${pluginMetadata.version}"
                    } state changed from $before to $after"
                )
            }
        }.run {
            loadPluginMetadata()
            loadPluginClasses()
            injectArguments()
            loadPluginResources()
            pluginMetadata.id!! to this
        }
    }
) {

    fun reload(id: String) {
        this.map[id]!!.apply {
            try {
                onFinalize()
                pluginClassLoader.reloadAllClasses()
                loadPluginMetadata()
                loadPluginClasses()
                injectArguments()
                loadPluginResources()
                onInitialize()
            } catch (e: Throwable) {
                logger.error("Exception was thrown while processing plugin reloading.", e)
            }
        }
    }

    fun reloadAllPlugins() {
        logger.warn("Plugin reloading is highly experimental, in some cases it can cause severe problems.")
        this.map.keys.forEach { entry ->
            reload(entry)
        }
    }

    fun loadAll() {
        this.map.forEach { entry ->
            entry.value.onInitialize()
        }
    }

    fun getPluginInJarFileStream(id: String, resourceLocation: String): InputStream {
        val instance = this.map[id] ?: throw PluginException("Plugin $id not found.")
        return instance.getInJarFileStream(resourceLocation)
    }

    fun <R> usePluginInJarFile(id: String, resourceLocation: String, func: Function<InputStream, R>): R =
        (this.map[id] ?: throw PluginException("Plugin $id not found.")).useInJarFile(resourceLocation) {
            func.apply(this)
        }
}

private fun Manager<String, PluginInstance>.checkRequirements() {
    val dependencies = mutableListOf<PluginDependency>()
    dependencies += PluginDependency(
        ModuleDescriptor.Version.parse(BuildProperties["version"]!!),
        BuildProperties["applicationName"]!!
    )
    map.forEach {
        dependencies += PluginDependency(ModuleDescriptor.Version.parse(it.value.pluginMetadata.version), it.key)
    }
    val unsatisfied = mutableMapOf<PluginMetadata, List<PluginDependencyRequirement>>()
    map.forEach {
        unsatisfied += it.value.pluginMetadata to it.value.checkPluginDependencyRequirements(dependencies)
    }
    if (unsatisfied.any { it.value.isNotEmpty() }) {
        val dependencyMap = mutableMapOf<String, String>()
        dependencies.forEach {
            dependencyMap += it.id to it.version.toString()
        }
        val builder = StringBuilder()
        builder.append("Incompatible plugin set.\n")
        builder.append("Unmet dependency listing:\n")
        unsatisfied.forEach {
            it.value.forEach { requirement ->
                builder.append(
                    "\t${it.key.id} ${it.key.version} requires ${requirement.id} ${requirement.requirement}, ${
                        if (requirement.id !in dependencyMap)
                            "which is missing!"
                        else
                            "but only the wrong version are present: ${dependencyMap[requirement.id]}!"
                    }\n"
                )
            }
        }
        builder.append("A potential solution has been determined:\n")
        unsatisfied.forEach { entry ->
            entry.value.forEach {
                builder.append(
                    if (it.id !in dependencyMap)
                        "\tInstall ${it.id} ${it.requirement}."
                    else
                        "\tReplace ${it.id} ${dependencyMap[it.id]} with ${it.id} ${it.requirement}"
                )
                builder.append("\n")
            }
        }
        throw PluginException(builder.toString())
    }
}