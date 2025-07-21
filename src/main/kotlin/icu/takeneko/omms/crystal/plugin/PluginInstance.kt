package icu.takeneko.omms.crystal.plugin

import icu.takeneko.omms.crystal.event.Event
import icu.takeneko.omms.crystal.event.EventArgs
import icu.takeneko.omms.crystal.i18n.*
import icu.takeneko.omms.crystal.parser.MinecraftParser
import icu.takeneko.omms.crystal.plugin.api.annotations.Config
import icu.takeneko.omms.crystal.plugin.api.annotations.EventHandler
import icu.takeneko.omms.crystal.plugin.api.annotations.InjectArgument
import icu.takeneko.omms.crystal.plugin.metadata.PluginDependency
import icu.takeneko.omms.crystal.plugin.metadata.PluginDependencyRequirement
import icu.takeneko.omms.crystal.plugin.metadata.PluginMetadata
import icu.takeneko.omms.crystal.plugin.resources.PluginResource
import icu.takeneko.omms.crystal.plugin.support.JarClassLoader
import icu.takeneko.omms.crystal.util.LoggerUtil.createLogger
import icu.takeneko.omms.crystal.util.file.FileUtil.joinFilePaths
import icu.takeneko.omms.crystal.util.reflect.methodsWithAnnotation
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Modifier
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.properties.Delegates

