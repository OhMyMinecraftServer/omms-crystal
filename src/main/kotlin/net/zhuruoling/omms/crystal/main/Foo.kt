package net.zhuruoling.omms.crystal.main

fun main() {
    val map = hashMapOf<String, Any>()
    map["wdnmd"] = 114514
    map["wdnmd,too"] = true
    map["yee"] = "Wdnmd"
    try {
        println(map.getOrWhat("wdnmd,too", false)) //return true
    }catch (e: Exception){
        e.printStackTrace()
    }
    try {
        println(map.getOrWhat("yee", "haaaaaa").uppercase())
    }catch (e: Exception){
        e.printStackTrace()
    }

    try {
        println(map.getOrWhat("wdnmd", false).not()) //will boom
    }catch (e: Exception){
        e.printStackTrace()
    }
}

fun <K,V,T> HashMap<K,V>.getOrWhat(k: K, default: T): T{
    return if (this.containsKey(k))
        this[k] as T
    else
        default
}

