package icu.takeneko.omms.crystal.permission

import kotlinx.serialization.Serializable

@Serializable
data class PermissionStorage(
    val defaultPermissionLevel: Permission = Permission.USER,
    val owner: List<String> = emptyList(),
    val admin: List<String> = emptyList(),
    val user: List<String> = emptyList(),
    val guest: List<String> = emptyList()
)