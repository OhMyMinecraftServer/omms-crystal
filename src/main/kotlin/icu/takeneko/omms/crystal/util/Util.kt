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
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import icu.takeneko.omms.crystal.command.CommandUtil
import icu.takeneko.omms.crystal.event.Event
import icu.takeneko.omms.crystal.main.SharedConstants
import icu.takeneko.omms.crystal.plugin.api.annotations.EventHandler
import icu.takeneko.omms.crystal.util.constants.Directory.getWorkingDir
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.io.File
import java.lang.reflect.Method
import java.util.*


const val PRODUCT_NAME = "Oh My Minecraft Server Crystal"
const val VERSION = "0.1.0"

fun joinFilePaths(vararg pathComponent: String): String =
    buildString {
        append(getWorkingDir())
        pathComponent.forEach {
            append(File.separator)
            append(it)
        }
    }

fun resolveCommand(command: String): Array<out String> {
    if (command.isEmpty()) error("Illegal command $command, to short or empty!")
    val stringTokenizer = StringTokenizer(command)
    val list = mutableListOf<String>()
    while (stringTokenizer.hasMoreTokens()) {
        list.add(stringTokenizer.nextToken())
    }
    return list.toTypedArray()
}

fun createServerLogger(): Logger = createLoggerWithPattern(
    "[%cyan(%d{yyyy-MM-dd HH:mm:ss.SSS})] [%boldYellow(%marker)/%highlight(%level)]: %msg%n",
    "ServerLogger"
)


fun createLogger(name: String, debug: Boolean = false): Logger = createLoggerWithPattern(
    "[%cyan(%d{yyyy-MM-dd HH:mm:ss.SSS})] [%boldYellow(%thread)/%highlight(%level)]: %msg%n",
    name,
    true,
    "[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%thread/%level]: %msg%n", debug
)


fun createLoggerWithPattern(
    pattern: String,
    name: String,
    logToFile: Boolean = false,
    fileLogPattern: String = "",
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
            this.pattern = fileLogPattern
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

fun <S> unregisterCommand(command: LiteralArgumentBuilder<S>, dispatcher: CommandDispatcher<S>): String? =
    CommandUtil.unRegisterCommand(command, dispatcher)

fun registerEventHandler(e: Event, handler: EventHandler) {
    SharedConstants.eventDispatcher.registerHandler(e, handler)
}

inline fun <reified A : Annotation> Class<*>.methodsWithAnnotation(): List<Method> =
    declaredMethods.filter { it.isAnnotationPresent(A::class.java) }