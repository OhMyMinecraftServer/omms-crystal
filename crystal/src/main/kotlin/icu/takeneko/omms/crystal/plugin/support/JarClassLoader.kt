package icu.takeneko.omms.crystal.plugin.support

import icu.takeneko.omms.crystal.util.LoggerUtil
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.net.URI
import java.net.URLClassLoader
import java.util.zip.ZipFile

class JarClassLoader(parent: ClassLoader) : URLClassLoader(arrayOf(), parent) {
    private val logger = LoggerUtil.createLogger("JarClassLoader", true)
    val allScanData = mutableMapOf<URI, List<ScanData>>()
    private val visitors = mutableListOf<JarVisitor>()

    fun loadJar(file: File) {
        val uri = file.toURI()
        val url = uri.toURL()
        if (this.urLs.contains(url)) {
            return
        }
        val result = scanClasses(file)
        logger.debug("Adding jar file {} to classpath", file)
        allScanData += uri to result
        this.addURL(url)
    }

    fun scanClasses(file: File): List<ScanData> {
        val scanData = mutableListOf<ScanData>()
        visitors.forEach { it.visitJarFile(file) }
        ZipFile(file).use { zip ->
            for (entry in zip.entries().asSequence()) {
                visitors.forEach { it.visitJarEntry(zip, entry) }
                if (entry.name.endsWith(".class")) {
                    val bytes = zip.getInputStream(entry).readBytes()
                    val classNode = ClassNode().apply {
                        ClassReader(bytes).accept(this@apply, 0)
                    }
                    scanData += ScanData(
                    entry.name.replace("/", ".").removeSuffix(".class"),
                        classNode.visibleAnnotations?.map { annotation ->
                            AnnotationData(
                                annotation.desc,
                                annotation.values.sliceAnnotationEntries()
                            )
                        }.orEmpty(),
                        classNode.methods.associate { method ->
                            MethodData(
                                method.access,
                                classNode.name,
                                method.name,
                                method.desc
                            ) to method.visibleAnnotations?.map { annotation ->
                                AnnotationData(
                                    annotation.desc,
                                    annotation.values.sliceAnnotationEntries()
                                )
                            }.orEmpty()
                        }
                    )
                }
            }
        }
        return scanData
    }

    fun addVisitor(visitor: JarVisitor) {
        visitors += visitor
    }

    private fun List<Any>?.sliceAnnotationEntries(): Map<String, Any> {
        if (this == null) return mapOf()
        return this.chunked(2).associate {
            it[0] as String to it[1]
        }
    }
}
