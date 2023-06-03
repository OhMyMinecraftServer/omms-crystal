package net.zhuruoling.omms.crystal.rcon

import net.zhuruoling.omms.crystal.config.Config
import nl.vv32.rcon.Rcon
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

object RconClient {

    private lateinit var rcon:Rcon
    fun connect(){
        val socketChannel = SocketChannel.open(InetSocketAddress(Config.rconPort.toInt()))
        rcon = Rcon.newBuilder()
            .withChannel(socketChannel)
            .withCharset(StandardCharsets.UTF_8)
            .withReadBufferCapacity(8192)
            .withWriteBufferCapacity(8192)
            .build()
        rcon.tryAuthenticate(Config.rconPassword)
    }

    fun close(){
        rcon.close()
    }

    fun executeCommand(command:String): String{
        return rcon.sendCommand(command)
    }

}