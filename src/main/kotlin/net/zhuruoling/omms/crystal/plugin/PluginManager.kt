package net.zhuruoling.omms.crystal.plugin

import net.zhuruoling.omms.crystal.util.Manager
import net.zhuruoling.omms.crystal.util.joinFilePaths
import java.io.File
import java.net.URL
import java.net.URLClassLoader

private lateinit var pluginClassLoader: URLClassLoader
private val pluginFileUrlList = mutableListOf<URL>()

object PluginManager : Manager<String, PluginInstance>(
    beforeInit = { pluginClassLoader = URLClassLoader.newInstance(pluginFileUrlList.toTypedArray()) },
    afterInit = {},
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
            metadata.id to this
        }
    }
) {


    fun loadAll() {
        this.map.forEach {
            it.value.onInitialize()
        }
    }
}