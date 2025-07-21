package icu.takeneko.omms.crystal.plugin.support

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
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

    fun scanClasses(file: File): List<ScanData> {
        val scanDatas = mutableListOf<ScanData>()
        ZipFile(file).use {
            for (entry in it.entries().asSequence()) {
                if (entry.name.endsWith(".class")) {
                    val bytes = it.getInputStream(entry).readBytes()
                    val classNode = ClassNode().apply {
                        ClassReader(bytes).accept(this@apply, 0)
                    }
                    scanDatas += ScanData(
                        entry.name.replace("/", "."),
                        classNode.visibleAnnotations.map {
                            AnnotationData(
                                it.desc,
                                it.values.sliceAnnotationEntries()
                            )
                        },
                        classNode.methods.associate {
                            MethodData(
                                it.access,
                                classNode.name,
                                it.name,
                                it.desc
                            ) to it.visibleAnnotations.map {
                                AnnotationData(
                                    it.desc,
                                    it.values.sliceAnnotationEntries()
                                )
                            }
                        }
                    )
                }
            }
        }
        return scanDatas
    }

    private fun List<Any>?.sliceAnnotationEntries(): Map<String, Any> {
        if (this == null) return mapOf()
        return this.chunked(2).associate {
            it[0] as String to it[1]
        }
    }
}