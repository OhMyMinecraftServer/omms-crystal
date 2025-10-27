package icu.takeneko.crystal.example

import icu.takeneko.omms.crystal.CrystalServer
import icu.takeneko.omms.crystal.command.LiteralCommand
import icu.takeneko.omms.crystal.command.execute
import icu.takeneko.omms.crystal.command.sendFeedback
import icu.takeneko.omms.crystal.event.EventBus
import icu.takeneko.omms.crystal.event.SubscribeEvent
import icu.takeneko.omms.crystal.event.server.CrystalSetupEvent
import icu.takeneko.omms.crystal.event.server.RegisterCommandEvent
import icu.takeneko.omms.crystal.plugin.annotation.Plugin
import icu.takeneko.omms.crystal.plugin.container.PluginContainer
import icu.takeneko.omms.crystal.util.LoggerUtil
import net.kyori.adventure.text.Component

@Plugin("example")
class ExamplePluginEntry constructor(
    private val pluginBus: EventBus,
    private val container: PluginContainer
) {
    private val logger = LoggerUtil.createLogger("ExamplePlugin")

    init {
        logger.info("Hello World from example plugin!")
        CrystalServer.eventBus.register(this)
        pluginBus.register(this)
    }

    @SubscribeEvent
    fun on(e: CrystalSetupEvent) {
        logger.info("on CrystalSetupEvent")
    }

    @SubscribeEvent
    fun on(e: RegisterCommandEvent) {
        e.register(
            LiteralCommand("example") {
                "arg1" {
                    execute {
                        sendFeedback(Component.text("EXAMPLE COMMAND ARG 1!"))
                    }
                }

                execute {
                    sendFeedback(Component.text("EXAMPLE COMMAND!"))
                }
            }
        )
        logger.info("on RegisterCommandEvent")
    }
}