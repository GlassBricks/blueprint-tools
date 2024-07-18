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
//    api("org.jgrapht:jgrapht-core:1.5.2")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    explicitApi()
}
