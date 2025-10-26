package icu.takeneko.omms.crystal.server.launcher

import icu.takeneko.omms.crystal.crystalspi.ICrystalServerInfoParser
import icu.takeneko.omms.crystal.crystalspi.ICrystalServerLauncher
import icu.takeneko.omms.crystal.foundation.ActionHost
import java.nio.file.Path

class ProcessServerLauncher : ICrystalServerLauncher {
    override fun launchServer(
        workingDir: Path,
        launchCommand: String,
        parser: ICrystalServerInfoParser
    ) {

    }

    override fun input(line: String) {

    }

    override fun stopServer(actionHost: ActionHost, force: Boolean) {
    }

    override fun destroy() {
    }

    override fun isServerRunning(): Boolean {
        return false
    }

    override fun key(): String = "process"
}