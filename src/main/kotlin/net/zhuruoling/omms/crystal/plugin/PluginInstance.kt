package net.zhuruoling.omms.crystal.plugin

import net.zhuruoling.omms.crystal.plugin.metadata.PluginMetadata
import java.net.URLClassLoader

class PluginInstance(val urlClassLoader: URLClassLoader, val fileFullPath: String) {

    lateinit var metadata: PluginMetadata
    private lateinit var mainClazzInstance: PluginInitializer

    fun loadPluginMetadata(){

    }

    fun loadPluginClasses(){

    }

    fun onInitialize(){
        mainClazzInstance.onInitialize()
    }

}