package icu.takeneko.omms.crystal.plugin.container.jar

import icu.takeneko.omms.crystal.CrystalServer
import icu.takeneko.omms.crystal.event.EventBus
import icu.takeneko.omms.crystal.event.SubscribeEvent
import icu.takeneko.omms.crystal.plugin.support.JarClassLoader
import icu.takeneko.omms.crystal.plugin.support.ScanData
import icu.takeneko.omms.crystal.util.LoggerUtil
import org.objectweb.asm.Type

object EventAutoSubscriber {
    private val logger = LoggerUtil.createLogger("EventAutoSubscriber")
    internal val SubscribeEventFqcn = "L" + Type.getInternalName(SubscribeEvent::class.java) + ";"

    fun handleClass(classLoader: JarClassLoader, name: String, scanData: ScanData, pluginBus: EventBus) {
        if (scanData.methodAnnotations.any { (m, l) -> l.any { it.desc == SubscribeEventFqcn } }) {
            logger.info("Auto subscribing {}", name)
            val clazz = classLoader.loadClass(name)
            CrystalServer.eventBus.register(clazz)
            pluginBus.register(clazz)
        }
    }
}