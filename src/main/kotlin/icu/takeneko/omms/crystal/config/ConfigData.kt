package icu.takeneko.omms.crystal.config

import kotlinx.serialization.Serializable

@Serializable
data class ConfigData(
    val __workingDir__note__: String = "The working dir of your server folder",
    val workingDir: String = "server",
    val launchCommand: String = "java -Xmx4G -Xms1G -jar server.jar nogui",
    val pluginDirectory: String = "plugins",
    val __serverType__note__: String = "Builtin Server Types: vanilla",
    val __serverType__note___: String = "ServerType, aka. ServerParser",
    val __serverType__note____: String = "When a plugin declared a ServerParser, it also implicitly declared a ServerType with the same name.",
    val serverType: String = "vanilla",
    val __encoding__note__: String = "encoding used in parsing server output",
    val encoding: String = "UTF-8",
    val commandPrefix: String = ".",
    val __rconClient__note__: String = "Configures builtin rcon client to execute command on server",
    var enableRcon: Boolean = false,
    var rconPort: Int = 25575,
    var rconPassword: String = "",
    val __rconServer__note__: String = "Configures builtin rcon server to accept remote command requests",
    val rconServerPort: Int = 25557,
    val rconServerPassword: String = "",
    val enableRconServer: Boolean = false,
    var lang: String = "en_us",
    val debugOptions: String = "N"
) {

}