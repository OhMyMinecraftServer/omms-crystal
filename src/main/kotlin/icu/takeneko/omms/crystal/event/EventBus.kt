package icu.takeneko.omms.crystal.event

import icu.takeneko.omms.crystal.util.LoggerUtil
import icu.takeneko.omms.crystal.util.constants.DebugOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.coroutines.Continuation

@Suppress("unused")
class EventBus(
    private val coroutineScope: CoroutineScope,
    private val eventBaseClass: Class<out Any> = Event::class.java
) {
    private val logger = LoggerUtil.createLogger("EventBus", DebugOptions.eventDebug())

    private val lookup = MethodHandles.lookup()

    private val continuationClass = Continuation::class.java

    private val eventSubscribers: MutableMap<Class<out Event>, MutableMap<EventPriority, MutableList<EventConsumer>>> =
        mutableMapOf()

    fun getSubscribers(clazz: Class<out Event>): MutableMap<EventPriority, MutableList<EventConsumer>> =
        eventSubscribers.computeIfAbsent(clazz) { mutableMapOf() }

    fun getSubscribers(clazz: Class<out Event>, priority: EventPriority): MutableList<EventConsumer> =
        getSubscribers(clazz).computeIfAbsent(priority) { mutableListOf() }

    fun register(obj: Any) {
        if (obj !is Class<*>) {
            registerInstanced(obj)
            return
        }
        val (normal, sus) = filterMethods(obj, true)
        registerMethods(normal, sus, true, null)
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractEventClass(mt: MethodHandle, isStatic: Boolean): Class<out Event> {
        val arg = mt.type().parameterType(if (isStatic) 0 else 1)
        if (eventBaseClass.isAssignableFrom(arg)) {
            return arg as Class<out Event>
        }
        error("Could not extract argument class from $mt")
    }

    private fun registerMethods(
        normal: List<Pair<Method, EventPriority>>,
        suspended: List<Pair<Method, EventPriority>>,
        isStatic: Boolean,
        instance: Any?
    ) {
        normal.forEach { (method, priority) ->
            val handle = lookup.unreflect(method)
            val eventClass = extractEventClass(handle, isStatic)
            getSubscribers(eventClass, priority).add(
                NoSuspendEventConsumer<Event> {
                    if (isStatic) {
                        handle.invoke(this)
                        return@NoSuspendEventConsumer
                    }
                    handle.invoke(instance, this)
                }
            )
        }

        suspended.forEach { (method, priority) ->
            val handle = lookup.unreflect(method)
            val eventClass = extractEventClass(handle, isStatic)
            getSubscribers(eventClass, priority).add(
                SuspendEventConsumer<Event> {
                    if (isStatic) {
                        return@SuspendEventConsumer kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn {
                            handle.invoke(this, it)
                        }
                    }
                    kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn {
                        handle.invoke(instance, this, it)
                    }
                }
            )
        }
    }

    private fun registerInstanced(obj: Any) {
        val (normal, sus) = filterMethods(obj::class.java, false)
        registerMethods(normal, sus, false, obj)
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun filterMethods(
        clazz: Class<*>,
        isStatic: Boolean
    ): Pair<List<Pair<Method, EventPriority>>, List<Pair<Method, EventPriority>>> {
        return clazz.declaredMethods
            .filter { it.isAnnotationPresent(SubscribeEvent::class.java) }
            .filter {
                when {
                    Modifier.isStatic(it.modifiers) == isStatic -> true
                    else -> {
                        if (isStatic) {
                            logger.error(
                                "Expected @SubscribeEvent method {} to be static because register()" +
                                        " was called with a class type. Either make the method static," +
                                        " or call register() with an instance of {}.",
                                it,
                                clazz
                            )
                        } else {
                            logger.error(
                                "Expected @SubscribeEvent method {} to NOT be static because " +
                                        "register() was called with an instance type. " +
                                        "Either make the method non-static, or call register({}.class).",
                                it,
                                clazz.simpleName
                            )
                        }
                        false
                    }
                }
            }.mapNotNull { method ->
                val params = method.parameters
                if (params.isEmpty()) return@mapNotNull null

                val firstParamType = params.first().type
                val matchesEvent = eventBaseClass.isAssignableFrom(firstParamType)
                when (params.size) {
                    1 if matchesEvent -> method to true
                    2 if matchesEvent && continuationClass.isAssignableFrom(params[1].type) -> method to false

                    else -> null
                }
            }.partition { (_, state) -> state }.let { (first, second) ->
                fun List<Pair<Method, Boolean>>.withPriority() =
                    map { (m) -> m to m.getAnnotation(SubscribeEvent::class.java).priority }

                first.withPriority() to second.withPriority()
            }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Event> subscribe(
        priority: EventPriority = EventPriority.NORMAL,
        noinline fn: T.() -> Unit
    ) {
        validateSuitableClass(T::class.java)
        getSubscribers(T::class.java, priority).add(NoSuspendEventConsumer<T>(fn as Event.() -> Unit))
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Event> subscribeSuspend(
        priority: EventPriority = EventPriority.NORMAL,
        noinline fn: suspend T.() -> Unit
    ) {
        validateSuitableClass(T::class.java)
        getSubscribers(T::class.java, priority).add(SuspendEventConsumer<T>(fn as suspend Event.() -> Unit))
    }

    private fun validateSuitableClass(e: Class<*>) {
        if (!eventBaseClass.isAssignableFrom(e)) {
            error("This bus only accepts subclasses of $eventBaseClass, which $e is not.")
        }
    }

    fun dispatch(e: Event) {
        validateSuitableClass(e.javaClass)
        coroutineScope.launch {
            dispatchSuspend(e)
        }
    }

    suspend fun dispatchSuspend(e: Event) {
        validateSuitableClass(e.javaClass)
        for (priority in EventPriority.entries) {
            for (function in getSubscribers(e.javaClass, priority)) {
                try {
                    function.accept(e)
                } catch (t: Throwable) {
                    logger.error("An exception was thrown while posing event {} to {}.", e, function, t)
                }
            }
        }
    }
}
