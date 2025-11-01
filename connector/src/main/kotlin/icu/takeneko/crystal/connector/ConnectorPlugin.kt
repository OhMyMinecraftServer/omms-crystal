package icu.takeneko.crystal.connector

import icu.takeneko.omms.crystal.CrystalServer
import icu.takeneko.omms.crystal.event.EventBus
import icu.takeneko.omms.crystal.event.SubscribeEvent
import icu.takeneko.omms.crystal.event.server.CrystalSetupEvent
import icu.takeneko.omms.crystal.plugin.annotation.Plugin
import icu.takeneko.omms.crystal.plugin.container.PluginContainer
import icu.takeneko.omms.crystal.util.LoggerUtil

@Plugin("connector")
class ConnectorPlugin constructor(
    private val pluginBus: EventBus,
    private val container: PluginContainer
) {
    private val logger = LoggerUtil.createLogger("ConnectorPlugin")

    init {
        logger.info("MCDReforged connector is launching!")
        CrystalServer.eventBus.register(this)
        pluginBus.register(this)
    }
}