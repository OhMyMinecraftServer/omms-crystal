package net.zhuruoling.omms.crystal.plugin

interface PluginInitializer {
    fun onInitialize()

    fun onFinalize(){}
}