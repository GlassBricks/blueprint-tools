plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
}

group = "glassbricks.factorio"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":blueprint"))
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    explicitApi()
    jvmToolchain(21)
}
