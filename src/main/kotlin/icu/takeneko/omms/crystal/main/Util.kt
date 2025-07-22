package icu.takeneko.omms.crystal.main

import icu.takeneko.omms.crystal.util.constants.DebugOptions

inline fun ifMainDebug(block: () -> Unit) {
    if (DebugOptions.mainDebug()) block()
}
