package icu.takeneko.omms.crystal.rcon

import icu.takeneko.omms.crystal.command.CommandManager
import icu.takeneko.omms.crystal.permission.Permission
import icu.takeneko.omms.crystal.util.LoggerUtil.createLogger
import icu.takeneko.omms.crystal.util.command.CommandSource
import icu.takeneko.omms.crystal.util.command.CommandSourceStack
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.nio.charset.StandardCharsets
import kotlin.math.min

class RconServer(
    private val password: String,
    private val socket: Socket
) : RconBase("RCON Client " + socket.getInetAddress()) {

    private var authenticated = false
    private val packetBuffer = ByteArray(1460)

    init {
        try {
            this.socket.soTimeout = 0
        } catch (_: Exception) {
            this.running = false
        }
    }

    override fun run() {
        while (true) {
            try {
                if (!this.running) continue
                val bufferInputStream = BufferedInputStream(this.socket.getInputStream())
                val packetLength = bufferInputStream.read(this.packetBuffer)
                if (packetLength < 10) return

                var index = 0
                val k = BufferHelper.getIntLE(this.packetBuffer, 0, packetLength)

                if (k != packetLength - 4) return

                index += 4
                val l = BufferHelper.getIntLE(this.packetBuffer, index, packetLength)
                index += 4
                val m = BufferHelper.getIntLE(this.packetBuffer, index)
                index += 4
                when (m) {
                    2 -> {
                        if (this.authenticated) {
                            val command = BufferHelper.getString(this.packetBuffer, index, packetLength)

                            try {
                                this.respond(l, executeCommand(command))
                            } catch (e: Exception) {
                                this.respond(l, "Error executing: $command (${e.message})")
                            }
                        } else {
                            this.fail()
                        }
                    }

                    3 -> {
                        val string = BufferHelper.getString(this.packetBuffer, index, packetLength)
                        if (string.isNotEmpty() && string == this.password) {
                            this.authenticated = true
                            this.respond(l, 2, "")
                        } else {
                            this.authenticated = false
                            this.fail()
                        }
                    }

                    else -> this.respond(l, "Unknown request ${m.toHexString()}")
                }
                continue
            } catch (_: IOException) {
            } catch (e: Exception) {
                logger.error("Exception whilst parsing RCON input", e)
            } finally {
                this.close()
                logger.info("Thread {} shutting down", this.description)
                this.running = false
            }
            return
        }
    }

    private fun respond(sessionToken: Int, responseType: Int, message: String) {
        val byteArrayOutputStream = ByteArrayOutputStream(1248)
        val dataOutputStream = DataOutputStream(byteArrayOutputStream)
        val bytes = message.toByteArray(StandardCharsets.UTF_8)
        dataOutputStream.apply {
            writeInt(Integer.reverseBytes(bytes.size + 10))
            writeInt(Integer.reverseBytes(sessionToken))
            writeInt(Integer.reverseBytes(responseType))
            write(bytes)
            write(0)
            write(0)
        }
        this.socket.outputStream.write(byteArrayOutputStream.toByteArray())
    }

    private fun fail() {
        this.respond(-1, 2, "")
    }

    private fun respond(sessionToken: Int, message: String) {
        var length = message.length

        do {
            val i = min(4096, length)
            this.respond(sessionToken, 0, message.substring(0, i))
            val message = message.substring(i, length)
            length = message.length
        } while (0 != i)
    }

    override fun stop() {
        this.running = false
        this.close()
        super.stop()
    }

    private fun executeCommand(command: String): String {
        val source = CommandSourceStack(CommandSource.REMOTE, permissionLevel = Permission.OWNER)
        CommandManager.execute(command, source)
        return source.feedbackText.joinToString("\n", limit = Int.MAX_VALUE)
    }

    private fun close() {
        try {
            this.socket.close()
        } catch (e: IOException) {
            logger.warn("Failed to close socket", e)
        }
    }

    companion object {
        private val logger = createLogger("RconBase", false)
    }
}
