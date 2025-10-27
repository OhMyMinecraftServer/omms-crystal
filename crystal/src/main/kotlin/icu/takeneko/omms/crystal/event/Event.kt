@file:Suppress("unused")

package icu.takeneko.omms.crystal.event

interface Event

interface PluginBusEvent : Event

open class CancellableEvent : Event {
    var isCancelled = false
}
