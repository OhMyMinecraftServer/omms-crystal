package icu.takeneko.omms.crystal

import com.mojang.brigadier.exceptions.CommandSyntaxException
import icu.takeneko.omms.crystal.command.BuiltinCommand
import icu.takeneko.omms.crystal.command.CommandHelpManager
import icu.takeneko.omms.crystal.command.CommandManager
import icu.takeneko.omms.crystal.config.ConfigManager
import icu.takeneko.omms.crystal.console.ConsoleHandler
import icu.takeneko.omms.crystal.crystalspi.ICrystalServerInfoParser
import icu.takeneko.omms.crystal.crystalspi.ICrystalServerLauncher
import icu.takeneko.omms.crystal.event.Event
import icu.takeneko.omms.crystal.event.EventBus
import icu.takeneko.omms.crystal.event.EventPriority
import icu.takeneko.omms.crystal.event.PluginBusEvent
import icu.takeneko.omms.crystal.event.SubscribeEvent
import icu.takeneko.omms.crystal.event.server.CrystalSetupEvent
import icu.takeneko.omms.crystal.event.server.PlayerChatEvent
import icu.takeneko.omms.crystal.event.server.PlayerJoinEvent
import icu.takeneko.omms.crystal.event.server.RconServerStartedEvent
import icu.takeneko.omms.crystal.event.server.RegisterCommandEvent
import icu.takeneko.omms.crystal.event.server.ServerStartedEvent
import icu.takeneko.omms.crystal.event.server.ServerStartingEvent
import icu.takeneko.omms.crystal.event.server.ServerStoppedEvent
import icu.takeneko.omms.crystal.event.server.ServerStoppingEvent
import icu.takeneko.omms.crystal.event.server.StartServerEvent
import icu.takeneko.omms.crystal.event.server.StopServerEvent
import icu.takeneko.omms.crystal.foundation.ActionHost
import icu.takeneko.omms.crystal.i18n.TranslateManager
import icu.takeneko.omms.crystal.permission.PermissionManager
import icu.takeneko.omms.crystal.plugin.PluginManager
import icu.takeneko.omms.crystal.rcon.RconClient
import icu.takeneko.omms.crystal.rcon.RconListener
import icu.takeneko.omms.crystal.server.ServerStatus
import icu.takeneko.omms.crystal.service.CrystalServiceManager
import icu.takeneko.omms.crystal.util.BuildProperties
import icu.takeneko.omms.crystal.util.LoggerUtil
import icu.takeneko.omms.crystal.util.PRODUCT_NAME
import icu.takeneko.omms.crystal.util.command.CommandSource
import icu.takeneko.omms.crystal.util.command.CommandSourceStack
import icu.takeneko.omms.crystal.util.constants.DebugOptions
import icu.takeneko.omms.crystal.util.file.FileUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.lang.management.ManagementFactory
import java.lang.reflect.InvocationTargetException
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

    lateinit var eventBus: EventBus
        private set

    val consoleHandler = ConsoleHandler()

    private var logger = LoggerUtil.createLogger("CrystalServer", false)

    val config
        get() = ConfigManager.config

    val classLoader: ClassLoader = Thread.currentThread().contextClassLoader

    lateinit var serverLauncher: ICrystalServerLauncher
        private set

    var rconListener: RconListener? = null
        private set

    var serverStatus = ServerStatus.STOPPED
        private set

    private val _availableParsers: MutableMap<String, ICrystalServerInfoParser> = mutableMapOf()

    val availableParsers: Map<String, ICrystalServerInfoParser>
        get() = _availableParsers

    var shouldKeepRunning = true

    fun bootstrap(args: Array<String>) {

        val duration = measureTime {
            Runtime.getRuntime().addShutdownHook(
                thread(name = "ShutdownThread", start = false) {
                    if (this::serverLauncher.isInitialized && serverLauncher.isServerRunning()) {
                        println("Stopping server because jvm is shutting down.")
                        serverLauncher.terminate(this)
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
                    val serverPath = Path(FileUtil.joinFilePaths("server"))
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
                eventBus = EventBus(this, Event::class.java)
                eventBus.register(this)
                logger = LoggerUtil.createLogger("CrystalServer", DebugOptions.mainDebug())
                TranslateManager.useLanguage(config.lang)
                TranslateManager.init()
                CommandHelpManager.init()
                PluginManager.bootstrap()
                PluginManager.loadAll()
                PermissionManager.init()
                CommandManager.init()
                consoleHandler.reload()
                if (config.rconServer.enabled) {
                    rconListener = RconListener.create()
                }
                bootstrapServices()
            }.onFailure { t ->
                logger.error("Unexpected error occurred.", t)
                exitProcess(1)
            }
        }
        this.postEvent(CrystalSetupEvent())
        logger.info("Startup preparations finished in {} milliseconds", duration.inWholeMilliseconds)
    }

    private fun bootstrapServices() {
        val launchers = CrystalServiceManager.load(ICrystalServerLauncher::class.java)
        this.serverLauncher = launchers[config.launcher]
            ?: throw IllegalArgumentException(
                "Crystal could not find the ICrystalServerLauncher(${config.launcher}) specified."
            )
        this._availableParsers += CrystalServiceManager.load(ICrystalServerInfoParser::class.java)
        logger.info("All available launchers: {}", launchers)
        logger.info("All available parsers: {}", this._availableParsers)
    }

    @SubscribeEvent
    fun onRegisterCommands(e: RegisterCommandEvent) {
        e.register(BuiltinCommand.helpCommand)
        e.register(BuiltinCommand.permissionCommand)
        e.register(BuiltinCommand.startCommand)
        e.register(BuiltinCommand.stopCommand)
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
        serverStatus = ServerStatus.STOPPED
        if (e.actionHost == this) {
            logger.info("Bye.")
            exit()
        }
    }

    @SubscribeEvent
    fun onStopServer(e: StopServerEvent) {
        if (!serverLauncher.isServerRunning) {
            logger.warn("Server is not running!")
        } else {
            serverLauncher.stopServer(e.actionHost, e.force)
            serverStatus = ServerStatus.STOPPING
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onStartServer(e: StartServerEvent) {
        if (serverLauncher.isServerRunning) {
            logger.warn("Server already running!")
        }
        logger.info("Starting server using command {} in directory: {}", e.launchCommand, e.workingDir)
        val parser = _availableParsers[config.serverType]
        if (parser == null){
            throw IllegalArgumentException("Crystal could not find the parser specified: ${config.serverType}")
        }
        serverLauncher.launchServer(e.workingDir, e.launchCommand, parser)
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
        if (serverLauncher.isServerRunning) {
            serverLauncher.input(command)
            return
        }
        logger.warn("There is no running server instance for command input to take action.")
    }

    @SubscribeEvent
    fun onServerStarted(e: ServerStartedEvent) {
        logger.debug("Server state changed to RUNNING")
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

    fun run() {
        postEvent(StartServerEvent(config.launchCommand, Path(config.workingDirectory)))
    }

    inline fun <reified T : Event> postEvent(e: T) {
        if (e is PluginBusEvent) {
            PluginManager.postEvent(e)
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

    fun exit() {
        PermissionManager.save()
        consoleHandler.interrupt()
        shouldKeepRunning = false
    }

    override val coroutineContext: CoroutineContext
        get() = coroutineDispatcher

    inline fun ifMainDebug(block: () -> Unit) {
        if (DebugOptions.mainDebug()) block()
    }

}