package icu.takeneko.omms.crystal.config

import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigData(
    @YamlComment("The working dir of your server folder")
    @SerialName("working-directory")
    val workingDirectory: String = "server",

    @SerialName("launch-command")
    val launchCommand: String = "java -Xmx4G -Xms1G -jar server.jar nogui",

    @SerialName("plugin-directory")
    val pluginDirectory: String = "plugins",

    @YamlComment(
        "Builtin Server Types: vanilla",
        "ServerType, aka. ServerParser",
        "When a plugin declared a ServerParser, it also implicitly declared a ServerType with the same name."
    )
    @SerialName("server-type")
    val serverType: String = "vanilla",

    @YamlComment("encoding used in parsing server output")
    val encoding: String = "UTF-8",

    @SerialName("command-prefix")
    val commandPrefix: String = ".",

    @YamlComment( "Configures builtin rcon client to execute command on server")
    @SerialName("rcon-client")
    val rconClient: RconClient = RconClient(),

    @YamlComment("Configures builtin rcon server to accept remote command requests")
    @SerialName("rcon-server")
    val rconServer: RconServer = RconServer(),

    var lang: String = "en_us",

    @SerialName("debug-options")
    val debugOptions: String = "N"
) {
    @Serializable
    data class RconClient(
        var enabled: Boolean = false,
        var port: Int = 25575,
        var password: String = ""
    )

    @Serializable
    data class RconServer(
        val enabled: Boolean = false,
        val port: Int = 25575,
        val password: String = ""
    )

}