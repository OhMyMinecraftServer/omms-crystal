import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm") version "1.7.10"
    java
    application
}


group = "net.zhuruoling.omms"
version = properties["version"]!!

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net")
}
tasks{
    shadowJar {
        archiveClassifier.set("full")
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.10")
    implementation("org.slf4j:slf4j-api:2.0.3")
    implementation("ch.qos.logback:logback-core:1.4.4")
    implementation("ch.qos.logback:logback-classic:1.4.4")
    implementation("com.mojang:brigadier:1.0.18")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.20")
    implementation("org.apache.groovy:groovy:4.0.2")
    implementation("org.jline:jline:3.21.0")
    implementation("cn.hutool:hutool-all:5.8.11")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.22")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.alibaba.fastjson2:fastjson2:2.0.20.graal")
    implementation("net.kyori:adventure-api:4.13.1")
    implementation("net.kyori:adventure-text-serializer-gson:4.13.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("net.zhuruoling.omms.crystal.main.MainKt")
}

fun generateProperties(){
    val propertiesFile = file("./src/main/resources/build.properties")
    if (propertiesFile.exists()) {
        propertiesFile.delete()
    }
    propertiesFile.createNewFile()
    val m = mutableMapOf<String, String>()
    propertiesFile.printWriter().use {writer ->
        properties.forEach {
            val str = it.value.toString()
            if ("@" in str || "(" in str || ")" in str || "extension" in str || "null" == str || "\'" in str || "\\" in str || "/" in str)return@forEach
            if ("PROJECT" in str.toUpperCaseAsciiOnly() || "PROJECT" in it.key.toUpperCaseAsciiOnly() || " " in str)return@forEach
            if ("GRADLE" in it.key.toUpperCaseAsciiOnly() || "GRADLE" in str.toUpperCaseAsciiOnly() || "PROP" in it.key.toUpperCaseAsciiOnly())return@forEach
            if("." in it.key || "TEST" in it.key.toUpperCaseAsciiOnly())return@forEach
            m += it.key to str
        }

        m.toSortedMap().forEach{
            writer.println("${it.key} = ${it.value}")
        }
    }
}

generateProperties()