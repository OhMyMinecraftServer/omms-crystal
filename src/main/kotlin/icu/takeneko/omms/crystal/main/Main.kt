package icu.takeneko.omms.crystal.main

import com.mojang.brigadier.exceptions.CommandSyntaxException
import icu.takeneko.omms.crystal.command.*
import icu.takeneko.omms.crystal.config.Config
import icu.takeneko.omms.crystal.config.Config.config
import icu.takeneko.omms.crystal.console.ConsoleHandler
import icu.takeneko.omms.crystal.event.*
import icu.takeneko.omms.crystal.i18n.TranslateManager
import icu.takeneko.omms.crystal.main.SharedConstants.consoleHandler
import icu.takeneko.omms.crystal.main.SharedConstants.eventDispatcher
import icu.takeneko.omms.crystal.main.SharedConstants.eventLoop
import icu.takeneko.omms.crystal.main.SharedConstants.serverThreadDaemon
import icu.takeneko.omms.crystal.permission.PermissionManager
import icu.takeneko.omms.crystal.plugin.PluginManager
import icu.takeneko.omms.crystal.rcon.RconClient
import icu.takeneko.omms.crystal.rcon.RconListener
import icu.takeneko.omms.crystal.server.ServerStatus
import icu.takeneko.omms.crystal.server.ServerThreadDaemon
import icu.takeneko.omms.crystal.server.serverStatus
import icu.takeneko.omms.crystal.text.Color
import icu.takeneko.omms.crystal.text.Text
import icu.takeneko.omms.crystal.text.TextGroup
import icu.takeneko.omms.crystal.util.BuildProperties
import icu.takeneko.omms.crystal.util.PRODUCT_NAME
import icu.takeneko.omms.crystal.util.createLogger
import icu.takeneko.omms.crystal.util.joinFilePaths
import java.lang.management.ManagementFactory
import java.nio.file.Files
import kotlin.concurrent.thread
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.system.exitProcess


fun exit() {
    thread(start = true, name = "ShutdownThread") {
        //PluginManager.unloadAll()
        rconListener?.stop()
        PermissionManager.writePermission()
        eventLoop.exit()
        eventDispatcher.shutdown()
        consoleHandler.interrupt()
    }
}

var rconListener: RconListener? = null


fun init() {
    val logger = createLogger("EventHandler")
    CommandManager.register(
        helpCommand,
        permissionCommand,
        startCommand,
        startCommand,
        stopCommand,
        pluginCommand
    )

    eventDispatcher.run {
        registerHandler(ServerStoppingEvent) {
            if (Config.config.enableRcon) {
                RconClient.close()
            }
        }
        registerHandler(ServerStoppedEvent) {
            it as ServerStoppedEventArgs
            logger.info("Server exited with return value ${it.retValue}")
            serverThreadDaemon = null
            serverStatus = ServerStatus.STOPPED
            if (it.who == "crystal") {
                logger.info("Bye.")
                exit()
            }
        }
        registerHandler(ServerStopEvent) {
            it as ServerStopEventArgs
            if (serverThreadDaemon == null) {
                logger.warn("Server is not running!")
            } else {
                serverThreadDaemon!!.stopServer(who = it.id, force = it.force)
                serverStatus = ServerStatus.STOPPING
            }
        }
        registerHandler(ServerStartingEvent) {
            it as ServerStartingEventArgs
            SharedConstants.serverVersion = it.version
        }
        registerHandler(ServerStartEvent) {
            val args = (it as ServerStartEventArgs)
            if (serverThreadDaemon != null) {
                logger.warn("Server already running!")
            }
            logger.info("Starting server using command ${args.startupCmd} at dir: ${args.workingDir}")

            serverThreadDaemon =
                ServerThreadDaemon(args.startupCmd, args.workingDir)
            serverThreadDaemon!!.start()
        }
        registerHandler(ServerStartingEvent) {
            it as ServerStartingEventArgs
            serverStatus = ServerStatus.STARTING
            logger.info("Server is running at pid ${it.pid}")
        }
        registerHandler(PlayerJoinEvent) {
            it as PlayerJoinEventArgs
            if (it.player !in PermissionManager) {
                PermissionManager[it.player] = PermissionManager.defaultPermissionLevel
            }
        }
        registerHandler(RconStartedEvent) {
            it as RconStartedEventArgs
            if (Config.config.enableRcon) {
                logger.info("Attempt to init rcon connection.")
                RconClient.connect()
                logger.info("Rcon connected.")
            }
        }
        registerHandler(ServerStartedEvent) {
            it as ServerStartedEventArgs
            serverStatus = ServerStatus.RUNNING
        }
        registerHandler(PlayerInfoEvent) {
            it as PlayerInfoEventArgs
            if (it.content.startsWith(Config.config.commandPrefix)) {
                val commandSourceStack =
                    CommandSourceStack(CommandSource.PLAYER, it.player, PermissionManager.getPermission(it.player))
                try {
                    CommandManager.execute(it.content, commandSourceStack)
                } catch (e: CommandSyntaxException) {
                    commandSourceStack.sendFeedback(
                        Text("Incomplete or invalid command${if (e.message != null) ", see errors below:" else ""}\n").withColor(
                            Color.RED
                        )
                    )
                    if (e.message != null) {
                        commandSourceStack.sendFeedback(Text(e.message!!).withColor(Color.RED))
                    }
                } catch (e: Exception) {
                    logger.error("An exception was thrown while processing command.", e)
                    commandSourceStack.sendFeedback(
                        TextGroup(
                            Text("Unexpected error occurred while executing command:\n").withColor(Color.RED),
                            Text(e.message!!).withColor(Color.RED)
                        )
                    )
                }
            }
        }
        registerHandler(ServerConsoleInputEvent) {
            it as ServerConsoleInputEventArgs
            if (serverThreadDaemon != null) {
                serverThreadDaemon!!.input(it.content)
            } else {
                logger.warn("Server is NOT running!")
            }
        }
    }
}

fun main(args: Array<String>) {
    println("Starting icu.takeneko.omms.crystal.main.MainKt.main()")
    Runtime.getRuntime().addShutdownHook(
        thread(name = "ShutdownThread", start = false) {
            if (serverThreadDaemon != null) {
                println("Stopping server because jvm is shutting down.")
                serverThreadDaemon!!.outputHandler.interrupt()
                serverThreadDaemon!!.stopServer(true)
            }
        }
    )
    consoleHandler = ConsoleHandler()
    consoleHandler.start()
    //registerEvents()
    val logger = createLogger("Main")
    logger.info("Hello World!")
    val os = ManagementFactory.getOperatingSystemMXBean()
    val runtime = ManagementFactory.getRuntimeMXBean()
    logger.info("$PRODUCT_NAME ${BuildProperties["version"]} is running on ${os.name} ${os.arch} ${os.version} at PID ${runtime.pid}")
    try {
        if (Config.load()) {
            val serverPath = Path(joinFilePaths("server"))
            if (!serverPath.exists() || !serverPath.isDirectory()) {
                Files.createDirectory(serverPath))
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
        SharedConstants.language = config.lang
        TranslateManager.init()
        CommandHelpManager.init()
        eventDispatcher = EventDispatcher()
        eventLoop = EventLoop()
        eventLoop.start()
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
        eventLoop.dispatch(
            ServerStartEvent,
            ServerStartEventArgs(Config.config.launchCommand, Config.config.workingDir)
        )
    } catch (e: Exception) {
        logger.error("Unexpected error occurred.", e)
        exitProcess(1)
    }
}
