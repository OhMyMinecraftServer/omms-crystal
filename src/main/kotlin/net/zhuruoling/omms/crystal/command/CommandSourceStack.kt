package net.zhuruoling.omms.crystal.command

import net.zhuruoling.omms.crystal.main.SharedConstants
import net.zhuruoling.omms.crystal.permission.Permission
import net.zhuruoling.omms.crystal.text.Text
import net.zhuruoling.omms.crystal.text.TextGroup
import net.zhuruoling.omms.crystal.text.TextSerializer
import net.zhuruoling.omms.crystal.util.createLogger

enum class CommandSource {
    CONSOLE, CENTRAL, PLAYER, PLUGIN
}

private val logger = createLogger("CommandSourceStack")

class CommandSourceStack(val from: CommandSource, val player: String? = null, val permissionLevel: Permission? = null) {
    val feedbackText = mutableListOf<String>()

    fun sendFeedback(text: TextGroup) {

        when (from) {
            CommandSource.PLAYER -> {
                assert(SharedConstants.serverController != null)
                SharedConstants.serverController!!.runCatching {
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

    fun sendFeedback(text: Text) {
        when (from) {
            CommandSource.PLAYER -> {
                assert(SharedConstants.serverController != null)
                SharedConstants.serverController!!.run {
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