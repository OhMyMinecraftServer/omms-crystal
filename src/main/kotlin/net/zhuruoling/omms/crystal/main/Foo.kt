package net.zhuruoling.omms.crystal.main

import net.zhuruoling.omms.crystal.config.Config
import net.zhuruoling.omms.crystal.event.EventDispatcher
import net.zhuruoling.omms.crystal.event.EventLoop
import net.zhuruoling.omms.crystal.event.registerEvents
import net.zhuruoling.omms.crystal.i18n.*
import net.zhuruoling.omms.crystal.plugin.PluginManager
import net.zhuruoling.omms.crystal.util.WORKING_DIR
import java.text.MessageFormat
import kotlin.io.path.div

fun main(args: Array<String>) {
    addBuiltinTranslations()
    Config.load()
    SharedConstants.eventDispatcher = EventDispatcher()
    SharedConstants.eventLoop = EventLoop()
    SharedConstants.eventLoop.start()
    registerEvents()
    PluginManager.init()
    //PluginManager.loadAll()
    val langEn = Identifier("en:us")
    val langZh = Identifier("zh:cn")
    SharedConstants.language = langEn
    val key = Identifier("crystal:test")
    val mgr = TranslateManager
    println(mgr.translate(TranslateKey(langEn, key)).translate)
    println(mgr.translate(TranslateKey(langZh, key)).translate)
    SharedConstants.language = langEn
    withTranslateContext("simple_op"){
        println(tr("set_op", "ZhuRuoLing"))
    }
    SharedConstants.language = langZh
    withTranslateContext("simple_op"){
        println(tr("set_op", "WDNMD"))
    }
    SharedConstants.eventLoop.exit()
}