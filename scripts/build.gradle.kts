plugins {
    kotlin("jvm")
}

group = "glassbricks.factorio"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":model"))
    implementation(project(":pole-optimizer"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
