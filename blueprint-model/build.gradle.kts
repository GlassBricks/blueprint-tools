import org.jetbrains.kotlin.ir.backend.jvm.jvmLibrariesProvidedByDefault

plugins {
    kotlin("jvm")
}

group = "glassbricks.factorio"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":blueprint"))
    api(project(":prototypes"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("reflect"))
    implementation(kotlin("reflect"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
    explicitApi()
}
