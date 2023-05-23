package net.zhuruoling.omms.crystal.event


import net.zhuruoling.omms.crystal.main.DebugOptions
import net.zhuruoling.omms.crystal.util.createLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors


class EventDispatcher {
    private val eventMap: ConcurrentHashMap<Event, ArrayList<(EventArgs) -> Unit>> = ConcurrentHashMap()
    private val executor = Executors.newFixedThreadPool(4)
    private val logger = createLogger("EventDispatcher", DebugOptions.eventDebug())
    fun dispatchEvent(e: Event, args: EventArgs) {
        if (DebugOptions.eventDebug()) logger.info("[DEBUG] Dispatching Event ${e.id} with args $args")
        val handlers = eventMap[e]
        if (handlers == null) {
            //logger.warn("${e.id} not exist.")
        } else {
            handlers.forEach {
                try {
                    executor.submit {
                        try {
                            it(args)
                        }catch (e:Exception){
                            logger.error("An Exception occurred while dispatching events.", e)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("An Exception occurred while dispatching events.", e)
                }
            }
        }
    }

    fun registerHandler(e: Event, handler: (EventArgs) -> Unit) {
        if (DebugOptions.eventDebug()) logger.info("[DEBUG] Registering event $e with handler $handler")
        var handlers = eventMap[e]
        if (handlers == null) {
            handlers = arrayListOf()
            handlers.add(handler)
        } else {
            handlers.add(handler)
        }
        eventMap[e] = handlers
    }

    fun unregisterHandler(e: Event, handler: (EventArgs) -> Unit) {
        val handlers: ArrayList<(EventArgs) -> Unit> =
            eventMap[e] ?: throw NoSuchElementException("Event $e does not exist.")
        handlers.remove(handler)
        eventMap[e] = handlers
    }

    fun shutdown() {
        Thread.sleep(1024)
        executor.shutdown()
    }
}