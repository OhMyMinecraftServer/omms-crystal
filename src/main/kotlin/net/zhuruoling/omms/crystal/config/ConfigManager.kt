package net.zhuruoling.omms.crystal.config

import com.alibaba.fastjson2.JSON
import com.google.gson.Gson
import net.zhuruoling.omms.crystal.util.Manager
import net.zhuruoling.omms.crystal.util.joinFilePaths
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.name


object ConfigManager : Manager<String, MutableMap<String, Any>>(
    afterInit = {},
    beforeInit = {  },
    fileNameFilter = {
        it.endsWith(".plugin.json")
    },
    initializer = {
        val storage = Gson().fromJson(FileReader(it), hashMapOf<String, Any>()::class.java)
        Pair(Path(it).fileName.name.split(".")[0], storage)
    },
    scanFolder = "config"
) {
    @Synchronized
    fun getConfig(
        pluginId: String,
        createIfNotExist: Boolean = true,
        defaultConfig: MutableMap<String, Any>? = null
    ): MutableMap<String, Any> {
        val jsonObject = this.map[pluginId]
        if (jsonObject == null) {
            if (createIfNotExist) {
                val dataMap = defaultConfig ?: mutableMapOf()
                this.map[pluginId] = dataMap
                writeConfig()
            } else {
                throw IllegalArgumentException("")
            }
        }
        if (defaultConfig != null){
            var replaceExists = false
            defaultConfig.forEach {
                if (!map[pluginId]!!.containsKey(it.key)){
                    replaceExists = true
                    map[pluginId]!![it.key] = it.value
                }
            }
            if (replaceExists){
                writeConfig()
            }
        }
        return this.map[pluginId]!!
    }

    @Synchronized
    private fun writeConfig() {
        this.map.forEach { (t, u) ->
            val configJson = JSON.toJSONString(u)
            val fileWriter = FileWriter(joinFilePaths("config", "${t}.plugin.json"), false)
            fileWriter.write(configJson)
            fileWriter.flush()
            fileWriter.close()
        }
    }

    @Synchronized
    fun getPluginConfig(
        pluginId: String,
        createIfNotExist: Boolean = true,
        defaultConfig: MutableMap<String, Any>? = null
    ): PluginConfig {
        return PluginConfig(pluginId, getConfig(pluginId, createIfNotExist, defaultConfig))
    }

    @Synchronized
    fun setConfigValue(pluginId: String, key: String, value: Any) {
        val m = map[pluginId] ?: mutableMapOf()
        m[key] = value
        map[pluginId] = m
        writeConfig()
    }

    @Synchronized
    fun removeConfigKey(pluginId: String, key: String){
        val m= map[pluginId] ?: throw java.lang.IllegalArgumentException("Illegal plugin id $pluginId (Not Exist)")
        m.remove(key)
        map[pluginId] = m
        writeConfig()
    }

    @Synchronized
    fun exists(pluginId: String) = map.containsKey(pluginId)

    @Synchronized
    fun deleteConfig(pluginId: String){
        if (!exists(pluginId)) throw java.lang.IllegalArgumentException("Illegal plugin id $pluginId (Not Exist)")
        Files.delete(Path(joinFilePaths("config", "${pluginId}.plugin.json")))
        map.remove(pluginId)
        writeConfig()
    }

}

class PluginConfig(val pluginId: String, val config: MutableMap<String, Any>) {

    operator fun get(key: String): Any? {
        return config[key]
    }

    fun <T> get(key: String, classOfTypeT: Class<out T>): T? {
        val any = config[key] ?: return null
        return any as T
    }

    fun getInt(key: String): Int? {
        return get(key, Int::class.java)
    }

    fun getString(key: String): String? {
        return get(key, String::class.java)
    }
}
