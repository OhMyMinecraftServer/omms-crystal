package icu.takeneko.omms.crystal.plugin.container.jar

import icu.takeneko.omms.crystal.event.EventBus
import icu.takeneko.omms.crystal.event.EventBusSubscriber
import icu.takeneko.omms.crystal.plugin.annotation.Plugin
import icu.takeneko.omms.crystal.plugin.container.PluginContainer
import icu.takeneko.omms.crystal.plugin.metadata.PluginMetadata
import icu.takeneko.omms.crystal.plugin.support.JarClassLoader
import icu.takeneko.omms.crystal.plugin.support.ScanData
import org.objectweb.asm.Type
import java.nio.file.Path

class JarPluginContainer(
    private val classLoader: JarClassLoader,
    private val pluginPath: Path,
    private val metadataFiles: MutableMap<String, PluginMetadata>
) : PluginContainer() {

    private val scanData = classLoader.allScanData[pluginPath.toUri()].orEmpty()
    private lateinit var pluginInstance: Any
    private var pluginConstruction: PluginConstructor
    private var metadata: PluginMetadata
    private val foundEventBusSubscribers = mutableMapOf<String, ScanData>()

    init {
        val foundPluginEntries = mutableMapOf<String, String>()
        scanData.forEach {
            if (it.classAnnotations.any { d -> d.desc == EventBusSubscriberFqcn }) {
                foundEventBusSubscribers += it.classFqcn to it
            }
            val (desc, values) = it.classAnnotations.firstOrNull { d -> d.desc == PluginFqcn } ?: return@forEach
            val id = values["id"]!! as String
            foundPluginEntries += it.classFqcn to id
        }
        if (foundPluginEntries.size > 1) {
            throw IllegalStateException("Crystal does not allow multiple Plugin entry in a single jar.")
        }
        var (clazzName, id) = foundPluginEntries.entries.first()
        if (id !in metadataFiles) {
            throw IllegalStateException("File $pluginPath contains plugin entrypoint class $clazzName for plugin with id $id, which does not exist or is not in the same file.")
        }
        this.metadata = metadataFiles[id]!!
        val clazz = classLoader.loadClass(clazzName)
        val allowedArgs = mapOf(
            EventBus::class.java to pluginEventBus,
            PluginContainer::class.java to this
        )
        if (clazz.declaredConstructors.size > 1) {
            throw IllegalStateException("Plugin class $clazzName must have exactly 1 public constructor, found ${clazz.declaredConstructors.size}")
        }
        val constructor = clazz.declaredConstructors[0]
        val parameters = constructor.parameters
        val constructionParameters = mutableListOf<Any>()
        if (parameters.size in 0..2) {
            if (parameters.all { p -> p.type in allowedArgs }) {
                for (parameter in parameters) {
                    constructionParameters += allowedArgs[parameter.type]!!
                }
                pluginConstruction = PluginConstructor {
                    return@PluginConstructor constructor.newInstance(*constructionParameters.toTypedArray())
                }
            } else {
                throw IllegalStateException("Plugin constructor has unsupported argument.")
            }
        } else {
            throw IllegalStateException("No viable constructor found for creating plugin instance, got $constructor")
        }


    }

    override fun constructPlugin() {
        pluginInstance = pluginConstruction.create()
        foundEventBusSubscribers.forEach { (clazz, data) ->
            EventAutoSubscriber.handleClass(classLoader,clazz, data, pluginEventBus)
        }
    }

    override fun getMetadata(): PluginMetadata = metadata

    companion object {
        internal val EventBusSubscriberFqcn = "L" + Type.getInternalName(EventBusSubscriber::class.java) + ";"
        internal val PluginFqcn = "L" + Type.getInternalName(Plugin::class.java) + ";"
    }

    private fun interface PluginConstructor {
        fun create(): Any
    }
}