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
    implementation("com.google.ortools:ortools-java:9.10.4067")

    testImplementation(kotlin("test"))
    testImplementation(project(":test-util"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    explicitApi()
}
