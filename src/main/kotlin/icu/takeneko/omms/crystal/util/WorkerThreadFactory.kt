package icu.takeneko.omms.crystal.util

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class WorkerThreadFactory : ThreadFactory {
    private val factory: ThreadFactory = Executors.defaultThreadFactory()
    private val threadCount = AtomicInteger(1)
    override fun newThread(r: Runnable): Thread =
        factory.newThread(r).apply { this.name = "Worker-Crystal-${threadCount.getAndIncrement()}" }
}