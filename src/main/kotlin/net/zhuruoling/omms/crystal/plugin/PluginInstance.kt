package net.zhuruoling.omms.crystal.plugin

import net.zhuruoling.omms.crystal.event.Event
import net.zhuruoling.omms.crystal.event.EventArgs
import net.zhuruoling.omms.crystal.event.getEventById
import net.zhuruoling.omms.crystal.i18n.*
import net.zhuruoling.omms.crystal.main.DebugOptions
import net.zhuruoling.omms.crystal.parser.MinecraftParser
import net.zhuruoling.omms.crystal.plugin.api.annotations.EventHandler
import net.zhuruoling.omms.crystal.plugin.metadata.PluginDependency
import net.zhuruoling.omms.crystal.plugin.metadata.PluginDependencyRequirement
import net.zhuruoling.omms.crystal.plugin.metadata.PluginMetadata
import net.zhuruoling.omms.crystal.plugin.resources.PluginResource
import net.zhuruoling.omms.crystal.util.createLogger
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.zip.ZipException
import java.util.zip.ZipFile

class PluginInstance(private val urlClassLoader: URLClassLoader, private val fileFullPath: String) {

    lateinit var metadata: PluginMetadata
    private lateinit var pluginClazz: Class<*>
    private lateinit var instance: PluginInitializer
    private var pluginState = PluginState.WAIT
    val eventListeners = mutableListOf<Pair<Event, (EventArgs) -> Unit>>()
    private val logger = createLogger("PluginInstance")
    val pluginParsers = mutableMapOf<String, MinecraftParser>()
    val resources = mutableMapOf<String, PluginResource>()
    fun loadPluginMetadata() {
        pluginState = PluginState.ERROR
        try {
            useInJarFile("crystal.plugin.json") {
                metadata = PluginMetadata.fromJson(readAllBytes().decodeToString())
                if (metadata.pluginDependencies != null) {
                    metadata.pluginDependencies!!.forEach { it2 ->
                        it2.parseRequirement()
                    }
                }
                checkMetadata()
            }
        } catch (e: Exception) {
            throw PluginException("Cannot read plugin jar file.", e)
        }
        pluginState = PluginState.PRE_LOAD
    }

    fun loadPluginClasses() {
        pluginState = PluginState.ERROR

        if (metadata.pluginInitializerClass != null) {
            try {
                pluginClazz = urlClassLoader.loadClass(metadata.pluginInitializerClass)
                val ins = pluginClazz.getConstructor().newInstance()
                if (ins !is PluginInitializer) {
                    throw PluginException("Plugin initializer class did not implement PluginInitializer.")
                }
                instance = ins
            } catch (e: Exception) {
                throw PluginException("Cannot load plugin initializer.", e)
            }
        }
        if (metadata.pluginEventHandlers != null) {
            if (metadata.pluginEventHandlers!!.isNotEmpty()) {
                val classes = mutableListOf<Class<out Any>>()
                metadata.pluginEventHandlers!!.forEach {
                    try {
                        classes += urlClassLoader.loadClass(it)
                    } catch (e: ClassNotFoundException) {
                        throw PluginException("Cannot load event handler class $it", e)
                    }
                }
                classes.map {
                    it.getDeclaredConstructor().run { isAccessible = true;this }.newInstance() to
                            Arrays.stream(it.declaredMethods)
                                .filter { it3 -> it3.annotations.any { it2 -> it2 is EventHandler } }
                                .map { it2 -> it2.getAnnotation(EventHandler::class.java).run { this.event to it2 } }
                                .toList()
                }.forEach { p ->
                    p.second.forEach { (s, m) ->
                        try {
                            m.isAccessible = true
                            val event = getEventById(s)
                            eventListeners += event to { args ->
                                try {
                                    m.invoke(p.first, args)
                                } catch (e: Exception) {
                                    logger.error(
                                        "Cannot invoke plugin(${metadata.id}) event listener ${m.toGenericString()}.",
                                        e
                                    )
                                }
                            }
                        } catch (e: IllegalArgumentException) {
                            throw PluginException("Cannot transform method ${m.toGenericString()} into EventHandler", e)
                        } catch (e: Exception) {
                            throw PluginException("", e)
                        }
                    }
                }
            }
        }
        if (metadata.pluginMinecraftParsers != null && metadata.pluginMinecraftParsers!!.isNotEmpty()) {
            metadata.pluginMinecraftParsers!!.forEach {
                try {
                    val clazz = urlClassLoader.loadClass(it.value)
                    val instance = clazz.getConstructor().newInstance()
                    if (instance !is MinecraftParser) {
                        throw PluginException("Plugin declared Minecraft parser class is not derived from MinecraftParser.")
                    }
                    pluginParsers += it.key to instance
                } catch (e: ClassNotFoundException) {
                    throw PluginException("Cannot load class ${it.value},", e)
                } catch (e: NoSuchMethodException) {
                    throw PluginException("Cannot load class ${it.value},", e)
                }
            }
        }

        pluginState = PluginState.INITIALIZED
    }

