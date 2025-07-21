package icu.takeneko.omms.crystal.plugin

import icu.takeneko.omms.crystal.main.SharedConstants
import icu.takeneko.omms.crystal.parser.ParserManager
import icu.takeneko.omms.crystal.plugin.metadata.PluginDependency
import icu.takeneko.omms.crystal.util.*
import icu.takeneko.omms.crystal.util.file.FileUtil.joinFilePaths
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
            ifPluginDebug {
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
    val dependencies = buildList {
        add(PluginDependency(ModuleDescriptor.Version.parse(BuildProperties["version"]!!), BuildProperties["applicationName"]!!))
        map.forEach {
            add(PluginDependency(ModuleDescriptor.Version.parse(it.value.pluginMetadata.version), it.key))
        }
    }
    val unsatisfied = buildMap {
        map.forEach { put(it.value.pluginMetadata, it.value.checkPluginDependencyRequirements(dependencies)) }
    }

    if (unsatisfied.any { it.value.isNotEmpty() }) {
        val dependencyMap = buildMap {
            dependencies.forEach {
                put(it.id, it.version.toString())
            }
        }
        val string = buildString {
            appendLine("Incompatible plugin set.")
            appendLine("Unmet dependency listing:")
            unsatisfied.forEach {
                it.value.forEach { requirement ->
                    appendLine(
                        "\t${it.key.id} ${it.key.version} requires ${requirement.id} ${requirement.requirement}, ${
                            if (requirement.id !in dependencyMap)
                                "which is missing!"
                            else
                                "but only the wrong version are present: ${dependencyMap[requirement.id]}!"
                        }"
                    )
                }
            }
            appendLine("A potential solution has been determined:")
            unsatisfied.forEach { entry ->
                entry.value.forEach {
                    appendLine(
                        if (it.id !in dependencyMap)
                            "\tInstall ${it.id} ${it.requirement}."
                        else
                            "\tReplace ${it.id} ${dependencyMap[it.id]} with ${it.id} ${it.requirement}"
                    )
                }
            }
        }
        throw PluginException(string)
    }
}