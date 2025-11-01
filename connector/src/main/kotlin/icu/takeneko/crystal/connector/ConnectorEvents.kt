package icu.takeneko.crystal.connector

import icu.takeneko.crystal.connector.mcdreforged.MCDReforged
import icu.takeneko.crystal.connector.mcdreforged.MCDReforgedBridge
import icu.takeneko.omms.crystal.event.EventBusSubscriber
import icu.takeneko.omms.crystal.event.SubscribeEvent
import icu.takeneko.omms.crystal.event.server.CrystalExitingEvent
import icu.takeneko.omms.crystal.event.server.CrystalSetupEvent
import icu.takeneko.omms.crystal.event.server.ServerRawLoggingEvent
import icu.takeneko.omms.crystal.event.server.ServerStartingEvent
import icu.takeneko.omms.crystal.event.server.StartServerEvent
import icu.takeneko.omms.crystal.main.MainThreadExecutor
import icu.takeneko.omms.crystal.util.LoggerUtil
import java.util.concurrent.locks.LockSupport
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.notExists

@EventBusSubscriber
object ConnectorEvents {
    private val logger = LoggerUtil.createLogger("ConnectorPlugin")

    @SubscribeEvent
    @JvmStatic
    fun on(e: CrystalExitingEvent) {
        MCDReforgedBridge.stopMCDR()
    }

    @SubscribeEvent
    @JvmStatic
    fun on(e: ServerStartingEvent) {
        MCDReforgedBridge.firstTimeServerStartup = false
        MCDReforgedBridge.pid = e.pid
    }

    @SubscribeEvent
    @JvmStatic
    fun on(e: StartServerEvent) {

    }

    @SubscribeEvent
    @JvmStatic
    fun on(e: ServerRawLoggingEvent) {
        MCDReforgedBridge.addLog(e.content)
    }

    @SubscribeEvent
    @JvmStatic
    fun on(e: CrystalSetupEvent) {
        MCDReforgedBridge.mcdrInstance = MCDReforged(
            Path("./mcdreforged")
                .apply { if (notExists()) createDirectory() })
            .also(MCDReforged::start)
        logger.info("Waiting for MCDReforged launch")
        while (!MCDReforgedBridge.mcdrInstance!!.isMCDRRunning) {
            LockSupport.parkNanos(1000)
        }
        logger.info("Bootstrapped MCDReforged")
    }
}