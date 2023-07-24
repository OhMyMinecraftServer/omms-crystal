package net.zhuruoling.omms.crystal.main

import net.zhuruoling.omms.crystal.config.Config
import net.zhuruoling.omms.crystal.event.EventDispatcher
import net.zhuruoling.omms.crystal.event.EventLoop
import net.zhuruoling.omms.crystal.event.registerEvents
import net.zhuruoling.omms.crystal.i18n.*
import net.zhuruoling.omms.crystal.plugin.PluginManager
import net.zhuruoling.omms.crystal.util.WORKING_DIR
import net.zhuruoling.omms.crystal.util.getWorkingDir
import java.text.MessageFormat
import kotlin.io.path.div


fun main(args: Array<String>) {

    TranslateManager.addBuiltinTranslations()
    Config.load()
    SharedConstants.eventDispatcher = EventDispatcher()
    SharedConstants.eventLoop = EventLoop()
    SharedConstants.eventLoop.start()
    registerEvents()
    for (lang in builtinTranslationLanguages) {
        println("use lang $lang")
        SharedConstants.language = lang
        withTranslateContext("crystal") {
            println("no fmt test: " + tr("test"))
            println("fmt test: " + tr("fmt_test", "arg1", "arg2"))
            println("key not exist: " + tr("not_exist"))
        }
        println("translate keys:")
        TranslateManager.getOrCreateDefaultLanguageProvider(lang).getAllTranslates().forEach { (t, u) ->
            println("($t=$u)")
        }
    }
    SharedConstants.eventLoop.exit()
}