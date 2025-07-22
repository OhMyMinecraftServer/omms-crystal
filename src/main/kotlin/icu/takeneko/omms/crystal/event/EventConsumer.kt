package icu.takeneko.omms.crystal.event

interface EventConsumer {
    suspend fun accept(e: Event)
}

class SuspendEventConsumer<T>(private val consumer: suspend Event.() -> Unit) : EventConsumer {
    override suspend fun accept(e: Event) {
        consumer(e)
    }
}

class NoSuspendEventConsumer<T>(private val consumer: Event.() -> Unit) : EventConsumer {
    override suspend fun accept(e: Event) {
        consumer(e)
    }
}
