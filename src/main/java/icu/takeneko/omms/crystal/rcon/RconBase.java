package icu.takeneko.omms.crystal.rcon;

import java.util.concurrent.atomic.AtomicInteger;

import icu.takeneko.omms.crystal.util.UtilKt;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public abstract class RconBase implements Runnable {
    private static final Logger logger = UtilKt.createLogger("RconBase", false);
    private static final AtomicInteger threadCounter = new AtomicInteger(0);
    protected volatile boolean running;
    protected final String description;
    @Nullable
    protected Thread thread;

    protected RconBase(String description) {
        this.description = description;
    }

    public synchronized boolean start() {
        if (!this.running) {
            this.running = true;
            this.thread = new Thread(this, description + " #" + threadCounter.incrementAndGet());
            this.thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler(logger));
            this.thread.start();
            logger.info("Thread {} started", this.description);
        }
        return true;
    }

    public synchronized void stop() {
        this.running = false;
        if (null != this.thread) {
            int i = 0;

            while(this.thread.isAlive()) {
                try {
                    this.thread.join(1000L);
                    ++i;
                    if (i >= 5) {
                        logger.warn("Waited {} seconds attempting force stop!", i);
                    } else if (this.thread.isAlive()) {
                        logger.warn("Thread {} ({}) failed to exit after {} second(s)", this, this.thread.getState(), i, new Exception("Stack:"));
                        this.thread.interrupt();
                    }
                } catch (InterruptedException ignored) {
                }
            }

            logger.info("Thread {} stopped", this.description);
            this.thread = null;
        }
    }

    public boolean isRunning() {
        return this.running;
    }

    public static class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        private final Logger logger;

        public UncaughtExceptionHandler(Logger logger) {
            this.logger = logger;
        }

        public void uncaughtException(Thread thread, Throwable throwable) {
            this.logger.error("Caught previously unhandled exception :");
            this.logger.error(thread.getName(), throwable);
        }
    }

}
