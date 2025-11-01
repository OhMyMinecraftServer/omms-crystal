package icu.takeneko.omms.crystal.main

import icu.takeneko.omms.crystal.CrystalServer
import icu.takeneko.omms.crystal.util.CrystalTask
import java.util.concurrent.locks.LockSupport

object MainThreadExecutor {
    private val tasks = ArrayDeque<CrystalTask<*>>()

    fun <R> execute(t: () -> R): CrystalTask<R> {
        return synchronized(tasks) {
            CrystalTask(t).also(tasks::add)
        }
    }

    fun run() {
        while (CrystalServer.shouldKeepRunning) {
            synchronized(tasks) {
                while (tasks.isNotEmpty()) {
                    for (function in tasks) {
                        function()
                    }
                    tasks.clear()
                }
            }
            LockSupport.parkNanos(1000)
        }
    }
}