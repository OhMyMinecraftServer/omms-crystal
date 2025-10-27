import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
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
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(rootProject.project("crystal"))
}