package icu.takeneko.omms.crystal.service

import com.google.common.collect.HashBasedTable
import icu.takeneko.omms.crystal.crystalspi.ICrystalService
import icu.takeneko.omms.crystal.service.provider.CrystalServiceProvider
import icu.takeneko.omms.crystal.service.provider.impl.JServiceProvider

object CrystalServiceManager {
    private val services = HashBasedTable.create<Class<out ICrystalService>, String, ICrystalService>()
    private val providers = mutableListOf<CrystalServiceProvider>()

    init {
        providers.add(JServiceProvider())
    }

    private fun registerProvider(provider: CrystalServiceProvider) {
        providers += provider
    }

    fun <T : ICrystalService> load(clazz: Class<T>): Map<String, T> {
        if (!services.containsRow(clazz)) {
            this.services.row(clazz) += providers.map { it.lookupServices(clazz).toList() }.flatMap { it }.toMap()
        }
        return services.row(clazz).mapValues { (_, value) -> value as T }
    }

    fun clearService(clazz: Class<out ICrystalService>){
        this.services.row(clazz).clear()
    }

}