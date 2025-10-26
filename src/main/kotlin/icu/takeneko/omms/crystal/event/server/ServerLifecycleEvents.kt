@file:Suppress("unused")

package icu.takeneko.omms.crystal.event.server

import icu.takeneko.omms.crystal.event.CancellableEvent
import icu.takeneko.omms.crystal.event.Event
import icu.takeneko.omms.crystal.foundation.ActionHost
import icu.takeneko.omms.crystal.CrystalServer
import java.nio.file.Path

class StartServerEvent(
    var launchCommand: String,
    var workingDir: Path
) : CancellableEvent()

class StopServerEvent(
    val actionHost: ActionHost = CrystalServer,
    var force: Boolean = false
) : CancellableEvent()

class ServerStoppingEvent : Event

class ServerStoppedEvent(val exitCode: Int, val actionHost: ActionHost) : Event

class ServerStartingEvent(val pid: Long, val version: String) : Event

class ServerStartedEvent(val timeElapsed: Double) : Event

class RconServerStartedEvent(val port: Int) : Event
