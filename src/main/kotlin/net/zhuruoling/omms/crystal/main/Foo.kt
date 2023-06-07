package net.zhuruoling.omms.crystal.main

import net.zhuruoling.omms.crystal.util.WORKING_DIR
import kotlin.io.path.div

fun main(args: Array<String>) {
    println(WORKING_DIR / "plugins" / "test.jar")
}