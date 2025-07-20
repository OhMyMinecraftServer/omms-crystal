package icu.takeneko.omms.crystal.permission

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import icu.takeneko.omms.crystal.util.joinFilePaths
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

val defaultPermissionFileContent = """
    {
        "defaultPermissionLevel":"USER",
        "owner":[],
        "admin":[],
        "user":[],
        "guest":[]
    }
""".trimIndent()

data class PermissionStorage(
    @SerializedName("defaultPermissionLevel")
    val defaultPermissionLevel: Permission,
    val owner: ArrayList<String>,
    val admin: ArrayList<String>,
    val user: ArrayList<String>,
    val guest: ArrayList<String>
)

enum class Permission {
    OWNER, ADMIN, USER, GUEST;
}

object PermissionManager {
    val gson = GsonBuilder().serializeNulls().create()
    val permissionMap: ConcurrentHashMap<String, Permission> = ConcurrentHashMap<String, Permission>()
    var defaultPermissionLevel = Permission.USER
    val filePath = Path(joinFilePaths("permissions.json"))

    @Synchronized
    fun init() {
        if (!filePath.exists()) {
            Files.createFile(filePath)
            filePath.writeText(defaultPermissionFileContent)
        }
        val fileContent = filePath.readText()
        val permissionStorage = gson.fromJson(fileContent, PermissionStorage::class.java)
        permissionStorage.guest.forEach {
            permissionMap[it] = Permission.GUEST
        }
        permissionStorage.user.forEach {
            permissionMap[it] = Permission.USER
        }
        permissionStorage.admin.forEach {
            permissionMap[it] = Permission.ADMIN
        }
        permissionStorage.owner.forEach {
            permissionMap[it] = Permission.OWNER
        }
        defaultPermissionLevel = permissionStorage.defaultPermissionLevel
    }

    @Synchronized
    fun setPermission(player: String, permissionLevel: Permission = defaultPermissionLevel) {
        permissionMap[player] = permissionLevel
    }

    operator fun set(player: String, permissionLevel: Permission) = setPermission(player, permissionLevel)

    @Synchronized
    fun deletePlayer(player: String) {
        permissionMap.remove(player)
    }

    @Synchronized
    fun forEach(function: (player: String, permission: Permission) -> Unit) {
        permissionMap.forEach { (t, u) -> function(t, u) }
    }

    fun playerExists(player: String): Boolean = permissionMap.containsKey(player)

    operator fun contains(player: String) = playerExists(player)

    operator fun get(player: String) = getPermission(player)

    @Synchronized
    fun writePermission() {
        val permissionStorage = convertToPermissionStorage()
        filePath.writeText(gson.toJson(permissionStorage))
    }

    fun convertToPermissionStorage(): PermissionStorage {
        val defaultPermissionLevel: Permission = defaultPermissionLevel
        val owner: ArrayList<String> = arrayListOf()
        val admin: ArrayList<String> = arrayListOf()
        val user: ArrayList<String> = arrayListOf()
        val guest: ArrayList<String> = arrayListOf()
        permissionMap.forEach { (player, permission) ->
            when (permission) {
                Permission.USER -> user.add(player)
                Permission.OWNER -> owner.add(player)
                Permission.ADMIN -> admin.add(player)
                Permission.GUEST -> guest.add(player)
            }
        }
        owner.sort()
        user.sort()
        admin.sort()
        guest.sort()
        return PermissionStorage(defaultPermissionLevel, owner, admin, user, guest)
    }

    @Synchronized
    fun getPermission(player: String): Permission = permissionMap[player] ?: defaultPermissionLevel
}

//level compare a > b : true
//else false
fun comparePermission(a: Permission, b: Permission): Boolean =
    toIntegerPermissionLevel(a) >= toIntegerPermissionLevel(b)

fun toIntegerPermissionLevel(permission: Permission): Int = when (permission) {
    Permission.OWNER -> 4
    Permission.ADMIN -> 3
    Permission.USER -> 2
    Permission.GUEST -> 1
}


fun resolvePermissionLevel(name: String): Permission =Permission.valueOf(name.uppercase(Locale.getDefault()))