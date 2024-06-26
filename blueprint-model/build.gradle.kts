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
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
    explicitApi()
}
