package icu.takeneko.omms.crystal.util

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

object LoggerUtil {
    fun createLogger(name: String, debug: Boolean = false): Logger = createLoggerWithPattern(
        "[%cyan(%d{yyyy-MM-dd HH:mm:ss.SSS})] [%boldYellow(%thread)/%highlight(%level)]: %green(\\(%logger\\)) %msg%n",
        name,
        true,
        "[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%thread/%level]: (%logger) %msg%n",
        debug
    )

    fun createLoggerWithPattern(
        pattern: String,
        name: String,
        logToFile: Boolean = false,
        logPattern: String = "",
        debug: Boolean = false
    ): Logger {
        val logger = (LoggerFactory.getLogger(name) as Logger).apply { detachAndStopAllAppenders() }

        val loggerContext: LoggerContext = logger.loggerContext
        val encoder = PatternLayoutEncoder().apply {
            this.context = loggerContext
            this.pattern = pattern
            start()
        }

        val filter = ThresholdFilter().apply {
            setLevel((if (debug) Level.DEBUG else Level.INFO).toString())
            start()
        }

        val appender = ConsoleAppender<ILoggingEvent>().apply {
            this.context = loggerContext
            this.encoder = encoder
            addFilter(filter)
            start()
        }

        if (logToFile) {
            val fileEncoder = PatternLayoutEncoder().apply {
                this.context = loggerContext
                this.pattern = logPattern
                start()
            }

            val fileAppender = RollingFileAppender<ILoggingEvent>()

            val policy = SizeAndTimeBasedRollingPolicy<ILoggingEvent>().apply {
                this.context = loggerContext
                setMaxFileSize(FileSize.valueOf("5 mb"))
                this.fileNamePattern = "logs/%d{yyyy-MM-dd}.log"
                this.maxHistory = 30
                setParent(fileAppender)
                start()
            }

            val fileFilter = ThresholdFilter()

            filter.apply {
                setLevel("INFO")
                start()
            }
            fileAppender.apply {
                this.encoder = fileEncoder
                this.context = loggerContext
                this.rollingPolicy = policy
                addFilter(fileFilter)
                start()
            }
            logger.addAppender(fileAppender)
        }
        logger.addAppender(appender)
        return logger
    }
}
