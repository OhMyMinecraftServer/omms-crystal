package net.zhuruoling.omms.crystal.server
class MinecraftVersions{

}

data class MinecraftVersion(private val versionId: Int, val versionDisplayName: String) : Comparable<MinecraftVersion> {
    override fun compareTo(other: MinecraftVersion): Int = this.versionId - other.versionId
}