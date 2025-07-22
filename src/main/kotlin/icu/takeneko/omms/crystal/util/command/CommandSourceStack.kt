package icu.takeneko.omms.crystal.util.command

import icu.takeneko.omms.crystal.main.CrystalServer
import icu.takeneko.omms.crystal.main.CrystalServer.serverThreadDaemon
import icu.takeneko.omms.crystal.permission.Permission
import icu.takeneko.omms.crystal.util.LoggerUtil.createLogger
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

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

    companion object {
        private val logger = createLogger("CommandSourceStack")
    }
}
