package icu.takeneko.omms.crystal.command

import cn.hutool.core.date.DateTime
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import icu.takeneko.omms.crystal.main.SharedConstants
import icu.takeneko.omms.crystal.permission.Permission
import icu.takeneko.omms.crystal.text.Text
import icu.takeneko.omms.crystal.text.TextGroup
import icu.takeneko.omms.crystal.text.TextSerializer
import icu.takeneko.omms.crystal.util.createLogger

enum class CommandSource {
    CONSOLE, CENTRAL, PLAYER, PLUGIN
}

private val logger = createLogger("CommandSourceStack")

class CommandSourceStack(val from: CommandSource, val player: String? = null, val permissionLevel: Permission? = null) {
    val feedbackText = mutableListOf<String>()

    fun sendFeedback(text: TextGroup) {

        when (from) {
            CommandSource.PLAYER -> {
                assert(SharedConstants.serverThreadDaemon != null)
                SharedConstants.serverThreadDaemon!!.runCatching {
                    text.getTexts().forEach {
                        this.input("tellraw $player ${TextSerializer.serialize(it)}")
                    }
                }
            }

            CommandSource.CENTRAL -> {
                text.getTexts().forEach {
                    feedbackText.add(it.toRawString())
                }

            }

            else -> {
                text.getTexts().forEach {
                    logger.info(it.toRawString())
                }

            }
        }
    }

    fun sendFeedback(text: TextComponent) {
        when (from) {
            CommandSource.PLAYER -> {
                assert(SharedConstants.serverThreadDaemon != null)
                SharedConstants.serverThreadDaemon!!.runCatching {
                    this.input("tellraw $player ${GsonComponentSerializer.gson().serialize(text)}")
                }
                DateTime.of(0).toString("YYYY-MM-DD hh-mm-ss.SSS")
            }

            CommandSource.CENTRAL -> {
                feedbackText.add(text.content())
            }

            else -> {
                logger.info(text.content())
            }
        }
    }

    fun sendFeedback(text: Text) {
        when (from) {
            CommandSource.PLAYER -> {
                assert(SharedConstants.serverThreadDaemon != null)
                SharedConstants.serverThreadDaemon!!.run {
                    this.input("tellraw $player ${TextSerializer.serialize(text)}")
                }
            }

            CommandSource.PLUGIN -> {
                logger.info(text.toRawString())
                feedbackText.add(text.toRawString())
            }

            CommandSource.CENTRAL -> {
                feedbackText.add(text.toRawString())
            }

            else -> {
                logger.info(text.toRawString())
            }
        }
    }
}