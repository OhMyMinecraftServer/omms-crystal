package icu.takeneko.omms.crystal.config

import icu.takeneko.omms.crystal.main.DebugOptions
import icu.takeneko.omms.crystal.server.ServerPropertiesAccess
import icu.takeneko.omms.crystal.util.createLogger
import icu.takeneko.omms.crystal.util.joinFilePaths
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
    enableRcon=false
    rconPort=25575
    lang=en_us
    rconPassword=
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
    private val logger = createLogger("Config")
    var serverWorkingDirectory: String = "server"
    var launchCommand: String = ""
    var pluginDirectory = ""
    var serverType = ""
    var commandPrefix = "."
    var parserName = ""
    var encoding = "UTF-8"
    var lang = "en_us"
    var enableRcon = false
    var rconPort = "25575"
    var rconPassword = ""
    fun load(): Boolean {
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
        lang = properties["lang"] as String? ?: "en_us"
        DebugOptions.parse(properties["debugOptions"] as String)
        enableRcon = (properties["enableRcon"] as String?).toBoolean()
        val port = properties["rconPort"] as String? ?: ""
        val password = properties["rconPassword"] as String? ?: ""
        reader.close()
        if (enableRcon and (port.isBlank() || password.isBlank())) {
            logger.error(
                "Rcon is enabled and no ${
                    if (port.isBlank() and password.isBlank())
                        "rcon password or port"
                    else
                        if (password.isBlank())
                            "password"
                        else
                            "port"
                } provided!"
            )
            logger.info("Attempt to fill config with server.properties")
            try{
                val serverProperties = ServerPropertiesAccess.tryAccess()
                enableRcon = (serverProperties["enable-rcon"] as String?).toBoolean()
                rconPassword = serverProperties["rcon.password"] as String? ?: ""
                rconPort = serverProperties["rcon.port"] as String? ?: "25575"
            }catch (e:Exception){
                throw RuntimeException("Bad config file, cannot fill config with detected environment.", e)
            }
        }
        return isInit
    }
}