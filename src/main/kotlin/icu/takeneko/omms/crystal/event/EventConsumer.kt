package icu.takeneko.omms.crystal.event

interface EventConsumer {
    suspend fun accept(e: Event)
}

class SuspendEventConsumer<T>(private val cons: suspend Event.() -> Unit) : EventConsumer {
    override suspend fun accept(e: Event) {
        cons(e)
    }
}

class NoSuspendEventConsumer<T>(private val cons: Event.() -> Unit) : EventConsumer {
    override suspend fun accept(e: Event) {
        cons(e)
    }
}