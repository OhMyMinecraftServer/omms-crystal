import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
    java
    id("maven-publish")
    application
}


group = "icu.takeneko"
version = properties["version"]!!

java{
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net")
    maven("https://jitpack.io")
}

tasks{
    shadowJar {
        archiveClassifier.set("full")
    }
}

dependencies {
    api(kotlin("stdlib"))
    api("com.google.code.gson:gson:2.10")
    api("org.slf4j:slf4j-api:2.0.3")
    api("ch.qos.logback:logback-core:1.4.4")
    api("ch.qos.logback:logback-classic:1.4.4")
    api("com.mojang:brigadier:1.0.18")
    api("org.jline:jline:3.21.0")
    api("commons-io:commons-io:2.11.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    api("com.alibaba.fastjson2:fastjson2:2.0.20.graal")
    api("net.kyori:adventure-api:4.13.1")
    api("net.kyori:adventure-text-serializer-gson:4.13.1")
    api("nl.vv32.rcon:rcon:1.2.0")
    api("net.bytebuddy:byte-buddy-agent:1.14.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

application {
    mainClass.set("icu.takeneko.omms.crystal.main.MainKt")
}

getComponents().withType(AdhocComponentWithVariants::class.java).forEach { c ->
    c.withVariantsFromConfiguration(project.configurations.shadowRuntimeElements.get()) {
        skip()
    }
}

publishing {
    repositories {
        mavenLocal()
        maven {
            name = "NekoMavenRelease"
            url = uri("https://maven.takeneko.icu/releases")
            credentials {
                username = project.findProperty("nekomaven.user") as String? ?: System.getenv("NEKO_USERNAME")
                password = project.findProperty("nekomaven.password") as String? ?: System.getenv("NEKO_TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
        }
    }
}

tasks.register("generateProperties"){
    doLast{
        generateProperties()
    }
}

tasks.getByName("processResources") {
    dependsOn("generateProperties")
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
            if("MAVEN" in it.key) return@forEach
            if ("@" in str || "(" in str || ")" in str || "extension" in str || "null" == str || "\'" in str || "\\" in str || "/" in str)return@forEach
            if ("PROJECT" in str.uppercase() || "PROJECT" in it.key.uppercase() || " " in str)return@forEach
            if ("GRADLE" in it.key.uppercase() || "GRADLE" in str.uppercase() || "PROP" in it.key.uppercase())return@forEach
            if("." in it.key || "TEST" in it.key.uppercase())return@forEach
            m += it.key to str
        }

        m.toSortedMap().forEach{
            writer.println("${it.key} = ${it.value}")
        }
    }
}