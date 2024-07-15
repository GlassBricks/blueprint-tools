plugins {
    kotlin("jvm")
}

group = "glassbricks.factorio"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    api(project(":model"))
    api(project(":pole-optimizer"))
    api("org.jetbrains.kotlinx:kotlin-jupyter-lib-ext:0.12.0-236")
    api("org.apache.xmlgraphics:batik-svggen:1.17")
    api("org.apache.xmlgraphics:batik-svg-dom:1.17")
    api("com.github.nwillc.ksvg:ksvg:master-SNAPSHOT")
}


sourceSets {
    main {
        kotlin {
            setSrcDirs(listOf("src"))
        }
    }
}
