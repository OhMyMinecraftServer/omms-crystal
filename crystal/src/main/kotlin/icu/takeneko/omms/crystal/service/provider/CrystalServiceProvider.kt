package icu.takeneko.omms.crystal.service.provider

import icu.takeneko.omms.crystal.crystalspi.ICrystalService

interface CrystalServiceProvider {
    fun lookupServices(clazz: Class<out ICrystalService>): Map<String, ICrystalService>
}