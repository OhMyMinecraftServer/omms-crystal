package icu.takeneko.omms.crystal.main

import icu.takeneko.omms.crystal.plugin.PluginException
import java.io.IOException
import java.io.InputStream
import java.lang.module.ModuleDescriptor.Version
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlin.io.path.isDirectory

class ServerClassLoader(val array: Array<URL>): URLClassLoader(array) {
    override fun loadClass(name: String): Class<*> {
        println(name)
        if ("org.apache.logging" in name){
            throw ClassNotFoundException("Class $name is forbidden.")
        }
        return super.loadClass(name)
    }
}

class JarServerLauncher(
    val librariesPath: Path,
    val versionsPath: Path,
    val serverJarPath: Path,
    val workingDir: Path
) {

    fun <R> useInJarFile(fileFullPath: Path, fileName: String, consumer: InputStream.() -> R): R =
        ZipFile(fileFullPath.toFile()).use {
            try {
                val entry = it.getEntry(fileName)
                val inputStream = it.getInputStream(entry)
                val r = consumer(inputStream)
                inputStream.close()
                r
            } catch (e: PluginException) {
                throw e
            } catch (e: ZipException) {
                throw PluginException("ZIP format error occurred while reading plugin jar file.", e)
            } catch (e: IOException) {
                throw PluginException("I/O error occurred while reading plugin jar file.", e)
            } catch (e: Exception) {
                throw PluginException("Cannot read plugin jar file.", e)
            }
        }

    private val librariesJarPaths = mutableListOf<Path>()
    private lateinit var classLoader: ServerClassLoader
    private lateinit var mainClass:String
    fun loadJars() {
        val jarPaths =
            Files.walk(librariesPath).filter {
                !it.isDirectory() && !it.toString().contains("log4j") && !it.toString()
                    .contains("slf4j") && !it.toString().contains("logging")
            }.toList().toMutableList()
        jarPaths += Files.walk(versionsPath).filter { !it.isDirectory() }.toList()
        jarPaths.add(serverJarPath)
        println(jarPaths.joinToString("\n"))
        mainClass = useInJarFile(serverJarPath, "META-INF/MANIFEST.MF") {
            this.bufferedReader().readLines().forEach {
                val item = it.split(": ")
                if (item[0] == "Main-Class") {
                    return@useInJarFile item.subList(1, item.size).joinToString(": ")
                }
            }
            return@useInJarFile null
        } ?: return
        println(mainClass)
        classLoader = ServerClassLoader(jarPaths.map { it.toFile().toURI().toURL() }.toTypedArray())
    }

    fun runMain() {
        val clazz = classLoader.loadClass(mainClass)
        clazz.declaredMethods.forEach {
            println(it)
        }
        clazz.getDeclaredMethod("main", Array<String>::class.java)
            .invoke(null, arrayOf(""))
    }
}

fun main(args: Array<String>) {
    val serverDir = Path.of(".")
    val jarServerLauncher = JarServerLauncher(
        serverDir.resolve("libraries"),
        serverDir.resolve("versions"),
        serverDir.resolve("fabric.jar"),
        serverDir
    )
    jarServerLauncher.loadJars()
    jarServerLauncher.runMain()
}
