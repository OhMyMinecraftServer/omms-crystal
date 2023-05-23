package net.zhuruoling.omms.crystal.event

import net.zhuruoling.omms.crystal.main.DebugOptions
import net.zhuruoling.omms.crystal.main.SharedConstants
import net.zhuruoling.omms.crystal.util.createLogger
import org.slf4j.LoggerFactory
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

typealias EventHandler = ((EventArgs) -> Unit)

class EventLoop : Thread("EventLoop") {
    val logger = createLogger("EventDispatcher", DebugOptions.eventDebug())
    private val queue =
        PriorityBlockingQueue<Pair<Event, EventArgs>>(128) { p1, p2 -> p1.first.priority - p2.first.priority }
    private var stopped = false

    override fun run() {
        while (!stopped or queue.isNotEmpty()) {
            while (queue.isNotEmpty()) {
                val pair = queue.poll()
                SharedConstants.eventDispatcher.dispatchEvent(pair.first, pair.second)
            }
            yield()
        }
        SharedConstants.eventDispatcher.shutdown()
        logger.info("EventLoop stopped.")
    }

    fun exit() {
        stopped = true
    }

    fun dispatch(e: Event, args: EventArgs) {
        queue.put(Pair(e, args))
    }
}



