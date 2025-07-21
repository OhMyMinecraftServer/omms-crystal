package icu.takeneko.omms.crystal.plugin.support

import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLClassLoader
import java.util.zip.ZipFile

class JarClassLoader(parent: ClassLoader) : URLClassLoader(arrayOf(), parent) {
    private val logger = LoggerFactory.getLogger("JarClassLoader")

    fun loadJar(file: File) {
        scanClasses(file)
        this.addURL(file.toURI().toURL())
    }

    fun scanClasses(file: File): ScanData {
        ZipFile(file).use {
            for (entry in it.entries().asSequence()) {
                if (entry.name.endsWith(".class")) {

                }
            }
        }
    }
}