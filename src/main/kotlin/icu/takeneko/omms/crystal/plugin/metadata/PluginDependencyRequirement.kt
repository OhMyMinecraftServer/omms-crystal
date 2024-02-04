package icu.takeneko.omms.crystal.plugin.metadata

import com.google.gson.annotations.SerializedName
import icu.takeneko.omms.crystal.plugin.pluginRequirementMatches
import icu.takeneko.omms.crystal.plugin.versionNamePattern
import java.lang.module.ModuleDescriptor


class PluginDependencyRequirement {
    @SerializedName(value = "id", alternate = ["pluginId"])
    lateinit var id: String
        private set
    @SerializedName(value = "requirement", alternate = ["require"])
    lateinit var requirement: String
        private set
    lateinit var symbol: String
        private set
    lateinit var parsedVersion: ModuleDescriptor.Version
        private set

    fun parseRequirement() {
        if (requirement == "*") {
            symbol = "*"
            return
        }
        val matcher = versionNamePattern.matcher(requirement)
        check(matcher.matches()) { "Unexpected value: $requirement" }
        val match = matcher.toMatchResult()
        symbol = match.group(1)
        parsedVersion = ModuleDescriptor.Version.parse(match.group(2))
    }

    fun requirementMatches(dependency: PluginDependency): Boolean {
        return pluginRequirementMatches(this, dependency)
    }
}
