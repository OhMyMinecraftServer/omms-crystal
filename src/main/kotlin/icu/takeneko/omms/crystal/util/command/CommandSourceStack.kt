package icu.takeneko.omms.crystal.util.command

import icu.takeneko.omms.crystal.main.CrystalServer
import icu.takeneko.omms.crystal.main.CrystalServer.serverThreadDaemon
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import icu.takeneko.omms.crystal.permission.Permission
import icu.takeneko.omms.crystal.text.Text
import icu.takeneko.omms.crystal.text.TextGroup
import icu.takeneko.omms.crystal.text.TextSerializer
import icu.takeneko.omms.crystal.util.createLogger
import net.kyori.adventure.text.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class CommandSource {
    CONSOLE, REMOTE, PLAYER, PLUGIN
}

private val logger = createLogger("CommandSourceStack")

class CommandSourceStack(val from: CommandSource, val player: String? = null, val permissionLevel: Permission? = null) {
    val feedbackText = mutableListOf<String>()

    fun sendFeedback(vararg text: TextComponent) {
        for (component in text) {
            when (from) {
                CommandSource.PLAYER -> {
                    assert(serverThreadDaemon != null)
                    CrystalServer.input("tellraw $player ${GsonComponentSerializer.gson().serialize(component)}")
                }

                CommandSource.REMOTE -> {
                    feedbackText.add(component.content())
                }

                else -> {
                    logger.info(component.content())
                }
            }
        }
    }
}