package icu.takeneko.omms.crystal.plugin.container

import icu.takeneko.omms.crystal.plugin.metadata.PluginMetadata
import icu.takeneko.omms.crystal.plugin.support.JarClassLoader
import java.nio.file.Path

class JarPluginContainer(
    private val classLoader: JarClassLoader,
    private val pluginPath: Path
) : PluginContainer() {

    override fun constructPlugin() {
    }

    override fun getMetadata(): PluginMetadata {
        TODO()
    }
}
