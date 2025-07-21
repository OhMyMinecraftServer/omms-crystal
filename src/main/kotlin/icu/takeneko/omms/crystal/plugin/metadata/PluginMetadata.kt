package icu.takeneko.omms.crystal.plugin.metadata

import com.google.gson.annotations.SerializedName
import icu.takeneko.omms.crystal.plugin.gsonForPluginMetadata


class PluginMetadata {
    var id: String? = null
    var version: String? = null
    var author: String? = null
    var link: String? = null

    @SerializedName(value = "main", alternate = ["pluginMain", "pluginMainClass"])
    var pluginInitializerClass: String? = null

    @SerializedName(value = "dependencies", alternate = ["pluginDependencies"])
    var pluginDependencies: List<PluginDependencyRequirement>? = mutableListOf()

    @SerializedName(value = "pluginEventHandlers", alternate = ["eventHandlers"])
    var pluginEventHandlers: List<String>? = mutableListOf()

    @SerializedName(value = "pluginMinecraftParsers", alternate = ["parsers", "minecraftParsers"])
    var pluginMinecraftParsers: Map<String, String>? = mutableMapOf()

    @SerializedName(value = "languages", alternate = ["res", "resources", "lang"])
    var resources: Map<String, String>? = null

    companion object {
        fun fromJson(s: String?): PluginMetadata = gsonForPluginMetadata.fromJson(s, PluginMetadata::class.java)
    }
}

