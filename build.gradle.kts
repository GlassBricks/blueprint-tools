import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version "2.0.0" apply false
    kotlin("plugin.serialization") version "2.0.0" apply false
}

repositories {
    mavenCentral()
}

subprojects {
    afterEvaluate {
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(17)
        }
    }
}
