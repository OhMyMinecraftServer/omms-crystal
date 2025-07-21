package icu.takeneko.omms.crystal.permission

import icu.takeneko.omms.crystal.util.file.FileUtil.JSON
import icu.takeneko.omms.crystal.util.file.readTextWithBuffer
import icu.takeneko.omms.crystal.util.file.writeTextWithBuffer
import icu.takeneko.omms.crystal.util.file.FileUtil.joinFilePaths
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists

object PermissionManager {
    private val cache = ConcurrentHashMap<String, Permission>()

    val players =
        EnumMap<Permission, MutableSet<String>>(Permission::class.java).apply {
            Permission.entries.forEach { level -> this[level] = ConcurrentHashMap.newKeySet() }
        }

    var defaultLevel = Permission.USER
        private set

    private val file = Path(joinFilePaths("permissions.json"))

    @Synchronized
    fun init() {
        if (!file.exists()) {
            file.createFile()
            file.writeTextWithBuffer(JSON.encodeToString(PermissionStorage()))
        }

        val storage = JSON.decodeFromString<PermissionStorage>(file.readTextWithBuffer())

        defaultLevel = storage.defaultPermissionLevel
        players.values.forEach(MutableSet<String>::clear)
        cache.clear()

        Permission.entries.forEach { level ->
            when (level) {
                Permission.OWNER -> storage.owner
                Permission.ADMIN -> storage.admin
                Permission.USER  -> storage.user
                Permission.GUEST -> storage.guest
            }.forEach { player ->
                players[level]!!.add(player)
                cache[player] = level
            }
        }
    }

    @Synchronized
    fun save() {
        file.writeTextWithBuffer(JSON.encodeToString(getPermissionStorage()))
    }

    fun getPermissionStorage(): PermissionStorage = PermissionStorage(
        defaultLevel,
        owner = players[Permission.OWNER]!!.sorted(),
        admin = players[Permission.ADMIN]!!.sorted(),
        user  = players[Permission.USER]!!.sorted(),
        guest = players[Permission.GUEST]!!.sorted()
    )

    fun remove(player: String) {
        movePlayer(player, null)
    }

    operator fun contains(player: String) = cache.containsKey(player)

    operator fun get(player: String) = cache[player] ?: defaultLevel

    operator fun set(player: String, level: Permission) = movePlayer(player, level)

    private fun movePlayer(player: String, newLevel: Permission?) {
        val oldLevel = cache[player]
        if (oldLevel != null) {
            players[oldLevel]!!.remove(player)
        }
        if (newLevel != null) {
            players[newLevel]!!.add(player)
            cache[player] = newLevel
        } else {
            cache.remove(player)
        }
    }
}
