package icu.takeneko.omms.crystal.main

inline fun ifMainDebug(block: () -> Unit) {
    if (DebugOptions.mainDebug()) block()
}