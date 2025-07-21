package icu.takeneko.omms.crystal.server


import icu.takeneko.omms.crystal.config.Config
import icu.takeneko.omms.crystal.event.*
import icu.takeneko.omms.crystal.event.server.PlayerChatEvent
import icu.takeneko.omms.crystal.event.server.PlayerJoinEvent
import icu.takeneko.omms.crystal.event.server.PlayerLeftEvent
import icu.takeneko.omms.crystal.event.server.RconServerStartedEvent
import icu.takeneko.omms.crystal.event.server.ServerLoggingEvent
import icu.takeneko.omms.crystal.event.server.ServerOverloadEvent
import icu.takeneko.omms.crystal.event.server.ServerStartedEvent
import icu.takeneko.omms.crystal.event.server.ServerStartingEvent
import icu.takeneko.omms.crystal.event.server.ServerStoppedEvent
import icu.takeneko.omms.crystal.event.server.ServerStoppingEvent
import icu.takeneko.omms.crystal.foundation.ActionHost
import icu.takeneko.omms.crystal.main.CrystalServer
import icu.takeneko.omms.crystal.parser.ParserManager
import icu.takeneko.omms.crystal.util.createLogger
import icu.takeneko.omms.crystal.util.createServerLogger
import icu.takeneko.omms.crystal.util.resolveCommand
import org.slf4j.MarkerFactory
import org.slf4j.event.Level
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.ConcurrentModificationException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.LockSupport

var serverStatus = ServerStatus.STOPPED

enum class ServerStatus {
    STOPPED, RUNNING, STOPPING, STARTING
}

class ServerThreadDaemon(
    private val launchCommand: String,
    private val workingDir: Path,
) : Thread("ServerThreadDaemon") {

    private val logger = createLogger("ServerThreadDaemon")
    private lateinit var out: OutputStream
    private lateinit var input: InputStream
    private val queue = ArrayBlockingQueue<String>(1024)
    private var actionHost: ActionHost = CrystalServer
    private var process: Process? = null
    lateinit var outputHandler: ServerOutputHandler


    override fun run() {
        try {
            process = Runtime.getRuntime().exec(resolveCommand(launchCommand), null, workingDir.toFile())
            out = process!!.outputStream
            input = process!!.inputStream
        } catch (e: Exception) {
            logger.error("Cannot start server.", e)
            CrystalServer.postEvent(ServerStoppedEvent(Int.MIN_VALUE, actionHost))
            return
        }
        outputHandler = ServerOutputHandler(process!!)
        outputHandler.start()
        val writer = out.writer(Charset.defaultCharset())
        while (process!!.isAlive) {
            try {
                if (queue.isNotEmpty()) {
                    synchronized(queue) {
                        while (queue.isNotEmpty()) {
                            val line = queue.poll()
                            ifServerDebug { logger.info("[DEBUG] Handling input $line") }
                            writer.appendLine(line)
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

        CrystalServer.destroyDaemon()
        CrystalServer.postEvent(ServerStoppedEvent(exitCode, actionHost))
    }

    fun input(str: String) {
        synchronized(queue) {
            queue.add(str)
        }
    }

    fun stopServer(force: Boolean = false, host: ActionHost) {
        this.actionHost = host
        if (force) {
            process!!.destroyForcibly()
        } else {
            input("stop")
        }
    }
}

class ServerOutputHandler(private val serverProcess: Process) : Thread("ServerOutputHandler") {
    private val serverLogger = createServerLogger()
    private val logger = createLogger("ServerOutputHandler")
    private lateinit var input: InputStream
    private val parser = ParserManager.getParser(Config.config.serverType)
        ?: error("Specified parser ${Config.config.serverType} does not exist.")

    private val strategies: List<(String) -> Event?> = listOf(
        { line -> parser.parseServerStartingInfo(line)?.let { ServerStartingEvent(serverProcess.pid(), it.version) } },
        { line -> parser.parseServerStartedInfo(line)?.let { ServerStartedEvent(it.timeElapsed) } },
        { line -> parser.parseServerOverloadInfo(line)?.let { ServerOverloadEvent(it.ticks, it.time) } },
        { line -> parser.parseServerStoppingInfo(line)?.let { ServerStoppingEvent() } },
        { line -> parser.parsePlayerJoinInfo(line)?.let { PlayerJoinEvent(it.player) } },
        { line -> parser.parseRconStartInfo(line)?.let { RconServerStartedEvent(it.port) } },
        { line -> parser.parsePlayerInfo(line)?.let { PlayerChatEvent(it.content, it) } },
        { line -> parser.parsePlayerLeftInfo(line)?.let { PlayerLeftEvent(it.player) } }
    )

    override fun run() {
        try {
            input = serverProcess.inputStream
            val reader = input.bufferedReader(Charset.forName(Config.config.encoding))
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
                            CrystalServer.postEvent(ServerLoggingEvent(info))
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
        } catch (_: InterruptedException) {
        }
    }

    private fun parseAndDispatch(info: String) {
        strategies.firstNotNullOfOrNull { it(info) }?.let(CrystalServer::postEvent)
    }
}