package icu.takeneko.omms.crystal.permission

import java.util.*

enum class Permission {
    GUEST,
    USER,
    ADMIN,
    OWNER;

    companion object {
        fun from(name: String): Permission =
            runCatching { valueOf(name.uppercase(Locale.getDefault())) }.getOrDefault(USER)
    }
}

fun Permission.isAtLeast(other: Permission): Boolean = this.ordinal >= other.ordinal