    private fun checkMetadata() {
        if (metadata.id == null) {
            throw PluginException("plugin $fileFullPath: plugin id is null")
        }
        if (metadata.version == null) {
            throw PluginException("plugin $fileFullPath: plugin version is null")
        }
    }

    fun checkPluginDependencyRequirements(dependencies: List<PluginDependency>): List<PluginDependencyRequirement> {
        pluginState = PluginState.ERROR
        var result = mutableListOf<PluginDependencyRequirement>()
        if (metadata.pluginDependencies != null) {
            result = metadata.pluginDependencies!!.filter { dependencies.none { it2 -> it.requirementMatches(it2) } }
                .toMutableList()
        }
        pluginState = PluginState.INITIALIZED
        return result
    }

    fun onInitialize() {
        pluginState = PluginState.ERROR
        try {
            instance.onInitialize()
        } catch (e: Exception) {
            throw PluginException("onInitialize", e)
        }
        pluginState = PluginState.LOADED
    }

    private fun <R> useInJarFile(fileName: String, consumer: InputStream.() -> R): R =
        ZipFile(File(fileFullPath)).use {
            try {
                val entry = it.getEntry(fileName)
                val inputStream = it.getInputStream(entry)
                consumer(inputStream)
            } catch (e: PluginException) {
                throw e
            } catch (e: ZipException) {
                throw PluginException("ZIP format error occurred while reading plugin jar file.", e)
            } catch (e: IOException) {
                throw PluginException("I/O error occurred while reading plugin jar file.", e)
            } catch (e: Exception) {
                throw PluginException("Cannot read plugin jar file.", e)
            }
        }

    fun loadPluginResources() {
        if (DebugOptions.pluginDebug()) logger.info("Loading plugin ${metadata.id} resources.")
        if (metadata.resources != null) {
            metadata.resources!!.forEach {
                logger.info("${metadata.id}: ${it.key} <- ${it.value}")
                useInJarFile(it.value) {
                    resources[it.key] = PluginResource.fromReader(it.key, reader(StandardCharsets.UTF_8))
                }
            }
        }
        resources.forEach {
            val resType = it.value.resMeta["type"] ?: return@forEach
            val namespace = it.value.resMeta["namespace"] ?: return@forEach
            if (resType == "lang") {
                val lang = Identifier(it.key.replace("_", ":"))
                TranslateManager.getOrCreateLanguageProvider(
                    lang,
                    impl = LanguageProviderImpl::class.java,
                    lang,
                    linkedMapOf<Identifier, TranslatableString>()
                ).apply {
                    it.value.resMap.forEach { (k, v) ->
                        val translateKey = TranslateKey(lang, Identifier(namespace, k))
                        if (DebugOptions.pluginDebug()) logger.info("$k -> $v")
                        addTranslateKey(translateKey, TranslatableString(translateKey, v))
                    }
                }
            }
        }
    }


}