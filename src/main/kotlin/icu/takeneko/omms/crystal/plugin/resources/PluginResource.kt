package icu.takeneko.omms.crystal.plugin.resources

import icu.takeneko.omms.crystal.main.DebugOptions
import java.io.InputStreamReader

class PluginResource(val bundleId: String) {

    var resDataMap = mutableMapOf<String, String>()
        private set
    var resMetaDataMap = mutableMapOf<String, String>()
        private set

    fun getResValue(id: String): String? {
        return resDataMap[id]
    }

    fun getResMetaValue(id: String): String? {
        return resMetaDataMap[id]
    }

    override fun toString(): String {
        return "meta = $resMetaDataMap, data = $resDataMap "
    }

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
                    resMeta += key.removePrefix("res?") to value
                    return@forEachLine
                }
                resStore += key to value
            }
            return PluginResource(id).run { this.resMetaDataMap = resMeta; this.resDataMap = resStore;this }
        }
    }
}
