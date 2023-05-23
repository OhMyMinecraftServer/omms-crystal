package net.zhuruoling.omms.crystal.config

import net.zhuruoling.omms.crystal.main.DebugOptions
import net.zhuruoling.omms.crystal.util.joinFilePaths
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.Path

val configContentBase: String
    get() = """
    #OMMS Crystal properties
    #${SimpleDateFormat("EEE MMM dd hh:mm:ss z YYYY", Locale.ENGLISH).format(Date())}
    #The working dir of your server folder
    workingDirectory=server
    launchCommand=java -Xmx4G -Xms1G -jar server.jar nogui
    pluginDirectory=plugins
    #Server Types:vanilla(builtin)
    serverType=vanilla
    #encoding used in parsing server output
    encoding=GBK
    commandPrefix=.
    #debug options:
    #   N:None/Off
    #   A:All
    #   E:Event
    #   O:Main
    #   P:Plugin
    #   S:Server
    debugOptions=N
""".trimIndent()

object Config {
    var serverWorkingDirectory: String = "server"
    var launchCommand: String = ""
    var pluginDirectory = ""
    var serverType = ""
    var commandPrefix = "."
    var parserName = ""
    var encoding = "UTF-8"
    fun load():Boolean {
        var isInit = false
        val configPath = joinFilePaths("config.properties")
        if (!Files.exists(Path(configPath))) {
            isInit = true
            Files.createFile(Path(configPath))
            val writer = FileWriter(File(configPath))
            writer.write(configContentBase)
            writer.flush()
            writer.close()
        }
        val properties = Properties()
        val reader = FileReader(configPath)
        properties.load(reader)
        serverWorkingDirectory = properties["workingDirectory"] as String
        serverType = properties["serverType"] as String
        launchCommand = properties["launchCommand"] as String
        pluginDirectory = properties["pluginDirectory"] as String
        commandPrefix = properties["commandPrefix"] as String
        parserName = if (serverType == "vanilla") "builtin" else serverType
        encoding = properties["encoding"] as String
        DebugOptions.parse(properties["debugOptions"] as String)
        reader.close()
        return isInit

    }
}