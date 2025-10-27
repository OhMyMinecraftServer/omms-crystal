package icu.takeneko.omms.crystal.plugin.metadata

import org.apache.maven.artifact.versioning.ArtifactVersion

fun interface VersionRequirement {

    fun matches(version: ArtifactVersion): MatchState

    companion object {
        val ANY = VersionRequirement { MatchState.MATCH }
    }

    enum class MatchState {
        MATCH, NOT_MATCH, SUGGEST
    }
}