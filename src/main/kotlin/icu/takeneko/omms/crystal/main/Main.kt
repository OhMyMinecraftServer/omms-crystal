package icu.takeneko.omms.crystal.main

import icu.takeneko.omms.crystal.main.CrystalServer.consoleHandler
import icu.takeneko.omms.crystal.main.CrystalServer.rconListener
import icu.takeneko.omms.crystal.permission.PermissionManager
import kotlin.concurrent.thread

fun exit() {
    thread(start = true, name = "ShutdownThread") {
        // PluginManager.unloadAll()
        rconListener?.stop()
        PermissionManager.save()
        // TODO
        // eventLoop.exit()
        // eventDispatcher.shutdown()
        consoleHandler.interrupt()
    }
}
