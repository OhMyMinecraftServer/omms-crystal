import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.Instant
import java.util.Properties

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
    java
    application
}

base {
    group = "icu.takeneko"
    version = properties["version"]!!
}

java {
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
    detektPlugins(libs.detekt.formatting)

    api(kotlin("stdlib"))
    api(libs.gson)
    api(libs.slf4j.api)
    api(libs.bundles.logback)
    api(libs.brigadier)
    api(libs.jline)
    api(libs.commons.io)
    api(libs.bundles.adventure)
    api(libs.rcon)
    api(libs.byte.buddy.agent)
    api(libs.bundles.kotlinx.coroutines)
    api(libs.kotlin.serialization.json) {
        isTransitive = false
    }
    api(libs.kaml)

    testImplementation(kotlin("test"))
}

application {
    mainClass = "icu.takeneko.omms.crystal.main.MainKt"
}

components.withType<AdhocComponentWithVariants>().forEach { component ->
    component.withVariantsFromConfiguration(project.configurations.shadowRuntimeElements.get()) {
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