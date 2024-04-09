package icu.takeneko.omms.crystal.config

import icu.takeneko.omms.crystal.main.DebugOptions
import icu.takeneko.omms.crystal.server.ServerPropertiesAccess
import icu.takeneko.omms.crystal.util.createLogger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.path.*

object Config {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val logger = createLogger("Config")
    lateinit var config:ConfigData
    private val configFile = Path("./config.json")
    fun load(): Boolean {
        var isInit = false
        if (!configFile.exists()){
            logger.error("Configuration is missing, creating default config.")
            isInit = true
            config = ConfigData()
            write()
        }
        try {
            config = configFile.inputStream().bufferedReader().use {
                json.decodeFromString(it.readText())
            }
            DebugOptions.parse(config.debugOptions)
        } catch (t:Throwable) {
            logger.error("Looks like Crystal is not properly configured at current directory, Crystal will not start up until the errors are resolved.", t)
            isInit = true
        }
        if (config.enableRcon and config.rconPassword.isBlank()) {
            logger.error("Rcon is enabled and no password provided!")
            logger.info("Attempt to fill config with server.properties")
            try{
                val serverProperties = ServerPropertiesAccess.tryAccess()
                config.enableRcon = (serverProperties["enable-rcon"] as String?).toBoolean()
                config.rconPassword = serverProperties["rcon.password"] as String? ?: ""
                config.rconPort = (serverProperties["rcon.port"] as String? ?: "25575").toInt()
            }catch (e:Exception){
                throw RuntimeException("Bad config file, cannot fill config with detected environment.", e)
            }
        }
        return isInit
    }

    private fun write(){
        val s = json.encodeToString(config)
        configFile.deleteIfExists()
        configFile.createFile()
        configFile.writeText(s)
    }

}