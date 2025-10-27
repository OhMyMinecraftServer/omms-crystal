package icu.takeneko.omms.crystal.plugin.metadata

import icu.takeneko.omms.crystal.util.LoggerUtil
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.apache.maven.artifact.versioning.ArtifactVersion
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.apache.maven.artifact.versioning.VersionRange

@Serializable
data class PluginMetadata(
    val id: String,
    val version: String,
    val dependencies: List<PluginDependency> = listOf()
) {
    fun parseVersion(): ArtifactVersion = DefaultArtifactVersion(version)
}

@Serializable
data class PluginDependency(
    val id: String,
    @SerialName("version")
    val versionRequirement: String,
    val type: Type
) {

    @kotlinx.serialization.Transient
    private val parsedRange = VersionRange.createFromVersionSpec(versionRequirement)
    private val parsedRequirement = this.type.createVersionRequirement(parsedRange)

    enum class Type {
        @SerialName("required")
        REQUIRED,
        @SerialName("optional")
        OPTIONAL,
        @SerialName("unsupported")
        UNSUPPORTED;

        fun createVersionRequirement(range: VersionRange): VersionRequirement {
            return when (this) {
                REQUIRED -> VersionRequirement {
                    if (range.containsVersion(it)) {
                        VersionRequirement.MatchState.MATCH
                    } else {
                        VersionRequirement.MatchState.NOT_MATCH
                    }
                }

                OPTIONAL -> VersionRequirement {
                    if (!range.containsVersion(it)) {
                        VersionRequirement.MatchState.SUGGEST
                    } else {
                        VersionRequirement.MatchState.MATCH
                    }
                }

                UNSUPPORTED -> VersionRequirement {
                    if (range.containsVersion(it)) {
                        VersionRequirement.MatchState.NOT_MATCH
                    } else {
                        VersionRequirement.MatchState.MATCH
                    }
                }
            }
        }
    }

    private companion object {
        private val logger = LoggerUtil.createLogger("PluginDependency")
    }
}