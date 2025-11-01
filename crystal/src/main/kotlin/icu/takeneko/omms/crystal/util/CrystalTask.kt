package icu.takeneko.omms.crystal.util

import java.util.concurrent.locks.LockSupport

class CrystalTask<R>(private val callback: () -> R) : () -> Unit {
    var finished: Boolean = false
        private set

    var result: Result<R>? = null

    fun markFinished(result: Result<R>) {
        finished = true
        this.result = result
    }

    fun join() {
        while (!this.finished) {
            LockSupport.parkNanos(1000)
        }
    }

    override fun invoke() {
        try {
            markFinished(Result.Success(callback()))
        } catch (t: Throwable) {
            markFinished(Result.Failure(t))
        }
    }
}