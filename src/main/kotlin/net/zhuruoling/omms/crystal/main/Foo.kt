package net.zhuruoling.omms.crystal.main


fun main() {
   Foo::class.java.methods.forEach {
       println(it.toGenericString())
   }
}

class Foo{
    fun method(){

    }
}