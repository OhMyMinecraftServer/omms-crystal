package icu.takeneko.omms.crystal.server

import icu.takeneko.omms.crystal.config.ConfigManager
import icu.takeneko.omms.crystal.crystalspi.ICrystalServerInfoParser
import icu.takeneko.omms.crystal.event.Event
import icu.takeneko.omms.crystal.event.server.*
import icu.takeneko.omms.crystal.CrystalServer
import icu.takeneko.omms.crystal.service.CrystalServiceManager
import icu.takeneko.omms.crystal.util.LoggerUtil.createLogger
import icu.takeneko.omms.crystal.util.LoggerUtil.createLoggerWithPattern
import org.slf4j.MarkerFactory
import org.slf4j.event.Level
import java.io.InputStream
import java.nio.charset.Charset
import java.util.concurrent.locks.LockSupport

class ServerOutputHandler(private val serverProcess: Process) : Thread("ServerOutputHandler") {
    private val serverLogger = createLoggerWithPattern(
        "[%cyan(%d{yyyy-MM-dd HH:mm:ss.SSS})] [%boldYellow(%marker)/%highlight(%level)]: %msg%n",
        "ServerLogger"
    )

    private val logger = createLogger("ServerOutputHandler")

    private lateinit var input: InputStream

    private val parser = CrystalServiceManager.load(ICrystalServerInfoParser::class.java)[ConfigManager.config.serverType]
        ?: error("Specified parser ${ConfigManager.config.serverType} does not exist.")

    private val strategies: List<(String) -> Event?> = listOf(
        { line -> parser.parseServerStartingInfo(line)?.let { ServerStartingEvent(serverProcess.pid(), it.version) } },
        { line -> parser.parseServerStartedInfo(line)?.let { ServerStartedEvent(it.timeElapsed) } },
        { line -> parser.parseServerOverloadInfo(line)?.let { ServerOverloadEvent(it.ticks, it.time) } },
        { line -> parser.parseServerStoppingInfo(line)?.let { ServerStoppingEvent() } },
        { line -> parser.parsePlayerJoinInfo(line)?.let { PlayerJoinEvent(it.player) } },
        { line -> parser.parseRconStartInfo(line)?.let { RconServerStartedEvent(it.port) } },
        { line -> parser.parsePlayerInfo(line)?.let { PlayerChatEvent(it) } },
        { line -> parser.parsePlayerLeftInfo(line)?.let { PlayerLeftEvent(it.player) } }
    )

    override fun run() {
        try {
            input = serverProcess.inputStream
            val reader = input.bufferedReader(Charset.forName(ConfigManager.config.encoding))
            while (serverProcess.isAlive) {
                try {
                    LockSupport.parkNanos(10)
                    val string = reader.readLine()
                    if (string != null) {
                        val info = parser.parseToBareInfo(string)
                        if (info == null) {
                            println(string)
                        } else {
                            // dispatch a global info first
                            CrystalServer.postEvent(ServerLoggingEvent(info))
                            // and then started to parse
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
