package icu.takeneko.omms.crystal.rcon

import icu.takeneko.omms.crystal.config.Config
import icu.takeneko.omms.crystal.util.createLogger
import org.slf4j.Logger
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException

class RconListener(private val listener: ServerSocket, private val password: String?) : RconBase("RCON Listener") {
    private val clients: MutableList<RconServer> = ArrayList<RconServer>()

    private fun removeStoppedClients() {
        this.clients.removeIf { client -> !client.isRunning() }
    }

    override fun run() {
        try {
            while (this.running) {
                try {
                    val socket = this.listener.accept()
                    val rconServer = RconServer(this.password, socket)
                    rconServer.start()
                    this.clients.add(rconServer)
                    this.removeStoppedClients()
                } catch (var7: SocketTimeoutException) {
                    this.removeStoppedClients()
                } catch (var8: IOException) {
                    if (this.running) {
                        logger.info("IO exception: ", var8)
                    }
                }
            }
        } finally {
            this.closeSocket(this.listener)
        }
    }

    fun create(): RconListener? {
        val hostname = "0.0.0.0"
        val rconPort: Int = Config.config.rconServer.port //rcon port
        if (0 < rconPort && 65535 >= rconPort) {
            val rconPassword: String = Config.config.rconServer.password
            if (rconPassword.isEmpty()) {
                logger.warn("No rcon password set in server.properties, rcon disabled!")
                return null
            } else {
                try {
                    val serverSocket = ServerSocket(rconPort, 0, InetAddress.getByName(hostname))
                    serverSocket.setSoTimeout(500)
                    val rconListener = RconListener(serverSocket, rconPassword)
                    if (!rconListener.start()) {
                        return null
                    } else {
                        logger.info("RCON running on {}:{}", hostname, rconPort)
                        return rconListener
                    }
                } catch (e: IOException) {
                    logger.warn("Unable to initialise RCON on {}:{}", hostname, rconPort, e)
                    return null
                }
            }
        } else {
            logger.warn("Invalid rcon port {} found in server.properties, rcon disabled!", rconPort)
            return null
        }
    }

    public override fun stop() {
        this.running = false
        this.closeSocket(this.listener)
        super.stop()
        for (client in clients) {
            if (client.isRunning()) {
                client.stop()
            }
        }
        this.clients.clear()
    }

    private fun closeSocket(socket: ServerSocket) {
        try {
            socket.close()
        } catch (e: IOException) {
            logger.warn("Failed to close socket", e)
        }
    }

    companion object {
        private val logger: Logger = createLogger("RconListener", false)
    }
}