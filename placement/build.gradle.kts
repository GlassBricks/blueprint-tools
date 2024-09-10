plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "glassbricks.factorio"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":model"))
    api("com.google.ortools:ortools-java:9.10.4067")
    api("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("org.slf4j:slf4j-simple:2.0.3")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
}

tasks.test {
    useJUnitPlatform()
}
