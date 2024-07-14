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
    api(project(":prototypes"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    explicitApi()
}
