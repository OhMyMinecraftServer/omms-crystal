package net.zhuruoling.omms.crystal.main

import cn.hutool.core.io.FileUtil
import cn.hutool.core.io.unit.DataSizeUtil
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.zhuruoling.omms.crystal.command.CommandManager
import net.zhuruoling.omms.crystal.command.literal
import net.zhuruoling.omms.crystal.config.Config
import net.zhuruoling.omms.crystal.event.*
import net.zhuruoling.omms.crystal.plugin.gsonForPluginMetadata
import net.zhuruoling.omms.crystal.rcon.RconClient
import net.zhuruoling.omms.crystal.server.ServerController
import net.zhuruoling.omms.crystal.util.createLogger
import net.zhuruoling.omms.crystal.util.joinFilePaths
import org.apache.commons.io.IOUtils
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.util.Arrays
import kotlin.concurrent.thread
import kotlin.system.exitProcess

data class Version(val id: String, val url: String)


data class VersionManifest(val versions: List<Version>)

data class DownloadItem(val url: String, val size: Int)
data class Downloads(val server: DownloadItem)
data class VersionJson(val id: String, val downloads: Downloads)
data class VersionJsonStorage(val version: String, val size: Int, val url: String)

val versionJsons = mutableListOf<VersionJson>()
val versionJsonStorages = mutableListOf<VersionJsonStorage>()

fun main(args: Array<String>) {
    val timeBegin = System.currentTimeMillis()
    val shipNotDownloaded = mutableListOf<Int>()
    val saveNotDownloaded = mutableListOf<Int>()
    var shipEnds = false
    var saveEnds = false
    var countShip = 0
    var countSave = 0
    println(args.joinToString())
    args.forEach {
        if (it.contains("ship:")) {
            countShip = it.split(":")[1].toInt()
        }
        if (it.contains("save:")) {
            countSave = it.split(":")[1].toInt()
        }
    }
    var count = 0
    thread(start = true) {
        for (i in 0 until countSave) {
            File(joinFilePaths("save", "1" + "$i".padStart(6, '0') + ".xml")).run {
                count++
                /*if (i % 1000 == 0)*/ println("[save] Check ${this.absolutePath} exists: ${exists()}")
                if (!exists()) {
                    saveNotDownloaded += i
                }
            }
        }
        saveEnds = true
    }
    thread(start = true) {
        for (i in 0 until countShip) {
            File(joinFilePaths("ship", "1" + "$i".padStart(6, '0') + ".xml")).run {
                count++
                /*if (i % 1000 == 0)*/ println("[ship] Check ${this.absolutePath} exists: ${exists()}")
                if (!exists()) {
                    shipNotDownloaded += i
                }
            }
        }
        shipEnds = true
    }
    while (!shipEnds || !saveEnds) Thread.sleep(50)
    val timeEnd = System.currentTimeMillis()
    println("Ship Not Downloaded: $shipNotDownloaded")
    println("Save Not Downloaded: $saveNotDownloaded")
    println("Finished $count files check in ${timeEnd - timeBegin}ms")
    println("Writing Files.")
    File(joinFilePaths("ships.json")).run {
        if (this.exists())
            this.deleteRecursively()
        this.createNewFile()
        writer().use {
            GsonBuilder().setPrettyPrinting().create().toJson(shipNotDownloaded, it)
        }
    }
    File(joinFilePaths("saves.json")).run {
        if (this.exists())
            this.deleteRecursively()
        this.createNewFile()
        writer().use {
            GsonBuilder().setPrettyPrinting().create().toJson(saveNotDownloaded, it)
        }
    }
    println("Done.")
}