package icu.takeneko.omms.crystal.rcon

import icu.takeneko.omms.crystal.util.LoggerUtil.createLogger
import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.Volatile

abstract class RconBase(val description: String?) : Runnable {
    @Volatile
    protected var running: Boolean = false

    protected var thread: Thread? = null

    @Synchronized
    fun start(): Boolean {
        if (!this.running) {
            this.running = true
            this.thread = Thread(this, description + " #" + threadCounter.incrementAndGet())
            this.thread!!.setUncaughtExceptionHandler(UncaughtExceptionHandler(logger))
            this.thread!!.start()
            logger.info("Thread {} started", this.description)
        }
        return true
    }

    @Synchronized
    open fun stop() {
        this.running = false
        if (null != this.thread) {
            var i = 0

            while (this.thread!!.isAlive) {
                try {
                    this.thread!!.join(1000L)
                    ++i
                    if (i >= 5) {
                        logger.warn("Waited {} seconds attempting force stop!", i)
                    } else if (this.thread!!.isAlive) {
                        logger.warn(
                            "Thread {} ({}) failed to exit after {} second(s)",
                            this,
                            this.thread!!.state,
                            i,
                            Exception("Stack:")
                        )
                        this.thread!!.interrupt()
                    }
                } catch (ignored: InterruptedException) {
                }
            }

            logger.info("Thread {} stopped", this.description)
            this.thread = null
        }
    }

    fun isRunning(): Boolean = this.running

    companion object {
        private val logger: Logger = createLogger("RconBase", false)
        private val threadCounter: AtomicInteger = AtomicInteger(0)

        class UncaughtExceptionHandler(private val logger: Logger) : Thread.UncaughtExceptionHandler {
            override fun uncaughtException(thread: Thread, throwable: Throwable?) {
                this.logger.error("Caught previously unhandled exception :")
                this.logger.error(thread.name, throwable)
            }
        }
    }

}