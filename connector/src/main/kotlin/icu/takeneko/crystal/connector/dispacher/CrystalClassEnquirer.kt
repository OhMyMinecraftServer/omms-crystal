package icu.takeneko.crystal.connector.dispacher

import icu.takeneko.omms.crystal.plugin.support.JarClassLoader
import jep.ClassEnquirer
import jep.ClassList

class CrystalClassEnquirer : ClassEnquirer {
    private val classList = ClassList.getInstance()
    private val currentClassLoader: JarClassLoader = CrystalClassEnquirer::class.java.classLoader as JarClassLoader
    private val allClasses = currentClassLoader.allScannedClasses.toList()
    private val javaPackageLookupCache = mutableMapOf<String, Boolean>()
    private val classNamesLookupCache = buildMap {
        for (name in allClasses) {
            val idx = name.indexOfLast { it == '/' }
            if (idx == -1) {
                this.computeIfAbsent("") { mutableListOf() } += name
                continue
            }
            val pkg = name.substring(0, idx)
            val clz = name.substring(idx + 1)
            this.computeIfAbsent(pkg) { mutableListOf() } += clz
        }
        for (s in ClassEnquirer.RESTRICTED_PKG_NAMES) {
            this -= s
        }
    }

    private val subPackageLookupCache: Map<String, MutableList<String>> = buildMap {
        for (name in allClasses) {
            val pkg = name.split('/').toMutableList()
            if (pkg.size > 1) {
                pkg.removeLast()
            }
            var fullPkg = ""
            pkg.subList(0, pkg.size - 1).forEachIndexed { index, string ->
                fullPkg += if (fullPkg.isEmpty()) string else ".$string"
                this.computeIfAbsent(fullPkg) { mutableListOf() } += pkg[index + 1]
            }
        }
        for (s in ClassEnquirer.RESTRICTED_PKG_NAMES) {
            this -= s
        }
    }


    override fun isJavaPackage(name: String): Boolean = javaPackageLookupCache.computeIfAbsent(name) { key ->
        currentClassLoader.allScannedClasses.any { it.startsWith(key.replace(".", "/")) }
    } || classList.isJavaPackage(name)

    override fun getClassNames(pkgName: String): Array<String> {
        return (classNamesLookupCache[pkgName].orEmpty() + classList.getClassNames(pkgName)).toTypedArray()
    }

    override fun getSubPackages(pkgName: String): Array<String> {
        return (subPackageLookupCache[pkgName].orEmpty() + classList.getSubPackages(pkgName)).toTypedArray()
    }
}