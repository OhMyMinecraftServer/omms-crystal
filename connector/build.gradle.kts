import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
    java
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

val shadowed by configurations.creating
tasks {
    shadowJar {
        dependsOn(build)
        this.configurations = listOf(shadowed)
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
    register("copyBuiltPlugins", Copy::class.java) {
        dependsOn(shadowJar)
        from(shadowJar.get().outputs)
        into(project(":crystal").layout.projectDirectory.dir("run").dir("plugins"))
    }
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(rootProject.project("crystal"))
    runtimeOnly(kotlin("stdlib"))
    runtimeOnly(rootProject.project("crystal"))
    compileOnly("black.ninia:jep:4.2.2")
    // https://mvnrepository.com/artifact/black.ninia/jep
    shadowed("black.ninia:jep:4.2.2")
}