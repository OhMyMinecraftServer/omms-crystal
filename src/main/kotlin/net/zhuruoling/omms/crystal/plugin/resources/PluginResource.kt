package net.zhuruoling.omms.crystal.plugin.resources

import java.io.InputStreamReader

class PluginResource(val bundleId: String) {

    var resMap = mutableMapOf<String, String>()
        private set
    var resMeta = mutableMapOf<String, String>()
        private set

    fun getResValue(id: String): String? {
        return resMap[id]
    }

    fun getResMetaValue(id: String): String? {
        return resMeta[id]
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
                    value = s.subList(1, s.size - 1).joinToString("=")
                }
                if (key.startsWith("res?")) {
                    resMeta += key.removePrefix("res?") to value
                    return@forEachLine
                }
                resStore += key to value
            }
            return PluginResource(id).run { this.resMeta = resMeta; this.resMap = resStore;this }
        }
    }
}
