package icu.takeneko.omms.crystal.server

import icu.takeneko.omms.crystal.main.DebugOptions

inline fun ifServerDebug(block: () -> Unit) {
    if (DebugOptions.serverDebug()) block()
}