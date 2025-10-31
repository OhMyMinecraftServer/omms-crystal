package icu.takeneko.crystal.connector.mcdreforged

import icu.takeneko.crystal.connector.dispacher.PythonDispatcher
import org.slf4j.MarkerFactory
import java.nio.file.Path
import java.util.concurrent.locks.LockSupport
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.notExists

class MCDReforged(val workingDir: Path): PythonDispatcher() {
    private val logger = MCDReforgedBridge.logger

    override fun pyEntrypoint() {
        interpreter.invoke("run_patches")
        val configPath = workingDir / "config.yml"
        val permissionPath = workingDir / "permission.yml"
        if (configPath.notExists()) {
            logger.info(MarkerFactory.getMarker("PythonDispatcher"), "Initializing environment for MCDReforged")
            interpreter.invoke("prepare_environment", configPath.toString(), permissionPath.toString())
        }
        interpreter.invoke("launch_mcdr", configPath.toString(), permissionPath.toString())
    }
}

fun main() {
    val mcdr = MCDReforged(Path("."))
    mcdr.start()
    while (mcdr.isAlive) {
        LockSupport.parkNanos(10000)
    }
}