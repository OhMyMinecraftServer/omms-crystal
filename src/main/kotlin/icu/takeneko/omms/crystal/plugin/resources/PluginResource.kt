package icu.takeneko.omms.crystal.plugin.resources

import java.io.InputStreamReader

class PluginResource(val bundleId: String) {

    val resDataMap = mutableMapOf<String, String>()

    val resMetaDataMap = mutableMapOf<String, String>()

    fun getResValue(id: String): String? = resDataMap[id]

    fun getResMetaValue(id: String): String? = resMetaDataMap[id]

    override fun toString(): String = "meta = $resMetaDataMap, data = $resDataMap "

    companion object {
        fun fromReader(id: String, reader: InputStreamReader): PluginResource {
            val resStore = mutableMapOf<String, String>()
            val resMeta = mutableMapOf<String, String>()
            reader.forEachLine {
                if (it.startsWith("#")) return@forEachLine
                val s = it.split("=")
                val key = s.first()
                var value = ""
                if (s.size > 1) {
                    value = s.subList(1, s.size).joinToString("=")
                }
                if (key.startsWith("res?")) {
                    resMeta[key.removePrefix("res?")] = value
                    return@forEachLine
                }
                resStore[key] = value
            }
            return PluginResource(id).run {
                this.resMetaDataMap.clear()
                this.resMetaDataMap.putAll(resMeta)
                this.resDataMap.clear()
                this.resDataMap.putAll(resStore)
                this
            }
        }
    }
}
