package icu.takeneko.omms.crystal.plugin.instance

import icu.takeneko.omms.crystal.plugin.support.JarClassLoader
import icu.takeneko.omms.crystal.plugin.metadata.PluginMetadata
import java.nio.file.Path

class JarPluginContainer(
    private val classLoader: JarClassLoader,
    private val pluginPath: Path
): PluginContainer() {
    init {
        classLoader.loadJar(pluginPath.toFile())
    }

    override fun constructPlugin() {
    }

    override fun getMetadata(): PluginMetadata {

    }
}