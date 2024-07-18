plugins {
    kotlin("jvm")
}

group = "glassbricks.factorio"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    api(project(":prototypes"))
    api(project(":model"))
    api(project(":pole-optimizer"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
    api("org.jetbrains.kotlinx:kotlin-jupyter-api:0.12.0-236")
    api("org.jetbrains.kotlinx:kotlin-jupyter-lib-ext:0.12.0-236")
    api("com.github.nwillc.ksvg:ksvg:master-SNAPSHOT")
}


sourceSets {
    main {
        kotlin {
            setSrcDirs(listOf("src"))
        }
    }
}
