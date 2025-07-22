package icu.takeneko.omms.crystal.plugin

import icu.takeneko.omms.crystal.foundation.Manager
import icu.takeneko.omms.crystal.plugin.instance.PluginContainer
import icu.takeneko.omms.crystal.plugin.support.JarClassLoader
import java.nio.file.Path
import kotlin.io.path.extension

object PluginManager : Manager<PluginContainer>("plugins") {
    private val classLoader = JarClassLoader(Thread.currentThread().contextClassLoader)
    override fun shouldAcceptFile(path: Path): Boolean = path.extension.lowercase() == "jar"

    override fun createInstance(path: Path): PluginContainer {
        TODO("Not yet implemented")
    }

    fun loadAll() {
        TODO()
    }

    fun reload(id: String) {
        TODO()
    }

    fun reloadAll() {
        TODO()
    }
}
