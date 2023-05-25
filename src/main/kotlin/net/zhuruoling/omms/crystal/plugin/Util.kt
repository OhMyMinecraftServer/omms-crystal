package net.zhuruoling.omms.crystal.plugin

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.zhuruoling.omms.crystal.plugin.metadata.PluginDependency
import net.zhuruoling.omms.crystal.plugin.metadata.PluginDependencyRequirement
import net.zhuruoling.omms.crystal.plugin.metadata.PluginMetadataExclusionStrategy
import java.util.regex.Pattern

val gsonForPluginMetadata: Gson = GsonBuilder()
    .addDeserializationExclusionStrategy(PluginMetadataExclusionStrategy)
    .addSerializationExclusionStrategy(PluginMetadataExclusionStrategy)
    .create()

val versionNamePattern: Pattern = Pattern.compile("([><=]=?)([0-9A-Za-z.]+)")

fun pluginRequirementMatches(self: PluginDependencyRequirement, dependency: PluginDependency): Boolean {
    if (self.id != dependency.id) return false
    if (self.symbol == "*")return true
    return when (self.symbol) {
        ">=" -> dependency.version >= self.parsedVersion
        "<=" -> dependency.version <= self.parsedVersion
        ">" ->  dependency.version > self.parsedVersion
        "<" -> dependency.version < self.parsedVersion
        "==" -> self.parsedVersion == dependency.version
        else -> throw IllegalStateException("${self.symbol} is not a valid version comparator.")
    }
}
