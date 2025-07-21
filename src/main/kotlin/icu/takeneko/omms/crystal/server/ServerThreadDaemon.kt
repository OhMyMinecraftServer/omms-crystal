package icu.takeneko.omms.crystal.server

import icu.takeneko.omms.crystal.event.server.ServerStoppedEvent
import icu.takeneko.omms.crystal.foundation.ActionHost
import icu.takeneko.omms.crystal.main.CrystalServer
import icu.takeneko.omms.crystal.util.LoggerUtil.createLogger
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.LockSupport

var serverStatus = ServerStatus.STOPPED

class ServerThreadDaemon(
    private val launchCommand: String,
    private val workingDir: Path,
) : Thread("ServerThreadDaemon") {

    private val logger = createLogger("ServerThreadDaemon")

    private val queue = ArrayBlockingQueue<String>(1024)

    private lateinit var out: OutputStream

    private lateinit var input: InputStream

    private var actionHost: ActionHost = CrystalServer

    private var process: Process? = null

    lateinit var outputHandler: ServerOutputHandler

    override fun run() {
        try {
            process = Runtime.getRuntime().exec(resolveCommandLine(launchCommand), null, workingDir.toFile())
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
            synchronized(queue) {
                if (queue.isNotEmpty()) {
                    while (queue.isNotEmpty()) {
                        val line = queue.poll()
                        ifServerDebug { logger.info("[DEBUG] Handling input $line") }
                        writer.appendLine(line)
                        writer.flush()
                    }
                }
            }
            LockSupport.parkNanos(1000)
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

    private fun resolveCommandLine(command: String): Array<out String> {
        if (command.isEmpty()) error("Illegal command $command, to short or empty!")

        val stringTokenizer = StringTokenizer(command)
        val list = buildList {
            while (stringTokenizer.hasMoreTokens()) {
                add(stringTokenizer.nextToken())
            }
        }
        return list.toTypedArray()
    }
}
