import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
    java
    application
    alias(libs.plugins.shadow)
}

base {
    group = "icu.takeneko"
    version = properties["version"]!!
}

//shadowJar {
//    archiveClassifier = "full"
//}

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
    compileOnly(kotlin("stdlib"))
    compileOnly(rootProject.project("crystal"))
    runtimeOnly(kotlin("stdlib"))
    runtimeOnly(rootProject.project("crystal"))
    // https://mvnrepository.com/artifact/black.ninia/jep
    implementation("black.ninia:jep:4.2.2")
}