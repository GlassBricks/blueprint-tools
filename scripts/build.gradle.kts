plugins {
    kotlin("jvm")
    application
    id("com.gradleup.shadow") version "8.3.0"
}

group = "glassbricks.factorio"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    api(project(":prototypes"))
    api(project(":model"))
    api(project(":placement"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
    api("org.jetbrains.kotlinx:kotlin-jupyter-api:0.12.0-236")
    api("org.jetbrains.kotlinx:kotlin-jupyter-lib-ext:0.12.0-236")
    api("com.github.nwillc.ksvg:ksvg:master-SNAPSHOT")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.0")
}


sourceSets {
    main {
        kotlin {
            setSrcDirs(listOf("src"))
        }
    }
    test {
        kotlin {
            setSrcDirs(listOf("test"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass = "scripts.OptimizeKt"
}
