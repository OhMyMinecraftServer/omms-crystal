package icu.takeneko.omms.crystal.util.command

import icu.takeneko.omms.crystal.main.CrystalServer.serverThreadDaemon
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import icu.takeneko.omms.crystal.permission.Permission
import icu.takeneko.omms.crystal.text.Text
import icu.takeneko.omms.crystal.text.TextGroup
import icu.takeneko.omms.crystal.text.TextSerializer
import icu.takeneko.omms.crystal.util.LoggerUtil.createLogger
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CommandSourceStack(val from: CommandSource, val player: String? = null, val permissionLevel: Permission? = null) {
    val feedbackText = mutableListOf<String>()

    fun sendFeedback(text: TextGroup) {
        when (from) {
            CommandSource.PLAYER -> {
                assert(serverThreadDaemon != null)
                serverThreadDaemon!!.runCatching {
                    text.getTexts().forEach {
                        this.input("tellraw $player ${TextSerializer.serialize(it)}")
                    }
                }
            }

            CommandSource.REMOTE -> text.getTexts().forEach {
                feedbackText.add(it.toRawString())
            }

            else -> text.getTexts().forEach {
                logger.info(it.toRawString())
            }

        }
    }

    fun sendFeedback(text: TextComponent) {
        when (from) {
            CommandSource.PLAYER -> {
                assert(serverThreadDaemon != null)
                serverThreadDaemon!!.runCatching {
                    this.input("tellraw $player ${GsonComponentSerializer.gson().serialize(text)}")
                }
                LocalDateTime.now(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("YYYY-MM-DD hh-mm-ss.SSS"))
            }

            CommandSource.REMOTE -> feedbackText.add(text.content())

            else -> logger.info(text.content())
        }
    }

    fun sendFeedback(text: Text) {
        when (from) {
            CommandSource.PLAYER -> {
                assert(serverThreadDaemon != null)
                serverThreadDaemon!!.run {
                    this.input("tellraw $player ${TextSerializer.serialize(text)}")
                }
            }

            CommandSource.PLUGIN -> {
                logger.info(text.toRawString())
                feedbackText.add(text.toRawString())
            }

            CommandSource.REMOTE -> feedbackText.add(text.toRawString())

            else -> logger.info(text.toRawString())
        }
    }

    companion object {
        private val logger = createLogger("CommandSourceStack")
    }
}