package icu.takeneko.crystal.example

import icu.takeneko.omms.crystal.event.EventBusSubscriber
import icu.takeneko.omms.crystal.event.SubscribeEvent
import icu.takeneko.omms.crystal.event.server.ServerStartingEvent
import icu.takeneko.omms.crystal.util.LoggerUtil

@EventBusSubscriber
object ExampleEvents {
    private val logger = LoggerUtil.createLogger("ExampleEvents")

    @SubscribeEvent
    @JvmStatic
    fun on(e: ServerStartingEvent) {
        logger.info("Server Starting!")
    }
}