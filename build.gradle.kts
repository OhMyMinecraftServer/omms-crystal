import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.Instant
import java.util.Properties

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
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

tasks {
    shadowJar {
        archiveClassifier = "full"
    }
    test {
        useJUnitPlatform()
    }
    compileJava {
        targetCompatibility = "17"
    }
    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
    register("generateProperties") {
        doLast {
            generateProperties()
        }
    }
    processResources {
        dependsOn("generateProperties")
    }
}

dependencies {
    api(kotlin("stdlib"))
    api("com.google.code.gson:gson:2.13.1")
    api("org.slf4j:slf4j-api:2.0.17")
    api("ch.qos.logback:logback-core:1.5.18")
    api("ch.qos.logback:logback-classic:1.5.18")
    api("com.mojang:brigadier:1.0.18")
    api("org.jline:jline:3.30.4")
    api("commons-io:commons-io:2.20.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    api("net.kyori:adventure-api:4.23.0")
    api("net.kyori:adventure-text-serializer-gson:4.23.0")
    api("nl.vv32.rcon:rcon:1.2.0")
    api("net.bytebuddy:byte-buddy-agent:1.17.6")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation(kotlin("test"))
}

application {
    mainClass = "icu.takeneko.omms.crystal.main.MainKt"
}

getComponents().withType(AdhocComponentWithVariants::class.java).forEach { c ->
    c.withVariantsFromConfiguration(project.configurations.shadowRuntimeElements.get()) {
        skip()
    }
}

publishing {
    repositories {
        mavenLocal()
        maven("https://maven.takeneko.icu/releases") {
            name = "NekoMavenRelease"
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

fun generateProperties() {
    val propertiesFile = file("./src/main/resources/build.properties")

    propertiesFile.delete()
    propertiesFile.createNewFile()

    val shouldSkip: (Pair<String, *>) -> Boolean = { (k, v) ->
        val s = v.toString()
        "MAVEN" in k ||
                "@" in s || "(" in s || ")" in s || "extension" in s || s == "null" ||
                '\'' in s || '\\' in s || '/' in s ||
                "PROJECT" in s.uppercase() || "PROJECT" in k.uppercase() || ' ' in s ||
                "GRADLE" in k.uppercase() || "GRADLE" in s.uppercase() || "PROP" in k.uppercase() ||
                '.' in k || "TEST" in k.uppercase()
    }

    val props = Properties().apply {
        properties.filterKeys { !shouldSkip(it to properties[it]) }
            .toSortedMap()
            .forEach { (k, v) -> setProperty(k, v.toString()) }
    }

    propertiesFile.outputStream().use { out ->
        props.store(out, "Auto-generated at ${Instant.now()}")
    }
}