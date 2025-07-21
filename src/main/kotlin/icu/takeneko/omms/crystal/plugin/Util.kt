package icu.takeneko.omms.crystal.plugin

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import icu.takeneko.omms.crystal.plugin.metadata.PluginDependency
import icu.takeneko.omms.crystal.plugin.metadata.PluginDependencyRequirement
import icu.takeneko.omms.crystal.plugin.metadata.PluginMetadataExclusionStrategy
import icu.takeneko.omms.crystal.util.constants.DebugOptions
import net.bytebuddy.agent.ByteBuddyAgent
import java.lang.instrument.Instrumentation
import java.util.regex.Pattern

val gsonForPluginMetadata: Gson = GsonBuilder()
    .addDeserializationExclusionStrategy(PluginMetadataExclusionStrategy)
    .addSerializationExclusionStrategy(PluginMetadataExclusionStrategy)
    .create()

val versionNamePattern: Pattern = Pattern.compile("([><=]=?)([0-9A-Za-z.]+)")

fun pluginRequirementMatches(self: PluginDependencyRequirement, dependency: PluginDependency): Boolean {
    if (self.id != dependency.id) return false
    if (self.symbol == "*") return true
    return when (self.symbol) {
        ">=" -> dependency.version >= self.parsedVersion
        "<=" -> dependency.version <= self.parsedVersion
        ">" -> dependency.version > self.parsedVersion
        "<" -> dependency.version < self.parsedVersion
        "==" -> self.parsedVersion == dependency.version
        else -> error("${self.symbol} is not a valid version comparator.")
    }
}

object InstrumentationAccess {
    val instrumentation: Instrumentation by lazy {
        ByteBuddyAgent.install()
    }
}

inline fun ifPluginDebug(block: () -> Unit) {
    if (DebugOptions.pluginDebug()) block()
}