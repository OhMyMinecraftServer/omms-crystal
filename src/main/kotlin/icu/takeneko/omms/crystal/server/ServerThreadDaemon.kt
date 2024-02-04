package icu.takeneko.omms.crystal.server


import icu.takeneko.omms.crystal.config.Config
import icu.takeneko.omms.crystal.event.*
import icu.takeneko.omms.crystal.main.DebugOptions
import icu.takeneko.omms.crystal.main.SharedConstants
import icu.takeneko.omms.crystal.main.SharedConstants.serverThreadDaemon
import icu.takeneko.omms.crystal.parser.ParserManager
import icu.takeneko.omms.crystal.util.createLogger
import icu.takeneko.omms.crystal.util.createServerLogger
import icu.takeneko.omms.crystal.util.resolveCommand
import org.slf4j.MarkerFactory
import org.slf4j.event.Level
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.ConcurrentModificationException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.LockSupport

var serverStatus = ServerStatus.STOPPED

enum class LaunchParameter

enum class ServerStatus {
    STOPPED, RUNNING, STOPPING, STARTING
}

class ServerThreadDaemon(
    private val launchCommand: String,
    private val workingDir: String,
    vararg launchParameters: LaunchParameter?
) :
    Thread("ServerThreadDaemon") {

    private val launchParameters: Array<out LaunchParameter?>
    private val logger = createLogger("ServerThreadDaemon")
    private lateinit var out: OutputStream
    private lateinit var input: InputStream
    private val queue = ArrayBlockingQueue<String>(1024)
    private var who = "crystal"
    private var process: Process? = null
    lateinit var outputHandler: ServerOutputHandler

    init {
        this.launchParameters = launchParameters
    }


    override fun run() {
        try {
            process = Runtime.getRuntime().exec(resolveCommand(launchCommand), null, File(workingDir))
            out = process!!.outputStream
            input = process!!.inputStream
        } catch (e: Exception) {
            logger.error("Cannot start server.", e)
            SharedConstants.eventLoop.dispatch(ServerStoppedEvent, ServerStoppedEventArgs(Integer.MIN_VALUE, who))
            return
        }
        outputHandler = ServerOutputHandler(process!!, *launchParameters)
        outputHandler.start()
        val writer = out.writer(Charset.defaultCharset())
        while (process!!.isAlive) {
            try {
                if (queue.isNotEmpty()) {
                    synchronized(queue) {
                        while (queue.isNotEmpty()) {
                            val line = queue.poll()
                            if (DebugOptions.serverDebug()) logger.info("[DEBUG] Handling input $line")
                            writer.write(line + "\n")
                            writer.flush()
                        }
                    }
                }
                sleep(10)
            } catch (e: ConcurrentModificationException) {
                e.printStackTrace()
            }
        }
        val exitCode = process!!.exitValue()
        //logger.info("Server exited with exit code $exitCode.")
        serverThreadDaemon = null
        SharedConstants.eventLoop.dispatch(ServerStoppedEvent, ServerStoppedEventArgs(exitCode, who))
    }

    fun input(str: String) {
        synchronized(queue) {
            queue.add(str)
        }
    }

    fun stopServer(force: Boolean = false, who: String = "crystal") {
        this.who = who
        if (force) {
            process!!.destroyForcibly()
        } else {
            input("stop")
        }
    }
}


class ServerOutputHandler(private val serverProcess: Process, vararg launchParameters: LaunchParameter?) :
    Thread("ServerOutputHandler") {
    private val launchParameters: Array<out LaunchParameter?>
    private val serverLogger = createServerLogger()
    private val logger = createLogger("ServerOutputHandler")
    private lateinit var input: InputStream
    private val parser = ParserManager.getParser(Config.parserName)
        ?: throw IllegalArgumentException("Specified parser ${Config.parserName} does not exist.")

    init {
        this.launchParameters = launchParameters
    }

    override fun run() {
        try {
            input = serverProcess.inputStream
            val reader = input.bufferedReader(Charset.forName(Config.encoding))
            while (serverProcess.isAlive) {
                try {
                    LockSupport.parkNanos(10)
                    val string = reader.readLine()
                    if (string != null) {
                        val info = parser.parseToBareInfo(string)
                        if (info == null) {
                            println(string)
                        } else {
                            //dispatch a global info first
                            SharedConstants.eventLoop.dispatch(ServerInfoEvent, ServerInfoEventArgs(info))
                            //and then started to parse
                            parseAndDispatch(info.info)
                            when (info.level) {
                                Level.DEBUG -> serverLogger.debug(MarkerFactory.getMarker(info.thread), info.info)
                                Level.ERROR -> serverLogger.error(MarkerFactory.getMarker(info.thread), info.info)
                                Level.INFO -> serverLogger.info(MarkerFactory.getMarker(info.thread), info.info)
                                Level.TRACE -> serverLogger.trace(MarkerFactory.getMarker(info.thread), info.info)
                                Level.WARN -> serverLogger.warn(MarkerFactory.getMarker(info.thread), info.info)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (e !is InterruptedException) {
                        logger.error("Error occurred while reading server output.", e)
                    }
                }
            }
        } catch (ignored: InterruptedException) {
            //logger.detachAndStopAllAppenders()
        }
    }

    private fun parseAndDispatch(processedInfo: String) {
        val serverStartingInfo = parser.parseServerStartingInfo(processedInfo)
        if (serverStartingInfo != null) {
            dispatchEvent(ServerStartingEvent, ServerStartingEventArgs(serverProcess.pid(), serverStartingInfo.version))
            return
        }
        val serverStartedInfo = parser.parseServerStartedInfo(processedInfo)
        if (serverStartedInfo != null) {
            dispatchEvent(ServerStartedEvent, ServerStartedEventArgs(timeUsed = serverStartedInfo.timeElapsed))
            return
        }
        val serverOverloadInfo = parser.parseServerOverloadInfo(processedInfo)
        if (serverOverloadInfo != null) {
            dispatchEvent(
                ServerOverloadEvent,
                ServerOverloadEventArgs(serverOverloadInfo.ticks, serverOverloadInfo.time)
            )
            return
        }
        val serverStoppingInfo = parser.parseServerStoppingInfo(processedInfo)
        if (serverStoppingInfo != null) {
            dispatchEvent(ServerStoppingEvent, ServerStoppingEventArgs())
            return
        }
        val playerJoinInfo = parser.parsePlayerJoinInfo(processedInfo)
        if (playerJoinInfo != null) {
            dispatchEvent(PlayerJoinEvent, PlayerJoinEventArgs(player = playerJoinInfo.player))
            return
        }
        val rconInfo = parser.parseRconStartInfo(processedInfo)
        if (rconInfo != null) {
            dispatchEvent(RconStartedEvent, RconStartedEventArgs(rconInfo.port))
            return
        }
        val playerInfo = parser.parsePlayerInfo(processedInfo)
        if (playerInfo != null) {
            dispatchEvent(
                PlayerInfoEvent,
                PlayerInfoEventArgs(content = playerInfo.content, player = playerInfo.player)
            )
            return
        }
        val playerLeftInfo = parser.parsePlayerLeftInfo(processedInfo)
        if (playerLeftInfo != null) {
            dispatchEvent(PlayerLeftEvent, PlayerLeftEventArgs(player = playerLeftInfo.player))
            return
        }
        return
    }

    private fun dispatchEvent(e: Event, args: EventArgs) {
        SharedConstants.eventLoop.dispatch(e, args)
    }
}