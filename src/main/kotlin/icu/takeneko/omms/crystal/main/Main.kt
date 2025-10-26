package icu.takeneko.omms.crystal.main

import icu.takeneko.omms.crystal.CrystalServer
import java.util.concurrent.locks.LockSupport

fun main(args: Array<String>) {
    CrystalServer.bootstrap(args)
    CrystalServer.run()
    while (CrystalServer.shouldKeepRunning) {
        LockSupport.parkNanos(1000)
    }
}