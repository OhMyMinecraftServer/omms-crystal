package icu.takeneko.omms.crystal.main

import com.mojang.brigadier.exceptions.CommandSyntaxException
import icu.takeneko.omms.crystal.command.*
import icu.takeneko.omms.crystal.command.BuiltinCommand.helpCommand
import icu.takeneko.omms.crystal.command.BuiltinCommand.permissionCommand
import icu.takeneko.omms.crystal.command.BuiltinCommand.pluginCommand
import icu.takeneko.omms.crystal.command.BuiltinCommand.startCommand
import icu.takeneko.omms.crystal.command.BuiltinCommand.stopCommand
import icu.takeneko.omms.crystal.config.ConfigManager
import icu.takeneko.omms.crystal.console.ConsoleHandler
import icu.takeneko.omms.crystal.event.Event
import icu.takeneko.omms.crystal.event.EventBus
import icu.takeneko.omms.crystal.event.PluginBusEvent
import icu.takeneko.omms.crystal.event.SubscribeEvent
import icu.takeneko.omms.crystal.event.server.*
import icu.takeneko.omms.crystal.foundation.ActionHost
import icu.takeneko.omms.crystal.i18n.TranslateManager
import icu.takeneko.omms.crystal.permission.PermissionManager
import icu.takeneko.omms.crystal.plugin.PluginManager
import icu.takeneko.omms.crystal.rcon.RconClient
import icu.takeneko.omms.crystal.rcon.RconListener
import icu.takeneko.omms.crystal.server.ServerStatus
import icu.takeneko.omms.crystal.server.ServerThreadDaemon
import icu.takeneko.omms.crystal.server.serverStatus
import icu.takeneko.omms.crystal.util.BuildProperties
import icu.takeneko.omms.crystal.util.LoggerUtil.createLogger
import icu.takeneko.omms.crystal.util.PRODUCT_NAME
import icu.takeneko.omms.crystal.util.command.CommandSource
import icu.takeneko.omms.crystal.util.command.CommandSourceStack
import icu.takeneko.omms.crystal.util.constants.DebugOptions
import icu.takeneko.omms.crystal.util.file.FileUtil.joinFilePaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.lang.management.ManagementFactory
import java.nio.file.Files
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.system.exitProcess
import kotlin.time.measureTime

@OptIn(ExperimentalCoroutinesApi::class)
object CrystalServer : CoroutineScope, ActionHost {
    val coroutineDispatcher = Dispatchers.IO.limitedParallelism(Runtime.getRuntime().availableProcessors())

    val eventBus = EventBus(this, Event::class.java)

    val consoleHandler = ConsoleHandler()

    val logger = createLogger("CrystalServer")

    val config
        get() = ConfigManager.config

    var rconListener: RconListener? = null

    var serverThreadDaemon: ServerThreadDaemon? = null

