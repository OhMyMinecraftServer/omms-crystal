package icu.takeneko.omms.crystal.plugin

interface PluginInitializer {
    fun onInitialize()

    fun onFinalize()
}