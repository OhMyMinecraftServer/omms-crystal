package icu.takeneko.omms.crystal.rcon

import icu.takeneko.omms.crystal.config.ConfigManager
import nl.vv32.rcon.Rcon
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

object RconClient {
    private lateinit var rcon: Rcon

    fun connect() {
        val socketChannel = SocketChannel.open(InetSocketAddress(ConfigManager.config.rconClient.port))
        rcon = Rcon.newBuilder()
            .withChannel(socketChannel)
            .withCharset(StandardCharsets.UTF_8)
            .withReadBufferCapacity(8192)
            .withWriteBufferCapacity(8192)
            .build()
        rcon.tryAuthenticate(ConfigManager.config.rconClient.password)
    }

    fun close() = rcon.close()

    fun executeCommand(command: String): String = rcon.sendCommand(command)
}
