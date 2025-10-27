package icu.takeneko.omms.crystal.server.launcher

import icu.takeneko.omms.crystal.crystalspi.ICrystalServerInfoParser
import icu.takeneko.omms.crystal.crystalspi.ICrystalServerLauncher
import icu.takeneko.omms.crystal.event.server.ServerStartedEvent
import icu.takeneko.omms.crystal.event.server.ServerStartingEvent
import icu.takeneko.omms.crystal.event.server.ServerStoppedEvent
import icu.takeneko.omms.crystal.event.server.ServerStoppingEvent
import icu.takeneko.omms.crystal.foundation.ActionHost
import icu.takeneko.omms.crystal.CrystalServer
import icu.takeneko.omms.crystal.util.LoggerUtil
import java.nio.file.Path

class DryServerLauncher : ICrystalServerLauncher {

    private var isServerRunning = false
    private val logger = LoggerUtil.createLogger("DryServerLauncher")

    override fun launchServer(
        workingDir: Path,
        launchCommand: String,
        parser: ICrystalServerInfoParser
    ) {
        isServerRunning = true
        CrystalServer.postEvent(ServerStartingEvent(-1, "unknown"))
        CrystalServer.postEvent(ServerStartedEvent(0.0))
    }

    override fun input(line: String) {
        logger.info("Input: {}", line)
        if (line == "stop") {
            CrystalServer.postEvent(ServerStoppingEvent())
            CrystalServer.postEvent(ServerStoppedEvent(0, CrystalServer))
            isServerRunning = false
        }
    }

    override fun stopServer(actionHost: ActionHost, force: Boolean) {
        isServerRunning = false
        CrystalServer.postEvent(ServerStoppingEvent())
        CrystalServer.postEvent(ServerStoppedEvent(0, actionHost))
    }

    override fun destroy() {
    }

    override fun isServerRunning(): Boolean = isServerRunning

    override fun key(): String = "dry"
}