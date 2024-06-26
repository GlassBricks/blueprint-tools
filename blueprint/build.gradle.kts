plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    `java-library`
}

repositories {
    mavenCentral()
}

group = "glassbricks.factorio"

fun kotlinx(module: String, version: String? = null): String =
    "org.jetbrains.kotlinx:kotlinx-$module${version?.let { ":$it" } ?: ""}"
dependencies {
    api(kotlinx("serialization-json", "1.7.0"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Xmx2G")
}
kotlin {
    explicitApi()
    jvmToolchain(17)
}
