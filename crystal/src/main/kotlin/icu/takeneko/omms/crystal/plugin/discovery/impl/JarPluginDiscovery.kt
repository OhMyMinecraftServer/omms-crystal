package icu.takeneko.omms.crystal.plugin.discovery.impl

import icu.takeneko.omms.crystal.crystalspi.ICrystalPluginDiscovery
import icu.takeneko.omms.crystal.plugin.PluginManager
import icu.takeneko.omms.crystal.plugin.container.jar.JarPluginContainer
import icu.takeneko.omms.crystal.plugin.container.PluginContainer
import icu.takeneko.omms.crystal.plugin.metadata.PluginMetadata
import icu.takeneko.omms.crystal.plugin.support.JarVisitor
import icu.takeneko.omms.crystal.util.LoggerUtil
import icu.takeneko.omms.crystal.util.constants.DebugOptions
import icu.takeneko.omms.crystal.util.file.FileUtil
import icu.takeneko.omms.crystal.util.file.decodeFromString
import java.io.File
import java.net.URI
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.io.path.pathString

class JarPluginDiscovery : ICrystalPluginDiscovery, JarVisitor {
    private val logger = LoggerUtil.createLogger("JarPluginDiscovery", DebugOptions.pluginDebug())
    private val metadataFiles = mutableMapOf<String, PluginMetadata>()
    private val yaml = FileUtil.YAML
    private var currentJarURI: URI? = null

    override fun collectPlugins(): List<PluginContainer> {
        return PluginManager.pluginFiles.map {
            try {
                logger.info("Considering plugin file {}", it.pathString)
                JarPluginContainer(PluginManager.pluginClassLoader, it, metadataFiles)
            } catch (t: Throwable) {
                throw IllegalStateException("Failed to create plugin container", t)
            }
        }
    }

    override fun bootstrap() {
        PluginManager.pluginClassLoader.addVisitor(this)
    }

    override fun key(): String = "jar"

    override fun visitJarFile(file: File) {
        currentJarURI = file.toURI()
    }

    override fun visitJarEntry(file: ZipFile, entry: ZipEntry) {
        if (currentJarURI == null) throw IllegalStateException("No jar was currently visiting")
        if (entry.name == "crystal.plugin.yaml") {
            val meta = yaml.decodeFromString<PluginMetadata>(file.getInputStream(entry).readAllBytes().decodeToString())
            metadataFiles += meta.id to meta
        }
    }
}