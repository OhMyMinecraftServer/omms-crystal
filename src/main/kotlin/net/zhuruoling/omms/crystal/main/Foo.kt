package net.zhuruoling.omms.crystal.main

import net.zhuruoling.omms.crystal.event.registerEvents
import net.zhuruoling.omms.crystal.plugin.PluginManager
import net.zhuruoling.omms.crystal.util.BuildProperties


fun main() {
   registerEvents()
   println(BuildProperties.map.toString())
   PluginManager.init()
   PluginManager.loadAll()
}