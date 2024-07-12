import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

plugins {
    kotlin("jvm") version "2.0.0" apply false
    kotlin("plugin.serialization") version "2.0.0" apply false
}

repositories {
    mavenCentral()
}

subprojects {
    extensions.findByName("kotlin")?.apply {
        this as KotlinProjectExtension
        jvmToolchain(21)
    }
}

group = "glassbricks.factorio"
