package net.zhuruoling.omms.crystal.plugin

import net.zhuruoling.omms.crystal.main.SharedConstants
import net.zhuruoling.omms.crystal.parser.ParserManager
import net.zhuruoling.omms.crystal.plugin.metadata.PluginDependency
import net.zhuruoling.omms.crystal.plugin.metadata.PluginDependencyRequirement
import net.zhuruoling.omms.crystal.plugin.metadata.PluginMetadata
import net.zhuruoling.omms.crystal.util.BuildProperties
import net.zhuruoling.omms.crystal.util.Manager
import net.zhuruoling.omms.crystal.util.VERSION
import net.zhuruoling.omms.crystal.util.joinFilePaths
import java.io.File
import java.lang.module.ModuleDescriptor
import java.net.URL
import java.net.URLClassLoader

private lateinit var pluginClassLoader: URLClassLoader
private val pluginFileUrlList = mutableListOf<URL>()

object PluginManager : Manager<String, PluginInstance>(
    beforeInit = { pluginClassLoader = URLClassLoader.newInstance(pluginFileUrlList.toTypedArray()) },
    afterInit = {
        val dependencies = mutableListOf<PluginDependency>()
        dependencies += PluginDependency(ModuleDescriptor.Version.parse(BuildProperties["version"]!!), BuildProperties["applicationName"]!!)
        map.forEach {
            dependencies += PluginDependency(ModuleDescriptor.Version.parse(it.value.metadata.version), it.key)
        }
        val unsatisfied = mutableMapOf<PluginMetadata, List<PluginDependencyRequirement>>()
        map.forEach {
            unsatisfied += it.value.metadata to it.value.checkPluginDependencyRequirements(dependencies)
        }
        if (unsatisfied.isNotEmpty()) {
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
                        "\t${it.key.id} ${it.key.version} requires ${requirement.id} ${requirement.requirement}, ${if (requirement.id !in dependencyMap) "which is missing!" else "but only the wrong version are present: ${dependencyMap[requirement.id]}!"}\n"
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
        PluginInstance(pluginClassLoader, joinFilePaths("plugins", it)).run {
            loadPluginMetadata()
            loadPluginClasses()
            metadata.id!! to this
        }
    }
) {


    fun loadAll() {
        this.map.forEach { entry ->
            entry.value.onInitialize()
        }
    }
}