    fun initialize(args: Array<String>) {
        val duration = measureTime {
            eventBus.register(this)
            Runtime.getRuntime().addShutdownHook(
                thread(name = "ShutdownThread", start = false) {
                    if (serverThreadDaemon != null) {
                        println("Stopping server because jvm is shutting down.")
                        serverThreadDaemon!!.outputHandler.interrupt()
                        serverThreadDaemon!!.stopServer(true, this)
                    }
                }
            )
            consoleHandler.start()

            logger.info("Hello World!")
            val os = ManagementFactory.getOperatingSystemMXBean()
            logger.info(
                "{} {} is running on {} {} {} at PID {}",
                PRODUCT_NAME,
                BuildProperties["version"],
                os.name,
                os.arch,
                os.version,
                ManagementFactory.getRuntimeMXBean().pid
            )

            runCatching {
                if (ConfigManager.load()) {
                    val serverPath = Path(joinFilePaths("server"))
                    if (!serverPath.exists() || !serverPath.isDirectory()) {
                        Files.createDirectory(serverPath)
                    }
                    exitProcess(1)
                }
                ifMainDebug {
                    logger.info("Config:")
                    logger.info("\tServerWorkingDirectory: {}", config.workingDirectory)
                    logger.info("\tLaunchCommand: {}", config.launchCommand)
                    logger.info("\tPluginDirectory: {}", config.pluginDirectory)
                    logger.info("\tServerType: {}", config.serverType)
                    logger.info("\tDebugOptions: {}", DebugOptions)
                }
                TranslateManager.useLanguage(config.lang)
                TranslateManager.init()
                CommandHelpManager.init()
                PluginManager.init()
                PluginManager.loadAll()
                PermissionManager.init()
                consoleHandler.reload()
                if (config.rconServer.enabled) {
                    rconListener = RconListener.create()
                }
            }.onFailure { t ->
                logger.error("Unexpected error occurred.", t)
                exitProcess(1)
            }
        }

        logger.info("Startup preparations finished in {}", duration.inWholeMilliseconds)

        if (args.contains("--no-server")) {
            Thread.sleep(1500)
            exit()
            exitProcess(0)
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
        if (ConfigManager.config.rconClient.enabled) {
            RconClient.close()
        }
    }

    @SubscribeEvent
    fun onServerStoppedEvent(e: ServerStoppedEvent) {
        logger.info("Server exited with return value {}", e.exitCode)
        serverThreadDaemon = null
        serverStatus = ServerStatus.STOPPED
        if (e.actionHost == this) {
            logger.info("Bye.")
            exit()
        }
    }

    @SubscribeEvent
    fun onStopServer(e: StopServerEvent) {
        if (serverThreadDaemon == null) {
            logger.warn("Server is not running!")
        } else {
            serverThreadDaemon!!.stopServer(host = e.actionHost, force = e.force)
            serverStatus = ServerStatus.STOPPING
        }
    }

    @SubscribeEvent
    fun onStartServer(e: StartServerEvent) {
        if (serverThreadDaemon != null) {
            logger.warn("Server already running!")
        }
        logger.info("Starting server using command {} in directory: {}", e.launchCommand, e.workingDir)

        serverThreadDaemon =
            ServerThreadDaemon(e.launchCommand, e.workingDir)
        serverThreadDaemon!!.start()
    }

    @SubscribeEvent
    fun onServerStarting(e: ServerStartingEvent) {
        serverStatus = ServerStatus.STARTING
        logger.info("Server is running at PID {}", e.pid)
    }

    @SubscribeEvent
    fun onPlayerJoin(e: PlayerJoinEvent) {
        if (e.player !in PermissionManager) {
            PermissionManager[e.player] = PermissionManager.defaultLevel
        }
    }

    @SubscribeEvent
    fun onPlayerInfo(e: PlayerChatEvent) {
        if (!e.content.startsWith(ConfigManager.config.commandPrefix)) return

        val commandSourceStack =
            CommandSourceStack(CommandSource.PLAYER, e.info.player, PermissionManager[e.info.player])
        try {
            CommandManager.execute(e.content, commandSourceStack)
        } catch (e: CommandSyntaxException) {
            commandSourceStack.sendFeedback(
                Component.text("Incomplete or invalid command${if (e.message != null) ", see errors below:" else ""}\n")
                    .color(NamedTextColor.RED)
            )
            if (e.message != null) {
                commandSourceStack.sendFeedback(Component.text(e.message!!).color(NamedTextColor.RED))
            }
        } catch (e: Exception) {
            logger.error("An exception was thrown while processing command.", e)
            commandSourceStack.sendFeedback(
                Component.text("Unexpected error occurred while executing command:\n").color(NamedTextColor.RED),
                Component.text(e.message!!).color(NamedTextColor.RED).hoverEvent(Component.text(e.stackTraceToString()))
            )
        }
    }

    fun input(command: String) {
        if (serverThreadDaemon != null) {
            serverThreadDaemon?.input(command)
        }
        logger.warn("There is no running server instance for command input to take action.")
    }

    @SubscribeEvent
    fun onServerStarted(e: ServerStartedEvent) {
        serverStatus = ServerStatus.RUNNING
    }

    @SubscribeEvent
    fun onRconServerStarted(e: RconServerStartedEvent) {
        if (config.rconClient.enabled) {
            logger.info("Attempt to init rcon connection.")
            RconClient.connect()
            logger.info("Rcon connected.")
        }
    }

//    TODO
//    @SubscribeEvent
//    fun onServerLogging(e: ServerLoggingEvent) {
//        if (serverThreadDaemon != null) {
//            serverThreadDaemon!!.input(it.content)
//        } else {
//            logger.warn("Server is NOT running!")
//        }
//    }

    fun run() {
        postEvent(StartServerEvent(config.launchCommand, Path(config.workingDirectory)))
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
