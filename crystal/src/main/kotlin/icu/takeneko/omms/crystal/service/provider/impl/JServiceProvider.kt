package icu.takeneko.omms.crystal.service.provider.impl

import icu.takeneko.omms.crystal.crystalspi.ICrystalService
import icu.takeneko.omms.crystal.CrystalServer
import icu.takeneko.omms.crystal.plugin.PluginManager
import icu.takeneko.omms.crystal.service.provider.CrystalServiceProvider
import java.util.ServiceLoader

class JServiceProvider : CrystalServiceProvider {

    override fun lookupServices(clazz: Class<out ICrystalService>): Map<String, ICrystalService> {
        return lookupServices(clazz, CrystalServer.classLoader) + lookupServices(clazz, PluginManager.pluginClassLoader)
    }

    private fun lookupServices(clazz: Class<out ICrystalService>, classLoader: ClassLoader): Map<String, ICrystalService> {
        val serviceLoader = ServiceLoader.load(clazz, classLoader)
        return buildMap {
            for (service in serviceLoader) {
                this += service.key() to service
            }
        }
    }

}