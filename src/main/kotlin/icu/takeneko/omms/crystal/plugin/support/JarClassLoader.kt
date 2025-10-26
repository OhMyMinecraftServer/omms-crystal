package icu.takeneko.omms.crystal.plugin.support

import icu.takeneko.omms.crystal.util.LoggerUtil
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLClassLoader
import java.util.zip.ZipFile

class JarClassLoader(parent: ClassLoader) : URLClassLoader(arrayOf(), parent) {
    private val logger = LoggerUtil.createLogger("JarClassLoader", true)

    fun loadJar(file: File) {
        scanClasses(file)
        logger.debug("Adding jar file {} to classpath", file)
        this.addURL(file.toURI().toURL())
    }

    fun scanClasses(file: File): List<ScanData> {
        val scanData = mutableListOf<ScanData>()
        ZipFile(file).use { zip ->
            for (entry in zip.entries().asSequence()) {
                if (entry.name.endsWith(".class")) {
                    val bytes = zip.getInputStream(entry).readBytes()
                    val classNode = ClassNode().apply {
                        ClassReader(bytes).accept(this@apply, 0)
                    }
                    scanData += ScanData(
                        entry.name.replace("/", "."),
                        classNode.visibleAnnotations.map { annotation ->
                            AnnotationData(
                                annotation.desc,
                                annotation.values.sliceAnnotationEntries()
                            )
                        },
                        classNode.methods.associate { method ->
                            MethodData(
                                method.access,
                                classNode.name,
                                method.name,
                                method.desc
                            ) to method.visibleAnnotations.map { annotation ->
                                AnnotationData(
                                    annotation.desc,
                                    annotation.values.sliceAnnotationEntries()
                                )
                            }
                        }
                    )
                }
            }
        }
        return scanData
    }

    private fun List<Any>?.sliceAnnotationEntries(): Map<String, Any> {
        if (this == null) return mapOf()
        return this.chunked(2).associate {
            it[0] as String to it[1]
        }
    }
}
