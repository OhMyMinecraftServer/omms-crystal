package icu.takeneko.omms.crystal.main

import icu.takeneko.omms.crystal.command.CommandHelpManager
import icu.takeneko.omms.crystal.command.helpCommand
import icu.takeneko.omms.crystal.command.permissionCommand
import icu.takeneko.omms.crystal.command.pluginCommand
import icu.takeneko.omms.crystal.command.startCommand
import icu.takeneko.omms.crystal.command.stopCommand
import icu.takeneko.omms.crystal.config.Config
import icu.takeneko.omms.crystal.console.ConsoleHandler
import icu.takeneko.omms.crystal.event.Event
import icu.takeneko.omms.crystal.event.EventBus
import icu.takeneko.omms.crystal.event.PluginBusEvent
import icu.takeneko.omms.crystal.event.SubscribeEvent
import icu.takeneko.omms.crystal.event.server.LaunchServerEvent
import icu.takeneko.omms.crystal.event.server.RegisterCommandEvent
import icu.takeneko.omms.crystal.event.server.ServerStoppedEvent
import icu.takeneko.omms.crystal.event.server.ServerStoppingEvent
import icu.takeneko.omms.crystal.foundation.ActionHost
import icu.takeneko.omms.crystal.i18n.TranslateManager
import icu.takeneko.omms.crystal.permission.PermissionManager
import icu.takeneko.omms.crystal.plugin.PluginManager
import icu.takeneko.omms.crystal.rcon.RconListener
import icu.takeneko.omms.crystal.server.ServerThreadDaemon
import icu.takeneko.omms.crystal.util.BuildProperties
import icu.takeneko.omms.crystal.util.PRODUCT_NAME
import icu.takeneko.omms.crystal.util.createLogger
import icu.takeneko.omms.crystal.util.joinFilePaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.lang.management.ManagementFactory
import java.nio.file.Files
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.system.exitProcess

@OptIn(ExperimentalCoroutinesApi::class)
object CrystalServer : CoroutineScope, ActionHost {
    val coroutineDispatcher = Dispatchers.IO.limitedParallelism(Runtime.getRuntime().availableProcessors())
    var rconListener: RconListener? = null
    val eventBus = EventBus(this, Event::class.java)
    var serverThreadDaemon: ServerThreadDaemon? = null
    val consoleHandler: ConsoleHandler = ConsoleHandler()
    val logger = createLogger("CrystalServer")
    val config
        get() = Config.config

    fun initialize(args: Array<String>) {
        val start = System.currentTimeMillis()
        eventBus.register(this)
        Runtime.getRuntime().addShutdownHook(
            thread(name = "ShutdownThread", start = false) {
                if (serverThreadDaemon != null) {
                    println("Stopping server because jvm is shutting down.")
                    serverThreadDaemon!!.outputHandler.interrupt()
                    serverThreadDaemon!!.stopServer(true)
                }
            }
        )
        consoleHandler.start()
        logger.info("Hello World!")
        val os = ManagementFactory.getOperatingSystemMXBean()
        val runtime = ManagementFactory.getRuntimeMXBean()
        logger.info("$PRODUCT_NAME ${BuildProperties["version"]} is running on ${os.name} ${os.arch} ${os.version} at PID ${runtime.pid}")
        try {
            if (Config.load()) {
                val serverPath = Path(joinFilePaths("server"))
                if (!serverPath.exists() || !serverPath.isDirectory()) {
                    Files.createDirectory(serverPath)
                }
                exitProcess(1)
            }
            ifMainDebug {
                logger.info("Config:")
                logger.info("\tServerWorkingDirectory: {}", config.workingDir)
                logger.info("\tLaunchCommand: {}", config.launchCommand)
                logger.info("\tPluginDirectory: {}", config.pluginDirectory)
                logger.info("\tServerType: {}", config.serverType)
                logger.info("\tDebugOptions: {}", DebugOptions)
            }
            TranslateManager.useLanguage(config.lang)
            TranslateManager.init()
            CommandHelpManager.init()
            init()
            PluginManager.init()
            PluginManager.loadAll()
            PermissionManager.init()
            consoleHandler.reload()
            if (config.enableRconServer) {
                rconListener = RconListener.create()
            }
            val end = System.currentTimeMillis()
            logger.info("Startup preparations finished in ${end - start} milliseconds.")
            if (args.contains("--noserver")) {
                Thread.sleep(1500)
                exit()
                exitProcess(0)
            }

        } catch (e: Exception) {
            logger.error("Unexpected error occurred.", e)
            exitProcess(1)
        }
    }

    @SubscribeEvent
    fun onRegisterCommands(e: RegisterCommandEvent) {
        e.register(helpCommand)
        e.register(permissionCommand)
        e.register(startCommand)
        e.register(stopCommand)
        e.register(pluginCommand)
    }

    @SubscribeEvent
    fun onServerStopping(e: ServerStoppingEvent) {

    }

    fun run() {
        postEvent(LaunchServerEvent(config.launchCommand, Path(config.workingDir)))
    }

    inline fun <reified T : Event> postEvent(e: T) {
        if (e is PluginBusEvent) {

            return
        }
        eventBus.dispatch(e)
    }

    inline fun <reified T : Event> postEventWithReturn(e: T): T {
        if (e is PluginBusEvent) {
            return e
        }
        eventBus.dispatch(e)
        return e
    }

    fun destroyDaemon() {
        this.serverThreadDaemon = null
    }

    override val coroutineContext: CoroutineContext
        get() = coroutineDispatcher
}