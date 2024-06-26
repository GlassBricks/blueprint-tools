plugins {
    kotlin("jvm") version "2.0.0"
}

repositories {
    mavenCentral()
}

group = "glassbricks.factorio"

dependencies {
    implementation(project(":prototypes"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
