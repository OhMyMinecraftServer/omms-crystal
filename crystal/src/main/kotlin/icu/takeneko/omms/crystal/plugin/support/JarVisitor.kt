package icu.takeneko.omms.crystal.plugin.support

import java.io.File
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

interface JarVisitor {
    fun visitJarFile(file: File)

    fun visitJarEntry(file: ZipFile, entry: ZipEntry)
}