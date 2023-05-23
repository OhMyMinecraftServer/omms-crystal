package net.zhuruoling.omms.crystal.util

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
import net.zhuruoling.omms.crystal.command.CommandUtil
import net.zhuruoling.omms.crystal.event.Event
import net.zhuruoling.omms.crystal.event.EventHandler
import net.zhuruoling.omms.crystal.main.SharedConstants
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.io.File
import java.util.*
import java.util.function.Consumer


const val PRODUCT_NAME = "Oh My Minecraft Server Crystal"

fun getWorkingDir(): String {
    val directory = File("")
    return directory.absolutePath
}

fun joinFilePaths(vararg pathComponent: String): String {
    val paths: Array<out String?> = pathComponent
    val path = StringBuilder()
    path.append(getWorkingDir())
    Arrays.stream(paths).toList().forEach(Consumer { x: String? ->
        path.append(File.separator)
        path.append(x)
    })
    return path.toString()
}

fun resolveCommand(command: String): Array<out String> {
    if (command.isEmpty()) throw IllegalArgumentException("Illegal command $command, to short or empty!")
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
    val logger = LoggerFactory.getLogger(name) as Logger
    logger.detachAndStopAllAppenders()
    val loggerContext: LoggerContext = logger.loggerContext
    val encoder = PatternLayoutEncoder()
    encoder.context = loggerContext
    encoder.pattern = pattern
    encoder.start()
    val filter = ThresholdFilter()
    filter.setLevel((if (debug) Level.DEBUG else Level.INFO).toString())
    filter.start()
    val appender: ConsoleAppender<ILoggingEvent> = ConsoleAppender<ILoggingEvent>()
    appender.context = loggerContext
    appender.encoder = encoder
    appender.addFilter(filter)
    appender.start()
    if (logToFile) {
        val fileEncoder = PatternLayoutEncoder()
        fileEncoder.context = loggerContext
        fileEncoder.pattern = fileLogPattern
        fileEncoder.start()
        val fileAppender = RollingFileAppender<ILoggingEvent>()
        val policy = SizeAndTimeBasedRollingPolicy<ILoggingEvent>()
        policy.context = loggerContext
        policy.setMaxFileSize(FileSize.valueOf("5 mb"))
        policy.fileNamePattern = "logs/%d{yyyy-MM-dd}.log"
        policy.maxHistory = 30
        policy.setParent(fileAppender)
        policy.start()
        val fileFilter = ThresholdFilter()
        filter.setLevel("INFO")
        filter.start()
        fileAppender.encoder = fileEncoder
        fileAppender.context = loggerContext
        fileAppender.rollingPolicy = policy
        fileAppender.addFilter(fileFilter)
        fileAppender.start()
        logger.addAppender(fileAppender)
    }
    logger.addAppender(appender)
    return logger
}

fun <S> unregisterCommand(command: LiteralArgumentBuilder<S>, dispatcher: CommandDispatcher<S>): String? =
    CommandUtil.unRegisterCommand(command, dispatcher)

fun registerEventHandler(e: Event, handler: EventHandler){
    SharedConstants.eventDispatcher.registerHandler(e, handler)
}
