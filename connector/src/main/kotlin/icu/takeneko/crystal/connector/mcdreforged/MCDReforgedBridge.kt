package icu.takeneko.crystal.connector.mcdreforged

import icu.takeneko.omms.crystal.CrystalServer
import icu.takeneko.omms.crystal.event.server.StartServerEvent
import icu.takeneko.omms.crystal.event.server.StopServerEvent
import icu.takeneko.omms.crystal.server.ServerStatus
import icu.takeneko.omms.crystal.util.BuildProperties
import icu.takeneko.omms.crystal.util.LoggerUtil
import kotlinx.coroutines.runBlocking
import org.slf4j.MarkerFactory
import java.util.concurrent.locks.LockSupport
import kotlin.io.path.Path

object MCDReforgedBridge {
    internal var mcdrInstance: MCDReforged? = null
    internal val logger = LoggerUtil.createLoggerWithPattern(
        "[%cyan(%d{yyyy-MM-dd HH:mm:ss.SSS})] [%boldYellow(MCDReforged)/%highlight(%level)]: %green(\\(%marker\\)) %msg%n",
        "MCDReforged"
    )
    internal var pid: Long? = null
    private val logLines = mutableListOf<String>()
    internal var firstTimeServerStartup = true

    fun log(name: String, level: Int, message: String, traceback: String?) {
        val realMessage = if (traceback != null) message + "\n" + traceback else message
        when (level) {
            0 -> logger.trace(MarkerFactory.getMarker(name), realMessage)
            10 -> logger.debug(MarkerFactory.getMarker(name), realMessage)
            20 -> logger.info(MarkerFactory.getMarker(name), realMessage)
            30 -> logger.warn(MarkerFactory.getMarker(name), realMessage)
            40 -> logger.error(MarkerFactory.getMarker(name), realMessage)
        }
    }

    fun pythonBridgePollEvents() {
        mcdrInstance?.runTasks()
    }

    fun stopMCDR() {
        mcdrInstance?.callTerminate()
    }

    fun setMCDRRunning(state: Boolean) {
        this.mcdrInstance?.isMCDRRunning = state
    }

    fun pollServerStdout(): String? {
        if (CrystalServer.serverStatus == ServerStatus.STOPPED) {
            return null
        }
        return synchronized(logLines) {
            while (logLines.isEmpty()) {
                if (CrystalServer.serverStatus == ServerStatus.STOPPED) {
                    return null
                }
                mcdrInstance?.runTasks()
                LockSupport.parkNanos(10000)
            }
            val strings = logLines.toList()
            logLines.clear()
            strings.joinToString("\n")
        }
    }

    fun getCrystalVersion(): String? {
        return BuildProperties["version"]
    }

    fun dispatchStopServer(force: Boolean): Boolean {
        return runBlocking {
            if (CrystalServer.serverStatus != ServerStatus.RUNNING) {
                return@runBlocking false
            }
            val result = CrystalServer.postEventWithReturn(StopServerEvent())
            return@runBlocking !result.isCancelled
        }
    }

    fun getServerPid() = pid

    fun dispatchStartServer(): Boolean {
        if (firstTimeServerStartup) {
            logger.info(
                MarkerFactory.getMarker("Connector"),
                "Blocking MCDReforged until crystal has dispatched server launch."
            )
            while (firstTimeServerStartup) {
                LockSupport.parkNanos(10000)
            }
            return true
        }
        return runBlocking {
            logger.info(MarkerFactory.getMarker("Connector"), "MCDReforged is dispatching server launch.")
            if (CrystalServer.serverStatus != ServerStatus.STOPPED) {
                return@runBlocking false
            }
            val result = CrystalServer.postEventWithReturn(
                StartServerEvent(
                    CrystalServer.config.launchCommand,
                    Path(CrystalServer.config.workingDirectory)
                )
            )
            return@runBlocking !result.isCancelled
        }
    }

    fun addLog(content: String) {
        synchronized(logLines) {
            logLines += content
        }
    }
}