class PluginInstance(
    private val classLoader: JarClassLoader,
    private val fileFullPath: String,
    private val pluginStateChangeListener: PluginInstance.(PluginState, PluginState) -> Unit = { _, _ -> }
) {
    var pluginMetadata: PluginMetadata = PluginMetadata()
    private lateinit var pluginClass: Class<*>
    private lateinit var instance: PluginInitializer
    private var _pluginState = PluginState.WAIT
    private lateinit var pluginConfigPath: Path
    private lateinit var pluginConfigFile: File
    private var pluginState by Delegates.observable(PluginState.ERROR) { _, before, after ->
        pluginStateChangeListener(this, before, after)
    }

    val eventListeners = mutableListOf<Pair<Event, (EventArgs) -> Unit>>()
    private val logger = createLogger("PluginInstance")
    val pluginParsers = mutableMapOf<String, MinecraftParser>()
    val pluginResources = mutableMapOf<String, PluginResource>()

    fun loadPluginMetadata() {
        pluginState = PluginState.ERROR
        try {
            useInJarFile("crystal.plugin.json") {
                pluginMetadata = PluginMetadata.fromJson(readAllBytes().decodeToString())
                pluginMetadata.pluginDependencies?.forEach { it.parseRequirement() }
                checkMetadata()
            }
            pluginConfigPath = Path(joinFilePaths("config", pluginMetadata.id!!))
        } catch (e: Exception) {
            throw PluginException("Cannot read plugin jar file.", e)
        }
        pluginState = PluginState.PRE_LOAD
    }

    fun loadPluginClasses() {
        pluginState = PluginState.ERROR
        loadInitializer()
        loadEventHandlers()
        loadMinecraftParsers()

        pluginState = PluginState.INITIALIZED
    }

    private fun loadInitializer() {
        val className = pluginMetadata.pluginInitializerClass ?: return
        try {
            pluginClass = classLoader.loadClass(className)
            val ins = pluginClass.getConstructor().newInstance()
            if (ins !is PluginInitializer) {
                throw PluginException("Plugin initializer class did not implement PluginInitializer.")
            }
            instance = ins
        } catch (e: Exception) {
            throw PluginException("Cannot load plugin initializer.", e)
        }
    }

    private fun loadEventHandlers() {
        val handlerClassNames = pluginMetadata.pluginEventHandlers.takeIf { !it.isNullOrEmpty() } ?: return
        handlerClassNames.map { classLoader.loadClass(it) }.map { clazz ->
            val constructor = clazz.getDeclaredConstructor().apply { isAccessible = true }
            val targetMethods = clazz.methodsWithAnnotation<EventHandler>().associateBy { method ->
                val eventType = method.getAnnotation(EventHandler::class.java)!!.event.java
                method.trySetAccessible()
                eventType
            }
            constructor.newInstance() to targetMethods
        }.forEach { (constructor, methods) ->
            methods.forEach { (clazz, method) ->
                try {
                    method.trySetAccessible()
                    val event = findPluginEventInstance(clazz)
                    eventListeners += event to { args: Any? ->
                        try {
                            method.invoke(constructor, args)
                        } catch (e: Exception) {
                            logger.error(
                                "Cannot invoke plugin({}) event listener {}.",
                                pluginMetadata.id, method.toGenericString(), e
                            )
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    throw PluginException(
                        "Cannot transform method ${method.toGenericString()} into EventHandler",
                        e
                    )
                } catch (e: Exception) {
                    throw PluginException("", e)
                }
            }
        }
    }

    private fun loadMinecraftParsers() {
        val parsers = pluginMetadata.pluginMinecraftParsers.takeIf { !it.isNullOrEmpty() } ?: return
        parsers.forEach { parser ->
            try {
                val clazz = classLoader.loadClass(parser.value)
                val instance = clazz.getConstructor().newInstance()
                if (instance !is MinecraftParser) {
                    throw PluginException("Plugin declared Minecraft parser class is not derived from MinecraftParser.")
                }
                pluginParsers[parser.key] = instance
            } catch (e: ClassNotFoundException) {
                throw PluginException("Cannot load class ${parser.value},", e)
            } catch (e: NoSuchMethodException) {
                throw PluginException("Cannot load class ${parser.value},", e)
            }
        }
    }

    private fun findPluginEventInstance(clazz: Class<out Event>) =
        if (clazz.declaredFields.any { it.name == "INSTANCE" && it.type == clazz }) {
            clazz.getDeclaredField("INSTANCE").apply { isAccessible = true }.get(null)
        } else {
            clazz.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
        } as Event


    fun injectArguments() {
        pluginState = PluginState.ERROR

        ifPluginDebug { logger.info("[DEBUG] Injecting argument") }

        for (field in pluginClass.declaredFields) {
            field.isAccessible = true
            if (field.isAnnotationPresent(InjectArgument::class.java)) {
                when (val name = field.getAnnotation(InjectArgument::class.java)!!.name) {
                    "pluginConfig" -> {
                        ifPluginDebug { logger.info("[DEBUG] Injecting pluginConfig into $field") }
                        require(field.type != Path::class.java) {
                            "Illegal field type of pluginConfig injection.(Require java.nio.file.Path, but found ${field.type.name})"
                        }
                        field.set(instance, pluginConfigPath)
                    }

                    else -> error("Illegal injection type $name")
                }
                continue
            }
            if (field.isAnnotationPresent(Config::class.java) && field.isAnnotationPresent(InjectArgument::class.java)) {
                error("@Config cannot be used simultaneously with @InjectArgument (at field $field in class $pluginClass).")
            }
            if (field.isAnnotationPresent(Config::class.java)) {
                val configClass = field.type
                val defaultConfig = try {
                    configClass.getDeclaredField("DEFAULT").get(null)
                } catch (_: NoSuchFieldException) {
                    try {
                        configClass.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
                    } catch (e: Exception) {
                        throw PluginException("Cannot create default config.", e)
                    }
                }
                pluginConfigFile = (pluginConfigPath / "${pluginMetadata.id}.json").toFile()
                if (!pluginConfigFile.exists()) {
                    pluginConfigFile.createNewFile()
                    pluginConfigFile.writer().use {
                        gsonForPluginMetadata.toJson(defaultConfig, it)
                    }
                }
                if (pluginConfigFile.exists()) {
                    val configInstance = pluginConfigFile.reader().use {
                        fillFieldsUseDefault(gsonForPluginMetadata.fromJson(it, configClass), defaultConfig)
                    }
                    field.set(instance, configInstance)
                }
            }
        }
        pluginState = PluginState.INITIALIZED
    }

    private fun <T> fillFieldsUseDefault(t: T, default: T): T {
        t!!::class.java.declaredFields.forEach {
            if (Modifier.isStatic(it.modifiers)) return@forEach
            if (it.get(t) == null) {
                it.set(t, it.get(default))
            }
        }
        return t
    }

    private fun checkMetadata() {
        if (pluginMetadata.id == null) {
            throw PluginException("plugin $fileFullPath: plugin id is null")
        }
        if (pluginMetadata.version == null) {
            throw PluginException("plugin $fileFullPath: plugin version is null")
        }
    }

    fun checkPluginDependencyRequirements(dependencies: List<PluginDependency>): List<PluginDependencyRequirement> {
        pluginState = PluginState.ERROR
        val result = buildList {
            if (pluginMetadata.pluginDependencies != null) {
                addAll(pluginMetadata.pluginDependencies!!.filter { dependencies.none { it2 -> it.requirementMatches(it2) } })
            }
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

    fun <R> useInJarFile(fileName: String, consumer: InputStream.() -> R): R =
        ZipFile(File(fileFullPath)).use {
            try {
                val entry = it.getEntry(fileName)
                val inputStream = it.getInputStream(entry)
                val r = consumer(inputStream)
                inputStream.close()
                r
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

    fun getInJarFileStream(path: String): InputStream? = ZipFile(File(fileFullPath)).use {
        val entry = it.getEntry(path)
        it.getInputStream(entry)
    }

    fun onFinalize() {
        pluginState = PluginState.ERROR
        try {
            instance.onFinalize()
        } catch (e: Exception) {
            throw PluginException("onFinalize", e)
        }
        pluginState = PluginState.LOADED
    }

    fun loadPluginResources() {
        ifPluginDebug { logger.info("Loading plugin ${pluginMetadata.id} resources.") }

        pluginMetadata.resources?.forEach {
            ifPluginDebug { logger.info("[DEBUG] ${pluginMetadata.id}: ${it.key} <- ${it.value}") }

            useInJarFile(it.value) {
                pluginResources[it.key] = PluginResource.fromReader(it.key, reader(StandardCharsets.UTF_8))
            }
        }

        ifPluginDebug {
            if (pluginResources.isEmpty()) {
                logger.info("[DEBUG] Plugin ${pluginMetadata.id} has no resources.")
            }
        }

        pluginResources.forEach {
            ifPluginDebug { logger.info("[DEBUG] Resource ${it.value}") }
            val resType = it.value.resMetaDataMap["type"] ?: return@forEach
            val namespace = it.value.resMetaDataMap["namespace"] ?: return@forEach
            if (resType == "lang") {
                val lang = it.key
                TranslateManager.getOrCreateDefaultLanguageProvider(lang).apply {
                    it.value.resDataMap.forEach { (k, v) ->
                        val translateKey = TranslateKey(lang, namespace, k)
                        ifPluginDebug { logger.info("[DEBUG] Translation: $k -> $v") }
                        this.addTranslateKey(translateKey, v)
                    }
                }
            }
        }
    }
}