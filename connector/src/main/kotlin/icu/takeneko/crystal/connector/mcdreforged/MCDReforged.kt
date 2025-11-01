package icu.takeneko.crystal.connector.mcdreforged

import icu.takeneko.omms.crystal.util.CrystalTask
import icu.takeneko.crystal.connector.dispacher.PythonDispatcher
import org.slf4j.MarkerFactory
import java.nio.file.Path
import java.util.concurrent.locks.LockSupport
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.notExists

class MCDReforged(val workingDir: Path) : PythonDispatcher() {
    private val logger = MCDReforgedBridge.logger
    internal var ready = false
        private set
    var isMCDRRunning: Boolean = false
        internal set

    override fun pyEntrypoint() {
        logger.info(MarkerFactory.getMarker("Connector"), "Bootstrapping MCDReforged")
        interpreter.invoke("run_patches")
        val configPath = workingDir / "config.yml"
        val permissionPath = workingDir / "permission.yml"
        if (configPath.notExists()) {
            logger.info(MarkerFactory.getMarker("PythonDispatcher"), "Initializing environment for MCDReforged")
            interpreter.invoke("prepare_environment", configPath.toString(), permissionPath.toString())
        }
        ready = true
        interpreter.invoke("launch_mcdr", configPath.toString(), permissionPath.toString())
    }

    fun callTerminate() {
        addTask {
            interpreter.invoke("stop_mcdr")
        }
    }

    fun <R> addTask(fn: () -> R): CrystalTask<R> {
        return CrystalTask(fn).apply(taskDeque::add)
    }

    fun waitForReady() {
        while (!ready) {
            LockSupport.parkNanos(1000)
        }
    }
}

fun main() {
    val mcdr = MCDReforged(Path("."))
    mcdr.start()
    while (mcdr.isAlive) {
        LockSupport.parkNanos(10000)
    }
}