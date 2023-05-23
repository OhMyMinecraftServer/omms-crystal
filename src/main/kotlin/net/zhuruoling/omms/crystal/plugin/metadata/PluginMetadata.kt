package net.zhuruoling.omms.crystal.plugin.metadata

import com.google.gson.annotations.SerializedName
import net.zhuruoling.omms.crystal.plugin.gsonForPluginMetadata
import java.util.regex.Pattern



class PluginMetadata {
    lateinit var id: String
    lateinit var version: String
    var author: String? = null
    var link: String? = null

    @SerializedName(value = "main", alternate = ["pluginMain", "pluginMainClass"])
    var pluginMainClass: String? = null

    @SerializedName(value = "dependencies", alternate = ["pluginDependencies"])
    var pluginDependencies: List<PluginDependencyRequirement>? = mutableListOf()

    @SerializedName(value = "pluginRequestHandler", alternate = ["requestHandler"])
    var pluginEventHandlers: List<String>? = mutableListOf()

    companion object {
        fun fromJson(s: String?): PluginMetadata {
            return gsonForPluginMetadata.fromJson(s, PluginMetadata::class.java)
        }
    }
}

fun requirementMatches(self: PluginDependencyRequirement, dependency: PluginDependency): Boolean {
    if (self.id != dependency.id) return false
    return when (self.symbol) {
        ">=" -> self.parsedVersion >= dependency.version
        "<=" -> self.parsedVersion <= dependency.version
        ">" -> self.parsedVersion >= dependency.version
        "<" -> self.parsedVersion <= dependency.version
        "==" -> self.parsedVersion >= dependency.version
        else -> throw IllegalStateException("${self.symbol} is not a valid version comparator.")
    }
}

