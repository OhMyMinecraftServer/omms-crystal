package icu.takeneko.omms.crystal.main

import icu.takeneko.omms.crystal.plugin.PluginException
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import java.util.zip.ZipException
import kotlin.io.path.isDirectory

class ServerClassLoader(val array: Array<URL>) : URLClassLoader(array) {
    override fun loadClass(name: String): Class<*> {
        if ("org.apache.logging" in name) {
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
    private val librariesJarPaths = mutableListOf<Path>()
    private lateinit var classLoader: ServerClassLoader
    private lateinit var mainClass: String

    fun loadJars() {
        val jarPaths = buildList {
            addAll(
                Files.walk(librariesPath)
                    .filter { Files.isRegularFile(it) }
                    .filter {
                        it.toString().let { s ->
                            !s.contains("log4j", true) &&
                                    !s.contains("slf4j", true) &&
                                    !s.contains("logging", true)
                        }
                    }.toList()
            )
            addAll(Files.walk(versionsPath).filter { !it.isDirectory() }.toList())
            add(serverJarPath)
        }
        println(jarPaths.joinToString("\n"))

        mainClass = JarFile(serverJarPath.toFile()).use { jar ->
            try {
                jar.manifest.mainAttributes.getValue("Main-Class")
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
        println(mainClass)
        classLoader = ServerClassLoader(jarPaths.map { it.toFile().toURI().toURL() }.toTypedArray())
    }

    fun runMain() {
        val clazz = classLoader.loadClass(mainClass)
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
