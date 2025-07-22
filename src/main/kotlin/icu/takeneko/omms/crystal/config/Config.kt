package icu.takeneko.omms.crystal.config

import icu.takeneko.omms.crystal.server.ServerProperties
import icu.takeneko.omms.crystal.util.LoggerUtil.createLogger
import icu.takeneko.omms.crystal.util.constants.DebugOptions
import icu.takeneko.omms.crystal.util.file.FileUtil.YAML
import icu.takeneko.omms.crystal.util.file.decodeFromString
import icu.takeneko.omms.crystal.util.file.encodeToString
import kotlin.io.path.*

object Config {
    private val logger = createLogger("Config")

    lateinit var config: ConfigData

    private val configFile = Path("./config.yaml")

    fun load(): Boolean {
        var isInit = false

        if (!configFile.exists()) {
            logger.error("Configuration is missing, creating default config.")
            isInit = true
            config = ConfigData()
            write()
        }
        try {
            config = configFile.inputStream().bufferedReader().use {
                YAML.decodeFromString(it.readText())
            }
            DebugOptions.parse(config.debugOptions)
        } catch (t: Throwable) {
            logger.error(
                "Looks like Crystal is not properly configured at current directory, Crystal will not start up until the errors are resolved.",
                t
            )
            isInit = true
        }
        if (config.rconClient.enabled && config.rconClient.password.isBlank()) {
            logger.error("Rcon is enabled and no password provided!")
            logger.info("Attempt to fill config with server.properties")
            try {
                val serverProperties = ServerProperties.properties
                config.rconClient.enabled = (serverProperties["enable-rcon"] as String?).toBoolean()
                config.rconClient.password = serverProperties["rcon.password"] as String? ?: ""
                config.rconClient.port = (serverProperties["rcon.port"] as String? ?: "25575").toInt()
            } catch (e: Exception) {
                throw RuntimeException("Bad config file, cannot fill config with detected environment.", e)
            }
        }
        return isInit
    }

    private fun write() {
        val s = YAML.encodeToString(config)
        configFile.deleteIfExists()
        configFile.createFile()
        configFile.writeText(s)
    }
}
